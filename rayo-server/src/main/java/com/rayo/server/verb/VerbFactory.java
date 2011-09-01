package com.rayo.server.verb;

import com.rayo.core.verb.Verb;
import com.rayo.core.xml.XmlProvider;
import com.voxeo.moho.Participant;

public interface VerbFactory {

    public VerbHandler<?,Participant> createVerbHandler();

    public XmlProvider getXmlProvider();

    public Class<? extends Verb> getModelClass();

}