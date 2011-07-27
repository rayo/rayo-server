package com.tropo.core.xml.providers;

import static com.voxeo.utils.Strings.isEmpty;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;

import com.tropo.core.verb.Choices;
import com.tropo.core.verb.Input;
import com.tropo.core.verb.InputCompleteEvent;
import com.tropo.core.verb.InputCompleteEvent.Reason;
import com.tropo.core.verb.InputMode;

public class InputProvider extends BaseProvider {

    // XML -> Object
    // ================================================================================

    private static final Namespace NAMESPACE = new Namespace("", "urn:xmpp:rayo:input:1");
    private static final Namespace COMPLETE_NAMESPACE = new Namespace("", "urn:xmpp:rayo:input:complete:1");
    
    @Override
    protected Object processElement(Element element) throws Exception {
        if (element.getName().equals("input")) {
            return buildInput(element);
        } else if (element.getNamespace().equals(COMPLETE_NAMESPACE)) {
            return buildCompleteCommand(element);
        }
        return null;
    }

    private Object buildCompleteCommand(Element element) {
        
    	InputCompleteEvent event = new InputCompleteEvent();

    	String reasonValue = element.getName().toUpperCase();
        Reason reason = Reason.valueOf(reasonValue);
        event.setReason(reason);

        if (element.attributeValue("confidence") != null) {
            event.setConfidence(toFloatConfidence(element.attributeValue("confidence")));           
        }
        if (element.attributeValue("mode") != null) {
            String modeValue = element.attributeValue("mode").toUpperCase();
            InputMode mode = InputMode.valueOf(modeValue);
            event.setMode(mode);           
        }

        if (element.element("interpretation") != null) {
            event.setInterpretation(element.element("interpretation").getText());          
        }
        if (element.element("utterance") != null) {
            event.setUtterance(element.element("utterance").getText());            
        }
    	
    	return event;
    }

    @SuppressWarnings("unchecked")
	private Object buildInput(Element element) throws URISyntaxException {
        
    	Input input = new Input();
        
        if (element.attribute("buffering") != null) {
        	input.setBuffering(toBoolean("buffering", element));
        }
        if (element.attribute("confidence") != null) {
        	input.setConfidence(toFloatConfidence(element.attributeValue("confidence")));
        }
        if (element.attribute("dtmf-hot-word") != null) {
        	input.setDtmfHotword(toBoolean("dtmf-hot-word",element));
        }
        if (element.attribute("dtmf-type-ahead") != null) {
        	input.setDtmfTypeAhead(toBoolean("dtmf-type-ahead",element));
        }
        if (element.attribute("initial-timeout") != null) {
        	input.setInitialTimeout(toInteger("initial-timeout",element));
        }
        if (element.attribute("offset") != null) {
        	input.setInputMode(loadInputMode(element));
        }
        if (element.attribute("inter-sig-timeout") != null) {
        	input.setInterSigTimeout(toInteger("inter-sig-timeout", element));
        }
        if (element.attribute("recognizer") != null) {
        	input.setRecognizer(element.attributeValue("recognizer"));
        }
        if (element.attribute("max-timeout") != null) {
        	input.setMaxTimeout(toInteger("max-timeout", element));
        }
        if (element.attribute("sensitivity") != null) {
        	input.setSensitivity(toFloat("sensitivity", element));
        }
        if (element.attribute("supervised") != null) {
        	input.setSupervised(toBoolean("supervised",element));
        }
        if (element.attribute("supervised") != null) {
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
        if (input.getBuffering() != null ) {
        	root.addAttribute("buffering", input.getBuffering().toString());        	
        }
        if (input.getDtmfHotword() != null ) {
        	root.addAttribute("dtmf-hot-word", input.getDtmfHotword().toString());        	
        }
        if (input.getDtmfTypeAhead() != null ) {
        	root.addAttribute("dtmf-type-ahead", String.valueOf(input.getDtmfTypeAhead()));        	
        }
        if (input.getSupervised() != null ) {
        	root.addAttribute("supervised", String.valueOf(input.getSupervised()));        	
        }
        if (input.getConfidence() != null ) {
        	root.addAttribute("confidence", String.valueOf(input.getConfidence()));        	
        }
        if (input.getInitialTimeout() != null ) {
        	root.addAttribute("initial-timeout", String.valueOf(input.getInitialTimeout()));        	
        }
        if (input.getInputMode() != null ) {
        	root.addAttribute("input-mode", input.getInputMode().toString());        	
        }
        if (input.getInterSigTimeout() != null ) {
        	root.addAttribute("inter-sig-timeout", input.getInterSigTimeout().toString());        	
        }
        if (input.getRecognizer() != null ) {
        	root.addAttribute("recognizer", input.getRecognizer());        	
        }
        if (input.getMaxTimeout() != null ) {
        	root.addAttribute("max-timeout", String.valueOf(input.getMaxTimeout()));        	
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
