package com.tropo.server.verb;

import java.util.List;

import com.tropo.core.verb.Choices;
import com.tropo.core.verb.Input;
import com.tropo.core.verb.InputCompleteEvent;
import com.tropo.core.verb.InputCompleteEvent.Reason;
import com.tropo.core.verb.VerbCompleteEvent;
import com.voxeo.moho.State;
import com.voxeo.moho.media.input.Grammar;
import com.voxeo.moho.media.input.InputCommand;

public class InputHandler extends AbstractLocalVerbHandler<Input> {

	private com.voxeo.moho.media.Input input;
	
	@Override
	public void start() {
		
        final List<Choices> choicesList = model.getGrammars();
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
        if (model.getInitialTimeout() != null) {
        	inputCommand.setInitialTimeout(model.getInitialTimeout());
        }
        if (model.getMaxTimeout() != null) {
        	inputCommand.setMaxTimeout(model.getMaxTimeout());
        }
        if (model.getInterSigTimeout() != null) {
        	inputCommand.setMaxTimeout(model.getInterSigTimeout());
        }
        if (model.getRecognizer() != null) {
        	inputCommand.setSpeechLanguage(model.getRecognizer());
        }
        if (model.getDtmfHotword() != null) {
        	inputCommand.setDtmfHotword(model.getDtmfHotword());
        }
        if (model.getDtmfTypeAhead() != null) {
        	inputCommand.setDtmfTypeahead(model.getDtmfTypeAhead());
        }
        if (model.getSupervised() != null) {
        	inputCommand.setSupervised(model.getSupervised());
        }
        if (model.getConfidence() != null) {
        	inputCommand.setConfidence(model.getConfidence());
        }
        if (model.getInputMode() != null) {
        	inputCommand.setInputMode(getMohoMode(model.getInputMode()));
        }
        if (model.getSensitivity() != null) {
        	inputCommand.setSensitivity(model.getSensitivity());
        }
        if (model.getTerminator() != null) {
        	inputCommand.setTermChar(model.getTerminator());
        }

        input = media.input(inputCommand);		
	}
	
	@Override
	public void stop(boolean hangup) {
		
        if(hangup) {
            complete(new InputCompleteEvent(model, Reason.NOMATCH));
        }
        else {
            input.stop();
        }
	}
	

    @State
    public void onInputComplete(com.voxeo.moho.event.InputCompleteEvent event) {
        
        InputCompleteEvent completeEvent = null;
        
        switch (event.getCause()) {
        case MATCH:
            completeEvent = new InputCompleteEvent(model, Reason.SUCCESS);
            completeEvent.setConcept(event.getConcept());
            completeEvent.setInterpretation(event.getInterpretation());
            completeEvent.setConfidence(event.getConfidence());
            completeEvent.setUtterance(event.getUtterance());
            completeEvent.setNlsml(event.getNlsml());
            completeEvent.setTag(event.getTag());
            completeEvent.setMode(getTropoMode(event.getInputMode()));
            break;
        case INI_TIMEOUT:
            completeEvent = new InputCompleteEvent(model, Reason.NOINPUT);
            break;
        case IS_TIMEOUT:
        case MAX_TIMEOUT:
            completeEvent = new InputCompleteEvent(model, Reason.TIMEOUT);
            break;
        case NO_MATCH:
            completeEvent = new InputCompleteEvent(model, Reason.NOMATCH);
            break;
        case CANCEL:
            completeEvent = new InputCompleteEvent(model, VerbCompleteEvent.Reason.STOP);
            break;
        case DISCONNECT:
            completeEvent = new InputCompleteEvent(model, VerbCompleteEvent.Reason.HANGUP);
            break;
        case ERROR:
        case UNKNOWN:
        default:
            completeEvent = new InputCompleteEvent(model, "Internal Server Error");
        }
        
        complete(completeEvent);
    }
}
