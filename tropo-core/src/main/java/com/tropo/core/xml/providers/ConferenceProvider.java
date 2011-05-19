package com.tropo.core.xml.providers;

import java.net.URISyntaxException;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;

import com.tropo.core.verb.Conference;
import com.tropo.core.verb.ConferenceCompleteEvent;
import com.tropo.core.verb.KickCommand;
import com.tropo.core.verb.OffHoldEvent;
import com.tropo.core.verb.OnHoldEvent;
import com.tropo.core.verb.PromptItems;

public class ConferenceProvider extends BaseProvider {

    // XML -> Object
    // ================================================================================

    @Override
    protected Object processElement(Element element) throws Exception {
        if (element.getName().equals("conference")) {
            return buildConference(element);
        }
        else if (element.getName().equals("kick")) {
            return buildKick(element);
        }
        else if (element.getName().equals("complete")) {
            return buildCompleteCommand(element);
        }
        else if (element.getName().equals("on-hold")) {
            return new OnHoldEvent();
        }
        else if (element.getName().equals("off-hold")) {
            return new OffHoldEvent();
        }
        return null;
    }

    private Object buildConference(Element element) throws URISyntaxException {

        Element root = element;
        Conference conference = new Conference();
        
        if (root.attributeValue("name") != null) {
            conference.setRoomName(root.attributeValue("name"));
        }
        if (root.attributeValue("mute") != null) {
            conference.setMute(toBoolean(root.attributeValue("mute")));
        }
        if (root.attributeValue("terminator") != null) {
            conference.setTerminator(toTerminator(root.attributeValue("terminator")));
        }
        if (root.attributeValue("tone-passthrough") != null) {
            conference.setTonePassthrough(toBoolean(root.attributeValue("tone-passthrough")));
        }
        if (root.attributeValue("beep") != null) {
            conference.setBeep(toBoolean(root.attributeValue("beep")));
        }
        if (root.attributeValue("moderator") != null) {
            conference.setModerator(toBoolean(root.attributeValue("moderator")));
        }
        if (root.element("announcement") != null) {
            conference.setAnnouncement(extractPromptItems(root.element("announcement")));
        }
        if (root.element("music") != null) {
            conference.setHoldMusic(extractPromptItems(root.element("music")));
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

    private Object buildCompleteCommand(Element element) throws URISyntaxException {

        ConferenceCompleteEvent conferenceComplete = new ConferenceCompleteEvent();
        if (element.attributeValue("reason") != null) {
            conferenceComplete.setReason(ConferenceCompleteEvent.Reason.valueOf(element.attributeValue("reason")));
        }
        if (element.element("error") != null) {
            conferenceComplete.setErrorText(element.elementText("error"));
        }
        if(element.element("kick") != null) {
            conferenceComplete.setKickReason(element.elementText("kick"));
        }

        return conferenceComplete;
    }

    // Object -> XML
    // ================================================================================

    @Override
    protected void generateDocument(Object object, Document document) throws Exception {

        if (object instanceof Conference) {
            createConference(object, document);
        }
        else if (object instanceof ConferenceCompleteEvent) {
            createConferenceCompleteEvent(object, document);
        }
        else if (object instanceof KickCommand) {
            createKick((KickCommand) object, document);
        }
        else if (object instanceof OnHoldEvent) {
            createOnHoldEvent(object, document);
        }
        else if (object instanceof OffHoldEvent) {
            createOffHoldEvent(object, document);
        }
    }

    private void createConference(Object object, Document document) throws Exception {

        Conference conference = (Conference) object;
        Element root = document.addElement(new QName("conference", new Namespace("", "urn:xmpp:ozone:conference:1")));

        root.addAttribute("name", conference.getRoomName());
        root.addAttribute("mute", String.valueOf(conference.isMute()));
        if (conference.getTerminator() != null) {
            root.addAttribute("terminator", conference.getTerminator().toString());
        }
        root.addAttribute("tone-passthrough", String.valueOf(conference.isTonePassthrough()));
        root.addAttribute("beep", String.valueOf(conference.isBeep()));
        root.addAttribute("moderator", String.valueOf(conference.isModerator()));

        PromptItems announcement = conference.getAnnouncement();
        if(announcement != null) {
            addPromptItems(announcement, root.addElement("announcement"));
        }

        PromptItems holdMusic = conference.getHoldMusic();
        if(holdMusic != null) {
            addPromptItems(holdMusic, root.addElement("music"));
        }

    }

    private void createConferenceCompleteEvent(Object object, Document document) throws Exception {

        ConferenceCompleteEvent conferenceComplete = (ConferenceCompleteEvent) object;
        Element root = document.addElement(new QName("complete", new Namespace("", "urn:xmpp:ozone:conference:1")));

        if (conferenceComplete.getReason() != null) {
            root.addAttribute("reason", conferenceComplete.getReason().toString());
        }

        if (conferenceComplete.getErrorText() != null) {
            root.addElement("error").setText(conferenceComplete.getErrorText());
        }
        
        if(conferenceComplete.getKickReason() != null) {
            root.addElement("kick").setText(conferenceComplete.getKickReason());
        }
    }

    private void createKick(KickCommand command, Document document) throws Exception {
        Element element = document.addElement(new QName("kick", new Namespace("", "urn:xmpp:ozone:conference:1")));
        if (command.getReason() != null) {
            element.setText(command.getReason());
        }
    }

    private void createOnHoldEvent(Object object, Document document) {
        document.addElement(new QName("on-hold", new Namespace("", "urn:xmpp:ozone:conference:1")));
    }
    
    private void createOffHoldEvent(Object object, Document document) {
        document.addElement(new QName("off-hold", new Namespace("", "urn:xmpp:ozone:conference:1")));
    }
    
    @Override
    public boolean handles(Class<?> clazz) {

        //TODO: Refactor out to spring configuration and put everything in the base provider class
        return clazz == Conference.class 
            || clazz == KickCommand.class 
            || clazz == ConferenceCompleteEvent.class
            || clazz == OnHoldEvent.class
            || clazz == OffHoldEvent.class;
    }
}
