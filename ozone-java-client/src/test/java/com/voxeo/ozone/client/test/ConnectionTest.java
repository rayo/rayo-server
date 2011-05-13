package com.voxeo.ozone.client.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.tropo.core.AnswerCommand;
import com.voxeo.ozone.client.SimpleXmppConnection;
import com.voxeo.ozone.client.XmppConnection;
import com.voxeo.ozone.client.XmppException;
import com.voxeo.ozone.client.internal.NettyServer;
import com.voxeo.ozone.client.test.config.TestConfig;
import com.voxeo.ozone.client.test.util.MockResponseHandler;
import com.voxeo.servlet.xmpp.ozone.extensions.Extension;
import com.voxeo.servlet.xmpp.ozone.stanza.Bind;
import com.voxeo.servlet.xmpp.ozone.stanza.Error.Condition;
import com.voxeo.servlet.xmpp.ozone.stanza.Error.Type;
import com.voxeo.servlet.xmpp.ozone.stanza.IQ;
import com.voxeo.servlet.xmpp.ozone.stanza.XmppObject;

public class ConnectionTest {
	
	private XmppConnection connection;

	@Before
	public void setUp() throws Exception {
		
		 NettyServer.newInstance(TestConfig.port);
	}

	@Test
	public void testSendFailsOnNotAuthenticated() throws Exception {
		
		connection = new SimpleXmppConnection(TestConfig.serverEndpoint, TestConfig.port);
		connection.connect();
		
		IQ iq = new IQ(IQ.Type.set)
			.setChild(new Bind().setResource("clienttest"));
		try {
			connection.send(iq);
		} catch (XmppException xe) {
			assertEquals(xe.getMessage(), "Not authenticated. You need to authenticate first.");
			assertEquals(xe.getError().getCondition(), Condition.not_authorized);
			assertEquals(xe.getError().getType(), Type.cancel);
			return;
		}
		fail("Expected exception");
	}

	@Test
	public void testSendFailsOnNotConnected() throws Exception {
		
		connection = new SimpleXmppConnection(TestConfig.serverEndpoint, TestConfig.port);
		
		IQ iq = new IQ(IQ.Type.set)
			.setChild(new Bind().setResource("clienttest"));
		try {
			connection.send(iq);
		} catch (XmppException xe) {
			assertEquals(xe.getMessage(), "Not connected to the server. You need to connect first.");
			assertEquals(xe.getError().getCondition(), Condition.service_unavailable);
			assertEquals(xe.getError().getType(), Type.cancel);
			return;
		}
		fail("Expected exception");
	}

	@Test
	public void testSendFailsOnNonExistentServer() throws Exception {
		
		connection = new SimpleXmppConnection("1234", TestConfig.port);
		
		try {
			connection.connect();
		} catch (XmppException xe) {
			assertEquals(xe.getMessage(), "Error while connecting to 1234:10300");
			assertEquals(xe.getError().getCondition(), Condition.service_unavailable);
			return;
		}
		fail("Expected exception");
	}

	@Test
	public void testWaitForWithTimeout() throws Exception {
		
		connection = new SimpleXmppConnection(TestConfig.serverEndpoint, TestConfig.port);		
		connection.connect();
		connection.login("userc", "1", "voxeo");
		
		long time = System.currentTimeMillis();
		XmppObject object = connection.waitFor("something", 100);
		long timeoff = System.currentTimeMillis();
		assertNull(object);
		assertTrue(timeoff - time >= 100 && timeoff - time < 300);
	}	

	@Test
	public void testWaitForDefaultTimeout() throws Exception {
		
		connection = new SimpleXmppConnection(TestConfig.serverEndpoint, TestConfig.port);		
		connection.connect();
		connection.login("userc", "1", "voxeo");
		((SimpleXmppConnection)connection).setDefaultTimeout(100);
		
		long time = System.currentTimeMillis();
		XmppObject object = connection.waitFor("something");
		long timeoff = System.currentTimeMillis();
		assertNull(object);
		System.out.println("Test time: " + (timeoff - time));
		assertTrue(timeoff - time >= 100 && timeoff - time < 300);
	}	

	@Test
	public void testWaitForExtensionWithTimeout() throws Exception {
		
		connection = new SimpleXmppConnection(TestConfig.serverEndpoint, TestConfig.port);		
		connection.connect();
		connection.login("userc", "1", "voxeo");
		
		long time = System.currentTimeMillis();
		XmppObject object = connection.waitForExtension("something", 100);
		long timeoff = System.currentTimeMillis();
		assertNull(object);
		assertTrue(timeoff - time >= 100 && timeoff - time < 300);
	}	

	@Test
	public void testWaitForExtensionDefaultTimeout() throws Exception {
		
		connection = new SimpleXmppConnection(TestConfig.serverEndpoint, TestConfig.port);		
		connection.connect();
		connection.login("userc", "1", "voxeo");
		((SimpleXmppConnection)connection).setDefaultTimeout(100);
		
		long time = System.currentTimeMillis();
		XmppObject object = connection.waitForExtension("something");
		long timeoff = System.currentTimeMillis();
		assertNull(object);
		System.out.println("Test time: " + (timeoff - time));
		assertTrue(timeoff - time >= 100 && timeoff - time < 300);
	}
	
	@Test
	public void testSendAndWaitWithTimeout() throws Exception {
		
		connection = new SimpleXmppConnection(TestConfig.serverEndpoint, TestConfig.port);		
		connection.connect();
		connection.login("userc", "1", "voxeo");
		
		AnswerCommand answer = new AnswerCommand();
		IQ iq = new IQ(IQ.Type.set)
			.setFrom("userc@localhost")
			.setTo("something")
			.setChild(Extension.create(answer));

		long time = System.currentTimeMillis();
		XmppObject object = connection.sendAndWait(iq,100);
		long timeoff = System.currentTimeMillis();
		assertNull(object);
		assertTrue(timeoff - time >= 100 && timeoff - time < 300);		
	}	

	@Test
	public void testSendAndWaitWithDefaultTimeout() throws Exception {
		
		connection = new SimpleXmppConnection(TestConfig.serverEndpoint, TestConfig.port);		
		connection.connect();
		connection.login("userc", "1", "voxeo");
		((SimpleXmppConnection)connection).setDefaultTimeout(100);
		
		AnswerCommand answer = new AnswerCommand();
		IQ iq = new IQ(IQ.Type.set)
			.setFrom("userc@localhost")
			.setTo("something")
			.setChild(Extension.create(answer));

		long time = System.currentTimeMillis();
		XmppObject object = connection.sendAndWait(iq);
		long timeoff = System.currentTimeMillis();
		assertNull(object);
		assertTrue(timeoff - time >= 100 && timeoff - time < 300);		
	}

	@Test
	public void testSendWithResponseHandler() throws Exception {
		
		connection = new SimpleXmppConnection(TestConfig.serverEndpoint, TestConfig.port);		
		connection.connect();
		connection.login("userc", "1", "voxeo");
		
		MockResponseHandler handler = new MockResponseHandler();
		IQ iq = new IQ(IQ.Type.set)
			.setChild(new Bind().setResource("clienttest"));
		assertEquals(handler.getHandled(),0);
		connection.send(iq, handler);
		Thread.sleep(500);
		assertEquals(handler.getHandled(),1);
		
		// There is no response for answers
		AnswerCommand answer = new AnswerCommand();
		iq = new IQ(IQ.Type.set)
			.setFrom("userc@localhost")
			.setTo("something")
			.setChild(Extension.create(answer));
		connection.send(iq,handler);
		Thread.sleep(500);
		assertEquals(handler.getHandled(),1);
	}
	
	@After
	public void shutdown() throws Exception {
		
		connection.disconnect();
	}
}
