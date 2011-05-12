package com.voxeo.ozone.client.internal;

import org.dom4j.Element;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;

import com.tropo.core.xml.XmlProviderManager;
import com.voxeo.servlet.xmpp.ozone.extensions.XmlProviderManagerFactory;
import com.voxeo.servlet.xmpp.ozone.stanza.IQ;
import com.voxeo.servlet.xmpp.ozone.util.Dom4jParser;

public class NettyServerHandler extends SimpleChannelHandler {

	private boolean receivedFirstStream = false;
	private XmlProviderManager manager = XmlProviderManagerFactory.buildXmlProvider();
	
    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
    	
    	String message = (String)e.getMessage();
    	System.out.println(String.format("Received message %s", message));
    	
    	Channel channel = e.getChannel();
    	try {
			Thread.sleep(100);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		if (message.startsWith("<stream")) {
			sendResponse(channel,"<stream:stream xmlns='jabber:client' id='test' from='test.ozone.net' version='1.0' xmlns:stream='http://etherx.jabber.org/streams'>");
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
			sendResponse(channel,"<iq id='"+id+"' type='result' from='test.ozone.net' to='userc@test.ozone.net'><bind jid='userc@test.ozone.net/voxeo'/></iq>");
		} else if (message.contains("<session")) {
			Element element = Dom4jParser.parseXml(message);
			String id = element.attributeValue("id");
			sendResponse(channel,"<iq id='"+id+"' type='result' from='test.ozone.net' to='userc@test.ozone.net'><session/></iq>");
		} else if (message.startsWith("<iq")) {
			
			Element element = Dom4jParser.parseXml(message);
			IQ iq = manager.fromXML(element);
			if ("bind".equals(iq.getChildName())) {
				sendResponse(channel,iq.result().toString());
			}
		}
    }

    private void sendResponse(Channel channel, String response) {
    	
    	System.out.println(String.format("Sending response back %s",response));
    	channel.write(response);
    }

	@Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
        e.getCause().printStackTrace();
        
        Channel ch = e.getChannel();
        ch.close();
    }
}
