package com.tropo.server.verb;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import javax.media.mscontrol.join.Joinable.Direction;

import com.tropo.core.verb.Ssml;
import com.tropo.core.verb.Transfer;
import com.tropo.core.verb.TransferCompleteEvent;
import com.tropo.core.verb.TransferCompleteEvent.Reason;
import com.tropo.core.verb.VerbCompleteEvent;
import com.tropo.core.verb.VerbCompleteReason;
import com.tropo.server.ActorEventListener;
import com.voxeo.logging.Loggerf;
import com.voxeo.moho.Call;
import com.voxeo.moho.CallableEndpoint;
import com.voxeo.moho.Endpoint;
import com.voxeo.moho.Joint;
import com.voxeo.moho.Participant;
import com.voxeo.moho.RedirectException;
import com.voxeo.moho.State;
import com.voxeo.moho.event.HangupEvent;
import com.voxeo.moho.event.InputCompleteEvent;
import com.voxeo.moho.event.JoinCompleteEvent;
import com.voxeo.moho.event.Observer;
import com.voxeo.moho.media.Input;
import com.voxeo.moho.media.Prompt;
import com.voxeo.moho.media.input.DigitInputCommand;
import com.voxeo.moho.media.input.InputCommand;
import com.voxeo.moho.media.output.OutputCommand;
import com.voxeo.moho.media.output.OutputCommand.BargeinType;

public class TransferHandler extends AbstractLocalVerbHandler<Transfer, Call> implements Observer {

    private static final Loggerf log = Loggerf.getLogger(TransferHandler.class);

    private boolean running;

    private Input<Participant> input;
    private Prompt<Participant> ringBack;
    private Timer timer = new Timer();
    private Map<Call, Joint> joints = new HashMap<Call, Joint>();

    private Call peer;

    private boolean isRunning() {
        return running;
    }

    @Override
    public void start() {

        Ssml ringbackSsml = model.getRingbackTone();
        if(ringbackSsml != null) {
            OutputCommand outputCommand = output(ringbackSsml);
            outputCommand.setBargeinType(BargeinType.NONE);
            outputCommand.setVoiceName(ringbackSsml.getVoice());
            ringBack = getMediaService().prompt(outputCommand, null, 30);
        }

        running = true;

        // Timeout Timer
        timer.schedule(new TimerTask() {
            public void run() {
                if (isRunning()) {
                    complete(Reason.TIMEOUT);
                }
            }
        }, model.getTimeout().getMillis());

        for (URI destination : model.getTo()) {
            Endpoint endpoint = participant.getApplicationContext().createEndpoint(destination.toString());
            if (endpoint instanceof CallableEndpoint) {
                dial((CallableEndpoint) endpoint);
            }
        }

        Character terminator = model.getTerminator();
        if(terminator != null) {
            InputCommand inputCommand = new DigitInputCommand(terminator);
            input = getMediaService().input(inputCommand);
        }

    }

    // Commands
    // ================================================================================

    @Override
    public synchronized void stop(boolean hangup) {
        if(hangup) {
            complete(VerbCompleteEvent.Reason.HANGUP);
        }
        else {
            complete(VerbCompleteEvent.Reason.STOP);
        }
    }

    private void stopDialing() {
        
        // Stop Ringing
        if (ringBack != null) {
            ringBack.getOutput().stop();
        }

        // Stop Terminator
        if (input != null) {
            input.stop();
        }

        // Stop Timer
        if(timer != null) {
            timer.cancel();
        }

        // Cancel Active Calls.
        Set<Call> calls = joints.keySet();
        for (Call call : calls) {
            Joint joint = joints.get(call);
            joint.cancel(true);
            call.disconnect();
        }
        
        joints.clear();
        
    }

    // Moho Events
    // ================================================================================

    @State
    public synchronized void onDisconnect(HangupEvent event) {
        if(event.getSource() == peer) {
            complete(Reason.SUCCESS);
        }
    }
    
    @State
    public synchronized void onJoinComplete(JoinCompleteEvent event) {
        
        if (event.getSource() != participant) {

            // Make sure we're running or still interested in joining someone
            if(!isRunning() || peer != null) {
                log.info("Received JoinCompleteEvent but Transfer verb is either already joined or no longer running");
                return;
            }
            
            // Remove the candidate from the list
            joints.remove(event.getSource());
            
            switch (event.getCause()) {
            case JOINED:
                peer = (Call)event.getSource();
                participant.join(peer, resolveJoinType(), Direction.DUPLEX);
                stopDialing();
                break;
            case TIMEOUT:
                if (joints.size() == 0) {
                    complete(Reason.TIMEOUT);
                }
                break;
            case DISCONNECTED:
                if (joints.size() == 0) {
                    complete(VerbCompleteEvent.Reason.HANGUP);
                }
                break;
            case BUSY:
                if (joints.size() == 0) {
                    complete(Reason.BUSY);
                }
                break;
            case REJECT:
                if (joints.size() == 0) {
                    complete(Reason.REJECT);
                }
                break;
            case REDIRECT:
                // Determine the redirect target and spin up a new candidate
                List<String> redirectTargets = ((RedirectException) event.getException()).getTargets();
                for(String redirectTarget : redirectTargets) {
                    CallableEndpoint to = (CallableEndpoint) participant.getApplicationContext().createEndpoint(redirectTarget);
                    dial(to);
                }
                break;
            case ERROR:
                log.error("Error transfering call", event.getException());
                complete(VerbCompleteEvent.Reason.ERROR);
            default:
                log.error("Unhandled join cause [cause=%s]", event.getCause());
                complete(VerbCompleteEvent.Reason.ERROR);
            }
        }
    }

    @State
    public synchronized void onTermChar(InputCompleteEvent<Participant> event) {
        if (event.getSource() == participant) {
            switch (event.getCause()) {
            case MATCH:
                if (isRunning()) {
                    complete(Reason.TERMINATOR);
                }
                break;
            }
        }
    }

    // Utility
    // ================================================================================

    private void complete(VerbCompleteReason reason) {
        
        running = false;

        TransferCompleteEvent event = new TransferCompleteEvent(model, reason);

        // Stop active dialing
        stopDialing();
        
        // Join back to the media server
        if(peer != null) {
            peer.unjoin(participant);
        }

        complete(event);
    }

    private void dial(CallableEndpoint to) {
        Call destination = to.call(resolveFrom(), model.getHeaders());
        destination.addObserver(new ActorEventListener(actor));
        Joint joint = destination.join();
        joints.put(destination, joint);
    }

    private Endpoint resolveFrom() {
        Endpoint fromEndpoint = null;
        URI from = model.getFrom();
        if (from != null) {
        	participant.getApplicationContext().createEndpoint(from.toString());
        }
        else {
            fromEndpoint = participant.getAddress();
        }
        return fromEndpoint;
    }
    
    private Participant.JoinType resolveJoinType() {
    	
    	switch(model.getMedia()) {
    		case BRIDGE : return Participant.JoinType.BRIDGE;
    		case DIRECT : return Participant.JoinType.DIRECT;
    	}
    	return null;
    }

}
