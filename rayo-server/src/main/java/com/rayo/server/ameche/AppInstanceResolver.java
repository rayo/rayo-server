package com.rayo.server.ameche;

import java.util.List;

import org.dom4j.Element;

public interface AppInstanceResolver {

    public List<AppInstance> lookup(Element offer);

}
