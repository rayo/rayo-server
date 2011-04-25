package com.tropo.server.test;

import java.io.Serializable;
import java.util.Iterator;

import javax.servlet.sip.URI;

public class URIImpl implements URI, Serializable {
  protected String m_uri;

  protected String m_scheme;
  
  protected boolean m_flag = false;
  
  protected int m_hashCode = 0;

  /** Used by SipURLImpl ctor. */
  URIImpl() {
  }

  public URIImpl(String uri, String scheme) {
    m_flag = true;
    m_uri = uri;
    m_scheme = scheme;
  }

  public String getScheme() {
    return m_scheme;
  }

  public boolean isSipURI() {
    return false;
  }

  public String toString() {
    return m_uri;
  }

  // overridden by SipUriImpl subclass
  public URI getReadOnlyWrapper() {
    return this;
  }

  public URI clone() {
    try {
      return (URI)super.clone();
    }
    catch (CloneNotSupportedException e) {
      // can't happen
      return null;
    }
  }
  
  public int hashCode() {
    int h = m_hashCode;
    if (h == 0 || m_flag) {
      if (m_scheme == null && m_uri == null) {
        h = 0;
      }
      else {
        h = (m_scheme + m_uri).hashCode();
      }
      m_hashCode = h;
      m_flag = false;
    }
    return h;
  }
  
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof URIImpl)) {
      return false;
    }
    
    URIImpl uri = (URIImpl) obj;
    return uri.hashCode() == hashCode();
  }

@Override
public String getParameter(String arg0) {
	// TODO Auto-generated method stub
	return null;
}

@Override
public Iterator<String> getParameterNames() {
	// TODO Auto-generated method stub
	return null;
}

@Override
public void removeParameter(String arg0) {
	// TODO Auto-generated method stub
	
}

@Override
public void setParameter(String arg0, String arg1) {
	// TODO Auto-generated method stub
	
}
  
}