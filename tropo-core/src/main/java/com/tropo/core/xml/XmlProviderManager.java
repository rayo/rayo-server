package com.tropo.core.xml;

public interface XmlProviderManager extends XmlProvider {

    public void register(XmlProvider provider);

    public void unregister(XmlProvider provider);

}
