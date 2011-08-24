package com.tropo.core.xml.providers;

import static com.voxeo.utils.Strings.isEmpty;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;

import com.tropo.core.verb.Ask;
import com.tropo.core.verb.AskCompleteEvent;
import com.tropo.core.verb.InputMode;
import com.tropo.core.verb.AskCompleteEvent.Reason;
import com.tropo.core.verb.Choices;

public class AskProvider extends BaseProvider {

    private static final Namespace NAMESPACE = new Namespace("", "urn:xmpp:tropo:ask:1");
    private static final Namespace COMPLETE_NAMESPACE = new Namespace("", "urn:xmpp:tropo:ask:complete:1");

	@Override
	protected Object processElement(Element element) throws Exception {
		if (element.getName().equals("ask")) {
            return buildAsk(element);
        } else if (element.getNamespace().equals(RAYO_COMPONENT_NAMESPACE)) {
            return buildCompleteCommand(element);
        }
        return null;
    }

    private Object buildCompleteCommand(Element element) throws URISyntaxException {
    	
    	Element reasonElement = (Element)element.elements().get(0);
    	AskCompleteEvent event = new AskCompleteEvent();

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
    	
    	return event;
    }	

    @SuppressWarnings("unchecked")
    private Object buildAsk(Element element) throws URISyntaxException {

        Element root = element;
        Ask ask = new Ask();
        if (root.attributeValue("bargein") != null) {
            ask.setBargein(toBoolean("bargein", element));
        }
        if (root.attributeValue("min-confidence") != null) {
            ask.setMinConfidence(toFloatConfidence(root.attributeValue("min-confidence")));
        }
        if (root.attributeValue("mode") != null) {
            ask.setMode(loadInputMode(root));
        }

        ask.setRecognizer(root.attributeValue("recognizer"));

        if (root.attributeValue("terminator") != null) {
            ask.setTerminator(toTerminator(root.attributeValue("terminator")));
        }
        if (root.attributeValue("timeout") != null) {
            ask.setTimeout(toTimeout(root.attributeValue("timeout")));
        }
        ask.setVoice(root.attributeValue("voice"));

        Element promptElement = element.element("prompt");
        if (promptElement != null) {
            ask.setPrompt(extractSsml(promptElement));
        }

        List<Choices> choices = new ArrayList<Choices>();
        List<Element> choicesElements = root.elements("choices");
        for (Element choiceElement : choicesElements) {
            Choices choice = new Choices();
            choice.setContentType(choiceElement.attributeValue("content-type"));
            if (choiceElement.attributeValue("url") != null) {
                choice.setUri(toURI(choiceElement.attributeValue("url")));
            } else {
                String content = isEmpty(choiceElement.getText(), (String) null);
                choice.setContent(content);
            }
            choices.add(choice);
        }
        ask.setChoices(choices);

        return ask;
    }

    @Override
    protected void generateDocument(Object object, Document document) throws Exception {
    	if (object instanceof Ask) {
            createAsk((Ask)object, document);
        } else if (object instanceof AskCompleteEvent) {
            createAskCompleteEvent((AskCompleteEvent)object, document);
        }
    }

    private void createAsk(Ask ask, Document document) throws Exception {

        Element root = document.addElement(new QName("ask", NAMESPACE));
        if (ask.getVoice() != null) {
            root.addAttribute("voice", ask.getVoice());
        }

        if (ask.getPrompt() != null) {
            Element prompt = root.addElement("prompt");
            addSsml(ask.getPrompt(), prompt);
        }

        if (ask.getChoices() != null) {
            for (Choices choice : ask.getChoices()) {
                Element elementChoice = root.addElement("choices");
                if (choice.getContentType() != null) {
                    elementChoice.addAttribute("content-type", choice.getContentType());
                }
                if (choice.getUri() != null) {
                    elementChoice.addAttribute("url", choice.getUri().toString());
                }
                if (choice.getContent() != null) {
                	elementChoice.addCDATA(" " + choice.getContent() + " ");
                }
            }
        }
        root.addAttribute("min-confidence", String.valueOf(ask.getMinConfidence()));
        if (ask.getMode() != null) {
            root.addAttribute("mode", ask.getMode().name().toLowerCase());
        }
        if (ask.getRecognizer() != null) {
            root.addAttribute("recognizer", ask.getRecognizer());
        }
        if (ask.getTerminator() != null) {
            root.addAttribute("terminator", ask.getTerminator().toString());
        }
        if (ask.getTimeout() != null) {
            root.addAttribute("timeout", Long.toString(ask.getTimeout().getMillis()));
        }
        root.addAttribute("bargein", String.valueOf(ask.isBargein()));

    }

    private void createAskCompleteEvent(AskCompleteEvent event, Document document) throws Exception {

        Element completeElement = addCompleteElement(document, event, COMPLETE_NAMESPACE);
        
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
            }
        }
    }

	@Override
	public boolean handles(Class<?> clazz) {
		//TODO: Refactor out to spring configuration and put everything in the base provider class
		return clazz == Ask.class ||
			   clazz == AskCompleteEvent.class;
	}
	
}
