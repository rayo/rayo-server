package com.rayo.core.sip

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertNull
import static org.junit.Assert.assertTrue

import org.junit.Before
import org.junit.Test

import com.rayo.core.tel.TelURI


class TelURITest {

	@Before
	public void setup() {
	}

	@Test
	public void minTel() {
		String minTel = "tel:+12152065077;sescase=term;regstate=reg"
		TelURI tu = new TelURI(minTel);

		assertEquals("tel", tu.getScheme());
		assertEquals("12152065077", tu.getPhoneNumber());
	}

	@Test
	public void minBaseTel() {
		String minTel = "tel:+12152065077;sescase=term;regstate=reg"
		TelURI tu = new TelURI(minTel);

		assertEquals("+12152065077", tu.getBasePhoneNumber());
	}
}