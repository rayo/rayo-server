package com.tropo.server.verb;

import javax.media.mscontrol.join.Joinable.Direction;

import com.tropo.core.verb.Join;
import com.tropo.core.verb.JoinCompleteEvent;
import com.tropo.core.verb.JoinCompleteEvent.Reason;
import com.tropo.core.verb.VerbCompleteEvent;
import com.tropo.core.verb.VerbCompleteReason;
import com.voxeo.logging.Loggerf;
import com.voxeo.moho.Call;
import com.voxeo.moho.State;

public class JoinHandler extends AbstractLocalVerbHandler<Join> {
	
	private static final Loggerf log = Loggerf.getLogger(JoinHandler.class);
	
	@Override
	public void start() {
		
		if (model.getDirection() == null) {
			if (model.getType() == null) {
				this.call.join();
			} else {

			}
		} else {
			if (model.getType() ==  null) {
				this.call.join(Direction.valueOf(model.getDirection()));
			} else {
				
			}
		}
	}
	
	@Override
	public void setCall(Call call) {

        this.call = call;
	}
	
	@Override
	public void stop(boolean hangup) {
		
	}
	

    @State
    public synchronized void onJoinComplete(com.voxeo.moho.event.JoinCompleteEvent event) {
    	
    	System.out.println("Join completed!");
        
        switch (event.getCause()) {
        	case JOINED:
        		// TODO: Need to handle it?
        		break;
            case TIMEOUT:
                complete(Reason.TIMEOUT);
                break;
            case DISCONNECTED:
            	complete(VerbCompleteEvent.Reason.HANGUP);
                break;
            case BUSY:
                complete(Reason.BUSY);
                break;
            case REJECT:
            	complete(Reason.REJECT);
                break;
            case REDIRECT:
            	// TODO: Need to handle it?
                break;
            case ERROR:
                log.error("Error joining call", event.getException());
                complete(VerbCompleteEvent.Reason.ERROR);
            default:
                log.error("Unhandled join cause [cause=%s]", event.getCause());
                complete(VerbCompleteEvent.Reason.ERROR);
        }
    }  
    
    private void complete(VerbCompleteReason reason) {
        
        JoinCompleteEvent event = new JoinCompleteEvent(model, reason);

        // TODO: Do we need to stop active dialing in some cases like in Transfer?
        //stopDialing(); 
        
        complete(event);
    }
}
