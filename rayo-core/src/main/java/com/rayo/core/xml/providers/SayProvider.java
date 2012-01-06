package com.rayo.core.xml.providers;

import java.net.URISyntaxException;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;

import com.rayo.core.verb.PauseCommand;
import com.rayo.core.verb.ResumeCommand;
import com.rayo.core.verb.Say;
import com.rayo.core.verb.SayCompleteEvent;
import com.rayo.core.verb.SayCompleteEvent.Reason;

public class SayProvider extends BaseProvider {

    // XML -> Object
    // ================================================================================

    private static final Namespace NAMESPACE = new Namespace("", "urn:xmpp:tropo:say:1");
    private static final Namespace COMPLETE_NAMESPACE = new Namespace("", "urn:xmpp:tropo:say:complete:1");

    private static final QName PAUSE_QNAME = new QName("pause", NAMESPACE);
    private static final QName RESUME_QNAME = new QName("resume", NAMESPACE);
    
    @Override
    protected Object processElement(Element element) throws Exception {
        if (element.getName().equals("say")) {
            return buildSay(element);
        } else if (PAUSE_QNAME.equals(element.getQName())) {
            return buildPauseCommand(element);
        } else if (RESUME_QNAME.equals(element.getQName())) {
            return buildResumeCommand(element);
        } else if (element.getNamespace().equals(RAYO_COMPONENT_NAMESPACE)) {
            return buildCompleteCommand(element);
        }
        return null;
    }

    private Object buildCompleteCommand(Element element) {
    	   	
        Element reasonElement = (Element)element.elements().get(0);
    	String reasonValue = reasonElement.getName().toUpperCase();
        Reason reason = Reason.valueOf(reasonValue);
        
        SayCompleteEvent complete = new SayCompleteEvent();
        complete.setReason(reason);
        return complete;
    }

    private Object buildSay(Element element) throws URISyntaxException {
        Say say = new Say();
        say.setPrompt(extractSsml(element));
        return say;
    }

    private Object buildPauseCommand(Element element) throws URISyntaxException {
        return new PauseCommand();
    }

    private Object buildResumeCommand(Element element) throws URISyntaxException {
        return new ResumeCommand();
    }

    // Object -> XML
    // ================================================================================

    @Override
    protected void generateDocument(Object object, Document document) throws Exception {

        if (object instanceof Say) {
            createSay((Say) object, document);
        } else if (object instanceof PauseCommand) {
            createPauseCommand((PauseCommand) object, document);
        } else if (object instanceof ResumeCommand) {
            createResumeCommand((ResumeCommand) object, document);
        } else if (object instanceof SayCompleteEvent) {
            createSayCompleteEvent((SayCompleteEvent) object, document);
        }
    }

    private void createSay(Say say, Document document) throws Exception {
        Element root = document.addElement(new QName("say", NAMESPACE));
        if (say.getPrompt() != null) {
            addSsml(say.getPrompt(), root);
        }
    }

    private void createPauseCommand(PauseCommand command, Document document) throws Exception {
        document.addElement(new QName("pause", NAMESPACE));
    }

    private void createResumeCommand(ResumeCommand command, Document document) throws Exception {
        document.addElement(new QName("resume", NAMESPACE));
    }

    private void createSayCompleteEvent(SayCompleteEvent event, Document document) throws Exception {
        addCompleteElement(document, event, COMPLETE_NAMESPACE);
    }
}
