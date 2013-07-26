package com.rayo.server;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import javax.media.mscontrol.join.Joinable.Direction;

import com.rayo.core.AnsweredEvent;
import com.rayo.core.EndCommand;
import com.rayo.core.EndEvent;
import com.rayo.core.EndEvent.Reason;
import com.rayo.core.JoinCommand;
import com.rayo.core.JoinDestinationType;
import com.rayo.core.JoiningEvent;
import com.voxeo.logging.Loggerf;
import com.voxeo.moho.Call;
import com.voxeo.moho.Call.State;
import com.voxeo.moho.Mixer;
import com.voxeo.moho.Participant.JoinType;
import com.voxeo.moho.common.event.AutowiredEventListener;

/**
 * <p>This class is in charge of coordinating the dialing operation when one of the legs requests to be joined 
 * to one of multiple destinations. In such scenario this class acts as a coordinator taking care of all the 
 * concurrency requirements behind the multiple dials.</p> 
 * 
 * @author martin
 *
 */
public class DialingCoordinator {
	
	private static final Loggerf logger = Loggerf.getLogger(DialingCoordinator.class);
	
	private Map<String, DialingStatus> dials = new ConcurrentHashMap<String, DialingStatus>();
	
	enum Status {
		PENDING,
		DONE,
		END
	}
		
	class DialingStatus {
				
		ReentrantLock lock = new ReentrantLock();
		Status status = Status.PENDING;
		List<CallActor<?>> interestedParties = new ArrayList<CallActor<?>>();
		CallActor<?> targetActor;
	}
	
	public String prepareRinglist(String callId) {
		
		String ringlistId = UUID.randomUUID().toString();
		dials.put(ringlistId, new DialingStatus());
		
		return ringlistId;
	}
	
	public void directDial(final CallActor<?> sourceCallActor, 
					  	   List<URI> destinations,
					       Map<String,String> headers,
					  	   final String ringlistId) {
		
		Call call = sourceCallActor.getCall();
        URI from = call.getInvitor().getURI();
		
        List<Call> calls = new ArrayList<Call>();
        for(URI destination : destinations) {        
        	final CallActor<?> targetCallActor = 
        		sourceCallActor.getCallManager().
        		createCallActor(destination, from, headers, call);
        	targetCallActor.participant.addObserver(new ActorEventListener(targetCallActor));
        	targetCallActor.mohoListeners.add(new AutowiredEventListener(targetCallActor));
        	prepareDial(sourceCallActor, targetCallActor, ringlistId, false);
        	calls.add(targetCallActor.getCall());
        }

        // Start dialing
        logger.debug("Joining call to multiple participants in Direct mode.", call.getId());
        call.join(JoinType.DIRECT, true, Direction.DUPLEX, true, calls.toArray(new Call[]{}));
        
        for(int i=0;i<calls.size();i++) {
        	sourceCallActor.getCallStatistics().outgoingCall();
        }
	}
	
	public void bridgeDial(final CallActor<?> sourceCallActor, 
					  	   List<URI> destinations,
					       Map<String,String> headers,
					  	   final String ringlistId) {
		
		Call call = sourceCallActor.getCall();
        URI from = call.getInvitor().getURI();
		
        for(URI destination : destinations) {        
        	// The null parameter below instructs Moho that this is an OOB call
        	// and not a continuation from an existing call. 
        	final CallActor<?> targetCallActor = 
        		sourceCallActor.getCallManager()
        		.createCallActor(destination, from, headers, null);
        	prepareDial(sourceCallActor, targetCallActor, ringlistId, true);
        }                  	
	}

	private void linkActor(final CallActor<?> sourceCallActor,
			final CallActor<?> targetCallActor, final Call call) {
		
		// Hang up the peer call when this actor is destroyed
		sourceCallActor.link(new ActorLink() {
		    @Override
		    public void postStop() {
		        targetCallActor.publish(new EndCommand(call.getId(), Reason.HANGUP));
		    }
		});
	}

	private void fireJoiningEvent(final CallActor<?> sourceCallActor,
			final CallActor<?> targetCallActor, final Call call) {
		
		// Announce joining
		String peerAddress = targetCallActor.getCall().getAddress().getURI().toString(); 
		sourceCallActor.fire(new JoiningEvent(call.getId(), 
				targetCallActor.getParticipantId(),
				peerAddress));
	}
	
	
	public void prepareDial(final CallActor<?> sourceCallActor, 
					   		final CallActor<?> targetCallActor, 
					   		final String ringlistId,
					   		final boolean bridge) {
		
		DialingStatus dialingStatus = dials.get(ringlistId);
		dialingStatus.lock.lock();
		try {
			if (dialingStatus.status == Status.PENDING) {
				dialingStatus.interestedParties.add(targetCallActor);
				dials.put(ringlistId ,  dialingStatus);
				
				final Call call = sourceCallActor.getCall();
		    	fireJoiningEvent(sourceCallActor, targetCallActor, call);
		    
		        // WARNING - NOT 'ACTOR THREAD'
		        // This even handler will fire on the caller's thread.
		        targetCallActor.addEventHandler(new EventHandler() {
		            @Override
		            public void handle(Object event) throws Exception {
		                if(event instanceof AnsweredEvent) {
		                	if (bridge) {
		                		onBridgeAnswered(sourceCallActor, targetCallActor, ringlistId);
		                	} else {
		                		onDirectAnswered(sourceCallActor, targetCallActor, ringlistId);
		                	}
		                } else if(event instanceof EndEvent) {   		                	
		                	handleEndEvent(sourceCallActor, targetCallActor, ringlistId, call);		                	
		                }
		            }
		        });
		
		        linkActor(sourceCallActor, targetCallActor, call);
		        if (bridge) {
			        // Start dialing
			        targetCallActor.publish(targetCallActor.getCall());
		        }
			}
		} finally {
			dialingStatus.lock.unlock();
		}
	}
	
	public boolean isCoordinating(String callId) {
		
		return dials.get(callId) != null;
	}
	
	private void onDirectAnswered(CallActor<?> sourceCallActor, 
							 	  CallActor<?> targetCallActor,
							 	  String ringlistId) {
		
		DialingStatus dialingStatus = dials.get(ringlistId);
		dialingStatus.lock.lock();
		try {
			logger.debug("Received answered event on call leg [%s].", targetCallActor.getCall().getId());
			
		    //TODO: MOHO-61. Hack!!
		    JoinCommand join = new JoinCommand();
		    targetCallActor.setJoinGroup(join.getJoinGroup());
		    sourceCallActor.setJoinGroup(join.getJoinGroup());
			
			dialingStatus.status = Status.DONE;
			dialingStatus.targetActor = targetCallActor;
				
			// End all other legs.
			logger.debug("Ending non answered call legs on ringlist.");
			for(CallActor<?> it: dialingStatus.interestedParties) {
				if (!it.getCall().getId().equals(targetCallActor.getCall().getId())) {
					logger.debug("Hanging up unanswered call leg [%s].", it.getCall().getId());
					it.publish(new EndCommand(it.getCall().getId(), Reason.HANGUP));
				}
			}
		} finally {
			dialingStatus.lock.unlock();
		}
	}
	
	
	private void onBridgeAnswered(CallActor<?> sourceCallActor, 
							 	  CallActor<?> targetCallActor,
							 	  String ringlistId) {
		
		DialingStatus dialingStatus = dials.get(ringlistId);
		dialingStatus.lock.lock();
		try {
			logger.debug("Received answered event on call leg [%s].", targetCallActor.getCall().getId());
			
			if (sourceCallActor.getCall().getCallState() == State.CONNECTED) {
				
				logger.debug("Received a post-connection phase connect request.");
				// call was already connected. Try to find the mixer first.
				String mixerName = "mixer-" + sourceCallActor.getCall().getId();
				Mixer mixer = sourceCallActor.getMixerManager().getMixer(mixerName);
				if (mixer == null) {
					logger.debug("Mixer [%s] not found. Moving calls into a conference.", mixerName);
					// no mixer yet. We have to create the conference and join the three participants
					mixer = sourceCallActor.getMixerManager().create(
						sourceCallActor.getCall().getApplicationContext(), mixerName, 1, false);

					String peerId = sourceCallActor.getCall().getParticipants()[0].getId();
					CallActor<?> peer = sourceCallActor.getCallManager().getCallRegistry().get(peerId);

					joinActorToMixer(targetCallActor, mixerName);
					
					// As per Willie's instructions, unjoin needs to be sync
					try {
						sourceCallActor.getCall().unjoin(peer.getCall()).get(2000, TimeUnit.MILLISECONDS);
					} catch (Exception e) {
						// TODO: fail call?
						logger.error(e.getMessage(),e);
					}

					joinActorToMixer(sourceCallActor, mixerName);					
					joinActorToMixer(peer, mixerName);
					
				} else {
					joinActorToMixer(targetCallActor, mixerName);
				}								
			}
			
			dialingStatus.status = Status.DONE;
			dialingStatus.targetActor = targetCallActor;
				
			// End all other legs.
			logger.debug("Ending non answered call legs on ringlist.");
			for(CallActor<?> it: dialingStatus.interestedParties) {
				if (!it.getCall().getId().equals(targetCallActor.getCall().getId())) {
					logger.debug("Hanging up unanswered call leg [%s].", it.getCall().getId());
					it.publish(new EndCommand(it.getCall().getId(), Reason.HANGUP));
				}
			}
		} finally {
			dialingStatus.lock.unlock();
		}
	}

	private void joinActorToMixer(CallActor<?> targetCallActor, String mixerName) {
		
		// join the new participant to the existing conference
		logger.debug("Joining a new participant [%s] to conference [%s]", 
			targetCallActor.getCall().getId(), mixerName);
		JoinCommand join = new JoinCommand();
		join.setTo(mixerName);
		join.setType(JoinDestinationType.MIXER);		
		join.setMedia(JoinType.BRIDGE_SHARED);
		join.setForce(true);
		// Join to the B Leg
		targetCallActor.publish(join);
	}

	private void handleEndEvent(final CallActor<?> sourceCallActor,
			final CallActor<?> targetCallActor, final String ringlistId,
			final Call call) {
		
		DialingStatus status = dials.get(ringlistId);
		if (status != null) {
			status.interestedParties.remove(targetCallActor);
			if (status.interestedParties.size() == 0 || 
				status.targetActor == targetCallActor) {		                		
				String mixerName = "mixer-" + sourceCallActor.getCall().getId();
				Mixer mixer = sourceCallActor.getMixerManager().getMixer(mixerName);
				if (mixer == null && sourceCallActor.getCall().getParticipants().length == 0) {
					if (status.targetActor == null) {
						// No one took the call
		            	sourceCallActor.publish(new EndCommand(call.getId(), Reason.REJECT));
					} else {
						// Everyone hung up
		            	sourceCallActor.publish(new EndCommand(call.getId(), Reason.HANGUP));										
					}
				}
				
		        // Cleanup
		        status.interestedParties.clear();
		        status.status = Status.END;
		        dials.remove(ringlistId);
			}
		}
	}
}
