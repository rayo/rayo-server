package com.voxeo.ozone.client.filter;

import com.voxeo.ozone.client.io.XmppReader;
import com.voxeo.servlet.xmpp.ozone.stanza.AbstractXmppObject;
import com.voxeo.servlet.xmpp.ozone.stanza.XmppObject;

public interface XmppObjectFilter {

	public static final int DEFAULT_TIMEOUT = 60000;
	
	/**
	 * Filters an Xmpp Object. This method will return the object if it passes the filter.
	 * 
	 * @param object Object to be filtered
	 * 
	 */
	public void filter(AbstractXmppObject object);
	
	/**
	 * <p>Polls the filter for the next object meeting the filter constraints. This method call will 
	 * block until an object that meets the filter constraints arrives to the polling thread.</p>
	 * 
	 * <p>This method will block until the default timeout expires or an object is received.</p>  
	 * 
	 * @return XmppObject Object that passes the filters or <code>null</code> if no object has been 
	 * received for the default timeout. 
	 */
	public XmppObject poll();
	
	/**
	 * <p>Polls the filter for the next object meeting the filter constraints. This method call will 
	 * block until an object that meets the filter constraints arrives to the polling thread.</p>
	 * 
	 * <p>This method will block until an object arrives or the specified timeout expires. The timeout 
	 * is passed in milliseconds.</p>  
	 * 
	 * @return XmppObject Object that passes the filters or <code>null</code> if no object has been 
	 * received for the default timeout. 
	 */	
	public XmppObject poll(int milliseconds);
	
	/**
	 * Sets the XMPP reader that will execute this filter. This method gives a chance to the filter to 
	 * store a reader reference to notify the reader about external events like for example a call to the
	 * {@link XmppObjectFilter.stop} method.
	 * 
	 * @param reader
	 */
	public void setReader(XmppReader reader);
	
	/**
	 * Stops this filter. Once this method is called the filter should not be callable anymore. 
	 * Implementations of this interface can use the {@link XmppObjectFilter.setReader} to cleanup 
	 * resources from the attached reader.
	 */
	public void stop();
}
