package com.rayo.core.sip

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertNull
import static org.junit.Assert.assertTrue

import org.junit.Before
import org.junit.Test


class SipURITest {

	@Before
	public void setup() {
	}

	@Test
	public void minAddress() {
		String minAddress = "sip:jdecastro@att.net:1234;foo=bar;bling=baz"
		SipURI su = new SipURI(minAddress);

		assertEquals("sip", su.getScheme());
		assertEquals("jdecastro", su.getUser());
		assertEquals("att.net", su.getHost());
		assertEquals(1234, su.getPort());
	}
}