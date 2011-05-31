package com.tropo.core.xml.providers;

import java.net.URISyntaxException;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;

import com.tropo.core.verb.PauseCommand;
import com.tropo.core.verb.ResumeCommand;
import com.tropo.core.verb.Say;
import com.tropo.core.verb.SayCompleteEvent;
import com.tropo.core.verb.SayCompleteEvent.Reason;

public class SayProvider extends BaseProvider {

    // XML -> Object
    // ================================================================================

    private static final Namespace NAMESPACE = new Namespace("", "urn:xmpp:ozone:say:1");
    private static final Namespace COMPLETE_NAMESPACE = new Namespace("", "urn:xmpp:ozone:say:complete:1");

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
        } else if (element.getNamespace().equals(COMPLETE_NAMESPACE)) {
            return buildCompleteCommand(element);
        }
        return null;
    }

    private Object buildCompleteCommand(Element element) {
        String reasonValue = element.getName().toUpperCase();
        Reason reason = Reason.valueOf(reasonValue);
        return new SayCompleteEvent(null, reason);
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

    @Override
    public boolean handles(Class<?> clazz) {

        //TODO: Refactor out to spring configuration and put everything in the base provider class
        return clazz == Say.class || 
               clazz == PauseCommand.class || 
               clazz == ResumeCommand.class || 
               clazz == SayCompleteEvent.class;
    }
}
