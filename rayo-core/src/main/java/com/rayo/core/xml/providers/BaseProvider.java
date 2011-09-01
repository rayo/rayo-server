package com.rayo.core.xml.providers;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.Node;
import org.dom4j.QName;
import org.dom4j.Text;
import org.joda.time.Duration;

import com.rayo.core.validation.Messages;
import com.rayo.core.validation.ValidationException;
import com.rayo.core.validation.Validator;
import com.rayo.core.verb.InputMode;
import com.rayo.core.verb.Ssml;
import com.rayo.core.verb.VerbCompleteEvent;
import com.rayo.core.verb.VerbCompleteReason;
import com.rayo.core.verb.VerbCompleteEvent.Reason;
import com.rayo.core.xml.XmlProvider;
import com.rayo.core.xml.XmlProviderManager;
import com.voxeo.utils.Enums;

public abstract class BaseProvider implements XmlProvider {

    protected static final Namespace RAYO_NAMESPACE = new Namespace("", "urn:xmpp:rayo:1");
    protected static final Namespace RAYO_COMPONENT_NAMESPACE = new Namespace("", "urn:xmpp:rayo:ext:1");
    protected static final Namespace RAYO_COMPLETE_NAMESPACE = new Namespace("", "urn:xmpp:rayo:ext:complete:1");

    private Validator validator;
    private XmlProviderManager manager;

    private List<String> namespaces;

    @Override
    @SuppressWarnings("unchecked")
    public Object fromXML(Element element) {

        Object returnValue = null;
        try {
            returnValue = processElement(element);
            if (returnValue == null) {
                return null;
            }
        } catch (ValidationException ve) {
            throw ve;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        validator.validate(returnValue);
        return returnValue;
    }    
    
    @Override
    public Element toXML(Object object) {

        try {
            Document document = DocumentHelper.createDocument();
            generateDocument(object, document);
            return document.getRootElement();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    protected abstract Object processElement(Element element) throws Exception;

    protected abstract void generateDocument(Object object, Document document) throws Exception;

    protected VerbCompleteEvent toVerbCompleteEvent(Element element) {
    	
        String reasonValue = element.getName().toUpperCase();
        Reason reason = VerbCompleteEvent.Reason.valueOf(reasonValue);
        VerbCompleteEvent event = new VerbCompleteEvent(reason);
        if(reason == Reason.ERROR) {
            event.setErrorText(element.getText());
        }
        return event;
    }
    
    protected Map<String, String> grabHeaders(Element node) {

        Map<String, String> headers = new HashMap<String, String>();
        @SuppressWarnings("unchecked")
        List<Element> elements = node.elements("header");
        for (Element element : elements) {
            headers.put(element.attributeValue("name"), element.attributeValue("value"));
        }
        return headers;
    }

    @SuppressWarnings("unchecked")
    protected Ssml extractSsml(Element node) throws URISyntaxException {

        StringBuilder builder = new StringBuilder();
        List<Node> elements = node.content();
        for (Node element : elements) {
            if (element instanceof Text || element instanceof Element) {
                String xml = element.asXML();
                //TODO: Better namespaces cleanup
                xml = xml.replaceAll(" xmlns=\"[^\"]*\"", "");
                builder.append(xml);
            }
        }
        Ssml ssml = new Ssml(builder.toString());
        ssml.setVoice(node.attributeValue("voice"));
        return ssml;
    }

    protected void addHeaders(Map<String, String> map, Element node) {

        if (map != null) {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                Element header = node.addElement("header");
                header.addAttribute("name", entry.getKey());
                header.addAttribute("value", entry.getValue());
            }
        }
    }

    protected void addSsml(Ssml item, Element root) throws DocumentException {

        if (item != null) {

            if (item.getVoice() != null) {
                root.addAttribute("voice", item.getVoice());
            }

            StringBuilder builder = new StringBuilder("<wrapper>");
            builder.append(item.getText());
            builder.append("</wrapper>");
            Document ssmlDoc = DocumentHelper.parseText(builder.toString());
            for (Object element : ssmlDoc.getRootElement().content()) {
                if (element instanceof Text) {
                    root.addText(((Text) element).asXML());
                } else if (element instanceof Element) {
                    root.add(((Element) element).createCopy());
                }
            }
        }
    }

    protected InputMode loadInputMode(Element element) {
        try {
            return InputMode.valueOf(element.attributeValue("mode").toUpperCase());
        } catch (Exception e) {
            throw new ValidationException(Messages.INVALID_INPUT_MODE);
        }
    }

    protected URI toURI(String string) {

        if (string == null || string.trim().equals("")) {
            return null;
        }
        string = string.trim();
        try {
            return new URI(string);
        } catch (URISyntaxException e) {
            throw new ValidationException(Messages.INVALID_URI);
        }
    }

    protected Boolean toBoolean(String attribute, Element element) {

    	String string = element.attributeValue(attribute);
        if (string == null) {
            throw new ValidationException(String.format(Messages.INVALID_BOOLEAN, attribute));
        }
        string = string.toLowerCase();
        if (string.equals("false") || string.equals("true")) {
            return Boolean.valueOf(string);
        }
        throw new ValidationException(String.format(Messages.INVALID_BOOLEAN, attribute));
    }

    protected int toInteger(String attribute, Element element) {

    	String string = element.attributeValue(attribute);
        if (string == null) {
            throw new ValidationException(String.format(Messages.INVALID_INTEGER, attribute));
        }
        string = string.toLowerCase();
        try {
        	return Integer.parseInt(string);
        } catch (NumberFormatException nfe) {
        	throw new ValidationException(String.format(Messages.INVALID_INTEGER, attribute));
        }
    }

    protected float toFloat(String attribute, Element element) {

    	String string = element.attributeValue(attribute);
        if (string == null) {
            throw new ValidationException(String.format(Messages.INVALID_FLOAT, attribute));
        }
        string = string.toLowerCase();
        try {
        	return Float.parseFloat(string);
        } catch (NumberFormatException nfe) {
        	throw new ValidationException(String.format(Messages.INVALID_FLOAT, attribute));
        }
    }

    protected Duration toDuration(String name, Element element) {
        try {
            return new Duration(Long.parseLong(element.attributeValue(name)));
        } catch (IllegalArgumentException iae) {
            throw new ValidationException(String.format(Messages.INVALID_DURATION, name));
        }
    }

    protected Duration toTimeout(String value) {
        try {
            return new Duration(Long.parseLong(value));
        } catch (IllegalArgumentException iae) {
            throw new ValidationException(Messages.INVALID_TIMEOUT);
        }
    }

    protected Float toFloatConfidence(String value) {

        try {
            return Float.valueOf(value);
        } catch (NumberFormatException nfe) {
            throw new ValidationException(Messages.INVALID_CONFIDENCE);
        }
    }

    protected Character toTerminator(String value) {

        if (value == null || value.length() != 1) {
            throw new ValidationException(Messages.INVALID_TERMINATOR);
        }
        return new Character(value.charAt(0));
    }
    
    protected <T extends Enum<T>> T toEnum(Class<T> enumClass, String name, Element element) {
        try {
            return Enum.valueOf(enumClass, element.attributeValue(name).toUpperCase());
        } catch (IllegalArgumentException iae) {
            throw new ValidationException(String.format(Messages.INVALID_ENUM, name));
        }
    }

    @SuppressWarnings("unchecked")
    protected List<Element> children(Element element) {
        return (List<Element>) element.elements();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected VerbCompleteEvent fillCompleteEvent(Element completeElement, VerbCompleteEvent event, Class<? extends Enum> reasonClazz) {
        VerbCompleteReason reason = null;
        List<Element> children = children(completeElement);
        if (!children.isEmpty()) {
            Element reasonElement = children.get(0);
            String reasonName = reasonElement.getName().toUpperCase();
            reason = Enums.valueOf(VerbCompleteEvent.Reason.class, reasonName, null);
            if (reason == null) {
                reason = (VerbCompleteReason) Enum.valueOf(reasonClazz, reasonName);
            }
            if (reason == VerbCompleteEvent.Reason.ERROR) {
                event.setErrorText(reasonElement.getText());
            }
        }
        event.setReason(reason);
        return event;
    }

    protected Element addCompleteElement(Document document, VerbCompleteEvent event, Namespace completeNamespace) {
        Element reasonElement = null;
        Element completeElement = document.addElement(new QName("complete", RAYO_COMPONENT_NAMESPACE));
        VerbCompleteReason reason = event.getReason();
        if (reason instanceof VerbCompleteEvent.Reason) {
            Reason globalReason = (VerbCompleteEvent.Reason) reason;
            String reasonValue = globalReason.name().toLowerCase();
            reasonElement = completeElement.addElement(new QName(reasonValue, RAYO_COMPLETE_NAMESPACE));
            if (globalReason == VerbCompleteEvent.Reason.ERROR) {
            	if (event.getErrorText() != null) {
            		reasonElement.setText(event.getErrorText());
            	}
            }
        } else {
            Enum<?> reasonEnum = (Enum<?>) reason;
            reasonElement = completeElement.addElement(new QName(reasonEnum.name().toLowerCase(), completeNamespace));
        }
        return reasonElement;
    }

    public void setValidator(Validator validator) {
        this.validator = validator;
    }

    protected Validator getValidator() {

        return validator;
    }

    @Override
    public boolean handles(Element element) {
        
    	return handles(element.getNamespace());
    }
    

    @Override
    public boolean handles(Namespace ns) {
    	
        for (String namespace : namespaces) {
            if (namespace.equals(ns.getURI())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public XmlProviderManager getManager() {
        return manager;
    }

    @Override
    public void setManager(XmlProviderManager manager) {
        this.manager = manager;
    }

    public List<String> getNamespaces() {
        return namespaces;
    }

    public void setNamespaces(List<String> namespaces) {
        this.namespaces = namespaces;
    }
}
