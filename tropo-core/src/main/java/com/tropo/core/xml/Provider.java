package com.tropo.core.xml;

import org.dom4j.Element;

public interface Provider {

    public Element toXML(Object object);

    public <T> T fromXML(Element element);

}
