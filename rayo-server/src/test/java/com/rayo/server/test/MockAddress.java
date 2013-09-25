/**
 * Copyright 2010 Voxeo Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.rayo.server.test;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.sip.Address;
import javax.servlet.sip.SipURI;
import javax.servlet.sip.TelURL;
import javax.servlet.sip.URI;

import com.voxeo.logging.Loggerf;

public class MockAddress implements Address {

	private static Loggerf logger = Loggerf.getLogger(MockAddress.class);

	private SipURI _suri = null;
	private TelURL _turi = null;

	@Override
	public String getDisplayName() {

		return null;
	}

	@Override
	public int getExpires() {

		return 0;
	}

	@Override
	public float getQ() {

		return 0;
	}

	@Override
	final public URI getURI() {
		URI result = null;

		if (_suri != null) {
			logger.debug("SipURI returned: " + _suri.toString());
			result = _suri;
		}
		if (_turi != null) {
			logger.debug("TelURL returned: " + _turi.toString());
			result = _turi;
		}
		return result;
	}

	@Override
	public boolean isWildcard() {

		return false;
	}

	@Override
	public void setDisplayName(String s) {

	}

	@Override
	public void setExpires(int i) {

	}

	@Override
	public void setQ(float f) {

	}

	@Override
	public void setURI(URI uri) {
		if (uri.isSipURI()) {
			logger.debug("SipURI found: " + uri);
			this.setSipUri((SipURI) uri);
		} else {
			logger.debug("TelURL found: " + uri);
			this.setTelUrl((TelURL) uri);
		}
	}

	@Override
	public String getParameter(String s) {

		return null;
	}

	@Override
	public Iterator<String> getParameterNames() {

		return null;
	}

	@Override
	public Set<Entry<String, String>> getParameters() {

		return null;
	}

	@Override
	public String getValue() {

		return null;
	}

	@Override
	public void removeParameter(String s) {

	}

	@Override
	public void setParameter(String s, String s1) {

	}

	@Override
	public void setValue(String s) {

	}

	public Object clone() {
		return null;
	}

	public void setSipUri(SipURI suri) {
		_suri = suri;
		_turi = null;
	}

	public void setTelUrl(TelURL turi) {
		_turi = turi;
		_suri = null;
	}
}
