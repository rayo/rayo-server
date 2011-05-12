package com.voxeo.ozone.client.response;

import com.voxeo.ozone.client.XmppConnection;
import com.voxeo.ozone.client.filter.XmppObjectFilter;
import com.voxeo.servlet.xmpp.ozone.stanza.XmppObject;

public class FilterCleaningResponseHandler implements ResponseHandler {

	private ResponseHandler handler;
	private XmppConnection connection;
	private XmppObjectFilter filter;

	public FilterCleaningResponseHandler(ResponseHandler handler, XmppConnection connection) {
		
		this.handler = handler;
		this.connection = connection;
	}
	
	public void setFilter(XmppObjectFilter filter) {
		
		this.filter = filter;
	}
	
	@Override
	public void handle(XmppObject response) {

		try {
			if (handler !=  null) {
				handler.handle(response);
			}
		} finally {
			if (connection != null && filter != null) {
				connection.removeFilter(filter);
			}
		}
	}
}
