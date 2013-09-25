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

import javax.servlet.sip.SipURI;
import javax.servlet.sip.URI;

import com.rayo.core.sip.SipGrammar;

public class MockSipURI implements SipURI {

	private String _user;
	private String _host;
	private String _uri;
	private String _scheme;
	private String _password;
	private int _port;
	private HashMap<String, String> _params = new HashMap<String, String>();
	private HashMap<String, String> _headers = new HashMap<String, String>();

	public MockSipURI(String uri) {
		if (uri != null) {
			_uri = uri;
			parse();
		}
	}

	final public URI clone() {
		return this;
	}

	@Override
	public String getHeader(String arg0) {
		return null;
	}

	@Override
	public Iterator<String> getHeaderNames() {
		return null;
	}

	@Override
	public String getHost() {
		return _host;
	}

	@Override
	public boolean getLrParam() {

		return false;
	}

	@Override
	public String getMAddrParam() {

		return null;
	}

	@Override
	public String getMethodParam() {

		return null;
	}

	@Override
	public int getPort() {

		return 0;
	}

	@Override
	public int getTTLParam() {

		return 0;
	}

	@Override
	public String getTransportParam() {

		return null;
	}

	@Override
	public String getUser() {
		return _user;
	}

	@Override
	public String getUserParam() {

		return null;
	}

	@Override
	public String getUserPassword() {

		return null;
	}

	@Override
	public boolean isSecure() {

		return false;
	}

	@Override
	final public void removeHeader(String arg0) {

	}

	@Override
	public void setHeader(String arg0, String arg1) {

	}

	@Override
	public void setHost(String arg0) {
		_host = arg0;
	}

	@Override
	public void setLrParam(boolean arg0) {

	}

	@Override
	public void setMAddrParam(String arg0) {

	}

	@Override
	public void setMethodParam(String arg0) {

	}

	@Override
	public void setPort(int arg0) {

	}

	@Override
	public void setSecure(boolean arg0) {

	}

	@Override
	public void setTTLParam(int arg0) {

	}

	@Override
	public void setTransportParam(String arg0) {

	}

	@Override
	public void setUser(String arg0) {
		_user = arg0;
	}

	@Override
	public void setUserParam(String arg0) {

	}

	@Override
	public void setUserPassword(String arg0) {

	}

	@Override
	public String getParameter(String name) {
		return (String) _params.get(name.toLowerCase());
	}

	@Override
	public Iterator<String> getParameterNames() {

		return null;
	}

	@Override
	public String getScheme() {

		return "sip";
	}

	@Override
	public boolean isSipURI() {

		return true;
	}

	@Override
	public void removeParameter(String arg0) {

	}

	@Override
	public void setParameter(String arg0, String arg1) {

	}

	private void parse() {
		int indexScheme = _uri.indexOf(':');
		if (indexScheme < 0)
			throw new IllegalArgumentException("Missing SIP scheme. URI=["
					+ _uri + "]");

		_scheme = _uri.substring(0, indexScheme);
		if (!_scheme.equals("sip") && !_scheme.equals("sips"))
			throw new IllegalArgumentException("Invalid SIP scheme. URI=["
					+ _uri + "]");

		int indexUser = _uri.indexOf('@', indexScheme + 1);
		int indexHost;

		if (indexUser >= 0) {
			int indexPassword = _uri.indexOf(':', indexScheme + 1);
			String sUser;
			if (indexPassword >= 0 && indexPassword < indexUser) {
				sUser = _uri.substring(indexScheme + 1, indexPassword);
				String sPassword = _uri.substring(indexPassword + 1, indexUser);
				if (!SipGrammar.__passwd.containsAll(sPassword))
					throw new IllegalArgumentException("Invalid password ["
							+ sPassword + "] in URI [" + _uri + "]");

				_password = SipGrammar.unescape(sPassword);
			} else {
				sUser = _uri.substring(indexScheme + 1, indexUser);
			}
			if (!SipGrammar.__user.containsAll(sUser))
				throw new IllegalArgumentException("Invalid user [" + sUser
						+ "] in URI [" + _uri + "]");

			_user = SipGrammar.unescape(sUser);
			indexHost = indexUser + 1;
		} else {
			indexHost = indexScheme + 1;
		}
		int indexPort = -1;

		if (_uri.charAt(indexHost) == '[') {
			int i = _uri.indexOf(']', indexHost);
			if (i < 0)
				throw new IllegalArgumentException("Invalid IPv6 in " + _uri);
			indexPort = _uri.indexOf(':', i);
		} else {
			indexPort = _uri.indexOf(':', indexHost);
		}
		int indexParams = _uri.indexOf(';', indexHost);
		int indexHeaders = _uri.indexOf('?', indexHost);

		if ((indexPort > indexParams && indexParams > -1)
				|| (indexPort > indexHeaders && indexHeaders > -1))
			indexPort = -1;

		int endHost = indexPort;
		if (endHost < 0)
			endHost = indexParams;

		if (endHost < 0)
			endHost = indexHeaders;

		String host;
		if (endHost < 0)
			host = _uri.substring(indexHost);
		else
			host = _uri.substring(indexHost, endHost);
		host = host.trim();
		if (!SipGrammar.__host.containsAll(host))
			throw new IllegalArgumentException("Invalid host [" + host
					+ "] in URI [" + _uri + "]");
		setHost(host);

		if (indexPort < 0) {
			_port = -1;
		} else {
			int endPort = indexParams;
			if (endPort < 0)
				endPort = indexHeaders;

			String sPort;
			if (endPort < 0)
				sPort = _uri.substring(indexPort + 1);
			else
				sPort = _uri.substring(indexPort + 1, endPort);
			try {
				_port = Integer.parseInt(sPort);
			} catch (NumberFormatException _) {
				throw new IllegalArgumentException("Invalid port number ["
						+ sPort + "] in [" + _uri + "]");
			}
		}
		if (indexParams >= 0) {
			String params;
			if (indexHeaders < 0)
				params = _uri.substring(indexParams + 1);
			else
				params = _uri.substring(indexParams + 1, indexHeaders);
			parseParams(params);
		}
		if (indexHeaders >= 0) {
			String headers = _uri.substring(indexHeaders + 1);
			parseHeaders(headers);
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
			if (!SipGrammar.__param.containsAll(name))
				throw new IllegalArgumentException("Invalid parameter name ["
						+ name + "] in [" + _uri + "]");

			if (!SipGrammar.__param.containsAll(value))
				throw new IllegalArgumentException("Invalid parameter value ["
						+ value + "] in [" + _uri + "]");

			_params.put(SipGrammar.unescape(name.toLowerCase()),
					SipGrammar.unescape(value));
		}
	}

	private void parseHeaders(String sHeaders) {
		StringTokenizer st = new StringTokenizer(sHeaders, "&");
		while (st.hasMoreTokens()) {
			String header = st.nextToken();
			String name;
			String value;
			int index = header.indexOf('=');

			if (index < 0)
				throw new IllegalArgumentException("Missing value in header ["
						+ header + "] in uri [" + _uri + "]");

			name = header.substring(0, index).trim();
			value = header.substring(index + 1).trim();

			if (!SipGrammar.__header.containsAll(name))
				throw new IllegalArgumentException("Invalid header name ["
						+ name + "] in [" + _uri + "]");

			if (!SipGrammar.__header.containsAll(value))
				throw new IllegalArgumentException("Invalid header value ["
						+ value + "] in [" + _uri + "]");

			_headers.put(SipGrammar.unescape(name), SipGrammar.unescape(value));
		}
	}
}
