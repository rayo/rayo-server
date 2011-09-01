package com.rayo.core.xml;

import org.dom4j.Namespace;

public interface XmlProviderManager extends XmlProvider {

    public void register(XmlProvider provider);

    public void unregister(XmlProvider provider);

    public XmlProvider findProvider(Namespace namespace);
}
