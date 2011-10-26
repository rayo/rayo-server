package com.rayo.core.xml.providers;

import java.net.URISyntaxException;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;

import com.rayo.core.FinishedSpeakingEvent;
import com.rayo.core.SpeakingEvent;
import com.rayo.core.verb.Conference;
import com.rayo.core.verb.ConferenceCompleteEvent;
import com.rayo.core.verb.KickCommand;
import com.rayo.core.verb.OffHoldEvent;
import com.rayo.core.verb.OnHoldEvent;
import com.rayo.core.verb.Ssml;

public class ConferenceProvider extends BaseProvider {

	private static final Namespace NAMESPACE = new Namespace("",
			"urn:xmpp:tropo:conference:1");
	private static final Namespace COMPLETE_NAMESPACE = new Namespace("",
			"urn:xmpp:tropo:conference:complete:1");

	// XML -> Object
	// ================================================================================

	@Override
	protected Object processElement(Element element) throws Exception {
		if (element.getName().equals("conference")) {
			return buildConference(element);
		} else if (element.getName().equals("kick")) {
			return buildKick(element);
		} else if (element.getName().equals("on-hold")) {
			return new OnHoldEvent();
		} else if (element.getName().equals("off-hold")) {
			return new OffHoldEvent();
		} else if (element.getName().equals("started-speaking")) {
			return buildSpeakingEvent(element);
		} else if (element.getName().equals("stopped-speaking")) {
			return buildFinishedSpeakingEvent(element);
		}
		return null;
	}

	private Object buildSpeakingEvent(Element element) {
		return new SpeakingEvent(new Conference(), element.attributeValue("call-id"));
	}

	private Object buildFinishedSpeakingEvent(Element element) {
		return new FinishedSpeakingEvent(new Conference(), element.attributeValue("call-id"));
	}

	private Object buildConference(Element element) throws URISyntaxException {

		Element root = element;
		Conference conference = new Conference();

		if (root.attributeValue("name") != null) {
			conference.setRoomName(root.attributeValue("name"));
		}
		if (root.attributeValue("mute") != null) {
			conference.setMute(toBoolean("mute", element));
		}
		if (root.attributeValue("terminator") != null) {
			conference.setTerminator(toTerminator(root
					.attributeValue("terminator")));
		}
		if (root.attributeValue("tone-passthrough") != null) {
			conference
					.setTonePassthrough(toBoolean("tone-passthrough", element));
		}
		if (root.attributeValue("beep") != null) {
			conference.setBeep(toBoolean("beep", element));
		}
		if (root.attributeValue("moderator") != null) {
			conference.setModerator(toBoolean("moderator", element));
		}
		if (root.element("announcement") != null) {
			conference
					.setAnnouncement(extractSsml(root.element("announcement")));
		}
		if (root.element("music") != null) {
			conference.setHoldMusic(extractSsml(root.element("music")));
		}

		return conference;
	}

	private Object buildKick(Element element) throws URISyntaxException {
		KickCommand command = new KickCommand();
		Element kickElement = element.element("kick");
		if (kickElement != null) {
			command.setReason(kickElement.getText());
		}
		return command;
	}

	// This will eventually need to be called from the RayoProvider since
	// <complete>is now under the main rayo namespace

	// private Object buildCompleteCommand(Element element) throws
	// URISyntaxException {
	//
	// ConferenceCompleteEvent conferenceComplete = new
	// ConferenceCompleteEvent();
	// if (element.attributeValue("reason") != null) {
	// conferenceComplete.setReason(ConferenceCompleteEvent.Reason.valueOf(element.attributeValue("reason")));
	// }
	// if (element.element("error") != null) {
	// conferenceComplete.setErrorText(element.elementText("error"));
	// }
	// if(element.element("kick") != null) {
	// conferenceComplete.setKickReason(element.elementText("kick"));
	// }
	//
	// return conferenceComplete;
	// }

	// Object -> XML
	// ================================================================================

	@Override
	protected void generateDocument(Object object, Document document)
			throws Exception {

		if (object instanceof Conference) {
			createConference(object, document);
		} else if (object instanceof ConferenceCompleteEvent) {
			createConferenceCompleteEvent((ConferenceCompleteEvent) object,
					document);
		} else if (object instanceof KickCommand) {
			createKick((KickCommand) object, document);
		} else if (object instanceof OnHoldEvent) {
			createOnHoldEvent(object, document);
		} else if (object instanceof OffHoldEvent) {
			createOffHoldEvent(object, document);
        } else if (object instanceof SpeakingEvent) {
            createSpeakingEvent((SpeakingEvent)object, document);
        } else if (object instanceof FinishedSpeakingEvent) {
            createFinishedSpeakingEvent((FinishedSpeakingEvent)object, document);
        }
	}

	private void createConference(Object object, Document document)
			throws Exception {

		Conference conference = (Conference) object;
		Element root = document.addElement(new QName("conference", NAMESPACE));

		root.addAttribute("name", conference.getRoomName());
		root.addAttribute("mute", String.valueOf(conference.isMute()));
		if (conference.getTerminator() != null) {
			root.addAttribute("terminator", conference.getTerminator()
					.toString());
		}
		root.addAttribute("tone-passthrough",
				String.valueOf(conference.isTonePassthrough()));
		root.addAttribute("beep", String.valueOf(conference.isBeep()));
		root.addAttribute("moderator", String.valueOf(conference.isModerator()));

		Ssml announcement = conference.getAnnouncement();
		if (announcement != null) {
			addSsml(announcement, root.addElement("announcement"));
		}

		Ssml holdMusic = conference.getHoldMusic();
		if (holdMusic != null) {
			addSsml(holdMusic, root.addElement("music"));
		}

	}

	private void createConferenceCompleteEvent(ConferenceCompleteEvent event,
			Document document) throws Exception {

		Element reasonElement = addCompleteElement(document, event,
				COMPLETE_NAMESPACE);

		// If Kick set reason
		if (event.getKickReason() != null) {
			reasonElement.setText(event.getKickReason());
		}
	}

	private void createKick(KickCommand command, Document document)
			throws Exception {
		Element element = document.addElement(new QName("kick", NAMESPACE));
		if (command.getReason() != null) {
			element.setText(command.getReason());
		}
	}

	private void createOnHoldEvent(Object object, Document document) {
		document.addElement(new QName("on-hold", NAMESPACE));
	}

	private void createOffHoldEvent(Object object, Document document) {
		document.addElement(new QName("off-hold", NAMESPACE));
	}

	private void createSpeakingEvent(SpeakingEvent event, Document document) {
		
		Element element = document.addElement(new QName("started-speaking", NAMESPACE));
		element.addAttribute("call-id", event.getSpeakerId());
	}

	private void createFinishedSpeakingEvent(FinishedSpeakingEvent event, Document document) {
		Element element = document.addElement(new QName("stopped-speaking", NAMESPACE));
		element.addAttribute("call-id", event.getSpeakerId());
	}

	@Override
	public boolean handles(Class<?> clazz) {

		// TODO: Refactor out to spring configuration and put everything in the
		// base provider class
		return clazz == Conference.class || clazz == KickCommand.class
				|| clazz == ConferenceCompleteEvent.class
				|| clazz == OnHoldEvent.class || clazz == OffHoldEvent.class
				|| clazz == SpeakingEvent.class
				|| clazz == FinishedSpeakingEvent.class;
	}
}
