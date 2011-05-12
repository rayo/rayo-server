package com.voxeo.servlet.xmpp.ozone.stanza;

import org.dom4j.Element;

import com.voxeo.servlet.xmpp.ozone.Namespaces;

public class Stream extends Stanza<Stream> {

	private boolean initial = false;

	public Stream() {
		
		super(Namespaces.STREAMS);
	}
	
	public Stream(Element element) {
		
		this();
		setElement(element);
	}
	
	@Override
	public String getStanzaName() {

		return "stream";
	}

	public boolean isInitial() {
		return initial;
	}

	public void setInitial(boolean initial) {
		this.initial = initial;
	}
	
	
}
