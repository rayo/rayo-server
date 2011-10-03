package com.rayo.core.xml.providers;

import static com.voxeo.utils.Strings.isEmpty;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;

import com.rayo.core.verb.Choices;
import com.rayo.core.verb.Input;
import com.rayo.core.verb.InputCompleteEvent;
import com.rayo.core.verb.InputMode;
import com.rayo.core.verb.OutputCompleteEvent;
import com.rayo.core.verb.InputCompleteEvent.Reason;

public class InputProvider extends BaseProvider {

    // XML -> Object
    // ================================================================================

    private static final Namespace NAMESPACE = new Namespace("", "urn:xmpp:rayo:input:1");
    private static final Namespace COMPLETE_NAMESPACE = new Namespace("", "urn:xmpp:rayo:input:complete:1");
    
    @Override
    protected Object processElement(Element element) throws Exception {
        if (element.getName().equals("input")) {
            return buildInput(element);
        } else if (element.getNamespace().equals(RAYO_COMPONENT_NAMESPACE)) {
            return buildCompleteCommand(element);
        }
        return null;
    }

    private Object buildCompleteCommand(Element element) {
        
        Element reasonElement = (Element)element.elements().get(0);
    	String reasonValue = reasonElement.getName().toUpperCase();
        Reason reason = Reason.valueOf(reasonValue);
            	
    	InputCompleteEvent event = new InputCompleteEvent();

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
        if (reasonElement.element("nlsml") != null) {
            event.setNlsml(reasonElement.element("nlsml").getText());            
        }    	
    	return event;
    }

    @SuppressWarnings("unchecked")
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
        if (element.attribute("terminator") != null) {
        	input.setTerminator(toTerminator(element.attributeValue("terminator")));
        }
        
        List<Choices> grammars = new ArrayList<Choices>();
        List<Element> grammarsElements = element.elements("grammar");
        for (Element choiceElement : grammarsElements) {
            Choices choice = new Choices();
            choice.setContentType(choiceElement.attributeValue("content-type"));
            if (choiceElement.attributeValue("url") != null) {
                choice.setUri(toURI(choiceElement.attributeValue("url")));
            } else {
                String content = isEmpty(choiceElement.getText(), (String) null);
                choice.setContent(content);
            }
            grammars.add(choice);
        }
        input.setGrammars(grammars);
        
        return input;
    }

    
    // Object -> XML
    // ================================================================================

    @Override
    protected void generateDocument(Object object, Document document) throws Exception {

        if (object instanceof Input) {
            createInput((Input) object, document);
        } else if (object instanceof InputCompleteEvent) {
            createInputCompleteEvent((InputCompleteEvent) object, document);
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
                    elementGrammar.setText(choice.getContent());
                }
            }
        }
    }
    
    private void createInputCompleteEvent(InputCompleteEvent event, Document document) throws Exception {
        
    	Element completeElement =  addCompleteElement(document, event, COMPLETE_NAMESPACE);
        if(event.getReason() instanceof Reason) {
            Reason reason = (Reason)event.getReason();
            if(reason == Reason.SUCCESS) {
                
                completeElement.addAttribute("confidence", String.valueOf(event.getConfidence()));
                
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
                if (event.getNlsml() != null) {
                	completeElement.addElement("nlsml").setText(event.getNlsml());
                }    
            }
        }
    }

    @Override
    public boolean handles(Class<?> clazz) {

        //TODO: Refactor out to spring configuration and put everything in the base provider class
        return clazz == Input.class || 
               clazz == InputCompleteEvent.class;
    }
}
