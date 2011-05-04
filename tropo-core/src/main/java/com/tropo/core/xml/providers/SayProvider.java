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
import com.tropo.core.verb.StopCommand;

public class SayProvider extends BaseProvider {

	@Override
	protected Object processElement(Element element) throws Exception {
		
		if (element.getName().equals("say")) {
			return buildSay(element);
		} else if (element.getName().equals("pause")) {
			return buildPauseCommand(element);
		} else if (element.getName().equals("resume")) {
			return buildResumeCommand(element);
		} else if (element.getName().equals("stop")) {
			return buildStopCommand(element);
		} else if (element.getName().equals("complete")) {
			return buildCompleteCommand(element);
		}
		return null;
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

	private Object buildResumeCommand(Element element)
			throws URISyntaxException {

		return new ResumeCommand();
	}

	private Object buildStopCommand(Element element) throws URISyntaxException {

		return new StopCommand();
	}

	private Object buildCompleteCommand(Element element)
			throws URISyntaxException {

		SayCompleteEvent sayComplete = new SayCompleteEvent();
		if (element.attributeValue("reason") != null) {
			sayComplete.setReason(SayCompleteEvent.Reason.valueOf(element
					.attributeValue("reason")));
		}
		if (element.element("error") != null) {
			sayComplete.setErrorText(element.elementText("error"));
		}
		return sayComplete;
	}

	@Override
	protected void generateDocument(Object object, Document document) throws Exception {

		if (object instanceof Say) {
			createSay(object, document);
		} else if (object instanceof PauseCommand) {
			createPauseCommand(object, document);
		} else if (object instanceof ResumeCommand) {
			createResumeCommand(object, document);
		} else if (object instanceof StopCommand) {
			createStopCommand(object, document);
		} else if (object instanceof SayCompleteEvent) {
			createSayCompleteEvent(object, document);
		}
	}

	private Document createSay(Object object, Document document)
			throws Exception {

		Say say = (Say) object;
		Element root = document.addElement(new QName("say", new Namespace("",
				"urn:xmpp:ozone:say:1")));
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

	private Document createPauseCommand(Object object, Document document)
			throws Exception {
		document.addElement(new QName("pause", new Namespace("",
				"urn:xmpp:ozone:say:1")));
		return document;
	}

	private Document createResumeCommand(Object object, Document document)
			throws Exception {
		document.addElement(new QName("resume", new Namespace("",
				"urn:xmpp:ozone:say:1")));
		return document;
	}

	private Document createStopCommand(Object object, Document document)
			throws Exception {
		document.addElement(new QName("stop", new Namespace("",
				"urn:xmpp:ozone:say:1")));
		return document;
	}

	private Document createSayCompleteEvent(Object object, Document document)
			throws Exception {

		SayCompleteEvent sayComplete = (SayCompleteEvent) object;
		Element root = document.addElement(new QName("complete", new Namespace(
				"", "urn:xmpp:ozone:say:1")));

		if (sayComplete.getReason() != null) {
			root.addAttribute("reason", sayComplete.getReason().toString());
		}

		if (sayComplete.getErrorText() != null) {
			root.addElement("error").setText(sayComplete.getErrorText());
		}

		return document;
	}

	@Override
	public boolean handles(Class<?> clazz) {

		//TODO: Refactor out to spring configuration and put everything in the base provider class
		return clazz == Say.class ||
			   clazz == PauseCommand.class ||
			   clazz == ResumeCommand.class ||
			   clazz == StopCommand.class ||
			   clazz == SayCompleteEvent.class;
	}
}
