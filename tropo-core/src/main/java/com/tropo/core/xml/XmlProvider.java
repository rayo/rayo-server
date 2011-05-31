package com.tropo.core.xml;

import org.dom4j.Element;

public interface XmlProvider {

    public Element toXML(Object object);

    public <T> T fromXML(Element element);

    public XmlProviderManager getManager();

    public void setManager(XmlProviderManager manager);

    public boolean handles(Element element);

    public boolean handles(Class<?> clazz);

}
