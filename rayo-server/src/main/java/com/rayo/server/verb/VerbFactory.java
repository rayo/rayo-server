package com.tropo.server.verb;

import com.tropo.core.verb.Verb;
import com.tropo.core.xml.XmlProvider;
import com.voxeo.moho.Participant;

public interface VerbFactory {

    public VerbHandler<?,Participant> createVerbHandler();

    public XmlProvider getXmlProvider();

    public Class<? extends Verb> getModelClass();

}