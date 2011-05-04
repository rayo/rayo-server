package com.tropo.core.xml;

import java.net.URISyntaxException;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;

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
import com.tropo.core.validation.Messages;
import com.tropo.core.validation.ValidationException;
import com.tropo.core.xml.providers.BaseProvider;

public class OzoneProvider extends BaseProvider {
	
	@Override
	protected Object processElement(Element element) throws Exception {

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
        } else if (element.getName().equals("info")) {
            return buildCallInfo(element);
        } else if (element.getName().equals("end")) {
            return buildCallEnd(element);
        }
        
        return null;
	}
	
    private Object buildCallEnd(Element element) {
        throw new UnsupportedOperationException();
    }

    private Object buildCallInfo(Element element) {
        throw new UnsupportedOperationException();
    }

    private Object buildOffer(Element element) throws URISyntaxException {

        Offer offer = new Offer(element.attributeValue("callId"));
        offer.setFrom(toURI(element.attributeValue("from")));
        offer.setTo(toURI(element.attributeValue("to")));
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
        try {
        	reject.setReason(CallRejectReason.valueOf(reasonElement.getName().toUpperCase()));
        } catch (IllegalArgumentException iae) {
        	throw new ValidationException(Messages.INVALID_REASON);
        }
        reject.setHeaders(grabHeaders(element));

        return reject;
    }

    private Object buildRedirectCommand(Element element) throws URISyntaxException {

        RedirectCommand reject = new RedirectCommand(null);
        reject.setTo(toURI(element.attributeValue("to")));
        reject.setHeaders(grabHeaders(element));

        return reject;
    }

    @Override
    protected void generateDocument(Object object, Document document) throws Exception {

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
        root.addAttribute("callId", offer.getCallId());

        addHeaders(offer.getHeaders(), root);

        return document;
    }

	@Override
    public boolean handles(Element element) {

		//TODO: Refactor out to spring configuration and put everything in the base provider class
		return element.getNamespace().getURI().equals(Namespaces.OZONE);
    }


	@Override
	public boolean handles(Class<?> clazz) {

		//TODO: Refactor out to spring configuration and put everything in the base provider class
		return clazz == Offer.class ||
			   clazz == EndEvent.class ||
			   clazz == RingEvent.class ||
			   clazz == AnswerEvent.class ||
			   clazz == AcceptCommand.class ||
			   clazz == AnswerCommand.class ||
			   clazz == HangupCommand.class ||
			   clazz == RejectCommand.class ||
			   clazz == RedirectCommand.class;
	}
}
