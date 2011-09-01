package com.rayo.server.verb;

import com.rayo.core.verb.Verb;
import com.rayo.core.xml.XmlProvider;
import com.voxeo.moho.Participant;

public abstract class AbstractVerbFactory implements VerbFactory {

    private XmlProvider xmlProvider;
    private Class<? extends Verb> modelClass;
    
    public abstract VerbHandler<?,Participant> createVerbHandler();

    public void setXmlProvider(XmlProvider xmlProvider) {
        this.xmlProvider = xmlProvider;
    }

    public XmlProvider getXmlProvider() {
        return xmlProvider;
    }

    public void setModelClass(Class<? extends Verb> modelClass) {
        this.modelClass = modelClass;
    }

    public Class<? extends Verb> getModelClass() {
        return modelClass;
    }

}
