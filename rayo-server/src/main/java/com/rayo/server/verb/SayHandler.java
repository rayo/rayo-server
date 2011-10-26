package com.rayo.server.verb;

import javax.validation.ConstraintValidatorContext;

import com.rayo.server.exception.ExceptionMapper;
import com.rayo.server.validation.SsmlValidator;
import com.rayo.core.validation.ValidationException;
import com.rayo.core.verb.PauseCommand;
import com.rayo.core.verb.ResumeCommand;
import com.rayo.core.verb.Say;
import com.rayo.core.verb.SayCompleteEvent;
import com.rayo.core.verb.SayCompleteEvent.Reason;
import com.rayo.core.verb.Ssml;
import com.rayo.core.verb.VerbCommand;
import com.rayo.core.verb.VerbCompleteEvent;
import com.voxeo.logging.Loggerf;
import com.voxeo.moho.Participant;
import com.voxeo.moho.State;
import com.voxeo.moho.event.OutputCompleteEvent;
import com.voxeo.moho.media.Output;
import com.voxeo.moho.media.output.AudibleResource;
import com.voxeo.moho.media.output.OutputCommand;
import com.voxeo.moho.media.output.OutputCommand.BargeinType;
import com.voxeo.servlet.xmpp.StanzaError;

public class SayHandler extends AbstractLocalVerbHandler<Say, Participant> {

    private Output<Participant> output;
    private SsmlValidator ssmlValidator;

    private static final Loggerf logger = Loggerf.getLogger(SayHandler.class);
    
    // Verb Lifecycle
    // ================================================================================

    @Override
    public void start() {

        Ssml prompt = model.getPrompt();
        AudibleResource audibleResource = resolveAudio(prompt);
        OutputCommand outcommand = new OutputCommand(audibleResource);
        outcommand.setBargeinType(BargeinType.NONE);
        outcommand.setVoiceName(prompt.getVoice());
        
        output = getMediaService().output(outcommand);
        
    }

    @Override
    public boolean isStateValid(ConstraintValidatorContext context) {

        if (isOnConference(participant)) {
        	context.buildConstraintViolationWithTemplate(
        			"Call is joined to a conference.")
        			.addNode(ExceptionMapper.toString(StanzaError.Condition.RESOURCE_CONSTRAINT))
        			.addConstraintViolation();
        	return false;
        }
        if (isOnHold(participant)) {
        	context.buildConstraintViolationWithTemplate(
				"Call is currently on hold.")
				.addNode(ExceptionMapper.toString(StanzaError.Condition.RESOURCE_CONSTRAINT))
				.addConstraintViolation();
        	return false;        	
        }
        return true;
    }
    
    // Commands
    // ================================================================================

    public void stop(boolean hangup) {
        if(hangup) {
            complete(new SayCompleteEvent(model, VerbCompleteEvent.Reason.HANGUP));
        }
        else {
        	if (output != null) {
        		output.stop();
        	}
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
    public void onSpeakComplete(OutputCompleteEvent<Participant> event) {
    	
    	if (event.getMediaOperation() != null && !event.getMediaOperation().equals(this)) {
    		logger.debug("Ignoring complete event as it is targeted to a different media operation");
    		return;
    	}

        switch(event.getCause()) {
        case BARGEIN:
        case END:
            complete(new SayCompleteEvent(model, Reason.SUCCESS));
            break;
        case DISCONNECT:
            complete(new SayCompleteEvent(model, VerbCompleteEvent.Reason.HANGUP));
            break;
        case CANCEL:
            complete(new SayCompleteEvent(model, VerbCompleteEvent.Reason.STOP));
            break;
        case ERROR:
        case UNKNOWN:
        	complete(new SayCompleteEvent(model, VerbCompleteEvent.Reason.ERROR, findErrorCause(event)));
        case TIMEOUT:
            complete(new SayCompleteEvent(model, VerbCompleteEvent.Reason.ERROR, "Timeout"));
            break;
        }
    }

	private String findErrorCause(com.voxeo.moho.event.OutputCompleteEvent<Participant> event) {

		try {
			ssmlValidator.validateSsml(model.getPrompt().getText());
		} catch (ValidationException ve) {
			return ve.getMessage();
		}
		
		String cause = event.getErrorText();
		if (cause != null) {
			if (cause.startsWith("NOT_FOUND")) {
				cause = "Could not find the Resource's URI";
			}
		} else {
			cause = "Unknown cause";
		}
		
		return cause;
	}
	
	public void setSsmlValidator(SsmlValidator ssmlValidator) {
		this.ssmlValidator = ssmlValidator;
	}    
}
