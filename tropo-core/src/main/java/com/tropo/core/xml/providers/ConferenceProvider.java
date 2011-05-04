package com.tropo.core.xml.providers;

import java.net.URISyntaxException;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;

import com.tropo.core.verb.Conference;
import com.tropo.core.verb.ConferenceCompleteEvent;
import com.tropo.core.verb.KickCommand;
import com.tropo.core.verb.PauseCommand;
import com.tropo.core.verb.ResumeCommand;
import com.tropo.core.verb.Say;
import com.tropo.core.verb.SayCompleteEvent;
import com.tropo.core.verb.StopCommand;
import com.tropo.core.xml.Namespaces;

public class ConferenceProvider extends BaseProvider {

	@Override
	protected Object processElement(Element element) throws Exception {

        if (element.getName().equals("conference")) {
            return buildConference(element);
        } else if (element.getName().equals("kick")) {
			return buildKick(element);
		} else if (element.getName().equals("complete")) {
			return buildCompleteCommand(element);
		}
		return null;
	}

    private Object buildConference(Element element) throws URISyntaxException {

        Element root = element;
        Conference conference = new Conference();
        if (root.attributeValue("terminator") != null) {
            conference.setTerminator(toTerminator(root.attributeValue("terminator")));
        }
        if (root.attributeValue("beep") != null) {
            conference.setBeep(toBoolean(root.attributeValue("beep")));
        }
        if (root.attributeValue("mute") != null) {
            conference.setMute(toBoolean(root.attributeValue("mute")));
        }
        if (root.attributeValue("tone-passthrough") != null) {
            conference.setTonePassthrough(toBoolean(root.attributeValue("tone-passthrough")));
        }
        if (root.attributeValue("id") != null) {
            conference.setRoomName(root.attributeValue("id"));
        }
        return conference;
    }

	private Object buildKick(Element element) throws URISyntaxException {
		
		return new KickCommand();		
	}
	
	private Object buildCompleteCommand(Element element) throws URISyntaxException {
		
		ConferenceCompleteEvent conferenceComplete = new ConferenceCompleteEvent();
		if (element.attributeValue("reason") != null) {
			conferenceComplete.setReason(ConferenceCompleteEvent.Reason.valueOf(element.attributeValue("reason")));			
		}		
		if (element.element("error") != null) {
			conferenceComplete.setErrorText(element.elementText("error"));
		}
		
		return conferenceComplete;
	}	

	@Override
	protected void generateDocument(Object object, Document document) throws Exception {

		if (object instanceof Conference) {
            createConference(object, document);
        } else if (object instanceof ConferenceCompleteEvent) {
            createConferenceCompleteEvent(object, document);
        } else if (object instanceof KickCommand) {
            createKick(object, document);
        }
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
    
	private Document createConferenceCompleteEvent(Object object, Document document) throws Exception {
		
		ConferenceCompleteEvent conferenceComplete = (ConferenceCompleteEvent)object;
		Element root = document.addElement(new QName("complete", new Namespace("","urn:xmpp:ozone:conference:1")));
		
		if (conferenceComplete.getReason() != null) {
			root.addAttribute("reason", conferenceComplete.getReason().toString());
		}
		
		if (conferenceComplete.getErrorText() != null) {
			root.addElement("error").setText(conferenceComplete.getErrorText());
		}		
		return document;
	}
    
	private Document createKick(Object object, Document document) throws Exception {
		
		document.addElement(new QName("kick", new Namespace("","urn:xmpp:ozone:conference:1")));
		return document;
	}

	@Override
    public boolean handles(Element element) {

		//TODO: Refactor out to spring configuration and put everything in the base provider class
		return element.getNamespace().getURI().equals(Namespaces.CONFERENCE);
    }


	@Override
	public boolean handles(Class<?> clazz) {

		//TODO: Refactor out to spring configuration and put everything in the base provider class
		return clazz == Conference.class ||
			   clazz == KickCommand.class ||
			   clazz == ConferenceCompleteEvent.class;
	}


}
