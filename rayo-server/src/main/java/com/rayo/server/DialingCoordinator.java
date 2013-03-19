package com.rayo.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import com.rayo.core.AnsweredEvent;
import com.rayo.core.EndCommand;
import com.rayo.core.EndEvent;
import com.rayo.core.EndEvent.Reason;
import com.rayo.core.JoinCommand;
import com.rayo.core.JoinDestinationType;
import com.rayo.core.JoiningEvent;
import com.voxeo.logging.Loggerf;
import com.voxeo.moho.Call;
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
	
	enum Status {
		PENDING,
		DONE
	}
		
	class DialingStatus {
				
		ReentrantLock lock = new ReentrantLock();
		Status status = Status.PENDING;
		List<CallActor<?>> interestedParties = new ArrayList<CallActor<?>>();
		CallActor<?> targetActor;
	}
	
	public void prepare(String callId) {
		
		dials.put(callId, new DialingStatus());
	}
	
	public void dial(final CallActor<?> sourceCallActor, final CallActor<?> targetCallActor) {
		
		DialingStatus dialingStatus = dials.get(sourceCallActor.getCall().getId());
		dialingStatus.lock.lock();
		try {
			if (dialingStatus.status == Status.PENDING) {
				dialingStatus.interestedParties.add(targetCallActor);
				dials.put(targetCallActor.getCall().getId(), dialingStatus);
				
				final Call call = sourceCallActor.getCall();
		    	// Announce joining
		        sourceCallActor.fire(new JoiningEvent(call.getId(), targetCallActor.getParticipantId()));
		    
		        // WARNING - NOT 'ACTOR THREAD'
		        // This even handler will fire on the caller's thread.
		        targetCallActor.addEventHandler(new EventHandler() {
		            @Override
		            public void handle(Object event) throws Exception {
		                if(event instanceof AnsweredEvent) {
		                    onAnswered(sourceCallActor, targetCallActor);
		                } else if(event instanceof EndEvent) {   		                	
		                	DialingStatus status = dials.get(targetCallActor.getCall().getId());
		                	if (status != null) {
		                		if (status.targetActor == targetCallActor) {
									logger.debug("Received end event on call leg [%s]. Theoretically we should now be hanging up call leg [%s].", 
											targetCallActor.getParticipantId(), call.getId());							
					                	sourceCallActor.publish(new EndCommand(call.getId(), Reason.HANGUP));
					                // Cleanup
					                for(CallActor<?> target: status.interestedParties) {
					                	dials.remove(target.getParticipantId());
					                }
					                dials.remove(targetCallActor.getParticipantId());
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
	
	private void onAnswered(CallActor<?> sourceCallActor, CallActor<?> targetCallActor) {
		
		DialingStatus dialingStatus = dials.get(sourceCallActor.getCall().getId());
		dialingStatus.lock.lock();
		try {
			logger.debug("Received answered event on call leg [%s].", targetCallActor.getCall().getId());
			logger.debug("Joining on BRIDGE_EXCLUSIVE mode call legs [%s] and [%s].", 
					sourceCallActor.getCall().getId(), targetCallActor.getCall().getId());
			
		    JoinCommand join = new JoinCommand();
		    join.setTo(targetCallActor.getCall().getId());
		    join.setType(JoinDestinationType.CALL);
		    join.setMedia(JoinType.BRIDGE_EXCLUSIVE);
		    // Join to the B Leg
		    sourceCallActor.publish(join);
        	
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
}
