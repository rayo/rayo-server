package com.tropo.core.xml.providers;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;

import com.tropo.core.verb.Transfer;
import com.tropo.core.verb.TransferCompleteEvent;

public class TransferProvider extends BaseProvider {

    private static final Namespace NAMESPACE = new Namespace("", "urn:xmpp:ozone:transfer:1");
    private static final Namespace COMPLETE_NAMESPACE = new Namespace("", "urn:xmpp:ozone:transfer:complete:1");

	@Override
	protected Object processElement(Element element) throws Exception {
		
		if (element.getName().equals("transfer")) {
            return buildTransfer(element);
		}
		
		return null;
	}
	
    @SuppressWarnings("unchecked")
    private Object buildTransfer(Element element) throws URISyntaxException {

		Element root = element;
		Transfer transfer = new Transfer();
		if (root.attributeValue("terminator") != null) {
			transfer.setTerminator(toTerminator(root.attributeValue("terminator")));
		}		
		if (root.attributeValue("timeout") !=  null) {
			transfer.setTimeout(toTimeout(root.attributeValue("timeout")));
		}
		if (root.attributeValue("answer-on-media") != null) {
			transfer.setAnswerOnMedia(toBoolean(root.attributeValue("answer-on-media")));
		}
		
		if(root.element("ring") != null) {
		    transfer.setRingbackTone(extractSsml(root.element("ring")));
		}

		if (root.attributeValue("from") != null) {
			transfer.setFrom(toURI(root.attributeValue("from")));
		}
		if (root.element("to") != null || root.attributeValue("to") != null) {
			List<URI> uriList = new ArrayList<URI>();
			String to = root.attributeValue("to");
			if (to != null && !to.trim().equals("")) {
				uriList.add(toURI(to));
			}
			List<Element> elements = root.elements("to");
			for(Element e: elements) {
				if (!e.getText().equals("")) {
					uriList.add(toURI(e.getText()));
				}
			}
			transfer.setTo(uriList);
		}
		
		transfer.setHeaders(grabHeaders(root));
		
		return transfer;

    }
    
    // This will eventually need to be called from the OzoneProvider since <complete>is now under the main ozone namespace
    
    //private Object buildCompleteCommand(Element element) throws URISyntaxException {
    //
    //	TransferCompleteEvent transferComplete = new TransferCompleteEvent();
    //	if (element.attributeValue("reason") != null) {
    //		transferComplete.setReason(TransferCompleteEvent.Reason.valueOf(element.attributeValue("reason")));			
    //	}		
    //	if (element.element("error") != null) {
    //		transferComplete.setErrorText(element.elementText("error"));
    //	}
    //	
    //	return transferComplete;
    //}	

	@Override
	protected void generateDocument(Object object, Document document) throws Exception {
        if (object instanceof Transfer) {
            createTransfer(object, document);
        } else if (object instanceof TransferCompleteEvent) {
            createTransferCompleteEvent((TransferCompleteEvent)object, document);
        }
    }

    private void createTransfer(Object object, Document document) throws Exception {

		Transfer transfer = (Transfer)object;
		Element root = document.addElement(new QName("transfer", NAMESPACE));

		addHeaders(transfer.getHeaders(), document.getRootElement());
		addSsml(transfer.getRingbackTone(), document.getRootElement());
		
		if (transfer.getTerminator() != null) {
			root.addAttribute("terminator", transfer.getTerminator().toString());
		}
		if (transfer.getTimeout() != null) {
			root.addAttribute("timeout", Long.toString(transfer.getTimeout().getMillis()));
		}
		if (transfer.getFrom() != null) {
			root.addAttribute("from", transfer.getFrom().toString());
		}
		if (transfer.getTo() != null) {
			if (transfer.getTo().size() == 1) {
				root.addAttribute("to", transfer.getTo().get(0).toString());
			} else {
				for (URI uri: transfer.getTo()) {
					root.addElement("to").setText(uri.toString());
				}
			}
		}
		root.addAttribute("answer-on-media", String.valueOf(transfer.isAnswerOnMedia()));
	}
    
	private void createTransferCompleteEvent(TransferCompleteEvent event, Document document) throws Exception {
	    addCompleteElement(document, event, COMPLETE_NAMESPACE);
	}

	@Override
	public boolean handles(Class<?> clazz) {
		//TODO: Refactor out to spring configuration and put everything in the base provider class
		return clazz == Transfer.class ||
			   clazz == TransferCompleteEvent.class;
	}
}
