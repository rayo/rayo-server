package com.voxeo.ozone.client.io;

import java.io.IOException;
import java.io.Writer;

import com.voxeo.ozone.client.XmppException;
import com.voxeo.servlet.xmpp.ozone.stanza.XmppObject;
import com.voxeo.servlet.xmpp.ozone.stanza.Error;

public class SimpleXmppWriter implements XmppWriter {

	private Writer writer;

	public SimpleXmppWriter(Writer writer) {
		
		this.writer = writer;
	}
	
	@Override
	public void write(XmppObject object) throws XmppException {

		write(object.toString());
	}
	
	@Override
	public void write(String string) throws XmppException {
		
		try {
	        writer.write(string.toString());
	        writer.flush();
		} catch (IOException ioe) {
			throw new XmppException("IO Error: Could not write", Error.Condition.remote_server_error, ioe);
		}
	}
	
	public Writer getWriter() {
		
		return writer;
	}
	
    /**
     * Sends to the server a new stream element. This operation may be requested several times
     * so we need to encapsulate the logic in one place. This message will be sent while doing
     * TLS, SASL and resource binding.
     *
     * @throws IOException If an error occurs while sending the stanza to the server.
     */
	@Override
    public void openStream(String serviceName) throws XmppException {
    	
        StringBuilder stream = new StringBuilder();
        stream.append("<stream:stream");
        stream.append(" to=\"").append(serviceName).append("\"");
        stream.append(" xmlns=\"jabber:client\"");
        stream.append(" xmlns:stream=\"http://etherx.jabber.org/streams\"");
        stream.append(" version=\"1.0\">");
        
        write(stream.toString());
    }
    
	private void closeStream() {
		
        try {
            writer.write("</stream:stream>");
            writer.flush();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                writer.close();
            } catch (Exception e) {
            	e.printStackTrace();
            }
        }		
	}
	
	public void close() throws XmppException {
		
		try {
			if (writer != null) {
				closeStream();
				writer.close();
			}
		} catch (IOException ioe) {
			throw new XmppException("IO Error", Error.Condition.remote_server_error, ioe);
		}
	}
	

}
