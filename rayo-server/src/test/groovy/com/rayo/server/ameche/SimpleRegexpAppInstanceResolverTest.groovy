package com.rayo.server.ameche;

import static org.junit.Assert.*

import org.dom4j.DocumentHelper
import org.dom4j.Element
import org.junit.Before
import org.junit.Test
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource

import com.rayo.core.CallDirection
import com.rayo.server.CallManager
import com.rayo.server.test.MockSIPFactoryImpl
import com.voxeo.moho.ApplicationContext

class SimpleRegexpAppInstanceResolverTest {

	def sipFactory;
	def applicationContext;
	def callManager;

	@Before
	void init() {
		sipFactory = new MockSIPFactoryImpl()

		applicationContext = [
			getSipFactory : { return sipFactory }
		] as ApplicationContext

		callManager = [
			getApplicationContext : { return applicationContext }
		] as CallManager
	}

	@Test
	void resolveAppInstance() throws Exception {

		Resource routes = new ByteArrayResource(
				".*=1:http://127.0.0.1:4444".getBytes());
		SimpleRegexpAppInstanceResolver resolver = new SimpleRegexpAppInstanceResolver(
				routes);
		resolver.setCallManager(callManager);

		Element offerElement = DocumentHelper.parseText(
				"<offer to=\"tel:+13055195825\" from=\"tel:+15613504458\"/>")
				.getRootElement();
		List<AppInstance> instances = resolver.lookup(offerElement,
				CallDirection.IN);

		assertNotNull(instances);
		assertEquals(instances.size(), 1);
		assertEquals(instances.get(0).getEndpoint(), new URI(
				"http://127.0.0.1:4444"));
		assertFalse(instances.get(0).isRequired());
	}

	@Test
	void resolveRequiredAppInstance() throws Exception {

		Resource routes = new ByteArrayResource(
				".*=1:http://127.0.0.1:4444:true".getBytes());
		SimpleRegexpAppInstanceResolver resolver = new SimpleRegexpAppInstanceResolver(
				routes);
		resolver.setCallManager(callManager);

		Element offerElement = DocumentHelper.parseText(
				"<offer to=\"tel:+13055195825\" from=\"tel:+15613504458\"/>")
				.getRootElement();
		List<AppInstance> instances = resolver.lookup(offerElement,
				CallDirection.IN);

		assertNotNull(instances);
		assertEquals(instances.size(), 1);
		assertEquals(instances.get(0).getEndpoint(), new URI(
				"http://127.0.0.1:4444"));
		assertTrue(instances.get(0).isRequired());
	}

	@Test
	void resolveRequiredAppInstanceWParameter() throws Exception {

		Resource routes = new ByteArrayResource(
				".*=1:http://127.0.0.1:4444:true".getBytes());
		SimpleRegexpAppInstanceResolver resolver = new SimpleRegexpAppInstanceResolver(
				routes);
		resolver.setCallManager(callManager);

		Element offerElement = DocumentHelper.parseText(
				"<offer to=\"tel:+13055195825;biz=baz\" from=\"tel:+15613504458;bling=blang\"/>")
				.getRootElement();
		List<AppInstance> instances = resolver.lookup(offerElement,
				CallDirection.IN);

		assertNotNull(instances);
		assertEquals(instances.size(), 1);
		assertEquals(instances.get(0).getEndpoint(), new URI(
				"http://127.0.0.1:4444"));
		assertTrue(instances.get(0).isRequired());
	}

	@Test
	void resolveRequiredAppInstanceWPServedUser() throws Exception {

		Resource routes = new ByteArrayResource(
				".*=1:http://127.0.0.1:4444:true".getBytes());
		SimpleRegexpAppInstanceResolver resolver = new SimpleRegexpAppInstanceResolver(
				routes);
		resolver.setCallManager(callManager);

		Element offerElement = DocumentHelper.parseText(
				"<offer to=\"tel:+13055195825;biz=baz\" from=\"tel:+15613504458;bling=blang\"> <header name=\"P-Served-User\" value=\"tel:+12152065077;sescase=term;regstate=reg\"/></offer>")
				.getRootElement();
		List<AppInstance> instances = resolver.lookup(offerElement,
				CallDirection.IN);

		assertNotNull(instances);
		assertEquals(instances.size(), 1);
		assertEquals(instances.get(0).getEndpoint(), new URI(
				"http://127.0.0.1:4444"));
		assertTrue(instances.get(0).isRequired());
	}
}
