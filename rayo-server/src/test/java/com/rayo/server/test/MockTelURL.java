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

import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;

import javax.servlet.sip.TelURL;

import com.rayo.core.sip.SipGrammar;
import com.voxeo.logging.Loggerf;

public class MockTelURL implements TelURL {

	private static Loggerf logger = Loggerf.getLogger(MockTelURL.class);

	private String _phoneNumber;
	private String _uri;
	private String _scheme;
	private String _number;
	private HashMap<String, String> _params = new HashMap<String, String>();

	public MockTelURL(String url) {
		if (url != null) {
			logger.debug("Url when creating: " + url);
			_uri = url;
			parse();
		}
	}

	public TelURL clone() {
		return null;
	}

	final public String getPhoneNumber() {
		if (_number != null) {
			return _number;
		}
		if (_phoneNumber != null) {
			return _phoneNumber;
		}

		return null;
	}

	@Override
	public String getParameter(String arg0) {
		return null;
	}

	@Override
	public Iterator<String> getParameterNames() {
		return null;
	}

	@Override
	public String getScheme() {
		return null;
	}

	@Override
	public boolean isSipURI() {
		return false;
	}

	@Override
	public void removeParameter(String arg0) {
	}

	@Override
	public void setParameter(String arg0, String arg1) {
	}

	@Override
	public String getPhoneContext() {
		return null;
	}

	@Override
	public boolean isGlobal() {
		return false;
	}

	@Override
	public void setPhoneNumber(String arg0) {
		_phoneNumber = arg0;
	}

	@Override
	public void setPhoneNumber(String arg0, String arg1) {
	}

	private void parse() {
		int indexScheme = _uri.indexOf(':');
		if (indexScheme < 0)
			throw new IllegalArgumentException("Missing TelURL scheme in ["
					+ _uri + "]");

		_scheme = _uri.substring(0, indexScheme);
		if (!"tel".equals(_scheme) && !"fax".equals(_scheme)) {
			throw new IllegalArgumentException("Invalid TelURL scheme ["
					+ _scheme + "] in [" + _uri + "]");
		}
		int indexParam = _uri.indexOf(';', indexScheme);
		if (indexParam < 0) {
			_number = _uri.substring(indexScheme + 1);
		} else {
			_number = _uri.substring(indexScheme + 1, indexParam);
			String normalizedNumber;
			if (_number.startsWith("+")) {
				normalizedNumber = _number.substring(1);
			} else {
				normalizedNumber = _number;
			}
			logger.debug("Parsed TEL number: " + _number);
			if (!SipGrammar.__phoneDigits.containsAll(normalizedNumber))
				throw new IllegalArgumentException("Invalid phone number ["
						+ _number + "] in URI [" + _uri + "]");
			String sParams = _uri.substring(indexParam + 1);
			parseParams(sParams);
		}
	}

	private void parseParams(String sParams) {
		StringTokenizer st = new StringTokenizer(sParams, ";");
		while (st.hasMoreTokens()) {
			String param = st.nextToken();
			String name;
			String value;
			int index = param.indexOf('=');

			if (index < 0) {
				name = param.trim();
				value = "";
			} else {
				name = param.substring(0, index).trim();
				value = param.substring(index + 1).trim();
			}
			if (!SipGrammar.__param.containsAll(name)) {
				throw new IllegalArgumentException("Invalid parameter name ["
						+ name + "] in [" + _uri + "]");
			}
			if (!SipGrammar.__param.containsAll(value)) {
				throw new IllegalArgumentException("Invalid parameter value ["
						+ value + "] in [" + _uri + "]");
			}
			_params.put(SipGrammar.unescape(name.toLowerCase()),
					SipGrammar.unescape(value));
		}
	}
}
