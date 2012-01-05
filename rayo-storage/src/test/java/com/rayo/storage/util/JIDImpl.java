package com.rayo.storage.util;

import java.io.Serializable;

import com.voxeo.servlet.xmpp.JID;

public class JIDImpl implements JID, Serializable {

  private static final long serialVersionUID = 1L;

  private String node;

  private String domain;

  private String resource;

  public JIDImpl(String stringJID) {

    int atIndex = stringJID.indexOf("@");

    if (atIndex > 0) {
      node = stringJID.substring(0, atIndex);
    }

    int slashIndex = stringJID.indexOf("/");

    if (slashIndex > 0) {
      resource = stringJID.substring(slashIndex + 1);
      domain = stringJID.substring(atIndex + 1, slashIndex);
    }
    else {
      domain = stringJID.substring(atIndex + 1);
    }
  }

  @Override
  public Object clone() {
    return new JIDImpl(toString());
  }

  public JID getBareJID() {
    if (node != null && node.length() > 0) {
      return new JIDImpl(node + "@" + domain);
    }
    return new JIDImpl(domain);
  }

  public String getDomain() {
    return domain;
  }

  public String getNode() {
    return node;
  }

  public String getResource() {
    return resource;
  }

  public void setDomain(String domain) {
    this.domain = domain;
  }

  public void setNode(String node) {
    this.node = node;
  }

  public void setResource(String resource) {
    this.resource = resource;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((domain == null) ? 0 : domain.hashCode());
    result = prime * result + ((node == null) ? 0 : node.hashCode());
    result = prime * result + ((resource == null) ? 0 : resource.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    JIDImpl other = (JIDImpl) obj;
    if (domain == null) {
      if (other.domain != null)
        return false;
    }
    else if (!domain.equals(other.domain))
      return false;
    if (node == null) {
      if (other.node != null)
        return false;
    }
    else if (!node.equals(other.node))
      return false;
    if (resource == null) {
      if (other.resource != null)
        return false;
    }
    else if (!resource.equals(other.resource))
      return false;
    return true;
  }

  @Override
  public String toString() {
    if (node != null && node.length() > 0) {
      StringBuffer sb = new StringBuffer();
      if (resource != null && resource.length() > 0) {
        return sb.append(node).append("@").append(domain).append("/").append(resource).toString();
      }
      return sb.append(node).append("@").append(domain).toString();
    }
    else if (resource != null && resource.length() > 0) {
      StringBuffer sb = new StringBuffer();
      return sb.append(domain).append("/").append(resource).toString();
    }
    return domain;
  }

}
