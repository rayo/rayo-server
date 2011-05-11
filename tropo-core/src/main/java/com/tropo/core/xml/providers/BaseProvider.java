package com.tropo.core.xml.providers;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.joda.time.Duration;

import com.tropo.core.validation.Messages;
import com.tropo.core.validation.ValidationException;
import com.tropo.core.validation.Validator;
import com.tropo.core.verb.AudioItem;
import com.tropo.core.verb.InputMode;
import com.tropo.core.verb.PromptItem;
import com.tropo.core.verb.PromptItems;
import com.tropo.core.verb.SsmlItem;
import com.tropo.core.xml.XmlProvider;

public abstract class BaseProvider implements XmlProvider {

	private Validator validator;
	
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
            generateDocument(object,document);
            return document.getRootElement();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
    
    protected abstract Object processElement(Element element) throws Exception;
    protected abstract void generateDocument(Object object, Document document) throws Exception;
	
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
	protected PromptItems extractPromptItems(Element node) throws URISyntaxException {

		PromptItems items = new PromptItems();
        List<Element> elements = node.elements();
		for(Element element: elements) {
			if (element.getName().equals("audio")) {
				AudioItem item = new AudioItem();
				item.setUri(toURI(element.attributeValue("url")));
				items.add(item);				
			} else if (element.getName().equals("speak")) {
				String xml = element.asXML();
				//TODO: Better namespaces cleanup
				xml = xml.replaceAll(" xmlns=\"[^\"]*\"","");
				SsmlItem ssml = new SsmlItem(xml);
				items.add(ssml);
			}
		}
		return items;
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

	protected void addPromptItems(PromptItems items, Element root) throws DocumentException {
		
		if (items != null) {
			for (PromptItem item: items) {
				if (item instanceof AudioItem) {
					Element audio = root.addElement("audio");
					audio.addAttribute("url", ((AudioItem) item).getUri().toString());
				} else if (item instanceof SsmlItem) {
					Document ssmlDoc = DocumentHelper.parseText(((SsmlItem) item).getText());
					root.add(ssmlDoc.getRootElement());
				}
			}
		}
	}
	
	protected InputMode loadInputMode(Element element) {
	
		try {
			return InputMode.valueOf(element.attributeValue("mode"));
		} catch (Exception e) {
			throw new ValidationException(Messages.INVALID_INPUT_MODE);
		}
	}

	protected URI toURI(String string) {
	
		if (string == null || string.trim().equals("")) {
			return null;
		}
		
		try {			
			return new URI(string);
		} catch (URISyntaxException e) {
			throw new ValidationException(Messages.INVALID_URI);
		}
	}

	protected Boolean toBoolean(String string) {
	
		if (string == null) {
			throw new ValidationException(Messages.INVALID_BOOLEAN);
		}
		string = string.toLowerCase();
		if (string.equals("false") || string.equals("true")) {
			return Boolean.valueOf(string);
		}
		throw new ValidationException(Messages.INVALID_BOOLEAN);
	}

	protected Duration toTimeout(String value) {
	
		try {
			return new Duration(value);
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
	
	public void setValidator(Validator validator) {
		this.validator = validator;
	}

	protected Validator getValidator() {
		
		return validator;
	}

	@Override
    public boolean handles(Element element) {

		for(String namespace: namespaces) {
			if (namespace.equals(element.getNamespace().getURI())) {
				return true;
			}
		}
		return false;
    }
	
	public List<String> getNamespaces() {
		return namespaces;
	}

	public void setNamespaces(List<String> namespaces) {
		this.namespaces = namespaces;
	}
}
