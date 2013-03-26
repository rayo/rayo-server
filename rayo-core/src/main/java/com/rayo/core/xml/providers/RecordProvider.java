package com.rayo.core.xml.providers;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;

import com.rayo.core.verb.Record;
import com.rayo.core.verb.RecordCompleteEvent;
import com.rayo.core.verb.RecordPauseCommand;
import com.rayo.core.verb.RecordResumeCommand;
import com.rayo.core.verb.VerbCompleteEvent;
import com.rayo.core.verb.VerbCompleteReason;
import com.rayo.core.verb.RecordCompleteEvent.Reason;


public class RecordProvider extends BaseProvider {

    // XML -> Object
    // ================================================================================

    private static final Namespace NAMESPACE = new Namespace("", "urn:xmpp:rayo:record:1");
    private static final Namespace COMPLETE_NAMESPACE = new Namespace("", "urn:xmpp:rayo:record:complete:1");
    
    private static final QName PAUSE_QNAME = new QName("pause", NAMESPACE);
    private static final QName RESUME_QNAME = new QName("resume", NAMESPACE);
    
    @Override
    protected Object processElement(Element element) throws Exception {
        if (element.getName().equals("record")) {
            return buildRecord(element);
        } else if (PAUSE_QNAME.equals(element.getQName())) {
            return buildPauseCommand(element);
        } else if (RESUME_QNAME.equals(element.getQName())) {
            return buildResumeCommand(element);
        } else if (RAYO_COMPONENT_NAMESPACE.equals(element.getNamespace())) {
            return buildCompleteCommand(element);
        }
        return null;
    }

    private Object buildPauseCommand(Element element) throws URISyntaxException {
        return new RecordPauseCommand();
    }

    private Object buildResumeCommand(Element element) throws URISyntaxException {
        return new RecordResumeCommand();
    }
    
    private Object buildRecord(Element element) throws URISyntaxException {
        
    	Record record = new Record();
    	if (element.attribute("to") !=  null) {
    		record.setTo(toURI(element.attributeValue("to")));
    	}    	
    	if (element.attribute("final-timeout") !=  null) {
    		record.setFinalTimeout(toDuration("final-timeout", element));
    	}
    	if (element.attribute("format") !=  null) {
    		record.setFormat(element.attributeValue("format"));
    	}

    	if (element.attribute("initial-timeout") !=  null) {
    		record.setInitialTimeout(toDuration("initial-timeout",element));
    	}
    	if (element.attribute("max-duration") !=  null) {
    		record.setMaxDuration(toDuration("max-duration", element));
    	}
    	if (element.attribute("start-beep") !=  null) {
    		record.setStartBeep(toBoolean("start-beep", element));
    	}
    	if (element.attribute("stop-beep") !=  null) {
    		record.setStopBeep(toBoolean("stop-beep", element));
    	}
    	if (element.attribute("start-paused") !=  null) {
    		record.setStartPaused(toBoolean("start-paused", element));
    	}
    	if (element.attribute("duplex") != null) {
    		record.setDuplex(toBoolean("duplex", element));
    	}
        return record;
    }
    
    private Object buildCompleteCommand(Element element) throws URISyntaxException {
    	
    	RecordCompleteEvent event = new RecordCompleteEvent();

    	@SuppressWarnings("unchecked")
        List<Element> children = (List<Element>)element.elements();
    	for (Element child: children) {
    		if (child.getName().equals("recording")) {
    			event.setUri(new URI(child.attributeValue("uri")));
    			if (child.attribute("duration") != null) {
    				event.setDuration(toDuration("duration", child));
    			}
    			if (child.attribute("size") != null) {
    				event.setSize(toLong("size", child));
    			}
    		} else {
    	    	String reasonValue = child.getName().toUpperCase();
    	        event.setReason(findReason(reasonValue));    			
    		}
    	}
            	
    	return event;
    }
    
    // Object -> XML
    // ================================================================================

    @Override
    protected void generateDocument(Object object, Document document) throws Exception {

        if (object instanceof Record) {
            createRecord((Record) object, document);
        } else if (object instanceof RecordCompleteEvent) {
        	createRecordCompleteEvent((RecordCompleteEvent) object, document);
        } else if (object instanceof RecordPauseCommand) {
            createPauseCommand((RecordPauseCommand) object, document);
        } else if (object instanceof RecordResumeCommand) {
            createResumeCommand((RecordResumeCommand) object, document);
        }
    }
    
    private void createPauseCommand(RecordPauseCommand command, Document document) throws Exception {
        document.addElement(new QName("pause", NAMESPACE));
    }

    private void createResumeCommand(RecordResumeCommand command, Document document) throws Exception {
        document.addElement(new QName("resume", NAMESPACE));
    }
    
	private void createRecordCompleteEvent(RecordCompleteEvent event, Document document) throws Exception {
	    
		addCompleteElement(document, event, COMPLETE_NAMESPACE);
		if (event.getUri() != null) {
			Element completeElement = document.getRootElement().addElement("recording", RecordProvider.COMPLETE_NAMESPACE.getURI());
			completeElement.addAttribute("uri", event.getUri().toString());
			
			completeElement.addAttribute("size", String.valueOf(event.getSize()));
			if (event.getDuration() != null) {
				completeElement.addAttribute("duration", Long.toString(event.getDuration().getMillis()));
			}
		}
	}
    
    private void createRecord(Record record, Document document) throws Exception {
    	
        Element root = document.addElement(new QName("record", NAMESPACE));
        if (record.getTo() != null) {
        	root.addAttribute("to", record.getTo().toString());
        }        
        if (record.getStartBeep() != null) {
        	root.addAttribute("start-beep", record.getStartBeep().toString());
        }
        if (record.getStopBeep() != null) {
        	root.addAttribute("stop-beep", record.getStopBeep().toString());
        }
        if (record.getStartPaused() != null) {
        	root.addAttribute("start-paused", record.getStartPaused().toString());
        }
        if (record.getFinalTimeout() != null) {
        	root.addAttribute("final-timeout", String.valueOf(record.getFinalTimeout().getMillis()));
        }
        if (record.getFormat() != null) {
        	root.addAttribute("format", record.getFormat());
        }
        if (record.getInitialTimeout() != null) {
        	root.addAttribute("initial-timeout", String.valueOf(record.getInitialTimeout().getMillis()));
        }
        if (record.getMaxDuration() != null) {
        	root.addAttribute("max-duration", String.valueOf(record.getMaxDuration().getMillis()));
        }
        if (record.getDuplex() != null) {
        	root.addAttribute("duplex", String.valueOf(record.getDuplex()));
        }
    }
    
    private VerbCompleteReason findReason(String reasonValue) {
    	
    	for (Reason reason: Reason.values()) {
    		if (reason.toString().equals(reasonValue)) {
    			return reason;
    		}
    	}
    	return VerbCompleteEvent.Reason.valueOf(reasonValue);
    }
}
