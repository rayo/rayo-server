package com.rayo.server.verb;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.rayo.core.verb.Verb;
import com.rayo.core.xml.XmlProviderManager;

public class DefaultVerbManager implements VerbManager {

    private XmlProviderManager xmlProviderManager;
    
    private Map<Class<? extends Verb>, VerbFactory> factoryMap = new ConcurrentHashMap<Class<? extends Verb>, VerbFactory>();

    public void registerVerbFactory(VerbFactory factory) {
        factoryMap.put(factory.getModelClass(), factory);
        xmlProviderManager.register(factory.getXmlProvider());
    }

    public VerbFactory getVerbFactory(Class<? extends Verb> modelClass) {
        return factoryMap.get(modelClass);
    }

    public void setXmlProviderManager(XmlProviderManager xmlProviderManager) {
        this.xmlProviderManager = xmlProviderManager;
    }

    public XmlProviderManager getXmlProviderManager() {
        return xmlProviderManager;
    }

}
