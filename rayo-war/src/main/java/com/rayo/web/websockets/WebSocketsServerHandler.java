package com.rayo.web.websockets;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.*;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders.Names;
import org.jboss.netty.handler.codec.http.HttpHeaders.Values;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.websocket.DefaultWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocket.WebSocketFrame;
import org.jboss.netty.handler.codec.http.websocket.WebSocketFrameDecoder;
import org.jboss.netty.handler.codec.http.websocket.WebSocketFrameEncoder;
import org.jboss.netty.util.CharsetUtil;

import com.rayo.web.DebugXmppMessageListener;
 
import java.security.MessageDigest;
 
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.*;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Values.WEBSOCKET;
import static org.jboss.netty.handler.codec.http.HttpHeaders.isKeepAlive;
import static org.jboss.netty.handler.codec.http.HttpHeaders.setContentLength;
import static org.jboss.netty.handler.codec.http.HttpMethod.GET;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1;
 
public class WebSocketsServerHandler extends SimpleChannelUpstreamHandler {
 
  private static final String WEBSOCKET_PATH = "/websocket";
 
  @Override
  public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
    Object msg = e.getMessage();
    if (msg instanceof HttpRequest) {
      handleHttpRequest(ctx, (HttpRequest) msg);
    } else if (msg instanceof WebSocketFrame) {
      handleWebSocketFrame(ctx, (WebSocketFrame) msg);
    }
  }
 
  private void handleHttpRequest(ChannelHandlerContext ctx, HttpRequest req) throws Exception {
    // Allow only GET methods.
    if (req.getMethod() != GET) {
      sendHttpResponse(
        ctx, req, new DefaultHttpResponse(HTTP_1_1, FORBIDDEN));
      return;
    }
 
    // Serve the WebSocket handshake request.
    if (req.getUri().equals(WEBSOCKET_PATH) &&
      Values.UPGRADE.equalsIgnoreCase(req.getHeader(CONNECTION)) &&
      WEBSOCKET.equalsIgnoreCase(req.getHeader(Names.UPGRADE))) {
 
      // Create the WebSocket handshake response.
      HttpResponse res = new DefaultHttpResponse(HTTP_1_1, new HttpResponseStatus(101, "Web Socket Protocol Handshake"));
      res.addHeader(Names.UPGRADE, WEBSOCKET);
      res.addHeader(CONNECTION, Values.UPGRADE);
 
      // Fill in the headers and contents depending on handshake method.
      if (req.containsHeader(SEC_WEBSOCKET_KEY1) &&
        req.containsHeader(SEC_WEBSOCKET_KEY2)) {
        // New handshake method with a challenge:
        res.addHeader(SEC_WEBSOCKET_ORIGIN, req.getHeader(ORIGIN));
        res.addHeader(SEC_WEBSOCKET_LOCATION, getWebSocketLocation(req));
        String protocol = req.getHeader(SEC_WEBSOCKET_PROTOCOL);
        if (protocol != null) {
          res.addHeader(SEC_WEBSOCKET_PROTOCOL, protocol);
        }
 
        // Calculate the answer of the challenge.
        String key1 = req.getHeader(SEC_WEBSOCKET_KEY1);
        String key2 = req.getHeader(SEC_WEBSOCKET_KEY2);
        int a = (int) (Long.parseLong(key1.replaceAll("[^0-9]", "")) / key1.replaceAll("[^ ]", "").length());
        int b = (int) (Long.parseLong(key2.replaceAll("[^0-9]", "")) / key2.replaceAll("[^ ]", "").length());
        long c = req.getContent().readLong();
        ChannelBuffer input = ChannelBuffers.buffer(16);
        input.writeInt(a);
        input.writeInt(b);
        input.writeLong(c);
        ChannelBuffer output = ChannelBuffers.wrappedBuffer(MessageDigest.getInstance("MD5").digest(input.array()));
        res.setContent(output);
      } else {
        // Old handshake method with no challenge:
        res.addHeader(WEBSOCKET_ORIGIN, req.getHeader(ORIGIN));
        res.addHeader(WEBSOCKET_LOCATION, getWebSocketLocation(req));
        String protocol = req.getHeader(WEBSOCKET_PROTOCOL);
        if (protocol != null) {
          res.addHeader(WEBSOCKET_PROTOCOL, protocol);
        }
      }
 
      // Upgrade the connection and send the handshake response.
      ChannelPipeline p = ctx.getChannel().getPipeline();
      p.remove("aggregator");
      p.replace("decoder", "wsdecoder", new WebSocketFrameDecoder());
 
      ctx.getChannel().write(res);
 
      p.replace("encoder", "wsencoder", new WebSocketFrameEncoder());
      return;
    }
 
    // Send an error page otherwise.
    sendHttpResponse(ctx, req, new DefaultHttpResponse(HTTP_1_1, FORBIDDEN));
  }
 
  private void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
    new DebugXmppMessageListener().handleRequest(ctx, new DefaultWebSocketFrame(frame.getTextData()));
  }
 
  private void sendHttpResponse(ChannelHandlerContext ctx, HttpRequest req, HttpResponse res) {
    // Generate an error page if response status code is not OK (200).
    if (res.getStatus().getCode() != 200) {
      res.setContent(
        ChannelBuffers.copiedBuffer(
          res.getStatus().toString(), CharsetUtil.UTF_8));
      setContentLength(res, res.getContent().readableBytes());
    }
 
    // Send the response and close the connection if necessary.
    ChannelFuture f = ctx.getChannel().write(res);
    if (!isKeepAlive(req) || res.getStatus().getCode() != 200) {
      f.addListener(ChannelFutureListener.CLOSE);
    }
  }
 
  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
    throws Exception {
    e.getCause().printStackTrace();
    e.getChannel().close();
  }
 
  private String getWebSocketLocation(HttpRequest req) {
    return "ws://" + req.getHeader(Names.HOST) + WEBSOCKET_PATH;
  }
}
