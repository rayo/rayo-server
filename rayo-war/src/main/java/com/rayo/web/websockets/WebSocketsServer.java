package com.rayo.web.websockets;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import org.apache.log4j.BasicConfigurator;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebSocketsServer {

	private final Logger log = LoggerFactory.getLogger(WebSocketsServer.class);
	private int portNumber;

	public void start(int port) {

		try {

			this.portNumber = port;
			ServerBootstrap bootstrap = new ServerBootstrap(
					new NioServerSocketChannelFactory(
							Executors.newCachedThreadPool(),
							Executors.newCachedThreadPool()));
			bootstrap.setPipelineFactory(new WebSocketsServerPipelineFactory());
			bootstrap.bind(new InetSocketAddress(port));

			log.info("-=[ STARTED ]=-");

		} catch (final Exception e) {
			log.error("start()", e);
		}
	}

	public Integer getPortNumber() {

		return portNumber;
	}

	public void setPortNumber(final Integer portNumber) {
		this.portNumber = portNumber;
	}

	public static void newInstance(int port) {

		BasicConfigurator.configure();
		final WebSocketsServer websok = new WebSocketsServer();
		// websok.setPortNumber( Integer.getInteger( args[0]) );
		websok.start(port);
	}
	
	// //////////////////////////////////////////////////////////
	public static void main(final String[] args) {

		newInstance(10000);
	}
}