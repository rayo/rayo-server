package com.tropo.core.xml.providers;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;

import com.tropo.core.verb.RecordCompleteEvent.Reason;
import com.tropo.core.verb.Record;
import com.tropo.core.verb.RecordCompleteEvent;
import com.tropo.core.verb.RecordPauseCommand;
import com.tropo.core.verb.RecordResumeCommand;
import com.tropo.core.verb.VerbCompleteEvent;
import com.tropo.core.verb.VerbCompleteReason;


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

    	if (element.attribute("append") !=  null) {
    		record.setAppend(toBoolean("append",element));
    	}
    	if (element.attribute("codec") !=  null) {
    		record.setCodec(element.attributeValue("codec"));
    	}
    	if (element.attribute("codec-params") !=  null) {
    		record.setCodecParameters(element.attributeValue("codec-params"));
    	}
    	if (element.attribute("dtmf-truncate") !=  null) {
    		record.setDtmfTruncate(toBoolean("dtmf-truncate", element));
    	}
    	if (element.attribute("final-timeout") !=  null) {
    		record.setFinalTimeout(toInteger("final-timeout", element));
    	}
    	if (element.attribute("format") !=  null) {
    		record.setFormat(element.attributeValue("format"));
    	} else {
    		record.setFormat("mp3");
    	}
    	if (element.attribute("initial-timeout") !=  null) {
    		record.setInitialTimeout(toInteger("initial-timeout",element));
    	}
    	if (element.attribute("max-length") !=  null) {
    		record.setMaxDuration(toInteger("max-length", element));
    	}
    	if (element.attribute("min-length") !=  null) {
    		record.setMinDuration(toInteger("min-length",element));
    	}
    	if (element.attribute("sample-rate") !=  null) {
    		record.setSampleRate(toInteger("sample-rate",element));
    	}
    	if (element.attribute("silence-terminate") !=  null) {
    		record.setSilenceTerminate(toBoolean("silence-terminate",element));
    	}
    	if (element.attribute("start-beep") !=  null) {
    		record.setStartBeep(toBoolean("start-beep", element));
    	}
    	if (element.attribute("start-pause-mode") !=  null) {
    		record.setStartPauseMode(toBoolean("start-pause-mode", element));
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
		}
	}
    
    private void createRecord(Record record, Document document) throws Exception {
    	
        Element root = document.addElement(new QName("record", NAMESPACE));
        if (record.getTo() != null) {
        	root.addAttribute("to", record.getTo().toString());
        }
        if (record.getAppend() != null) {
        	root.addAttribute("append", record.getAppend().toString());
        }

        if (record.getDtmfTruncate() != null) {
        	root.addAttribute("dtmf-truncate", record.getDtmfTruncate().toString());
        }
        if (record.getSilenceTerminate() != null) {
        	root.addAttribute("silence-terminate", record.getSilenceTerminate().toString());
        }
        if (record.getStartBeep() != null) {
        	root.addAttribute("start-beep", record.getStartBeep().toString());
        }
        if (record.getStartPauseMode() != null) {
        	root.addAttribute("start-pause-mode", record.getStartPauseMode().toString());
        }
        if (record.getCodec() != null) {
        	root.addAttribute("codec", record.getCodec());
        }
        if (record.getCodecParameters() != null) {
        	root.addAttribute("codec-params", record.getCodecParameters());
        }
        if (record.getFinalTimeout() != null) {
        	root.addAttribute("final-timeout", record.getFinalTimeout().toString());
        }
        if (record.getFormat() != null) {
        	root.addAttribute("format", record.getFormat());
        }
        if (record.getInitialTimeout() != null) {
        	root.addAttribute("initial-timeout", record.getInitialTimeout().toString());
        }
        if (record.getMaxDuration() != null) {
        	root.addAttribute("max-length", record.getMaxDuration().toString());
        }
        if (record.getMinDuration() != null) {
        	root.addAttribute("min-length", record.getMinDuration().toString());
        }
        if (record.getSampleRate() != null) {
        	root.addAttribute("sample-rate", record.getSampleRate().toString());
        }        
    }

    @Override
    public boolean handles(Class<?> clazz) {

        //TODO: Refactor out to spring configuration and put everything in the base provider class
        return clazz == Record.class ||
        	   clazz == RecordCompleteEvent.class ||
        	   clazz == RecordPauseCommand.class ||
        	   clazz == RecordResumeCommand.class;
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
