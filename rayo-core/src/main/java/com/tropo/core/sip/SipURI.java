// ========================================================================
// Copyright 2008-2009 NEXCOM Systems
// ------------------------------------------------------------------------
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at 
// http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ========================================================================

package com.tropo.core.sip;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

public class SipURI extends BaseURI {

    static final long serialVersionUID = 5118485527934946592L;

    public static final String TRANSPORT_PARAM = "transport";
    public static final String TTL_PARAM = "ttl";
    public static final String MADDR_PARAM = "maddr";
    public static final String METHOD_PARAM = "method";
    public static final String USER_PARAM = "user";
    public static final String LR_PARAM = "lr";

    private String _user;
    private String _password;
    private String _host;
    private String _scheme;
    private String _uri;
    private int _port;

    private HashMap<String, String> _params = new HashMap<String, String>();
    private HashMap<String, String> _headers = new HashMap<String, String>();

    public SipURI(String uri) {
        _uri = uri;
        parse();
    }

    public SipURI(String user, String host, int port) {
        if (user != null) _user = SipGrammar.unescape(user);

        setHost(host);
        _port = port;
        _scheme = "sip";
    }

    public Map<String, String> getParameters() {
        return _params;
    }

    private void parse() {
        int indexScheme = _uri.indexOf(':');
        if (indexScheme < 0) throw new IllegalArgumentException("Missing SIP scheme. URI=[" + _uri + "]");

        _scheme = _uri.substring(0, indexScheme);
        if (!_scheme.equals("sip") && !_scheme.equals("sips")) throw new IllegalArgumentException("Invalid SIP scheme. URI=[" + _uri + "]");

        int indexUser = _uri.indexOf('@', indexScheme + 1);
        int indexHost;

        if (indexUser >= 0) {
            int indexPassword = _uri.indexOf(':', indexScheme + 1);
            String sUser;
            if (indexPassword >= 0 && indexPassword < indexUser) {
                sUser = _uri.substring(indexScheme + 1, indexPassword);
                String sPassword = _uri.substring(indexPassword + 1, indexUser);
                if (!SipGrammar.__passwd.containsAll(sPassword)) throw new IllegalArgumentException("Invalid password [" + sPassword + "] in URI [" + _uri + "]");

                _password = SipGrammar.unescape(sPassword);
            }
            else {
                sUser = _uri.substring(indexScheme + 1, indexUser);
            }
            if (!SipGrammar.__user.containsAll(sUser)) throw new IllegalArgumentException("Invalid user [" + sUser + "] in URI [" + _uri + "]");

            _user = SipGrammar.unescape(sUser);
            indexHost = indexUser + 1;
        }
        else {
            indexHost = indexScheme + 1;
        }
        int indexPort = -1;

        if (_uri.charAt(indexHost) == '[') {
            int i = _uri.indexOf(']', indexHost);
            if (i < 0) throw new IllegalArgumentException("Invalid IPv6 in " + _uri);
            indexPort = _uri.indexOf(':', i);
        }
        else {
            indexPort = _uri.indexOf(':', indexHost);
        }
        int indexParams = _uri.indexOf(';', indexHost);
        int indexHeaders = _uri.indexOf('?', indexHost);

        if ((indexPort > indexParams && indexParams > -1) || (indexPort > indexHeaders && indexHeaders > -1)) indexPort = -1;

        int endHost = indexPort;
        if (endHost < 0) endHost = indexParams;

        if (endHost < 0) endHost = indexHeaders;

        String host;
        if (endHost < 0)
            host = _uri.substring(indexHost);
        else
            host = _uri.substring(indexHost, endHost);
        host = host.trim();
        if (!SipGrammar.__host.containsAll(host)) throw new IllegalArgumentException("Invalid host [" + host + "] in URI [" + _uri + "]");
        setHost(host);

        if (indexPort < 0) {
            _port = -1;
        }
        else {
            int endPort = indexParams;
            if (endPort < 0) endPort = indexHeaders;

            String sPort;
            if (endPort < 0)
                sPort = _uri.substring(indexPort + 1);
            else
                sPort = _uri.substring(indexPort + 1, endPort);
            try {
                _port = Integer.parseInt(sPort);
            }
            catch (NumberFormatException _) {
                throw new IllegalArgumentException("Invalid port number [" + sPort + "] in [" + _uri + "]");
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
            }
            else {
                name = param.substring(0, index).trim();
                value = param.substring(index + 1).trim();
            }
            if (!SipGrammar.__param.containsAll(name)) throw new IllegalArgumentException("Invalid parameter name [" + name + "] in [" + _uri + "]");

            if (!SipGrammar.__param.containsAll(value)) throw new IllegalArgumentException("Invalid parameter value [" + value + "] in [" + _uri + "]");

            _params.put(SipGrammar.unescape(name.toLowerCase()), SipGrammar.unescape(value));
        }
    }

    private void parseHeaders(String sHeaders) {
        StringTokenizer st = new StringTokenizer(sHeaders, "&");
        while (st.hasMoreTokens()) {
            String header = st.nextToken();
            String name;
            String value;
            int index = header.indexOf('=');

            if (index < 0) throw new IllegalArgumentException("Missing value in header [" + header + "] in uri [" + _uri + "]");

            name = header.substring(0, index).trim();
            value = header.substring(index + 1).trim();

            if (!SipGrammar.__header.containsAll(name)) throw new IllegalArgumentException("Invalid header name [" + name + "] in [" + _uri + "]");

            if (!SipGrammar.__header.containsAll(value)) throw new IllegalArgumentException("Invalid header value [" + value + "] in [" + _uri + "]");

            _headers.put(SipGrammar.unescape(name), SipGrammar.unescape(value));
        }
    }

    public boolean isSipURI() {
        return true;
    }

    public String getScheme() {
        return _scheme;
    }

    public boolean isSecure() {
        return "sips".equals(_scheme);
    }

    public void setSecure(boolean b) {
        if (b)
            _scheme = "sips";
        else
            _scheme = "sip";
    }

    public String getUser() {
        return _user;
    }

    public void setUser(String user) {
        _user = user;
    }

    public String getUserPassword() {
        return _password;
    }

    public void setUserPassword(String password) {
        _password = password;
    }

    public String getHost() {
        return _host;
    }

    public void setHost(String host) {
        if (host.contains(":") && !host.contains("["))
            _host = "[" + host + "]";
        else
            _host = host;
    }

    public int getPort() {
        return _port;
    }

    public void setPort(int port) {
        if (port < 0)
            _port = -1;
        else
            _port = port;
    }

    public String getParameter(String name) {
        return (String) _params.get(name.toLowerCase());
    }

    public void setParameter(String name, String value) {
        if (name == null || value == null) throw new NullPointerException("Null value or name");
        _params.put(name.toLowerCase(), value);
    }

    public void removeParameter(String name) {
        _params.remove(name.toLowerCase());
    }

    public synchronized Iterator<String> getParameterNames() {
        return _params.keySet().iterator();
    }

    public String getTransportParam() {
        return getParameter(TRANSPORT_PARAM);
    }

    public void setTransportParam(String transport) {
        setParameter(TRANSPORT_PARAM, transport);
    }

    public String getMAddrParam() {
        return getParameter(MADDR_PARAM);
    }

    public void setMAddrParam(String maddr) {
        setParameter(MADDR_PARAM, maddr);
    }

    public String getMethodParam() {
        return getParameter(METHOD_PARAM);
    }

    public void setMethodParam(String method) {
        setParameter(METHOD_PARAM, method);
    }

    public int getTTLParam() {
        String ttl = getParameter(TTL_PARAM);
        if (ttl != null) {
            try {
                return Integer.parseInt(ttl);
            }
            catch (NumberFormatException _) {
            }
        }
        return -1;
    }

    public void setTTLParam(int ttl) {
        if (ttl < 0)
            removeParameter(TTL_PARAM);
        else
            setParameter(TTL_PARAM, String.valueOf(ttl));
    }

    public String getUserParam() {
        return getParameter(USER_PARAM);
    }

    public void setUserParam(String user) {
        setParameter(USER_PARAM, user);
    }

    public boolean getLrParam() {
        return getParameter(LR_PARAM) != null;
    }

    public void setLrParam(boolean b) {
        if (b)
            setParameter(LR_PARAM, "");
        else
            removeParameter(LR_PARAM);
    }

    public synchronized Iterator<String> getHeaderNames() {
        return _headers.keySet().iterator();
    }

    public String getHeader(String name) {
        if (name == null) throw new NullPointerException("Name is null");
        return (String) _headers.get(name);
    }

    public void setHeader(String name, String value) {
        _headers.put(name, value);
    }

    public void clearHeaders() {
        _headers = new HashMap<String, String>();
    }

    public String toString() {
        StringBuffer sb = new StringBuffer(64);

        sb.append(_scheme);
        sb.append(':');
        if (_user != null) {
            sb.append(SipGrammar.escape(_user, SipGrammar.__user));
            if (_password != null) {
                sb.append(':');
                sb.append(SipGrammar.escape(_password, SipGrammar.__passwd));
            }
            sb.append('@');
        }
        sb.append(_host);
        if (_port > -1) {
            sb.append(':');
            sb.append(_port);
        }

        Iterator<String> it = getParameterNames();
        while (it.hasNext()) {
            String name = it.next();
            String value = getParameter(name);
            sb.append(';');
            sb.append(SipGrammar.escape(name, SipGrammar.__param));
            if (value != null && value.length() > 0) {
                sb.append('=');
                sb.append(SipGrammar.escape(value, SipGrammar.__param));
            }
        }

        Iterator<String> it2 = getHeaderNames();
        boolean first = true;
        while (it2.hasNext()) {
            String name = (String) it2.next();
            String value = getHeader(name);
            if (first) {
                first = false;
                sb.append('?');
            }
            else {
                sb.append('&');
            }
            sb.append(SipGrammar.escape(name, SipGrammar.__header));
            sb.append('=');
            sb.append(SipGrammar.escape(value, SipGrammar.__header));
        }
        return sb.toString();
    }

    public boolean equals(Object o) {
        if (o == null || !(o instanceof SipURI)) return false;

        SipURI other = (SipURI) o;
        if (!_scheme.equals(other.getScheme())) return false;

        if (!equalsUser(other)) return false;

        if (!equalsPassword(other)) return false;

        if (!_host.equalsIgnoreCase(other.getHost())) return false;

        if (_port != other.getPort()) return false;

        Map<String, String> otherParams = new HashMap<String, String>();
        Iterator<String> it = other.getParameterNames();
        while (it.hasNext()) {
            String key = it.next();
            otherParams.put(key, other.getParameter(key));

        }
        if (!equalsParameters(_params, otherParams)) return false;

        if (!equalsParameters(otherParams, _params)) return false;

        if (!equalsHeaders(other)) return false;

        return true;
    }

    private boolean equalsUser(SipURI other) {
        if (_user == null && other.getUser() == null) return true;

        if (_user != null && other.getUser() != null) return _user.equals(other.getUser());

        return false;
    }

    private boolean equalsPassword(SipURI other) {
        if (_password == null && other.getUserPassword() == null) return true;

        if (_password != null && other.getUserPassword() != null) return _password.equals(other.getUserPassword());

        return false;
    }

    private boolean equalsParameters(Map<String, String> m1, Map<String, String> m2) {
        Iterator<String> it = m1.keySet().iterator();
        while (it.hasNext()) {
            String name = it.next();
            if (m2.containsKey(name)) {
                String value1 = m1.get(name);
                String value2 = m2.get(name);
                if (!value1.equalsIgnoreCase(value2)) return false;
            }
        }
        if (m1.containsKey("user") && !m2.containsKey("user")) return false;

        if (m1.containsKey("ttl") && !m2.containsKey("ttl")) return false;

        if (m1.containsKey("method") && !m2.containsKey("method")) return false;

        if (m1.containsKey("maddr") && !m2.containsKey("maddr")) return false;

        if (m1.containsKey("transport") && !m2.containsKey("transport")) return false;

        return true;
    }

    private boolean equalsHeaders(SipURI other) {
        Iterator<String> it = other.getHeaderNames();
        int i = 0;
        while (it.hasNext()) {
            String name = it.next();
            if (_headers.containsKey(name)) {
                if (!_headers.get(name).equalsIgnoreCase(other.getHeader(name))) return false;
            }
            else {
                return false;
            }
            i++;
        }
        if (_headers.size() != i) return false;
        return true;
    }

    public void removeHeader(String name) {
        _headers.remove(name);
    }
}
