package com.voxeo.ozone.client.internal;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.string.StringDecoder;
import org.jboss.netty.handler.codec.string.StringEncoder;


public class NettyServer {

	private NettyServerHandler nettyServerHandler;
	private ChannelGroup group;
	private ChannelFactory factory;
	
	private static Map<Integer, NettyServer> servers = new ConcurrentHashMap<Integer, NettyServer>();
	
	public void assertReceived(String message) {
		
		nettyServerHandler.assertReceived(message);
	}
	
	public void sendOzoneOffer() {
		
		nettyServerHandler.sendOzoneOffer();
	}
	
	public static NettyServer newInstance(int port) throws Exception {
		
		NettyServer server = servers.get(port);
		if (server == null) {
			server = new NettyServer(port);
			servers.put(port, server);
		}
		server.resetState();
		return server;
	}
	
	private NettyServer(int port) throws Exception {
		
		nettyServerHandler = new NettyServerHandler();
	    
	    group = new DefaultChannelGroup("myServer"); 
	    
        factory =
            new NioServerSocketChannelFactory(
                    Executors.newCachedThreadPool(),
                    Executors.newCachedThreadPool());

        ServerBootstrap bootstrap = new ServerBootstrap(factory);

        bootstrap.setOption("child.tcpNoDelay", true);
        bootstrap.setOption("child.keepAlive", true);
        
        bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
            @Override
            public ChannelPipeline getPipeline() throws Exception {
                ChannelPipeline pipeline = Channels.pipeline();

                pipeline.addLast("decoder", new StringDecoder());
                pipeline.addLast("encoder", new StringEncoder());
                pipeline.addLast("handler", nettyServerHandler);

                return pipeline;
            }
        });
        
        Channel channel = bootstrap.bind(new InetSocketAddress(port));
        group.add(channel);
	}
	
	public void shutdown() {
		
		if (group != null) {
			group.close().awaitUninterruptibly();	
		}
		if (factory != null) {
			factory.releaseExternalResources();
		}
	}
	
	public static void shutdownAllInstances() {
		
		for (NettyServer server: servers.values()) {
			server.shutdown();
		}
	}

	public void resetState() {

		nettyServerHandler.resetState();
	}
}
