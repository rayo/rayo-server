package com.voxeo.ozone.client.internal;

import org.junit.After;
import org.junit.Before;

import com.voxeo.ozone.client.OzoneClient;
import com.voxeo.ozone.client.test.config.TestConfig;

public abstract class XmppIntegrationTest {

	protected OzoneClient ozone;
	private String username = "userc";
	private NettyServer server;
		
	@Before
	public void setUp() throws Exception {
		
		server = NettyServer.newInstance(TestConfig.port);

		ozone = new OzoneClient(TestConfig.serverEndpoint, TestConfig.port);
		login(username, "1", "voxeo");
		
		server.sendOzoneOffer();
		// Let the offer come back and be caught by the OzoneClient's listener
		Thread.sleep(100);
	}
		
	public void assertServerReceived(String message) {
		
		message = message.replaceAll("#callId", ozone.getLastCallId());
		server.assertReceived(message);
	}
	
	@After
	public void dispose() throws Exception {
		
		ozone.disconnect();
	}
		
	void login(String username, String password, String resource) throws Exception {
		
		ozone.connect(username, password, resource);		
	}
	
	void disconnect() throws Exception {
		
		ozone.disconnect();
	}
	
	protected void setUsername(String username) {
		
		this.username = username;
	}
}
