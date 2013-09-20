package com.rayo.server.ameche;

import java.net.URI;
import java.util.List;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.junit.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import com.rayo.core.CallDirection;

import static org.junit.Assert.*;

public class SimpleRegexpAppInstanceResolverTest {

	@Test
	public void resolveAppInstance() throws Exception {
		
		Resource routes = new ByteArrayResource(".*=1:http://127.0.0.1:4444".getBytes());
		SimpleRegexpAppInstanceResolver resolver = new SimpleRegexpAppInstanceResolver(routes);
		Element offerElement = DocumentHelper.parseText("<offer to=\"tel:+13055195825\" from=\"tel:+15613504458\"/>").getRootElement();
		List<AppInstance> instances = resolver.lookup(offerElement, CallDirection.IN);
		
		assertNotNull(instances);
		assertEquals(instances.size(), 1);
		assertEquals(instances.get(0).getEndpoint(), new URI("http://127.0.0.1:4444"));
		assertFalse(instances.get(0).isRequired());
	}

	@Test
	public void resolveRequiredAppInstance() throws Exception {
		
		Resource routes = new ByteArrayResource(".*=1:http://127.0.0.1:4444:true".getBytes());
		SimpleRegexpAppInstanceResolver resolver = new SimpleRegexpAppInstanceResolver(routes);
		Element offerElement = DocumentHelper.parseText("<offer to=\"tel:+13055195825\" from=\"tel:+15613504458\"/>").getRootElement();
		List<AppInstance> instances = resolver.lookup(offerElement, CallDirection.IN);
		
		assertNotNull(instances);
		assertEquals(instances.size(), 1);
		assertEquals(instances.get(0).getEndpoint(), new URI("http://127.0.0.1:4444"));
		assertTrue(instances.get(0).isRequired());
	}

}
