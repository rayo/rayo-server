package com.tropo.server.verb;

import com.tropo.core.verb.PauseCommand;
import com.tropo.core.verb.ResumeCommand;
import com.tropo.core.verb.Say;
import com.tropo.core.verb.SayCompleteEvent;
import com.tropo.core.verb.SayCompleteEvent.Reason;
import com.tropo.core.verb.VerbCommand;
import com.voxeo.moho.State;
import com.voxeo.moho.event.OutputCompleteEvent;
import com.voxeo.moho.media.Output;
import com.voxeo.moho.media.output.AudibleResource;
import com.voxeo.moho.media.output.OutputCommand;

public class SayHandler extends AbstractLocalVerbHandler<Say> {

    private Output output;

    // Verb Lifecycle
    // ================================================================================

    @Override
    public void start() {

        AudibleResource audibleResource = resolveAudio(model.getPrompt());
        OutputCommand outcommand = new OutputCommand(audibleResource);
        outcommand.setBargein(false);
        outcommand.setVoiceName(model.getVoice());
        
        output = media.output(outcommand);
        
    }

    // Commands
    // ================================================================================

    public void stop(boolean hangup) {
        if(hangup) {
            complete(new SayCompleteEvent(model, Reason.HANGUP));
        }
        else {
            output.stop();
        }
    }

    @Override
    public void onCommand(VerbCommand command) {
        if (command instanceof PauseCommand) {
            pause();
        } else if (command instanceof ResumeCommand) {
            resume();
        }
    }

    @State
    public void pause() {
        output.pause();
    }

    @State
    public void resume() {
        output.resume();
    }

    // Moho Events
    // ================================================================================

    @State
    public void onSpeakComplete(OutputCompleteEvent event) {
        switch(event.getCause()) {
        case BARGEIN:
        case END:
            complete(new SayCompleteEvent(model, Reason.SUCCESS));
            break;
        case DISCONNECT:
            complete(new SayCompleteEvent(model, Reason.HANGUP));
            break;
        case CANCEL:
            complete(new SayCompleteEvent(model, Reason.STOP));
            break;
        case ERROR:
        case UNKNOWN:
            complete(new SayCompleteEvent(model, Reason.ERROR));
            break;
        case TIMEOUT:
            complete(new SayCompleteEvent(model, Reason.TIMEOUT));
            break;
        }
    }

}
