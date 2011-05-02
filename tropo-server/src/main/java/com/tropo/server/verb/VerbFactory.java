package com.tropo.server.verb;

import com.tropo.core.verb.Verb;
import com.tropo.core.xml.XmlProvider;

public interface VerbFactory {

    public VerbHandler<?> createVerbHandler();

    public XmlProvider getXmlProvider();

    public Class<? extends Verb> getModelClass();

}