package com.voxeo.ozone.client.filter;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.voxeo.ozone.client.io.XmppReader;
import com.voxeo.servlet.xmpp.ozone.stanza.AbstractXmppObject;
import com.voxeo.servlet.xmpp.ozone.stanza.XmppObject;

public abstract class AbstractXmppObjectFilter implements XmppObjectFilter {

	private int DEFAULT_TIMEOUT = 20 * 1000; // 20 seconds default timeout
	
	private LinkedBlockingQueue<AbstractXmppObject> queue = new LinkedBlockingQueue<AbstractXmppObject>(1000);

	private XmppReader reader;
	
	
	@Override
	public void filter(AbstractXmppObject object) {
		
		object = doFilter(object);
		
		if (object != null) {
			try {
				queue.put(object);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	abstract AbstractXmppObject doFilter(AbstractXmppObject object);
	
	public XmppObject poll() {
		
		try {
			return queue.poll(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {}
		return null;
	}
	
	public XmppObject poll(int milliseconds) {
		
		try {
			return queue.poll(milliseconds, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {}
		return null;
	}
	
	@Override
	public void setReader(XmppReader reader) {

		this.reader = reader;
	}
	
	@Override
	public void stop() {

		reader.removeFilter(this);
		reader = null;
	}
	
	public void setDefaultTimeout(int timeout) {
		
		DEFAULT_TIMEOUT = timeout;
	}
}
