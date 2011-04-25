package com.tropo.core.xml;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;
import org.joda.time.Duration;

import com.tropo.core.AcceptCommand;
import com.tropo.core.AnswerCommand;
import com.tropo.core.AnswerEvent;
import com.tropo.core.CallRejectReason;
import com.tropo.core.EndEvent;
import com.tropo.core.HangupCommand;
import com.tropo.core.Offer;
import com.tropo.core.RedirectCommand;
import com.tropo.core.RejectCommand;
import com.tropo.core.RingEvent;
import com.tropo.core.verb.Ask;
import com.tropo.core.verb.AskCompleteEvent;
import com.tropo.core.verb.AudioItem;
import com.tropo.core.verb.Choices;
import com.tropo.core.verb.ChoicesList;
import com.tropo.core.verb.Conference;
import com.tropo.core.verb.ConferenceCompleteEvent;
import com.tropo.core.verb.InputMode;
import com.tropo.core.verb.PauseCommand;
import com.tropo.core.verb.PromptItem;
import com.tropo.core.verb.PromptItems;
import com.tropo.core.verb.ResumeCommand;
import com.tropo.core.verb.Say;
import com.tropo.core.verb.SayCompleteEvent;
import com.tropo.core.verb.SsmlItem;
import com.tropo.core.verb.StopCommand;
import com.tropo.core.verb.Transfer;
import com.tropo.core.verb.TransferCompleteEvent;

public class OzoneProvider implements Provider {

    @Override
    @SuppressWarnings("unchecked")
    public Object fromXML(Element element) {

        try {
            if (element.getName().equals("offer")) {
                return buildOffer(element);
            } else if (element.getName().equals("accept")) {
                return buildAcceptCommand(element);
            } else if (element.getName().equals("answer")) {
                return buildAnswerCommand(element);
            } else if (element.getName().equals("hangup")) {
                return buildHangupCommand(element);
            } else if (element.getName().equals("reject")) {
                return buildRejectCommand(element);
            } else if (element.getName().equals("redirect")) {
                return buildRedirectCommand(element);
            } else if (element.getName().equals("say")) {
                return buildSay(element);
            } else if (element.getName().equals("pause")) {
                return buildPauseCommand(element);
            } else if (element.getName().equals("resume")) {
                return buildResumeCommand(element);
            } else if (element.getName().equals("stop")) {
                return buildStopCommand(element);
            } else if (element.getName().equals("ask")) {
                return buildAsk(element);
            } else if (element.getName().equals("transfer")) {
                return buildTransfer(element);
            } else if (element.getName().equals("conference")) {
                return buildConference(element);
            } else if (element.getName().equals("info")) {
                return buildCallInfo(element);
            } else if (element.getName().equals("end")) {
                return buildCallEnd(element);
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private Object buildCallEnd(Element element) {
        throw new UnsupportedOperationException();
    }

    private Object buildCallInfo(Element element) {
        throw new UnsupportedOperationException();
    }

    private Object buildOffer(Element element) throws URISyntaxException {

        Offer offer = new Offer(element.attributeValue("callId"));
        offer.setFrom(new URI(element.attributeValue("from")));
        offer.setTo(new URI(element.attributeValue("to")));
        offer.setHeaders(grabHeaders(element));

        return offer;
    }

    private Object buildAcceptCommand(Element element) throws URISyntaxException {

        AcceptCommand accept = new AcceptCommand(null);
        accept.setHeaders(grabHeaders(element));

        return accept;
    }

    private Object buildAnswerCommand(Element element) throws URISyntaxException {

        AnswerCommand answer = new AnswerCommand(null);
        answer.setHeaders(grabHeaders(element));

        return answer;
    }

    private Object buildHangupCommand(Element element) throws URISyntaxException {

        HangupCommand hangup = new HangupCommand(null);
        hangup.setHeaders(grabHeaders(element));

        return hangup;
    }

    private Object buildRejectCommand(Element element) throws URISyntaxException {

        RejectCommand reject = new RejectCommand(null);
        Element reasonElement = (Element)element.elements().get(0);
        reject.setReason(CallRejectReason.valueOf(reasonElement.getName().toUpperCase()));
        reject.setHeaders(grabHeaders(element));

        return reject;
    }

    private Object buildRedirectCommand(Element element) throws URISyntaxException {

        RedirectCommand reject = new RedirectCommand(null);
        reject.setTo(new URI(element.attributeValue("to")));
        reject.setHeaders(grabHeaders(element));

        return reject;
    }

    private Object buildSay(Element element) throws URISyntaxException {

        Say say = new Say();
        say.setVoice(element.attributeValue("voice"));
        say.setPromptItems(extractPromptItems(element));

        return say;
    }

    private Object buildPauseCommand(Element element) throws URISyntaxException {

        return new PauseCommand();
    }

    private Object buildResumeCommand(Element element) throws URISyntaxException {

        return new ResumeCommand();
    }

    private Object buildStopCommand(Element element) throws URISyntaxException {

        return new StopCommand();
    }

    @SuppressWarnings("unchecked")
    private Object buildAsk(Element element) throws URISyntaxException {

        Element root = element;
        Ask ask = new Ask();
        if (root.attributeValue("bargein") != null) {
            ask.setBargein(Boolean.valueOf(root.attributeValue("bargein")));
        }
        if (root.attributeValue("min-confidence") != null) {
            ask.setMinConfidence(Float.valueOf(root.attributeValue("min-confidence")));
        }
        if (root.attributeValue("mode") != null) {
            ask.setMode(InputMode.valueOf(root.attributeValue("mode")));
        }
        ask.setRecognizer(root.attributeValue("recognizer"));
        if (root.attributeValue("terminator") != null) {
            ask.setTerminator(root.attributeValue("terminator").charAt(0));
        }
        if (root.attributeValue("timeout") != null) {
            ask.setTimeout(new Duration(root.attributeValue("timeout")));
        }
        ask.setVoice(root.attributeValue("voice"));
        if (root.attributeValue("bargein") != null) {
            ask.setBargein(Boolean.valueOf(root.attributeValue("bargein")));
        }
        
        Element promptElement = element.element("prompt");
        if(promptElement != null) {
            ask.setPromptItems(extractPromptItems(promptElement));
        }

        ChoicesList choices = new ChoicesList();
        List<Element> choicesElements = root.elements("choices");
        for (Element choiceElement : choicesElements) {
            Choices choice = new Choices();
            choice.setContentType(choiceElement.attributeValue("content-type"));
            if (choiceElement.attributeValue("url") != null) {
                choice.setUri(new URI(choiceElement.attributeValue("url")));
            }
            choice.setContent(choiceElement.getText());
            choices.add(choice);
        }
        ask.setChoices(choices);

        return ask;
    }

    @SuppressWarnings("unchecked")
    private Object buildTransfer(Element element) throws URISyntaxException {

        Element root = element;
        Transfer transfer = new Transfer();
        if (root.attributeValue("terminator") != null) {
            transfer.setTerminator(root.attributeValue("terminator").charAt(0));
        }
        if (root.attributeValue("timeout") != null) {
            transfer.setTimeout(new Duration(root.attributeValue("timeout")));
        }
        transfer.setVoice(root.attributeValue("voice"));

        if (root.attributeValue("from") != null) {
            transfer.setFrom(new URI(root.attributeValue("from")));
        }
        
        List<URI> uriList = new ArrayList<URI>();
        if (root.attributeValue("to") != null) {
            uriList.add(new URI(root.attributeValue("to")));
        }
        if (root.element("to") != null) {
            List<Element> destinations = root.elements("to");
            for (Element destination : destinations) {
                uriList.add(new URI(destination.getText()));
            }
        }
        transfer.setTo(uriList);

        Element promptElement = element.element("prompt");
        if(promptElement != null) {
            transfer.setPromptItems(extractPromptItems(promptElement));
        }
        
        transfer.setHeaders(grabHeaders(element));

        return transfer;
    }

    private Map<String, String> grabHeaders(Element node) {

        Map<String, String> headers = new HashMap<String, String>();
        @SuppressWarnings("unchecked")
        List<Element> elements = node.elements("header");
        for (Element element : elements) {
            headers.put(element.attributeValue("name"), element.attributeValue("value"));
        }
        return headers;
    }

    private Object buildConference(Element element) throws URISyntaxException {

        Element root = element;
        Conference conference = new Conference();
        if (root.attributeValue("terminator") != null) {
            conference.setTerminator(root.attributeValue("terminator").charAt(0));
        }
        if (root.attributeValue("beep") != null) {
            conference.setBeep(Boolean.valueOf(root.attributeValue("timeout")));
        }
        if (root.attributeValue("mute") != null) {
            conference.setMute(Boolean.valueOf(root.attributeValue("mute")));
        }
        if (root.attributeValue("tone-passthrough") != null) {
            conference.setMute(Boolean.valueOf(root.attributeValue("tone-passthrough")));
        }
        if (root.attributeValue("id") != null) {
            conference.setVerbId(root.attributeValue("id"));
        }
        return conference;
    }

    private PromptItems extractPromptItems(Element node) throws URISyntaxException {

        PromptItems items = new PromptItems();
        @SuppressWarnings("unchecked")
        List<Element> elements = node.elements();
        for (Element element : elements) {
            if (element.getName().equals("audio")) {
                AudioItem item = new AudioItem();
                item.setUri(new URI(element.attributeValue("url")));
                items.add(item);
            } else {
                String xml = element.asXML();
                //TODO: Better namespaces cleanup
                xml = xml.replaceAll(" xmlns=\"[^\"]*\"", "");
                SsmlItem ssml = new SsmlItem(xml);
                items.add(ssml);
            }
        }
        return items;
    }

    @Override
    public Element toXML(Object object) {

        try {
            Document document = DocumentHelper.createDocument();
            if (object instanceof Offer) {
                createOffer(object, document);
            } else if (object instanceof EndEvent) {
                createEndEvent(object, document);
            } else if (object instanceof RingEvent) {
                createRingEvent(object, document);
            } else if (object instanceof AnswerEvent) {
                createAnswerEvent(object, document);
            } else if (object instanceof AcceptCommand) {
                createAcceptCommand(object, document);
            } else if (object instanceof AnswerCommand) {
                createAnswerCommand(object, document);
            } else if (object instanceof HangupCommand) {
                createHangupCommand(object, document);
            } else if (object instanceof RejectCommand) {
                createRejectCommand(object, document);
            } else if (object instanceof RedirectCommand) {
                createRedirectCommand(object, document);
            } else if (object instanceof Say) {
                createSay(object, document);
            } else if (object instanceof PauseCommand) {
                createPauseCommand(object, document);
            } else if (object instanceof ResumeCommand) {
                createResumeCommand(object, document);
            } else if (object instanceof StopCommand) {
                createStopCommand(object, document);
            } else if (object instanceof SayCompleteEvent) {
                createSayCompleteEvent(object, document);
            } else if (object instanceof Ask) {
                createAsk(object, document);
            } else if (object instanceof AskCompleteEvent) {
                createAskCompleteEvent(object, document);
            } else if (object instanceof Transfer) {
                createTransfer(object, document);
            } else if (object instanceof TransferCompleteEvent) {
                createTransferCompleteEvent(object, document);
            } else if (object instanceof Conference) {
                createConference(object, document);
            } else if (object instanceof ConferenceCompleteEvent) {
                createConferenceCompleteEvent(object, document);
            }

            return document.getRootElement();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    private void createAnswerEvent(Object object, Document document) {
        document
        .addElement(new QName("info", new Namespace("", "urn:xmpp:ozone:1")))
            .addElement("answer");
    }

    private void createRingEvent(Object object, Document document) {
        document
            .addElement(new QName("info", new Namespace("", "urn:xmpp:ozone:1")))
                .addElement("ring");
    }

    private void createEndEvent(Object object, Document document) {
        EndEvent event = (EndEvent)object;
        Element root = document.addElement(new QName("end", new Namespace("", "urn:xmpp:ozone:1")));
        root.addElement(event.getReason().name().toLowerCase());
        addHeaders(event.getHeaders(), root);
    }

    private Document createAcceptCommand(Object object, Document document) {

        AcceptCommand accept = (AcceptCommand) object;
        Element root = document.addElement(new QName("accept", new Namespace("", "urn:xmpp:ozone:1")));
        addHeaders(accept.getHeaders(), root);

        return document;
    }

    private void addHeaders(Map<String, String> map, Element node) {

        if (map != null) {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                Element header = node.addElement("header");
                header.addAttribute("name", entry.getKey());
                header.addAttribute("value", entry.getValue());
            }
        }
    }

    private Document createAnswerCommand(Object object, Document document) {

        AnswerCommand answer = (AnswerCommand) object;
        Element root = document.addElement(new QName("answer", new Namespace("", "urn:xmpp:ozone:1")));
        addHeaders(answer.getHeaders(), root);

        return document;
    }

    private Document createHangupCommand(Object object, Document document) {

        HangupCommand hangup = (HangupCommand) object;
        Element root = document.addElement(new QName("hangup", new Namespace("", "urn:xmpp:ozone:1")));
        addHeaders(hangup.getHeaders(), root);

        return document;
    }

    private Document createRejectCommand(Object object, Document document) {

        RejectCommand reject = (RejectCommand) object;
        Element root = document.addElement(new QName("reject", new Namespace("", "urn:xmpp:ozone:1")));
        root.addElement(reject.getReason().name().toLowerCase());
        addHeaders(reject.getHeaders(), root);

        return document;
    }

    private Document createRedirectCommand(Object object, Document document) {

        RedirectCommand redirect = (RedirectCommand) object;
        Element root = document.addElement(new QName("redirect", new Namespace("", "urn:xmpp:ozone:1")));
        root.addAttribute("to", redirect.getTo().toString());
        addHeaders(redirect.getHeaders(), root);

        return document;
    }

    private Document createOffer(Object object, Document document) {

        Offer offer = (Offer) object;

        Element root = document.addElement(new QName("offer", new Namespace("", "urn:xmpp:ozone:1")));
        root.addAttribute("to", offer.getTo().toString());
        root.addAttribute("from", offer.getFrom().toString());

        addHeaders(offer.getHeaders(), root);

        return document;
    }

    private Document createSay(Object object, Document document) throws Exception {

        Say say = (Say) object;
        Element root = document.addElement(new QName("say", new Namespace("", "urn:xmpp:ozone:say:1")));
        if (say.getVoice() != null) {
            root.addAttribute("voice", say.getVoice());
        }
        if (say.getPromptItems() != null) {
            if (say.getPromptItems() != null) {
                addPromptItems(say.getPromptItems(), root);
            }
        }
        return document;
    }

    private void addPromptItems(PromptItems items, Element root) throws DocumentException {

        for (PromptItem item : items) {
            if (item instanceof AudioItem) {
                Element audio = root.addElement("audio");
                audio.addAttribute("url", ((AudioItem) item).getUri().toString());
            } else if (item instanceof SsmlItem) {
                Document ssmlDoc = DocumentHelper.parseText(((SsmlItem) item).getText());
                root.add(ssmlDoc.getRootElement());
            }
        }
    }

    private Document createPauseCommand(Object object, Document document) throws Exception {
        document.addElement(new QName("pause", new Namespace("", "urn:xmpp:ozone:say:1")));
        return document;
    }

    private Document createResumeCommand(Object object, Document document) throws Exception {
        document.addElement(new QName("resume", new Namespace("", "urn:xmpp:ozone:say:1")));
        return document;
    }

    private Document createStopCommand(Object object, Document document) throws Exception {
        document.addElement(new QName("stop", new Namespace("", "urn:xmpp:ozone:1")));
        return document;
    }

    private Document createSayCompleteEvent(Object object, Document document) throws Exception {
        SayCompleteEvent sayComplete = (SayCompleteEvent) object;
        Element root = document.addElement(new QName("complete", new Namespace("", "urn:xmpp:ozone:say:1")));
        root.addAttribute("reason", sayComplete.getReason().toString());
        return document;
    }

    private Document createAsk(Object object, Document document) throws Exception {

        Ask ask = (Ask) object;
        Element root = document.addElement(new QName("ask", new Namespace("", "urn:xmpp:ozone:ask:1")));
        if (ask.getVoice() != null) {
            root.addAttribute("voice", ask.getVoice());
        }

        if (ask.getPromptItems() != null) {
            Element prompt = root.addElement("prompt");
            addPromptItems(ask.getPromptItems(), prompt);
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
            root.addAttribute("timeout", ask.getTimeout().toString());
        }
        root.addAttribute("bargein", String.valueOf(ask.isBargein()));

        return document;
    }

    private Document createAskCompleteEvent(Object object, Document document) throws Exception {

        AskCompleteEvent askComplete = (AskCompleteEvent) object;
        Element root = document.addElement(new QName("complete", new Namespace("", "urn:xmpp:ozone:ask:1")));
        root.addAttribute("reason", askComplete.getReason().toString().toLowerCase());
        root.addAttribute("confidence", Float.toString(askComplete.getConfidence()));
        root.addElement("concept").setText(askComplete.getConcept());
        root.addElement("interpretation").setText(askComplete.getInterpretation());
        root.addElement("utterance").setText(askComplete.getUtterance());
        root.addElement("nlsml").setText(askComplete.getNlsml());
        root.addElement("tag").setText(askComplete.getTag());
        
        return document;
    }

    private Document createTransfer(Object object, Document document) throws Exception {

        Transfer transfer = (Transfer) object;
        Element root = document.addElement(new QName("transfer", new Namespace("", "urn:xmpp:ozone:transfer:1")));
        if (transfer.getVoice() != null) {
            root.addAttribute("voice", transfer.getVoice());
        }

        addHeaders(transfer.getHeaders(), root);
        addPromptItems(transfer.getPromptItems(), root);

        if (transfer.getTerminator() != null) {
            root.addAttribute("terminator", transfer.getTerminator().toString());
        }
        if (transfer.getTimeout() != null) {
            root.addAttribute("timeout", transfer.getTimeout().toString());
        }
        if (transfer.getFrom() != null) {
            root.addAttribute("from", transfer.getFrom().toString());
        }
        if (transfer.getTo() != null) {
            for (URI uri : transfer.getTo()) {
                root.addElement("to").setText(uri.toString());
            }
        }

        return document;
    }
    
    private Document createTransferCompleteEvent(Object object, Document document) {
        TransferCompleteEvent tranferComplete = (TransferCompleteEvent) object;
        Element root = document.addElement(new QName("complete", new Namespace("", "urn:xmpp:ozone:transfer:1")));
        root.addAttribute("reason", tranferComplete.getReason().toString());
        return document;
    }

    private Document createConference(Object object, Document document) throws Exception {

        Conference conference = (Conference) object;
        Element root = document.addElement(new QName("conference", new Namespace("", "urn:xmpp:ozone:conference:1")));

        if (conference.getTerminator() != null) {
            root.addAttribute("terminator", conference.getTerminator().toString());
        }
        if (conference.getId() != null) {
            root.addAttribute("id", conference.getId());
        }
        root.addAttribute("beep", String.valueOf(conference.isBeep()));
        root.addAttribute("mute", String.valueOf(conference.isMute()));
        root.addAttribute("tone-passthrough", String.valueOf(conference.isTonePassthrough()));

        return document;
    }
    
    private Document createConferenceCompleteEvent(Object object, Document document) {
        ConferenceCompleteEvent conferenceComplete = (ConferenceCompleteEvent) object;
        Element root = document.addElement(new QName("complete", new Namespace("", "urn:xmpp:ozone:conference:1")));
        root.addAttribute("reason", conferenceComplete.getReason().toString());
        return document;
    }

}
