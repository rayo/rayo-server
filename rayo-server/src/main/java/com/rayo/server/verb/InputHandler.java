package com.rayo.server.verb;

import java.util.List;

import com.rayo.core.verb.Choices;
import com.rayo.core.verb.Input;
import com.rayo.core.verb.InputCompleteEvent;
import com.rayo.core.verb.InputCompleteEvent.Reason;
import com.rayo.core.verb.VerbCompleteEvent;
import com.voxeo.logging.Loggerf;
import com.voxeo.moho.Call;
import com.voxeo.moho.Participant;
import com.voxeo.moho.State;
import com.voxeo.moho.event.InputDetectedEvent;
import com.voxeo.moho.media.input.Grammar;
import com.voxeo.moho.media.input.InputCommand;

public class InputHandler extends AbstractLocalVerbHandler<Input, Participant> {

	private com.voxeo.moho.media.Input<Participant> input;
	
	private static final Loggerf logger = Loggerf.getLogger(InputHandler.class);
	
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
        inputCommand.setInputMode(com.voxeo.moho.media.InputMode.DTMF);
        inputCommand.setSupervised(true);
        if (model.getInitialTimeout() != null) {
        	inputCommand.setInitialTimeout(model.getInitialTimeout().getMillis());
        }
        // TODO: put this back in when we get clarification from wei
        //if (model.getMaxDigits() != null) {
        //	inputCommand.setNumberOfDigits(model.getMaxDigits());
        //}
        if (model.getInterDigitTimeout() != null) {
        	inputCommand.setInterDigitsTimeout(model.getInterDigitTimeout().getMillis());
        }
        if (model.getRecognizer() != null) {
        	inputCommand.setRecognizer(model.getRecognizer());
        }
        if (model.getMinConfidence() != null) {
        	inputCommand.setMinConfidence(model.getMinConfidence());
        }
        if (model.getMode() != null) {
        	inputCommand.setInputMode(getMohoMode(model.getMode()));
        }
        if (model.getSensitivity() != null) {
        	inputCommand.setSensitivity(model.getSensitivity());
        }
        if (model.getTerminator() != null) {
        	inputCommand.setTerminator(model.getTerminator());
        }

        input = getMediaService().input(inputCommand);		
	}
	
	@Override
	public void stop(boolean hangup) {
		
        if(hangup) {
            complete(new InputCompleteEvent(model, Reason.NOMATCH));
        }
        else {
        	if (input != null) {
        		input.stop();
        	}
        }
	}
	

    @State
    public void onInputComplete(com.voxeo.moho.event.InputCompleteEvent<Participant> event) {
        
    	if (!event.getMediaOperation().equals(input)) {
    		logger.debug("Ignoring complete event as it is targeted to a different media operation");
    		return;
    	}

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
            completeEvent.setMode(getInputMode(event.getInputMode()));
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
        	String cause = event.getErrorText() == null ? "Internal Server Error" : event.getErrorText();
            completeEvent = new InputCompleteEvent(model, cause);
            break;
        case UNKNOWN:
        default:
        	if (participant instanceof Call) {
        		if (((Call)participant).getCallState() == com.voxeo.moho.Call.State.DISCONNECTED) {
        			completeEvent = new InputCompleteEvent(model, VerbCompleteEvent.Reason.HANGUP);      
        			break;
        		}
        	} 
            completeEvent = new InputCompleteEvent(model, "Internal Server Error");
        }
        
        complete(completeEvent);
    }
    
    public void onInputDetected(InputDetectedEvent<Participant> event) {
    	
    	System.out.println(String.format("Event: ", event));
    }
}
