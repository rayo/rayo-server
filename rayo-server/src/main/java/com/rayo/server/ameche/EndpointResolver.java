package com.rayo.server.ameche;

import java.net.URI;
import java.util.List;

import org.dom4j.Element;

public interface EndpointResolver {

    public List<URI> lookup(Element offer);

}
