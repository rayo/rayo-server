package com.tropo.core.xml.providers;

import java.net.URISyntaxException;
import java.util.List;

import javax.media.mscontrol.join.Joinable.Direction;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;
import org.joda.time.Duration;

import com.tropo.core.AcceptCommand;
import com.tropo.core.AnswerCommand;
import com.tropo.core.AnsweredEvent;
import com.tropo.core.CallRejectReason;
import com.tropo.core.DialCommand;
import com.tropo.core.DtmfEvent;
import com.tropo.core.EndEvent;
import com.tropo.core.HangupCommand;
import com.tropo.core.JoinCommand;
import com.tropo.core.JoinDestinationType;
import com.tropo.core.JoinedEvent;
import com.tropo.core.OfferEvent;
import com.tropo.core.RedirectCommand;
import com.tropo.core.RejectCommand;
import com.tropo.core.RingingEvent;
import com.tropo.core.UnjoinCommand;
import com.tropo.core.UnjoinedEvent;
import com.tropo.core.validation.Messages;
import com.tropo.core.validation.ValidationException;
import com.tropo.core.verb.HoldCommand;
import com.tropo.core.verb.StopCommand;
import com.tropo.core.verb.UnholdCommand;
import com.tropo.core.xml.XmlProvider;
import com.voxeo.moho.Participant.JoinType;

public class RayoProvider extends BaseProvider {
	
	@Override
	protected Object processElement(Element element) throws Exception {

        String elementName = element.getName();
        
        if (elementName.equals("offer")) {
        	return buildOfferEvent(element);
        } else if (elementName.equals("accept")) {
            return buildAcceptCommand(element);
        } else if (elementName.equals("hold")) {
            return buildHoldCommand(element);
        } else if (elementName.equals("unhold")) {
            return buildUnholdCommand(element);            
        } else if (elementName.equals("join")) {
            return buildJoinCommand(element);            
        } else if (elementName.equals("unjoin")) {
            return buildUnjoinCommand(element);            
        } else if (elementName.equals("joined")) {
            return buildJoinedEvent(element);            
        } else if (elementName.equals("unjoined")) {
            return buildUnjoinedEvent(element);            
        } else if (elementName.equals("answer")) {
            return buildAnswerCommand(element);
        } else if (elementName.equals("hangup")) {
            return buildHangupCommand(element);
        } else if (elementName.equals("reject")) {
            return buildRejectCommand(element);
        } else if (elementName.equals("redirect")) {
            return buildRedirectCommand(element);
        } else if (elementName.equals("answered")) {
            return buildAnsweredEvent(element);
        } else if (elementName.equals("ringing")) {
            return buildRingingEvent(element);
        } else if (elementName.equals("end")) {
            return buildCallEnd(element);
        } else if (elementName.equals("dial")) {
            return buildDialCommand(element);
        } else if (element.getName().equals("stop")) {
            return buildStopCommand(element);
        } else if (element.getName().equals("complete")) {
            return buildCompleteEvent(element);
        } else if (element.getName().equals("dtmf")) {
            return buildDtmfEvent(element);
        }
        
        return null;
	}
	
    private Object buildCompleteEvent(Element element) {
        
    	// Complete events may have multiple children that belong to 
    	// a particular namespace. If that's the case then we need to 
    	// find the appropriate provider to unmarshall this element
        @SuppressWarnings("unchecked")
        List<Element> children = (List<Element>)element.elements();
        Namespace namespace = RAYO_COMPLETE_NAMESPACE;
        for(Element child:children) {
        	if (!child.getNamespace().equals(namespace)) {
        		namespace = child.getNamespace();
        		break;
        	}
        }

        if (namespace.equals(RAYO_COMPLETE_NAMESPACE)) {        
        	return toVerbCompleteEvent(children.get(0));
        } else {
        	XmlProvider provider = getManager().findProvider(namespace); 
        	return provider.fromXML(element);
        }
    }

    private Object buildDialCommand(Element element) {
    	
        DialCommand command = new DialCommand();
        command.setFrom(toURI(element.attributeValue("from")));
        command.setTo(toURI(element.attributeValue("to")));
        command.setHeaders(grabHeaders(element));
        Element joinElement = element.element("join"); 
        if (joinElement != null) {
        	command.setJoin(buildJoinCommand(joinElement));
        }
        return command;
    }
    
    private Object buildCallEnd(Element element) {
        throw new UnsupportedOperationException();
    }

    private Object buildAnsweredEvent(Element element) {
        return new AnsweredEvent(null);
    }

    private Object buildRingingEvent(Element element) {
        return new RingingEvent(null);
    }

    private Object buildOfferEvent(Element element) throws URISyntaxException {

        OfferEvent offer = new OfferEvent(element.attributeValue("callId"));
        offer.setFrom(toURI(element.attributeValue("from")));
        offer.setTo(toURI(element.attributeValue("to")));
        offer.setHeaders(grabHeaders(element));

        return offer;
    }

    private Object buildDtmfEvent(Element element) {
        return new DtmfEvent(null, element.attributeValue("signal"));
    }

    private Object buildAcceptCommand(Element element) throws URISyntaxException {

        AcceptCommand accept = new AcceptCommand(null);
        accept.setHeaders(grabHeaders(element));

        return accept;
    }

    private Object buildHoldCommand(Element element) {

        return new HoldCommand();
    }


    private Object buildUnholdCommand(Element element) {

        return new UnholdCommand();
    }

    JoinCommand buildJoinCommand(Element element) {
        
    	JoinCommand join = new JoinCommand();
    	if (element.attribute("media") != null) {
    		join.setMedia(toEnum(JoinType.class, "media", element));
    	}
    	
    	if (element.attribute("direction") != null) {
    		join.setDirection(toEnum(Direction.class, "direction", element));
    	}
    	
    	if (element.attribute("call-id") != null) {
    		join.setTo(element.attributeValue("call-id"));
    		join.setType(JoinDestinationType.CALL);
    	} else if (element.attribute("mixer-id") != null) {
    		join.setTo(element.attributeValue("mixer-id"));
    		join.setType(JoinDestinationType.MIXER);    		
    	} 

        return join;
    }

    private UnjoinCommand buildUnjoinCommand(Element element) {
        
    	UnjoinCommand unjoin = new UnjoinCommand();

    	if (element.attribute("call-id") != null) {
    		unjoin.setFrom(element.attributeValue("call-id"));
    		unjoin.setType(JoinDestinationType.CALL);
    	} else if (element.attribute("mixer-id") != null) {
    		unjoin.setFrom(element.attributeValue("mixer-id"));
    		unjoin.setType(JoinDestinationType.MIXER);    		
    	}
        return unjoin;
    }
    
    private Object buildJoinedEvent(Element element) {

    	if (element.attribute("call-id") != null) {
    		return new JoinedEvent(null,element.attributeValue("call-id"), JoinDestinationType.CALL);
    	} else if (element.attribute("mixer-id") != null) {
    		return new JoinedEvent(null,element.attributeValue("mixer-id"), JoinDestinationType.MIXER);
    	} else return new JoinedEvent(null,null,null);
    }

    private Object buildUnjoinedEvent(Element element) {

    	if (element.attribute("call-id") != null) {
    		return new UnjoinedEvent(null,element.attributeValue("call-id"), JoinDestinationType.CALL);
    	} else if (element.attribute("mixer-id") != null) {
    		return new UnjoinedEvent(null,element.attributeValue("mixer-id"), JoinDestinationType.MIXER);
    	} else return new JoinedEvent(null,null,null);
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

        RejectCommand reject = new RejectCommand();
        @SuppressWarnings("unchecked")
        List<Element> children = (List<Element>)element.elements();
        if(!children.isEmpty()) {
            Element reasonElement = children.get(0);
            try {
                reject.setReason(CallRejectReason.valueOf(reasonElement.getName().toUpperCase()));
            } catch (IllegalArgumentException iae) {
                throw new ValidationException(Messages.INVALID_REASON);
            }
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

    private Object buildStopCommand(Element element) throws URISyntaxException {
        return new StopCommand();
    }

    @Override
    protected void generateDocument(Object object, Document document) throws Exception {

        if (object instanceof OfferEvent) {
            createOfferEvent(object, document);
        } else if (object instanceof EndEvent) {
            createEndEvent(object, document);
        } else if (object instanceof RingingEvent) {
            createRingEvent(object, document);
        } else if (object instanceof AnsweredEvent) {
            createAnswerEvent(object, document);
        } else if (object instanceof AcceptCommand) {
            createAcceptCommand(object, document);
        } else if (object instanceof HoldCommand) {
            createHoldCommand(object, document);            
        } else if (object instanceof UnholdCommand) {
            createUnholdCommand(object, document);            
        } else if (object instanceof JoinCommand) {
            createJoinCommand(object, document);            
        } else if (object instanceof UnjoinCommand) {
            createUnjoinCommand(object, document);            
        } else if (object instanceof JoinedEvent) {
            createJoinedEvent(object, document);            
        } else if (object instanceof UnjoinedEvent) {
            createUnjoinedEvent(object, document);            
        } else if (object instanceof AnswerCommand) {
            createAnswerCommand(object, document);
        } else if (object instanceof HangupCommand) {
            createHangupCommand(object, document);
        } else if (object instanceof RejectCommand) {
            createRejectCommand(object, document);
        } else if (object instanceof RedirectCommand) {
            createRedirectCommand(object, document);
        } else if (object instanceof DialCommand) {
            createDialCommand(object, document);
        } else if (object instanceof StopCommand) {
            createStopCommand((StopCommand)object, document);
        } else if (object instanceof DtmfEvent) {
            createDtmfEvent((DtmfEvent)object, document);
        }
    }

    private void createDtmfEvent(DtmfEvent event, Document document) {
        document.addElement(new QName("dtmf", RAYO_NAMESPACE)).addAttribute("signal", event.getSignal());
    }

    private Document createDialCommand(Object object, Document document) {
        
    	DialCommand command = (DialCommand) object;
        Element root = document.addElement(new QName("dial", RAYO_NAMESPACE));
        root.addAttribute("to", command.getTo().toString());
        if (command.getFrom() != null) {
        	root.addAttribute("from", command.getFrom().toString());
        }
        addHeaders(command.getHeaders(), root);
        if (command.getJoin() != null) {
        	createJoinCommand(command.getJoin(), root);
        }
        return document;
    }
    
    private void createAnswerEvent(Object object, Document document) {
        document.addElement(new QName("answered", RAYO_NAMESPACE));
    }

    private void createRingEvent(Object object, Document document) {
        document.addElement(new QName("ringing", RAYO_NAMESPACE));
    }

    private void createEndEvent(Object object, Document document) {
        EndEvent event = (EndEvent)object;
        Element root = document.addElement(new QName("end", RAYO_NAMESPACE));
        Element cause = root.addElement(event.getReason().name().toLowerCase());
        if (event.getErrorText() != null) {
        	cause.setText(event.getErrorText());
        }
        addHeaders(event.getHeaders(), root);
    }

    private Document createAcceptCommand(Object object, Document document) {

        AcceptCommand accept = (AcceptCommand) object;
        Element root = document.addElement(new QName("accept", RAYO_NAMESPACE));
        addHeaders(accept.getHeaders(), root);

        return document;
    }

    private Document createHoldCommand(Object object, Document document) {

        document.addElement(new QName("hold", RAYO_NAMESPACE));

        return document;
    }

    private Document createUnholdCommand(Object object, Document document) {

        document.addElement(new QName("unhold", RAYO_NAMESPACE));

        return document;
    }

    void createJoinCommand(JoinCommand join, Element element) {
    	
        Element root = element.addElement(new QName("join", RAYO_NAMESPACE));
        internalCreateJoinCommand(join, root);
    }
    
    void createJoinCommand(Object join, Document document) {
    	
        Element root = document.addElement(new QName("join", RAYO_NAMESPACE));
        internalCreateJoinCommand((JoinCommand)join, root);
    }
    
    private void internalCreateJoinCommand(JoinCommand join, Element joinElement) {
    	
        if (join.getDirection() != null) {
        	joinElement.addAttribute("direction", join.getDirection().name().toLowerCase());        	
        }
        if (join.getMedia() != null) {
        	joinElement.addAttribute("media", join.getMedia().name().toLowerCase());
        }
        if (join.getTo() != null) {
        	if (join.getType() == JoinDestinationType.CALL) {
        		joinElement.addAttribute("call-id", join.getTo());
        	} else {
        		joinElement.addAttribute("mixer-id", join.getTo());        		
        	}
        }
    }
   
    private Document createUnjoinCommand(Object object, Document document) {

    	UnjoinCommand unjoin = (UnjoinCommand)object;
        Element unjoinElement = document.addElement(new QName("unjoin", RAYO_NAMESPACE));
        if (unjoin.getFrom() != null) {
        	if (unjoin.getType() == JoinDestinationType.CALL) {
        		unjoinElement.addAttribute("call-id", unjoin.getFrom());
        	} else {
        		unjoinElement.addAttribute("mixer-id", unjoin.getFrom());        		
        	}
        }

        return document;
    }
    
    private Document createUnjoinedEvent(Object object, Document document) {

    	UnjoinedEvent event = (UnjoinedEvent)object;
        Element unjoined = document.addElement(new QName("unjoined", RAYO_NAMESPACE));
        if (event.getFrom() != null) {
        	if (event.getType() == JoinDestinationType.CALL) {
        		unjoined.addAttribute("call-id", event.getFrom());
        	} else {
        		unjoined.addAttribute("mixer-id", event.getFrom());        		
        	}
        }

        return document;
    }

    private Document createJoinedEvent(Object object, Document document) {

    	JoinedEvent event = (JoinedEvent)object;
        Element joined = document.addElement(new QName("joined", RAYO_NAMESPACE));
        if (event.getTo() != null) {
        	if (event.getType() == JoinDestinationType.CALL) {
        		joined.addAttribute("call-id", event.getTo());
        	} else {
        		joined.addAttribute("mixer-id", event.getTo());        		
        	}
        }

        return document;
    }
    
    private Document createAnswerCommand(Object object, Document document) {

        AnswerCommand answer = (AnswerCommand) object;
        Element root = document.addElement(new QName("answer", RAYO_NAMESPACE));
        addHeaders(answer.getHeaders(), root);

        return document;
    }

    private Document createHangupCommand(Object object, Document document) {

        HangupCommand hangup = (HangupCommand) object;
        Element root = document.addElement(new QName("hangup", RAYO_NAMESPACE));
        addHeaders(hangup.getHeaders(), root);

        return document;
    }

    private Document createRejectCommand(Object object, Document document) {

        RejectCommand reject = (RejectCommand) object;
        Element root = document.addElement(new QName("reject", RAYO_NAMESPACE));
        root.addElement(reject.getReason().name().toLowerCase());
        addHeaders(reject.getHeaders(), root);

        return document;
    }

    private Document createRedirectCommand(Object object, Document document) {

        RedirectCommand redirect = (RedirectCommand) object;
        Element root = document.addElement(new QName("redirect", RAYO_NAMESPACE));
        root.addAttribute("to", redirect.getTo().toString());
        addHeaders(redirect.getHeaders(), root);

        return document;
    }
    
    private void createStopCommand(StopCommand command, Document document) throws Exception {
        document.addElement(new QName("stop", RAYO_COMPONENT_NAMESPACE));
    }

    private Document createOfferEvent(Object object, Document document) {

        OfferEvent offer = (OfferEvent) object;

        Element root = document.addElement(new QName("offer", RAYO_NAMESPACE));
        root.addAttribute("to", offer.getTo().toString());
        root.addAttribute("from", offer.getFrom().toString());

        addHeaders(offer.getHeaders(), root);

        return document;
    }

	@Override
	public boolean handles(Class<?> clazz) {

		//TODO: Refactor out to spring configuration and put everything in the base provider class
		return clazz == OfferEvent.class ||
			   clazz == EndEvent.class ||
			   clazz == RingingEvent.class ||
			   clazz == AnsweredEvent.class ||
			   clazz == AcceptCommand.class ||
			   clazz == AnswerCommand.class ||
			   clazz == HangupCommand.class ||
			   clazz == RejectCommand.class ||
			   clazz == RedirectCommand.class ||
	           clazz == StopCommand.class  ||
			   clazz == DialCommand.class ||
		       clazz == DtmfEvent.class ||
		       clazz == HoldCommand.class ||
		       clazz == UnholdCommand.class ||
		       clazz == JoinCommand.class ||
		       clazz == UnjoinCommand.class ||
		       clazz == JoinedEvent.class ||
		       clazz == UnjoinedEvent.class;
	}
}
