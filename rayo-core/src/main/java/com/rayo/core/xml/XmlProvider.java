package com.rayo.core.xml;

import org.dom4j.Element;
import org.dom4j.Namespace;

public interface XmlProvider {

    public Element toXML(Object object);

    public <T> T fromXML(Element element);

    public XmlProviderManager getManager();

    public void setManager(XmlProviderManager manager);

    public boolean handles(Element element);
    
    public boolean handles(Namespace namespace);

    public boolean handles(Class<?> clazz);

}
