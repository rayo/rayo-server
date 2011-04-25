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

package com.tropo.server.test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.sip.Address;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.URI;
import javax.servlet.sip.ar.SipApplicationRoutingRegion;

public class MockSipSession implements SipSession {

  private Map<String, Object> _attributes;

  private ServletContext servletContext;

  private String handler;

  @Override
  public SipServletRequest createRequest(String s) throws IllegalStateException, IllegalArgumentException {

    return null;
  }

  @Override
  public SipApplicationSession getApplicationSession() {

    return null;
  }

  @Override
  final public Object getAttribute(String s) throws NullPointerException, IllegalStateException {
    if (_attributes == null) {
      _attributes = new ConcurrentHashMap<String, Object>();
    }
    return _attributes.get(s);
  }

  @Override
  public Enumeration<String> getAttributeNames() throws IllegalStateException {

    return null;
  }

  @Override
  public String getCallId() {

    return null;
  }

  @Override
  public long getCreationTime() {

    return 0;
  }

  @Override
  public String getId() {

    return null;
  }

  @Override
  public boolean getInvalidateWhenReady() {

    return false;
  }

  @Override
  public long getLastAccessedTime() {

    return 0;
  }

  @Override
  public Address getLocalParty() {

    return null;
  }

  @Override
  public SipApplicationRoutingRegion getRegion() throws IllegalStateException {

    return null;
  }

  @Override
  public Address getRemoteParty() {

    return null;
  }

  @Override
  final public ServletContext getServletContext() {
    return servletContext;
  }

  final public void setServletContext(ServletContext s) {
    servletContext = s;
  }

  @Override
  public State getState() throws IllegalStateException {

    return null;
  }

  @Override
  public URI getSubscriberURI() throws IllegalStateException {

    return null;
  }

  @Override
  public void invalidate() throws IllegalStateException {

  }

  @Override
  public boolean isReadyToInvalidate() throws IllegalStateException {

    return false;
  }

  @Override
  public boolean isValid() {

    return false;
  }

  @Override
  public void removeAttribute(String s) throws IllegalStateException {
    if (_attributes == null) {
      _attributes = new ConcurrentHashMap<String, Object>();
    }
    _attributes.remove(s);
  }

  @Override
  final public void setAttribute(String s, Object obj) throws NullPointerException, IllegalStateException {
    if (_attributes == null) {
      _attributes = new ConcurrentHashMap<String, Object>();
    }
    _attributes.put(s, obj);
  }

  @Override
  final public void setHandler(String s) throws ServletException, IllegalStateException {
    handler = s;
  }

  @Override
  public void setInvalidateWhenReady(boolean flag) {

  }

  @Override
  public void setOutboundInterface(InetSocketAddress inetsocketaddress) throws NullPointerException,
      IllegalArgumentException, IllegalStateException {

  }

  @Override
  public void setOutboundInterface(InetAddress inetaddress) throws NullPointerException, IllegalArgumentException,
      IllegalStateException {

  }

}
