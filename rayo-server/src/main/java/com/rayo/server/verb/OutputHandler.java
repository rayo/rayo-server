package com.rayo.server.verb;

import javax.jms.IllegalStateException;
import javax.validation.ConstraintValidatorContext;

import com.rayo.server.exception.ExceptionMapper;
import com.rayo.server.validation.SsmlValidator;
import com.rayo.core.validation.ValidationException;
import com.rayo.core.verb.Output;
import com.rayo.core.verb.OutputCompleteEvent;
import com.rayo.core.verb.PauseCommand;
import com.rayo.core.verb.ResumeCommand;
import com.rayo.core.verb.SayCompleteEvent.Reason;
import com.rayo.core.verb.SeekCommand;
import com.rayo.core.verb.SpeedDownCommand;
import com.rayo.core.verb.SpeedUpCommand;
import com.rayo.core.verb.Ssml;
import com.rayo.core.verb.VerbCommand;
import com.rayo.core.verb.VerbCompleteEvent;
import com.rayo.core.verb.VolumeDownCommand;
import com.rayo.core.verb.VolumeUpCommand;
import com.voxeo.logging.Loggerf;
import com.voxeo.moho.Participant;
import com.voxeo.moho.State;
import com.voxeo.moho.media.output.AudibleResource;
import com.voxeo.moho.media.output.OutputCommand;
import com.voxeo.moho.media.output.OutputCommand.BehaviorIfBusy;
import com.voxeo.servlet.xmpp.StanzaError;

public class OutputHandler extends AbstractLocalVerbHandler<Output, Participant> {

	private static final Loggerf logger = Loggerf.getLogger(OutputHandler.class);
	
    private com.voxeo.moho.media.Output<Participant> output;
    private SsmlValidator ssmlValidator;

    // Verb Lifecycle
    // ================================================================================

    @Override
    public void start() {
    	
        Ssml prompt = model.getPrompt();
        AudibleResource audibleResource = resolveAudio(prompt);
        OutputCommand outcommand = new OutputCommand(audibleResource);
        
        outcommand.setBahavior(BehaviorIfBusy.STOP);

        if (model.getBargeinType() != null) {
            outcommand.setBargeinType(model.getBargeinType());
        }
        if (model.getStartOffset() != null) {
            outcommand.setStartingOffset(model.getStartOffset().getMillis());
        }
        if (model.isStartPaused() != null) {
            outcommand.setStartInPausedMode(model.isStartPaused());
        }
        if (model.getRepeatInterval() != null) {
            outcommand.setRepeatInterval(model.getRepeatInterval().getMillis());
        }
        if (model.getRepeatTimes() != null) {
            outcommand.setRepeatTimes(model.getRepeatTimes());
        }
        if (model.getMaxTime() != null) {
            outcommand.setMaxtime(model.getMaxTime().getMillis());
        }
        if (prompt.getVoice() != null) {
            outcommand.setVoiceName(prompt.getVoice());
        }

        output = getMediaService().output(outcommand);
    }

    @Override
    public boolean isStateValid(ConstraintValidatorContext context) {

        if (isOnConference(participant)) {
            context.buildConstraintViolationWithTemplate("Call is joined to a conference.")
            	.addNode(ExceptionMapper.toString(StanzaError.Condition.RESOURCE_CONSTRAINT))
            		.addConstraintViolation();
            return false;
        }
        if (isOnHold(participant)) {
            context.buildConstraintViolationWithTemplate("Call is currently on hold.")
            	.addNode(ExceptionMapper.toString(StanzaError.Condition.RESOURCE_CONSTRAINT))
            		.addConstraintViolation();
            return false;
        }
        if (!canManipulateMedia()) {
            context.buildConstraintViolationWithTemplate("Media operations are not allowed in the current call status.")
            	.addNode(ExceptionMapper.toString(StanzaError.Condition.RESOURCE_CONSTRAINT))
            		.addConstraintViolation();
            return false;
        }

        return true;
    }

    // Commands
    // ================================================================================

    public void stop(boolean hangup) {
        if (hangup) {
            complete(new OutputCompleteEvent(model, VerbCompleteEvent.Reason.HANGUP));
        } else {
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
        } else if (command instanceof SeekCommand) {
            seek(((SeekCommand) command));
        } else if (command instanceof SpeedUpCommand) {
            speedUp((SpeedUpCommand) command);
        } else if (command instanceof SpeedDownCommand) {
            speedDown((SpeedDownCommand) command);
        } else if (command instanceof VolumeUpCommand) {
            volumeUp((VolumeUpCommand) command);
        } else if (command instanceof VolumeDownCommand) {
            volumeDown((VolumeDownCommand) command);
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

    public void seek(SeekCommand command) {

    	if (command.getDirection() == SeekCommand.Direction.FORWARD) {
    		output.move(true, command.getAmount());
    	} else {
    		output.move(false, command.getAmount());    		
    	}
    }

    public void speedUp(SpeedUpCommand command) {

        output.speed(true);
    }

    public void speedDown(SpeedDownCommand command) {

        output.speed(false);
    }

    public void volumeUp(VolumeUpCommand command) {

        output.volume(true);
    }

    public void volumeDown(VolumeDownCommand command) {

        output.volume(false);
    }

    // Moho Events
    // ================================================================================

    @State
    public void onSpeakComplete(com.voxeo.moho.event.OutputCompleteEvent<Participant> event) {
    	
    	if (!event.getMediaOperation().equals(output)) {
    		logger.debug("Ignoring complete event as it is targeted to a different media operation");
    		return;
    	}
    	
        switch (event.getCause()) {
        case BARGEIN:
        case END:
            complete(new OutputCompleteEvent(model, Reason.SUCCESS));
            break;
        case DISCONNECT:
            complete(new OutputCompleteEvent(model, VerbCompleteEvent.Reason.HANGUP));
            break;
        case CANCEL:
            complete(new OutputCompleteEvent(model, VerbCompleteEvent.Reason.STOP));
            break;
        case ERROR:
        case UNKNOWN:
        	complete(new OutputCompleteEvent(model, VerbCompleteEvent.Reason.ERROR, findErrorCause(event)));
        case TIMEOUT:
            complete(new OutputCompleteEvent(model, VerbCompleteEvent.Reason.ERROR, "Timeout"));
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
