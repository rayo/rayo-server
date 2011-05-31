package com.tropo.core.xml;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.dom4j.Element;

import com.tropo.core.validation.Messages;
import com.tropo.core.validation.ValidationException;

import static com.voxeo.utils.Objects.assertion;

public class DefaultXmlProviderManager implements XmlProviderManager {

    private List<XmlProvider> providers = new CopyOnWriteArrayList<XmlProvider>();

    @Override
    public Element toXML(Object object) {
        
        assertion(object != null, "Cannot serialize null reference");
        
        for (XmlProvider provider : providers) {
            if (provider.handles(object.getClass())) {
                Element element = provider.toXML(object);
                if (element != null) {
                	return element;
                }
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
                T object = (T)provider.fromXML(element);
                if (object != null) {
                	return object;
                }
            }
        }
        throw new ValidationException(Messages.UNKNOWN_NAMESPACE_ELEMENT);
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
        provider.setManager(this);
    }

    @Override
    public void unregister(XmlProvider provider) {
        providers.remove(provider);
    }

    @Override
    public XmlProviderManager getManager() {
        return this;
    }

    @Override
    public void setManager(XmlProviderManager manager) {
        throw new UnsupportedOperationException("This is already a root manager");
    }

}
