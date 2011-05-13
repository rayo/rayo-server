package com.voxeo.ozone.client.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.voxeo.ozone.client.SimpleXmppConnection;
import com.voxeo.ozone.client.XmppConnection;
import com.voxeo.ozone.client.filter.XmppObjectExtensionNameFilter;
import com.voxeo.ozone.client.filter.XmppObjectIdFilter;
import com.voxeo.ozone.client.filter.XmppObjectNameFilter;
import com.voxeo.ozone.client.internal.NettyServer;
import com.voxeo.ozone.client.test.config.TestConfig;
import com.voxeo.servlet.xmpp.ozone.stanza.Bind;
import com.voxeo.servlet.xmpp.ozone.stanza.IQ;

public class XmppFilterTest {
	
	private XmppConnection connection;

	@Before
	public void setUp() throws Exception {
		
		 NettyServer.newInstance(TestConfig.port);
	}
	
	@Test
	public void testAddExtensionNameFilter() throws Exception {
		
		connection = new SimpleXmppConnection(TestConfig.serverEndpoint, TestConfig.port);		
		XmppObjectExtensionNameFilter filter = new XmppObjectExtensionNameFilter("bind");
		connection.addFilter(filter);
		connection.connect();
		connection.login("userc", "1", "voxeo");
		
		assertNotNull(filter.poll());
	}	
	
	@Test
	public void testRemoveExtensionNameFilter() throws Exception {
		
		connection = new SimpleXmppConnection(TestConfig.serverEndpoint, TestConfig.port);
		
		XmppObjectExtensionNameFilter filter = new XmppObjectExtensionNameFilter("bind");
		filter.setDefaultTimeout(100);
		connection.addFilter(filter);
		connection.removeFilter(filter);
		connection.connect();
		connection.login("userc", "1", "voxeo");
		
		assertNull(filter.poll());
	}
	
	@Test
	public void testNotMatchedExtensionNameFilter() throws Exception {
		
		connection = new SimpleXmppConnection(TestConfig.serverEndpoint, TestConfig.port);
		
		XmppObjectExtensionNameFilter filter = new XmppObjectExtensionNameFilter("bibibi");
		filter.setDefaultTimeout(100);
		connection.addFilter(filter);
		connection.removeFilter(filter);
		connection.connect();
		connection.login("userc", "1", "voxeo");
		
		assertNull(filter.poll());
	}
	
	@Test
	public void testFilterTimeout() throws Exception {
		
		connection = new SimpleXmppConnection(TestConfig.serverEndpoint, TestConfig.port);
		
		XmppObjectExtensionNameFilter filter = new XmppObjectExtensionNameFilter("bibibi");
		connection.addFilter(filter);
		connection.removeFilter(filter);
		connection.connect();
		connection.login("userc", "1", "voxeo");

		long time = System.currentTimeMillis();
		assertNull(filter.poll(100));
		long timeoff = System.currentTimeMillis();
		assertTrue(timeoff - time >= 100 && timeoff - time < 300);				
	}
	
	@Test
	public void testFilterDefaultTimeout() throws Exception {
		
		connection = new SimpleXmppConnection(TestConfig.serverEndpoint, TestConfig.port);
		
		XmppObjectExtensionNameFilter filter = new XmppObjectExtensionNameFilter("bibibi");
		filter.setDefaultTimeout(100);
		connection.addFilter(filter);
		connection.removeFilter(filter);
		connection.connect();
		connection.login("userc", "1", "voxeo");

		long time = System.currentTimeMillis();
		assertNull(filter.poll());
		long timeoff = System.currentTimeMillis();
		assertTrue(timeoff - time >= 100 && timeoff - time < 300);				
	}
	
	@Test
	public void testAddObjectNameFilter() throws Exception {
		
		connection = new SimpleXmppConnection(TestConfig.serverEndpoint, TestConfig.port);		
		XmppObjectNameFilter filter = new XmppObjectNameFilter("iq");
		connection.addFilter(filter);
		connection.connect();
		connection.login("userc", "1", "voxeo");
		
		assertNotNull(filter.poll());
	}	
	
	@Test
	public void testRemoveObjectNameFilter() throws Exception {
		
		connection = new SimpleXmppConnection(TestConfig.serverEndpoint, TestConfig.port);
		
		XmppObjectNameFilter filter = new XmppObjectNameFilter("iq");
		filter.setDefaultTimeout(100);
		connection.addFilter(filter);
		connection.removeFilter(filter);
		connection.connect();
		connection.login("userc", "1", "voxeo");
		
		assertNull(filter.poll());
	}
	
	@Test
	public void testNotMatchedObjectNameFilter() throws Exception {
		
		connection = new SimpleXmppConnection(TestConfig.serverEndpoint, TestConfig.port);
		
		XmppObjectNameFilter filter = new XmppObjectNameFilter("bibibi");
		filter.setDefaultTimeout(100);
		connection.addFilter(filter);
		connection.removeFilter(filter);
		connection.connect();
		connection.login("userc", "1", "voxeo");
		
		assertNull(filter.poll());
	}
	
	@Test
	public void testAddObjectIdFilter() throws Exception {
		
		connection = new SimpleXmppConnection(TestConfig.serverEndpoint, TestConfig.port);		
		connection.connect();
		connection.login("userc", "1", "voxeo");
		
		IQ iq = new IQ(IQ.Type.set)
			.setChild(new Bind().setResource("clienttest"));
		XmppObjectIdFilter filter = new XmppObjectIdFilter(iq.getId());
		connection.addFilter(filter);
		
		connection.send(iq);
		assertNotNull(filter.poll());
	}	
	
	@Test
	public void testRemoveObjectIdFilter() throws Exception {
		
		connection = new SimpleXmppConnection(TestConfig.serverEndpoint, TestConfig.port);		
		connection.connect();
		connection.login("userc", "1", "voxeo");
		
		IQ iq = new IQ(IQ.Type.set)
			.setChild(new Bind().setResource("clienttest"));
		XmppObjectIdFilter filter = new XmppObjectIdFilter(iq.getId());
		filter.setDefaultTimeout(100);
		connection.addFilter(filter);
		connection.removeFilter(filter);
		
		connection.send(iq);
		assertNull(filter.poll());
	}	
	
	@Test
	public void testNotMatchedObjectIdFilter() throws Exception {
		
		connection = new SimpleXmppConnection(TestConfig.serverEndpoint, TestConfig.port);		
		connection.connect();
		connection.login("userc", "1", "voxeo");
		
		IQ iq = new IQ(IQ.Type.set)
			.setChild(new Bind().setResource("clienttest"));
		XmppObjectIdFilter filter = new XmppObjectIdFilter("abcdef");
		filter.setDefaultTimeout(100);
		connection.addFilter(filter);
		
		connection.send(iq);
		assertNull(filter.poll());
	}
	
	@After
	public void shutdown() throws Exception {
		
		connection.disconnect();
	}
}
