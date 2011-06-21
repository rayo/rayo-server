package com.tropo.server.verb;

import java.util.List;

import javax.validation.ConstraintValidatorContext;

import com.tropo.core.verb.Ask;
import com.tropo.core.verb.AskCompleteEvent;
import com.tropo.core.verb.AskCompleteEvent.Reason;
import com.tropo.core.verb.Choices;
import com.tropo.core.verb.Ssml;
import com.tropo.core.verb.VerbCompleteEvent;
import com.voxeo.moho.State;
import com.voxeo.moho.event.InputCompleteEvent;
import com.voxeo.moho.media.Prompt;
import com.voxeo.moho.media.input.Grammar;
import com.voxeo.moho.media.input.InputCommand;
import com.voxeo.moho.media.output.OutputCommand;

public class AskHandler extends AbstractLocalVerbHandler<Ask> {

    private Prompt prompt;
    
    @Override
    public void start() {

        OutputCommand outCommand = null;
        Ssml ssml = model.getPrompt();
        
        if (ssml != null) {
            outCommand = new OutputCommand(resolveAudio(ssml));
            outCommand.setBargein(model.isBargein());
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
        inputCommand.setInterSigTimeout(timeout);
        inputCommand.setSpeechLanguage(model.getRecognizer());
        inputCommand.setInputMode(getMohoMode(model.getMode()));
        inputCommand.setTermChar(model.getTerminator());
        inputCommand.setConfidence(model.getMinConfidence());

        prompt = media.prompt(outCommand, inputCommand, 0);
        
    }
    
    @Override
    public boolean isStateValid(ConstraintValidatorContext context) {

        if (isOnConference(call)) {
        	context.buildConstraintViolationWithTemplate(
        			"Call is joined to a conference.")
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
    public void onAskComplete(InputCompleteEvent event) {
        
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
            completeEvent = new AskCompleteEvent(model, "Internal Server Error");
        }
        
        complete(completeEvent);
    }

}
