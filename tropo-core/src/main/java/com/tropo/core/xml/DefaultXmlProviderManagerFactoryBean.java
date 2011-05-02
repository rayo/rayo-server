package com.tropo.core.xml;

import java.util.List;

import org.springframework.beans.factory.FactoryBean;

public class DefaultXmlProviderManagerFactoryBean implements FactoryBean<XmlProviderManager> {

    private DefaultXmlProviderManager xmlProviderManager;

    @Override
    public XmlProviderManager getObject() throws Exception {
        return xmlProviderManager;
    }

    @Override
    public Class<?> getObjectType() {
        return XmlProviderManager.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    public void setProviders(List<XmlProvider> providers) {
        xmlProviderManager = new DefaultXmlProviderManager();
        for (XmlProvider provider : providers) {
            xmlProviderManager.register(provider);
        }
    }

}
