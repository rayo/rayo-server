package com.rayo.web;

import java.util.Date;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.websocket.DefaultWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocket.WebSocketFrame;

import com.rayo.server.listener.XmppMessageListener;
import com.voxeo.servlet.xmpp.IQRequest;
import com.voxeo.servlet.xmpp.IQResponse;
import com.voxeo.servlet.xmpp.PresenceMessage;

public class DebugXmppMessageListener implements XmppMessageListener {

	public void onErrorSent(IQResponse response) {
		
		onMessage(response.toString(), Message.Type.ERROR);
	};
	
	@Override
	public void onIQReceived(IQRequest request) {

		onMessage(request.toString(), Message.Type.IN);
	}
	
	@Override
	public void onIQSent(IQResponse response) {
		
		onMessage(response.toString(), Message.Type.OUT);
	}
	
	@Override
	public void onPresenceSent(PresenceMessage message) {
		
		onMessage(message.toString(), Message.Type.IN);
	}
	
	public void onMessage(String message, Message.Type type) {

		if (!message.contains("<todo>")) {
			MessagesQueue.publish(new Message(StringEscapeUtils.escapeXml(
					String.format("%s : %s",DateFormatUtils.format(new Date(), "dd/MM/yyyy hh:mm:ss.SSS"),message)), type));
		}
	}
		
	public void handleRequest(final ChannelHandlerContext ctx, final WebSocketFrame frame) {
		
		try {

			if (!frame.isText())
				return;

			showIQs(ctx.getChannel());
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	private void showIQs(final Channel channel) {
		
		new Thread() {
			StringBuffer buf = null;

			@Override
			public void run() {
				if (channel == null)
					return;
				buf = new StringBuffer();
				try {
					while (channel.isConnected()) {

						Message message = MessagesQueue.poll();
						if (message != null) {
							buf.append("<div class=\"row");
							if (message.getType() == Message.Type.IN) {
								buf.append("-in\">");
							} else if (message.getType() == Message.Type.OUT) {
								buf.append("-out\">");				
							} else if (message.getType() == Message.Type.ERROR) {
								buf.append("-error\">");
							}
								
							buf.append(message.getMessage());
							buf.append("</div>");	
	
							if (channel.isOpen())
								channel.write(new DefaultWebSocketFrame(buf
										.toString()));
	
							buf.setLength(0);
						} else {
							Thread.sleep(100);
						}
					}
				} catch (final Exception e) {
					e.printStackTrace();
				}
			}
		}.start();
	}
}