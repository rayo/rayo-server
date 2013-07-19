package com.rayo.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import com.rayo.core.AnsweredEvent;
import com.rayo.core.EndCommand;
import com.rayo.core.EndEvent;
import com.rayo.core.UnjoinCommand;
import com.rayo.core.EndEvent.Reason;
import com.rayo.core.JoinCommand;
import com.rayo.core.JoinDestinationType;
import com.rayo.core.JoiningEvent;
import com.voxeo.logging.Loggerf;
import com.voxeo.moho.Call;
import com.voxeo.moho.Call.State;
import com.voxeo.moho.Mixer;
import com.voxeo.moho.Participant.JoinType;

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
	
	private JoinType joinType = JoinType.DIRECT;
	
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
	
	public void dial(final CallActor<?> sourceCallActor, 
					  final CallActor<?> targetCallActor, 
					  final String ringlistId) {
		
		DialingStatus dialingStatus = dials.get(ringlistId);
		dialingStatus.lock.lock();
		try {
			if (dialingStatus.status == Status.PENDING) {
				dialingStatus.interestedParties.add(targetCallActor);
				dials.put(ringlistId ,  dialingStatus);
				
				final Call call = sourceCallActor.getCall();
		    	// Announce joining
				String peerAddress = targetCallActor.getCall().getAddress().getURI().toString(); 
				if (peerAddress.indexOf(";") != -1) {
					peerAddress = peerAddress.substring(0, peerAddress.indexOf(";"));
				}
		        sourceCallActor.fire(new JoiningEvent(call.getId(), 
		        		targetCallActor.getParticipantId(),
		        		peerAddress));
		    
		        // WARNING - NOT 'ACTOR THREAD'
		        // This even handler will fire on the caller's thread.
		        targetCallActor.addEventHandler(new EventHandler() {
		            @Override
		            public void handle(Object event) throws Exception {
		                if(event instanceof AnsweredEvent) {
		                    onAnswered(sourceCallActor, targetCallActor, ringlistId);
		                } else if(event instanceof EndEvent) {   		                	
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
		        });
		
		        // Hang up the peer call when this actor is destroyed
		        sourceCallActor.link(new ActorLink() {
		            @Override
		            public void postStop() {
		                targetCallActor.publish(new EndCommand(call.getId(), Reason.HANGUP));
		            }
		        });
		    
		        // Start dialing
		        targetCallActor.publish(targetCallActor.getCall());
			}
		} finally {
			dialingStatus.lock.unlock();
		}
	}
	
	public boolean isCoordinating(String callId) {
		
		return dials.get(callId) != null;
	}
	
	private void onAnswered(CallActor<?> sourceCallActor, 
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
					joinActorToMixer(targetCallActor, mixerName);
					joinActorToMixer(sourceCallActor, mixerName);
					String peerId = sourceCallActor.getCall().getParticipants()[0].getId();
					CallActor<?> peer = sourceCallActor.getCallManager().getCallRegistry().get(peerId);
					joinActorToMixer(peer, mixerName);
					
					// Unjoin original peer
					UnjoinCommand unjoin = new UnjoinCommand();
					unjoin.setFrom(peerId);
					unjoin.setType(JoinDestinationType.CALL);
					sourceCallActor.publish(unjoin);
					
				} else {
					joinActorToMixer(targetCallActor, mixerName);
				}				
			} else {
				logger.debug("Joining on %s mode call legs [%s] and [%s].", 
					joinType, sourceCallActor.getCall().getId(), 
					targetCallActor.getCall().getId());
			
			    JoinCommand join = new JoinCommand();
			    join.setTo(targetCallActor.getCall().getId());
			    join.setType(JoinDestinationType.CALL);
			    join.setMedia(joinType);
			    
			    //TODO: MOHO-60. Hack!!
			    targetCallActor.setJoinGroup(join.getJoinGroup());
			    sourceCallActor.setJoinGroup(join.getJoinGroup());
			    
			    // Join to the B Leg
			    sourceCallActor.publish(join);
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

	public void setDialingMode(String dialingMode) {

		try {
			joinType = JoinType.valueOf(dialingMode);
		} catch (Exception e) {
			logger.error("Could not parse dialing mode %s. Would default to DIRECT.", dialingMode);
		}
	}
}
