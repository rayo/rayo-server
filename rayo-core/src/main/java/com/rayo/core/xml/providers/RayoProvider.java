package com.rayo.core.xml.providers;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import javax.media.mscontrol.join.Joinable.Direction;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;

import com.rayo.core.AcceptCommand;
import com.rayo.core.AnswerCommand;
import com.rayo.core.AnsweredEvent;
import com.rayo.core.CallRef;
import com.rayo.core.CallRejectReason;
import com.rayo.core.DestroyMixerCommand;
import com.rayo.core.DialCommand;
import com.rayo.core.DtmfCommand;
import com.rayo.core.DtmfEvent;
import com.rayo.core.EndEvent;
import com.rayo.core.HangupCommand;
import com.rayo.core.JoinCommand;
import com.rayo.core.JoinDestinationType;
import com.rayo.core.JoinedEvent;
import com.rayo.core.OfferEvent;
import com.rayo.core.RedirectCommand;
import com.rayo.core.RejectCommand;
import com.rayo.core.RingingEvent;
import com.rayo.core.StartedSpeakingEvent;
import com.rayo.core.StoppedSpeakingEvent;
import com.rayo.core.UnjoinCommand;
import com.rayo.core.UnjoinedEvent;
import com.rayo.core.validation.Messages;
import com.rayo.core.validation.ValidationException;
import com.rayo.core.verb.HoldCommand;
import com.rayo.core.verb.MuteCommand;
import com.rayo.core.verb.StopCommand;
import com.rayo.core.verb.UnholdCommand;
import com.rayo.core.verb.UnmuteCommand;
import com.rayo.core.verb.VerbRef;
import com.rayo.core.xml.XmlProvider;
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
        } else if (elementName.equals("mute")) {
            return buildMuteCommand(element);
        } else if (elementName.equals("unmute")) {
            return buildUnmuteCommand(element);            
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
        } else if (element.getName().equals("started-speaking")) {
            return buildStartedSpeakingEvent(element);
        } else if (element.getName().equals("stopped-speaking")) {
            return buildStoppedSpeakingEvent(element);
        } else if (element.getName().equals("dtmf")) {
        	if (element.attribute("signal") != null) {
        		return buildDtmfEvent(element);
        	}
        	return buildDtmfCommand(element);
        } else if (element.getName().equals("destroy-if-empty")) {
        	return buildDestroyIfEmptyCommand(element);
        } else if (element.getName().equals("ref")) {
        	return buildCallRef(element);
        }
        
        return null;
	}
	
	private Object buildCallRef(Element element) {
		return new CallRef(element.attributeValue("id"));
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

        EndEvent event = new EndEvent(null,null);
        if (element.elements().size() > 0) {
        	Element cause = null;
        	for (Object item: element.elements()) {
        		if (!((Element)item).getName().equals("header")) {
        			cause = (Element)item;
        			break;
        		}
        	}
        	if (cause != null) {
	        	EndEvent.Reason reason = EndEvent.Reason.valueOf(EndEvent.Reason.class, cause.getName().toUpperCase());
	        	event.setReason(reason);
	        	if (cause.getText() != null) {
	        		event.setErrorText(cause.getText());
	        	}
        	}
        }
        Map<String,String> headers = grabHeaders(element);
        event.setHeaders(headers);
        
        return event;
    }

    private Object buildAnsweredEvent(Element element) {
        return new AnsweredEvent(null, grabHeaders(element));
    }

    private Object buildRingingEvent(Element element) {
        return new RingingEvent(null, grabHeaders(element));
    }
    
    private Object buildOfferEvent(Element element) throws URISyntaxException {

        OfferEvent offer = new OfferEvent(element.attributeValue("callId"));
        offer.setFrom(toURI(element.attributeValue("from")));
        offer.setTo(toURI(element.attributeValue("to")));
        offer.setHeaders(grabHeaders(element));

        return offer;
    }
    
    private Object buildStartedSpeakingEvent(Element element) throws URISyntaxException {

        StartedSpeakingEvent speaking = new StartedSpeakingEvent();
        speaking.setSpeakerId(element.attributeValue("call-id"));

        return speaking;
    }
    
    private Object buildStoppedSpeakingEvent(Element element) throws URISyntaxException {

    	StoppedSpeakingEvent speaking = new StoppedSpeakingEvent();
        speaking.setSpeakerId(element.attributeValue("call-id"));

        return speaking;
    }
    
    private Object buildDtmfCommand(Element element) {
        return new DtmfCommand(element.attributeValue("tones"));
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

    private Object buildMuteCommand(Element element) {

        return new MuteCommand();
    }


    private Object buildUnmuteCommand(Element element) {

        return new UnmuteCommand();
    }
    
    JoinCommand buildJoinCommand(Element element) {
        
    	JoinCommand join = new JoinCommand();
    	if (element.attribute("media") != null) {
    		join.setMedia(toEnum(JoinType.class, "media", element));
    	}
    	
    	if (element.attribute("direction") != null) {
    		join.setDirection(toEnum(Direction.class, "direction", element));
    	}

    	if (element.attribute("force") != null) {
    		join.setForce(toBoolean("force", element));
    	}

    	if (element.attribute("call-id") != null) {
    		join.setTo(element.attributeValue("call-id"));
    		join.setType(JoinDestinationType.CALL);
    	} else if (element.attribute("mixer-name") != null) {
    		join.setTo(element.attributeValue("mixer-name"));
    		join.setType(JoinDestinationType.MIXER);    		
    	} 

        return join;
    }

    private UnjoinCommand buildUnjoinCommand(Element element) {
        
    	UnjoinCommand unjoin = new UnjoinCommand();

    	if (element.attribute("call-id") != null) {
    		unjoin.setFrom(element.attributeValue("call-id"));
    		unjoin.setType(JoinDestinationType.CALL);
    	} else if (element.attribute("mixer-name") != null) {
    		unjoin.setFrom(element.attributeValue("mixer-name"));
    		unjoin.setType(JoinDestinationType.MIXER);    		
    	}
        return unjoin;
    }
    
    private Object buildJoinedEvent(Element element) {

    	if (element.attribute("call-id") != null) {
    		return new JoinedEvent(null,element.attributeValue("call-id"), JoinDestinationType.CALL);
    	} else if (element.attribute("mixer-name") != null) {
    		return new JoinedEvent(null,element.attributeValue("mixer-name"), JoinDestinationType.MIXER);
    	} else return new JoinedEvent(null,null,null);
    }

    private Object buildUnjoinedEvent(Element element) {

    	if (element.attribute("call-id") != null) {
    		return new UnjoinedEvent(null,element.attributeValue("call-id"), JoinDestinationType.CALL);
    	} else if (element.attribute("mixer-name") != null) {
    		return new UnjoinedEvent(null,element.attributeValue("mixer-name"), JoinDestinationType.MIXER);
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
        } else {
        	throw new ValidationException(Messages.MISSING_REASON);
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
    
    private Object buildDestroyIfEmptyCommand(Element element) {
    	
    	DestroyMixerCommand command = new DestroyMixerCommand();
    	return command;
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
        } else if (object instanceof MuteCommand) {
            createMuteCommand(object, document);            
        } else if (object instanceof UnmuteCommand) {
            createUnmuteCommand(object, document);            
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
        } else if (object instanceof DtmfCommand) {
            createDtmfCommand((DtmfCommand)object, document);
        } else if (object instanceof StartedSpeakingEvent) {
            createStartedSpeakingEvent((StartedSpeakingEvent)object, document);
        } else if (object instanceof StoppedSpeakingEvent) {
            createStoppedSpeakingEvent((StoppedSpeakingEvent)object, document);
        } else if (object instanceof DestroyMixerCommand) {
        	createDestroyIfEmptyCommand((DestroyMixerCommand)object, document);
        } else if (object instanceof VerbRef) {
        	createVerbRef((VerbRef)object, document);
        } else if (object instanceof CallRef) {
        	createCallRef((CallRef)object, document);
        }
    }

    private void createCallRef(CallRef ref, Document document) {
		Element root = document.addElement(new QName("ref", RAYO_NAMESPACE));
		root.addAttribute("id", ref.getCallId());
	}

    private void createVerbRef(VerbRef ref, Document document) {
		Element root = document.addElement(new QName("ref", RAYO_NAMESPACE));
		root.addAttribute("id", ref.getVerbId());
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
        
    	AnsweredEvent event = (AnsweredEvent)object;
    	Element root = document.addElement(new QName("answered", RAYO_NAMESPACE));
    	addHeaders(event.getHeaders(), root);
    }

    private void createRingEvent(Object object, Document document) {
    	
    	RingingEvent event = (RingingEvent)object;
        Element root = document.addElement(new QName("ringing", RAYO_NAMESPACE));
        addHeaders(event.getHeaders(), root);
        
    }
    
    private void createEndEvent(Object object, Document document) {
        EndEvent event = (EndEvent)object;
        Element root = document.addElement(new QName("end", RAYO_NAMESPACE));
        if (event.getReason() != null) {
	        Element cause = root.addElement(event.getReason().name().toLowerCase());
	        if (event.getErrorText() != null) {
	        	cause.setText(event.getErrorText());
	        }
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

    private Document createMuteCommand(Object object, Document document) {

        document.addElement(new QName("mute", RAYO_NAMESPACE));

        return document;
    }

    private Document createUnmuteCommand(Object object, Document document) {

        document.addElement(new QName("unmute", RAYO_NAMESPACE));

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
        if (join.getForce() != null) {
        	joinElement.addAttribute("force", join.getForce().toString());
        }
        if (join.getTo() != null) {
        	if (join.getType() == JoinDestinationType.CALL) {
        		joinElement.addAttribute("call-id", join.getTo());
        	} else {
        		joinElement.addAttribute("mixer-name", join.getTo());        		
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
        		unjoinElement.addAttribute("mixer-name", unjoin.getFrom());        		
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
        		unjoined.addAttribute("mixer-name", event.getFrom());        		
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
        		joined.addAttribute("mixer-name", event.getTo());        		
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
        if (reject.getReason() != null) {
        	root.addElement(reject.getReason().name().toLowerCase());
        }
        addHeaders(reject.getHeaders(), root);

        return document;
    }

    private Document createDtmfCommand(DtmfCommand dtmf, Document document) {

        Element root = document.addElement(new QName("dtmf", RAYO_NAMESPACE));
        root.addAttribute("tones", dtmf.getTones());

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
    
    private Document createStartedSpeakingEvent(Object object, Document document) {

        StartedSpeakingEvent event = (StartedSpeakingEvent) object;

        Element root = document.addElement(new QName("started-speaking", RAYO_NAMESPACE));
        root.addAttribute("call-id", event.getSpeakerId());

        return document;
    }
    
    private Document createStoppedSpeakingEvent(Object object, Document document) {

    	StoppedSpeakingEvent event = (StoppedSpeakingEvent) object;

        Element root = document.addElement(new QName("stopped-speaking", RAYO_NAMESPACE));
        root.addAttribute("call-id", event.getSpeakerId());

        return document;
    }
    
    private Document createDestroyIfEmptyCommand(Object object, Document document) {
    	
    	document.addElement(new QName("destroy-if-empty", RAYO_NAMESPACE));
    	
    	return document;
    }
}
