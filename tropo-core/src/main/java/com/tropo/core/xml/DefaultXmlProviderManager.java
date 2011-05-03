package com.tropo.core.xml;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.dom4j.Element;
import static com.voxeo.utils.Objects.assertion;

public class DefaultXmlProviderManager implements XmlProviderManager {

    private List<XmlProvider> providers = new CopyOnWriteArrayList<XmlProvider>();

    @Override
    public Element toXML(Object object) {
        
        assertion(object != null, "Cannot serialize null reference");
        
        for (XmlProvider provider : providers) {
            if (provider.handles(object.getClass())) {
                return provider.toXML(object);
            }
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T fromXML(Element element) {

        assertion(element != null, "Cannot deserialize null reference");

        for (XmlProvider provider : providers) {
            if (provider.handles(element)) {
                return (T)provider.fromXML(element);
            }
        }
        return null;
    }

    @Override
    public boolean handles(Element element) {
        for (XmlProvider provider : providers) {
            if (provider.handles(element)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean handles(Class<?> clazz) {
        for (XmlProvider provider : providers) {
            if (provider.handles(clazz)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void register(XmlProvider provider) {
        providers.add(provider);
    }

    @Override
    public void unregister(XmlProvider provider) {
        providers.remove(provider);
    }

}
