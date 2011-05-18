package com.tropo.server.verb;

import java.util.List;

import com.tropo.core.verb.Ask;
import com.tropo.core.verb.AskCompleteEvent;
import com.tropo.core.verb.Choices;
import com.tropo.core.verb.PromptItem;
import com.tropo.core.verb.AskCompleteEvent.Reason;
import com.voxeo.moho.State;
import com.voxeo.moho.event.InputCompleteEvent;
import com.voxeo.moho.media.InputMode;
import com.voxeo.moho.media.Prompt;
import com.voxeo.moho.media.input.Grammar;
import com.voxeo.moho.media.input.InputCommand;
import com.voxeo.moho.media.output.AudibleResource;
import com.voxeo.moho.media.output.OutputCommand;

public class LocalAskHandler extends AbstractLocalVerbHandler<Ask> {

    private Prompt prompt;
    
    @Override
    public void start() {

        OutputCommand outCommand = null;
        List<PromptItem> promptItems = model.getPromptItems();
        
        if (promptItems != null && !promptItems.isEmpty()) {
            AudibleResource[] audibleResources = resolveAudio(promptItems);
            outCommand = new OutputCommand(audibleResources);
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
        inputCommand.setInputMode(InputMode.valueOf(model.getMode().name()));
        inputCommand.setTermChar(model.getTerminator());
        inputCommand.setConfidence(model.getMinConfidence());

        prompt = media.prompt(outCommand, inputCommand, 0);
        
    }
    
    // Commands
    // ================================================================================

    public void stop(boolean hangup) {
        prompt.getOutput().stop();
        prompt.getInput().stop();
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
            completeEvent = new AskCompleteEvent(model, Reason.STOP);
            break;
        default:
            completeEvent = new AskCompleteEvent(model, "Could not complete Ask at this time.");
        }
        
        complete(completeEvent);
    }

}
