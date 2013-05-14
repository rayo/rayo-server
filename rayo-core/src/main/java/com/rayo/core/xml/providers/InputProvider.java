package com.rayo.core.xml.providers;

import static com.voxeo.utils.Strings.isEmpty;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;

import com.rayo.core.validation.Messages;
import com.rayo.core.validation.ValidationException;
import com.rayo.core.verb.Choices;
import com.rayo.core.verb.CpaData;
import com.rayo.core.verb.Input;
import com.rayo.core.verb.InputCompleteEvent;
import com.rayo.core.verb.SignalEvent;
import com.rayo.core.verb.InputCompleteEvent.Reason;
import com.rayo.core.verb.InputMode;

public class InputProvider extends BaseProvider {

    // XML -> Object
    // ================================================================================

    private static final Namespace NAMESPACE = new Namespace("", "urn:xmpp:rayo:input:1");
    private static final Namespace COMPLETE_NAMESPACE = new Namespace("", "urn:xmpp:rayo:input:complete:1");
    
    private static final String CPA_DTMF_URI = "urn:xmpp:rayo:cpa:dtmf:1";
    private static final String CPA_MODEM_URI = "urn:xmpp:rayo:cpa:modem:1";
    private static final String CPA_FAX_URI = "urn:xmpp:rayo:cpa:fax:1";
    private static final String CPA_FAX_CNG_URI = "urn:xmpp:rayo:cpa:fax-cng:1";
    private static final String CPA_BEEP_URI = "urn:xmpp:rayo:cpa:beep:1";
    private static final String CPA_RING_URI = "urn:xmpp:rayo:cpa:ring:1";
    private static final String CPA_SIT_URI = "urn:xmpp:rayo:cpa:sit:1";
    private static final String CPA_OFFHOOK_URI = "urn:xmpp:rayo:cpa:offhook:1";
    private static final String CPA_SPEECH_URI = "urn:xmpp:rayo:cpa:speech:1";

    // used just for testing purposes
    private static final String CPA_FOO_URI = "urn:xmpp:rayo:cpa:foo:1";

    @Override
    protected Object processElement(Element element) throws Exception {
        if (element.getName().equals("input")) {
            return buildInput(element);
        } else if (element.getNamespace().equals(RAYO_COMPONENT_NAMESPACE)) {
            return buildCompleteCommand(element);
        } else if (element.getName().equals("signal")) {
            return buildSignalEvent(element);
        }
        return null;
    }

    private Object buildSignalEvent(Element element) {

    	SignalEvent event = new SignalEvent();
        if (element.attributeValue("duration") != null) {
            event.setDuration(toLong("duration", element));           
        }
        if (element.attributeValue("type") != null) {
            event.setType(element.attributeValue("type"));           
        }
        if (element.attributeValue("tone") != null) {
            event.setTone(element.attributeValue("tone"));           
        }        
        if (element.attributeValue("source") != null) {
            event.setSource(element.attributeValue("source"));           
        }   
        return event;
	}

	private Object buildCompleteCommand(Element element) {
        
    	InputCompleteEvent event = new InputCompleteEvent();
        Element reasonElement = (Element)element.elements().get(0);
        if (reasonElement.getName().equals("signal")) {
        	event.setReason(Reason.MATCH);
        	event.setSignalEvent((SignalEvent)buildSignalEvent(reasonElement));
        } else {
	    	String reasonValue = reasonElement.getName().toUpperCase();
	        Reason reason = Reason.valueOf(reasonValue);	
	        event.setReason(reason);
	
	        if (reasonElement.attributeValue("confidence") != null) {
	            event.setConfidence(toFloatConfidence(reasonElement.attributeValue("confidence")));           
	        }
	        if (reasonElement.attributeValue("mode") != null) {
	            String modeValue = reasonElement.attributeValue("mode").toUpperCase();
	            InputMode mode = InputMode.valueOf(modeValue);
	            event.setMode(mode);           
	        }
	
	        if (reasonElement.element("interpretation") != null) {
	            event.setInterpretation(reasonElement.element("interpretation").getText());          
	        }
	        if (reasonElement.element("utterance") != null) {
	            event.setUtterance(reasonElement.element("utterance").getText());            
	        }
	        
	        if (reasonElement.element("concept") != null) {
	            event.setConcept(reasonElement.element("concept").getText());          
	        }
	        if (reasonElement.element("tag") != null) {
	            event.setTag(reasonElement.element("tag").getText());            
	        }
	        if (reasonElement.element("result") != null) {
	            event.setNlsml(reasonElement.element("result").asXML());            
	        }    	
        }	        
	    return event;
    }

	private Object buildInput(Element element) throws URISyntaxException {
        
    	Input input = new Input();
        
        if (element.attribute("min-confidence") != null) {
        	input.setMinConfidence(toFloatConfidence(element.attributeValue("min-confidence")));
        }
        if (element.attribute("initial-timeout") != null) {
        	input.setInitialTimeout(toDuration("initial-timeout",element));
        }
        if (element.attribute("mode") != null) {
        	input.setMode(loadInputMode(element));
        }
        if (element.attribute("inter-digit-timeout") != null) {
        	input.setInterDigitTimeout(toDuration("inter-digit-timeout", element));
        }
        if (element.attribute("recognizer") != null) {
        	input.setRecognizer(element.attributeValue("recognizer"));
        }
        if (element.attribute("sensitivity") != null) {
        	input.setSensitivity(toFloat("sensitivity", element));
        }
        if (element.attribute("max-silence") != null) {
        	input.setMaxSilence(toDuration("max-silence", element));
        }
        if (element.attribute("terminator") != null) {
        	input.setTerminator(toTerminator(element.attributeValue("terminator")));
        }
        
        processGrammars(element, input);
        return input;
    }
    
    @SuppressWarnings("unchecked")
    private void processGrammars(Element element, Input input) {

        List<Choices> grammars = new ArrayList<Choices>();
		List<Element> grammarsElements = element.elements("grammar");
        for (Element choiceElement : grammarsElements) {
        	if (isCpaGrammar(choiceElement)) {
        		processCpaData(input, choiceElement);        		
        	} else {
	            Choices choice = new Choices();
	        	String content = isEmpty(choiceElement.getText(), (String) null);
	        	String contentType = choiceElement.attributeValue("content-type");
	            choice.setContentType(contentType);
	            if (choiceElement.attributeValue("url") != null) {
	                choice.setUri(toURI(choiceElement.attributeValue("url")));
	            } else {
	            	if (content != null && content.startsWith("<![CDATA[")) {
	            		content = content.substring(9, content.length()-3);
	            	}
	                choice.setContent(content);
	            }
	            grammars.add(choice);        		
        	}
        }
        
        input.setGrammars(grammars);
	}

	private boolean isCpaGrammar(Element choiceElement) {

    	return (choiceElement.element("ruleref") != null &&        		
        	choiceElement.element("ruleref").attributeValue("uri").contains("urn:xmpp:rayo:cpa"));
	}

	@SuppressWarnings("unchecked")
	private void processCpaData(Input input, Element choiceElement) {
		// CPA grammar
		CpaData data = new CpaData();
		List<Element> metas = choiceElement.elements("meta");
		for(Element meta: metas) {
			String name = meta.attributeValue("name");
			if (name.equals("maxTime")) {
				data.setMaxTime(Long.parseLong(meta.attributeValue("content")));
			}
			if (name.equals("minSpeechDuration")) {
				data.setMinSpeechDuration(Long.parseLong(meta.attributeValue("content")));
			}
			if (name.equals("minVolume")) {
				data.setMinVolume(Long.parseLong(meta.attributeValue("content")));
			}
			if (name.equals("finalSilence")) {
				data.setFinalSilence(Long.parseLong(meta.attributeValue("content")));
			}
			if (name.equals("terminate")) {
				data.setTerminate(Boolean.parseBoolean(meta.attributeValue("content")));
			}
		}
		List<Element> signalElements = choiceElement.elements("ruleref");
		List<String> signals = new ArrayList<String>();
		for(Element signalElement: signalElements) {
			String uri = signalElement.attributeValue("uri");
			if (uri != null) {
				String signal = getSignal(uri);
				if (signal != null) {
					signals.add(signal);
				}
			}
		}
		data.setSignals(signals.toArray(new String[]{}));
		input.setCpaData(data);
	}
    
    private String getSignal(String uri) {
    	
    	if (uri.equals(CPA_BEEP_URI)) {
    		return "beep";
    	} else if (uri.equals(CPA_MODEM_URI)) {
    		return "dtmf";
    	} else if (uri.equals(CPA_FAX_URI)) {
    		return "fax";
    	} else if (uri.equals(CPA_FAX_CNG_URI)) {
    		return "fax-cng";
    	} else if (uri.equals(CPA_DTMF_URI)) {
    		return "dtmf";
    	} else if (uri.equals(CPA_RING_URI)) {
    		return "ring";
    	} else if (uri.equals(CPA_SIT_URI)) {
    		return "sit";
    	} else if (uri.equals(CPA_OFFHOOK_URI)) {
    		return "offhook";
    	} else if (uri.equals(CPA_SPEECH_URI)) {
    		return "speech";
    	} else if (uri.equals(CPA_FOO_URI)) {
    		return "foo";
    	}
    	return null;
    }

	// Object -> XML
    // ================================================================================

    @Override
    protected void generateDocument(Object object, Document document) throws Exception {

        if (object instanceof Input) {
            createInput((Input) object, document);
        } else if (object instanceof InputCompleteEvent) {
            createInputCompleteEvent((InputCompleteEvent) object, document);
        } else if (object instanceof SignalEvent) {
            createSignalEvent((SignalEvent) object, document);
        }
    }
    
    private void createInput(Input input, Document document) throws Exception {
    	
        Element root = document.addElement(new QName("input", NAMESPACE));
        if (input.getMinConfidence() != null ) {
        	root.addAttribute("min-confidence", String.valueOf(input.getMinConfidence()));        	
        }
        if (input.getInitialTimeout() != null ) {
        	root.addAttribute("initial-timeout", Long.toString(input.getInitialTimeout().getMillis()));        	
        }
        if (input.getMode() != null ) {
        	root.addAttribute("mode", input.getMode().toString());        	
        }
        if (input.getInterDigitTimeout() != null ) {
        	root.addAttribute("inter-digit-timeout", Long.toString(input.getInterDigitTimeout().getMillis()));        	
        }
        if (input.getRecognizer() != null ) {
        	root.addAttribute("recognizer", input.getRecognizer());        	
        }
        if (input.getSensitivity() != null ) {
        	root.addAttribute("sensitivity", String.valueOf(input.getSensitivity()));        	
        }
        if (input.getMaxSilence() != null ) {
        	root.addAttribute("max-silence", Long.toString(input.getMaxSilence().getMillis()));        	
        }
        if (input.getTerminator() != null ) {
        	root.addAttribute("terminator", String.valueOf(input.getTerminator()));        	
        }
        
        if (input.getGrammars() != null) {
            for (Choices choice : input.getGrammars()) {
                Element elementGrammar = root.addElement("grammar");
                if (choice.getContentType() != null) {
                    elementGrammar.addAttribute("content-type", choice.getContentType());
                }
                if (choice.getUri() != null) {
                    elementGrammar.addAttribute("url", choice.getUri().toString());
                }
                if (choice.getContent() != null) {
                    //elementGrammar.setText(choice.getContent());
                    elementGrammar.addCDATA(choice.getContent());
                }
            }
        }
        
        if (input.getCpaData() != null) {
            Element elementGrammar = root.addElement("grammar");
            elementGrammar.addAttribute("content-type", "application/srgs+xml");
            addMeta(elementGrammar, input.getCpaData().getMaxTime(), "maxTime");
            addMeta(elementGrammar, input.getCpaData().getMinSpeechDuration(), "minSpeechDuration");
            addMeta(elementGrammar, input.getCpaData().getMinVolume(), "minVolume");
            addMeta(elementGrammar, input.getCpaData().getFinalSilence(), "finalSilence");
            addMeta(elementGrammar, input.getCpaData().isTerminate(), "terminate");
            
            for (String signal: input.getCpaData().getSignals()) {
            	Element rule = elementGrammar.addElement("ruleref");
            	String uri = getUri(signal);
            	if (uri == null) {
            		throw new ValidationException(String.format(Messages.INVALID_SIGNAL,signal));
            	}
            	rule.addAttribute("uri", uri);
            }
        }
    }
    
    private void addMeta(Element grammar, Object object, String name) {
    	
    	if (object != null) {
    		Element meta = grammar.addElement("meta");
    		meta.addAttribute("name", name);
    		meta.addAttribute("content", object.toString());
    	}
    }
    
    private String getUri(String signal) {
    	
    	if (signal.equals("beep")) {
    		return CPA_BEEP_URI;
    	} else if (signal.equals("modem")) {
    		return CPA_MODEM_URI;
    	} else if (signal.equals("fax")) {
    		return CPA_FAX_URI;
    	} else if (signal.equals("fax-cng")) {
    		return CPA_FAX_CNG_URI;
    	} else if (signal.equals("dtmf")) {
    		return CPA_DTMF_URI;
    	} else if (signal.equals("ring")) {
    		return CPA_RING_URI;
    	} else if (signal.equals("sit")) {
    		return CPA_SIT_URI;
    	} else if (signal.equals("offhook")) {
    		return CPA_OFFHOOK_URI;
    	} else if (signal.equals("speech")) {
    		return CPA_SPEECH_URI;
    	} else if (signal.equals("foo")) {
    		return CPA_FOO_URI;
    	}

    	return null;
    }
    
    private void createInputCompleteEvent(InputCompleteEvent event, Document document) throws Exception {

        if(event.getReason() instanceof Reason) {
            Reason reason = (Reason)event.getReason();
            if(reason == Reason.MATCH && event.getSignalEvent() != null) {
                Element completeElement = document.addElement(new QName("complete", RAYO_COMPONENT_NAMESPACE));
                createSignalEvent(event.getSignalEvent(), completeElement);            	
            } else {
            	Element completeElement =  addCompleteElement(document, event, COMPLETE_NAMESPACE);
                completeElement.addAttribute("confidence", String.valueOf(event.getConfidence()));
            	
                if (event.getNlsml() != null) {
                    completeElement.add(buildNsmlElement(event.getNlsml()));
                }    
                if(event.getMode()!= null) {
                    completeElement.addAttribute("mode", event.getMode().name().toLowerCase());
                }
                if (event.getInterpretation() != null) {
                    completeElement.addElement("interpretation").setText(event.getInterpretation());
                }
                if (event.getUtterance() != null) {
                    completeElement.addElement("utterance").setText(event.getUtterance());
                }

                if (event.getTag() != null) {
                	completeElement.addElement("tag").setText(event.getTag());
                }
                if (event.getConcept() != null) {
                	completeElement.addElement("concept").setText(event.getConcept());
                }            	
            }
        }
    }
    
    private void createSignalEvent(SignalEvent event, Document document) throws Exception {
        
    	Element signalElement =  document.addElement(new QName("signal", NAMESPACE));
    	fillSignalEvent(event, signalElement);
    }
    
    private void createSignalEvent(SignalEvent event, Element element) throws Exception {
        
    	Element signalElement =  element.addElement(new QName("signal", NAMESPACE));
    	fillSignalEvent(event, signalElement);
    }
    
    private void fillSignalEvent(SignalEvent event, Element signalElement) {
    	
        if (event.getType() != null) {
        	signalElement.addAttribute("type", event.getType());        	
        }
        if (event.getDuration() != null && event.getDuration() != -1) {
        	signalElement.addAttribute("duration", String.valueOf(event.getDuration()));        	
        }
        if (event.getTone() != null) {
        	signalElement.addAttribute("tone", String.valueOf(event.getTone()));        	
        }
        if (event.getSource() != null) {
        	signalElement.addAttribute("source", String.valueOf(event.getSource()));        	
        }
    }
    
    private Element buildNsmlElement(String nlsml) throws Exception {
        //FIXME: We can't set the namespace after parsing sinc that would only update the namespace for the root element 
        nlsml = nlsml.replace("<result", "<result xmlns=\"http://www.w3c.org/2000/11/nlsml\" ");
    	Element element = (Element)DocumentHelper.parseText(nlsml).getRootElement();
        return element;
    }    
}
