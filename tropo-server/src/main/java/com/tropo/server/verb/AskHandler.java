package com.tropo.server.verb;

import java.util.List;

import javax.validation.ConstraintValidatorContext;

import com.tropo.core.validation.ValidationException;
import com.tropo.core.verb.Ask;
import com.tropo.core.verb.AskCompleteEvent;
import com.tropo.core.verb.AskCompleteEvent.Reason;
import com.tropo.core.verb.Choices;
import com.tropo.core.verb.Ssml;
import com.tropo.core.verb.VerbCompleteEvent;
import com.tropo.server.exception.ExceptionMapper;
import com.tropo.server.validation.SsmlValidator;
import com.voxeo.moho.Participant;
import com.voxeo.moho.State;
import com.voxeo.moho.event.InputCompleteEvent;
import com.voxeo.moho.media.Prompt;
import com.voxeo.moho.media.input.Grammar;
import com.voxeo.moho.media.input.InputCommand;
import com.voxeo.moho.media.output.OutputCommand;
import com.voxeo.moho.media.output.OutputCommand.BargeinType;
import com.voxeo.servlet.xmpp.StanzaError;

public class AskHandler extends AbstractLocalVerbHandler<Ask,Participant> {

    private Prompt<Participant> prompt;
    
    private SsmlValidator ssmlValidator;
    
    @Override
    public void start() {

        OutputCommand outCommand = null;
        Ssml ssml = model.getPrompt();
        
        if (ssml != null) {
            outCommand = new OutputCommand(resolveAudio(ssml));
            outCommand.setBargeinType(model.isBargein() ? BargeinType.ANY : BargeinType.NONE);
            outCommand.setVoiceName(model.getVoice());
        }

        final List<Choices> choicesList = model.getChoices();
        Grammar[] grammars = new Grammar[choicesList.size()];
        
        for(int i=0; i<grammars.length; i++) {
            Choices choices =  choicesList.get(i);
            Grammar grammar = null;
            if(choices.getUri() != null) {
                grammar = new Grammar(choices.getUri());
            }
            else {
                grammar = new Grammar(choices.getContentType(), choices.getContent());
            }
            grammars[i] = grammar;
        }
        
        InputCommand inputCommand = new InputCommand(grammars);
        
        long timeout = model.getTimeout().getMillis();
        inputCommand.setInitialTimeout(timeout);
        inputCommand.setInterDigitsTimeout(timeout);
        inputCommand.setRecognizer(model.getRecognizer());
        inputCommand.setInputMode(getMohoMode(model.getMode()));
        inputCommand.setTerminator(model.getTerminator());
        inputCommand.setMinConfidence(model.getMinConfidence());

        prompt = getMediaService().prompt(outCommand, inputCommand, 0);
        
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
            complete(new AskCompleteEvent(model, Reason.NOMATCH));
        }
        else {
            prompt.getOutput().stop();
            prompt.getInput().stop();
        }
    }

    // Moho Events
    // ================================================================================

    @State
    public void onAskComplete(InputCompleteEvent<Participant> event) {
        
        AskCompleteEvent completeEvent = null;
        
        switch (event.getCause()) {
        case MATCH:
            completeEvent = new AskCompleteEvent(model, Reason.SUCCESS);
            completeEvent.setConcept(event.getConcept());
            completeEvent.setInterpretation(event.getInterpretation());
            completeEvent.setConfidence(event.getConfidence());
            completeEvent.setUtterance(event.getUtterance());
            completeEvent.setNlsml(event.getNlsml());
            completeEvent.setTag(event.getTag());
            completeEvent.setMode(getTropoMode(event.getInputMode()));
            break;
        case INI_TIMEOUT:
            completeEvent = new AskCompleteEvent(model, Reason.NOINPUT);
            break;
        case IS_TIMEOUT:
        case MAX_TIMEOUT:
            completeEvent = new AskCompleteEvent(model, Reason.TIMEOUT);
            break;
        case NO_MATCH:
            completeEvent = new AskCompleteEvent(model, Reason.NOMATCH);
            break;
        case CANCEL:
            completeEvent = new AskCompleteEvent(model, VerbCompleteEvent.Reason.STOP);
            break;
        case DISCONNECT:
            completeEvent = new AskCompleteEvent(model, VerbCompleteEvent.Reason.HANGUP);
            break;
        case ERROR:
        case UNKNOWN:
        default:
        	complete(new AskCompleteEvent(model, VerbCompleteEvent.Reason.ERROR, findErrorCause(event)));
        }
        
        complete(completeEvent);
    }
    
	private String findErrorCause(com.voxeo.moho.event.InputCompleteEvent<Participant> event) {

		try {
			ssmlValidator.validateSsml(model.getPrompt().getText());
		} catch (ValidationException ve) {
			return ve.getMessage();
		}
		return event.getErrorText() == null ? "Internal Server Error" : event.getErrorText();
	}

	public void setSsmlValidator(SsmlValidator ssmlValidator) {
		this.ssmlValidator = ssmlValidator;
	}
}
