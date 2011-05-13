package com.voxeo.ozone.client.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.dom4j.Element;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;

import com.tropo.core.verb.RefEvent;
import com.tropo.core.xml.XmlProviderManager;
import com.voxeo.servlet.xmpp.ozone.extensions.Extension;
import com.voxeo.servlet.xmpp.ozone.extensions.XmlProviderManagerFactory;
import com.voxeo.servlet.xmpp.ozone.stanza.IQ;
import com.voxeo.servlet.xmpp.ozone.util.Dom4jParser;

public class NettyServerHandler extends SimpleChannelHandler {

	private boolean receivedFirstStream = false;
	private XmlProviderManager manager = XmlProviderManagerFactory.buildXmlProvider();
	private Channel channel;
	
	private List<String> messages = new ArrayList<String>();
	private String offerId;
	private String callId;
		
	@Override
	public void channelBound(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {

		super.channelBound(ctx, e);
		this.channel = e.getChannel();
	}
	
	public void sendOzoneOffer() {
		
		offerId = UUID.randomUUID().toString();
		callId = UUID.randomUUID().toString();
		String offer = "<iq type=\"set\" id=\"%s\" from=\"%s@localhost\" to=\"userc@localhost/voxeo\"> " + 
		   "<offer xmlns=\"urn:xmpp:ozone:1\" to=\"sip:userc@localhost:5060\" from=\"sip:test@someip.com:6089\">" +
		   "<header name=\"Max-Forwards\" value=\"70\"/>" +
		   "</offer></iq>";

		sendResponse(channel,String.format(offer,offerId,callId));
	}
	
    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
    
    	String message = (String)e.getMessage();
    	System.out.println(String.format("Received message %s", message));
    	
    	try {
			Thread.sleep(100);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		if (message.startsWith("<stream")) {
			sendResponse(channel,"<stream:stream xmlns='jabber:client' id='test' from='localhost' version='1.0' xmlns:stream='http://etherx.jabber.org/streams'>");
			sendResponse(channel,"<stream:features><mechanisms xmlns='urn:ietf:params:xml:ns:xmpp-sasl'><mechanism>DIGEST-MD5</mechanism><mechanism>PLAIN</mechanism></mechanisms></stream:features>");
			if (receivedFirstStream) {
				sendResponse(channel,"<stream:features><bind xmlns='urn:ietf:params:xml:ns:xmpp-bind'/></stream:features>");				
				sendResponse(channel,"<stream:features><session xmlns='urn:ietf:params:xml:ns:xmpp-session'/></stream:features>");				
			}
			receivedFirstStream = true;			
		} else if (message.startsWith("<auth")) {
			sendResponse(channel,"<challenge xmlns='urn:ietf:params:xml:ns:xmpp-sasl'>cmVhbG09InNvbWVyZWFsbSIsbm9uY2U9Ik9BNk1HOXRFUUdtMmhoIixxb3A9ImF1dGgiLGNoYXJzZXQ9dXRmLTgsYWxnb3JpdGhtPW1kNS1zZXNzCg==</challenge>");    		
			sendResponse(channel,"<success xmlns='urn:ietf:params:xml:ns:xmpp-sasl'/>");
		} else if (message.contains("<bind")) {
			Element element = Dom4jParser.parseXml(message);
			String id = element.attributeValue("id");
			sendResponse(channel,"<iq id='"+id+"' type='result' from='localhost' to='userc@localhost'><bind jid='userc@localhost/voxeo'/></iq>");
		} else if (message.contains("<session")) {
			Element element = Dom4jParser.parseXml(message);
			String id = element.attributeValue("id");
			sendResponse(channel,"<iq id='"+id+"' type='result' from='localhost' to='userc@localhost'><session/></iq>");
		} else if (message.startsWith("<iq")) {
			// We may receive a buffer with several IQ messages. 
			// This should be managed much better with the Netty framework.
			// But do not have enough time right now
			message = message.trim();
			do {
				int iqEnd = message.indexOf("</iq>");
				String iq = message.substring(0,iqEnd+5);				
				processIQMessage(iq);
				if (iqEnd+5 == message.length()) {
					break;
				} else {
					message = message.substring(iqEnd+5);
				}				
			} while(true);
		}
    }

	private void processIQMessage(String message) {
		Element element = Dom4jParser.parseXml(message);
		IQ iq = new IQ(element);
		storeIQ(iq);
		
		if (iq.getChildName().equals("say")) {
			// send ref back
			RefEvent ref = new RefEvent();
			ref.setCallId(callId);
			String sayId = UUID.randomUUID().toString();
			ref.setJid(callId+"@localhost/" + sayId);
			
			IQ response = iq.result(Extension.create(ref)); 
			sendResponse(channel, response.toString());
		}
	}

    private void sendResponse(Channel channel, String response) {
    	
    	System.out.println(String.format("Sending to client: %s",response));
    	channel.write(response);
    }

	@Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
        e.getCause().printStackTrace();
        
        Channel ch = e.getChannel();
        ch.close();
    }

	private void storeIQ(IQ iq) {
		
		messages.add(iq.copy().setId("*").toString());
	}
	
	public void assertReceived(String message) {

		// Transform it first to the same format dom4j creates in the server
		Element element = Dom4jParser.parseXml(message);
		IQ messageIq = new IQ(element);
		message = messageIq.toString();
		
		if (!messages.contains(message)) {
			String errorMessage = String.format("Message %s was not received by the server", message);
			System.out.println("ERROR: " + errorMessage);
			System.out.println("List of received messages:");
			System.out.println("--------------------------");
			int i = 1;
			for (String string: messages) {
				System.out.println(i + ": " + string);
				i++;
			}
			System.out.println(errorMessage);
			throw new IllegalStateException(errorMessage);
		}
	}
}
