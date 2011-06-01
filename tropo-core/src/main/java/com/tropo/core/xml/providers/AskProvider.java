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
import com.tropo.core.verb.Choices;

public class AskProvider extends BaseProvider {

    @Override
    protected Object processElement(Element element) throws Exception {

        if (element.getName().equals("ask")) {
            return buildAsk(element);
        } else if (element.getName().equals("complete")) {
            return buildCompleteCommand(element);
        }
        return null;
    }

    private Object buildCompleteCommand(Element element) throws URISyntaxException {

        AskCompleteEvent askComplete = new AskCompleteEvent();
        if (element.attributeValue("reason") != null) {
            askComplete.setReason(AskCompleteEvent.Reason.valueOf(element.attributeValue("reason")));
        }
        if (element.element("error") != null) {
            askComplete.setErrorText(element.elementText("error"));
        }
        if (element.attributeValue("concept") != null) {
            askComplete.setConcept(element.attributeValue("concept"));
        }
        if (element.attributeValue("interpretation") != null) {
            askComplete.setInterpretation(element.attributeValue("interpretation"));
        }
        if (element.attributeValue("nlsml") != null) {
            askComplete.setNlsml(element.attributeValue("nlsml"));
        }
        if (element.attributeValue("confidence") != null) {
            askComplete.setConfidence(toFloatConfidence(element.attributeValue("confidence")));
        }
        if (element.attributeValue("tag") != null) {
            askComplete.setTag(element.attributeValue("tag"));
        }
        if (element.attributeValue("utterance") != null) {
            askComplete.setUtterance(element.attributeValue("utterance"));
        }

        return askComplete;
    }

    @SuppressWarnings("unchecked")
    private Object buildAsk(Element element) throws URISyntaxException {

        Element root = element;
        Ask ask = new Ask();
        if (root.attributeValue("bargein") != null) {
            ask.setBargein(toBoolean(root.attributeValue("bargein")));
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
            createAsk(object, document);
        } else if (object instanceof AskCompleteEvent) {
            createAskCompleteEvent(object, document);
        }
    }

    private Document createAsk(Object object, Document document) throws Exception {

        Ask ask = (Ask) object;
        Element root = document.addElement(new QName("ask", new Namespace("", "urn:xmpp:ozone:ask:1")));
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
                    elementChoice.setText(choice.getContent());
                }
            }
        }
        root.addAttribute("min-confidence", String.valueOf(ask.getMinConfidence()));
        if (ask.getMode() != null) {
            root.addAttribute("mode", ask.getMode().toString());
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

        return document;
    }

    private Document createAskCompleteEvent(Object object, Document document) throws Exception {

        AskCompleteEvent askComplete = (AskCompleteEvent) object;
        Element root = document.addElement(new QName("complete", new Namespace("", "urn:xmpp:ozone:ask:1")));
        if (askComplete.getReason() != null) {
            root.addAttribute("reason", askComplete.getReason().toString());
        }
        if (askComplete.getConcept() != null) {
            root.addAttribute("concept", askComplete.getConcept());
        }
        if (askComplete.getInterpretation() != null) {
            root.addAttribute("interpretation", askComplete.getInterpretation());
        }
        if (askComplete.getNlsml() != null) {
            root.addAttribute("nlsml", askComplete.getNlsml());
        }
        root.addAttribute("confidence", String.valueOf(askComplete.getConfidence()));
        if (askComplete.getTag() != null) {
            root.addAttribute("tag", askComplete.getTag());
        }
        if (askComplete.getUtterance() != null) {
            root.addAttribute("utterance", askComplete.getUtterance());
        }
        if (askComplete.getErrorText() != null) {
            root.addElement("error").setText(askComplete.getErrorText());
        }
        return document;
    }

    @Override
    public boolean handles(Class<?> clazz) {

        //TODO: Refactor out to spring configuration and put everything in the base provider class
        return clazz == Ask.class || clazz == AskCompleteEvent.class;
    }
}
