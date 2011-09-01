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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.sip.Address;
import javax.servlet.sip.Parameterable;
import javax.servlet.sip.ServletParseException;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipServletMessage;
import javax.servlet.sip.SipSession;

public class MockSipServletMessage implements SipServletMessage {

  private Map<String, Object> _attributes;

  private byte[] rawContent;

  private MockSipSession session;

  private String contentType;

  private int expires;

  private Address from;
  private Address to;
  
  @Override
  public void addAcceptLanguage(Locale locale) {

  }

  @Override
  public void addAddressHeader(String s, Address address, boolean flag) {

  }

  @Override
  public void addHeader(String s, String s1) {

  }

  @Override
  public void addParameterableHeader(String s, Parameterable parameterable, boolean flag) {

  }

  @Override
  public Locale getAcceptLanguage() {

    return null;
  }

  @Override
  public Iterator<Locale> getAcceptLanguages() {

    return null;
  }

  @Override
  public Address getAddressHeader(String s) throws ServletParseException {

    return null;
  }

  @Override
  public ListIterator<Address> getAddressHeaders(String s) throws ServletParseException {

    return null;
  }

  @Override
  public SipApplicationSession getApplicationSession() {

    return null;
  }

  @Override
  public SipApplicationSession getApplicationSession(boolean flag) {

    return null;
  }

  @Override
  final public Object getAttribute(String s) {
    if (_attributes == null) {
      _attributes = new ConcurrentHashMap<String, Object>();
    }
    return _attributes.get(s);
  }

  @Override
  public Enumeration<String> getAttributeNames() {

    return null;
  }

  @Override
  public String getCallId() {

    return null;
  }

  @Override
  public String getCharacterEncoding() {

    return null;
  }

  @Override
  public Object getContent() throws IOException, UnsupportedEncodingException {

    return null;
  }

  @Override
  public Locale getContentLanguage() {

    return null;
  }

  @Override
  public int getContentLength() {

    return 0;
  }

  @Override
  final public String getContentType() {
    return contentType;
  }

  @Override
  final public int getExpires() {
    return expires;
  }

  @Override
  public Address getFrom() {

    return from;
  }

  @Override
  public String getHeader(String s) {

    return null;
  }

  @Override
  public HeaderForm getHeaderForm() {

    return null;
  }

  @Override
  public Iterator<String> getHeaderNames() {

    return null;
  }

  @Override
  public ListIterator<String> getHeaders(String s) {

    return null;
  }

  @Override
  public String getInitialRemoteAddr() {

    return null;
  }

  @Override
  public int getInitialRemotePort() {

    return 0;
  }

  @Override
  public String getInitialTransport() {

    return null;
  }

  @Override
  public String getLocalAddr() {

    return null;
  }

  @Override
  public int getLocalPort() {

    return 0;
  }

  @Override
  public String getMethod() {

    return null;
  }

  @Override
  public Parameterable getParameterableHeader(String s) throws ServletParseException {

    return null;
  }

  @Override
  public ListIterator<? extends Parameterable> getParameterableHeaders(String s) throws ServletParseException {

    return null;
  }

  @Override
  public String getProtocol() {

    return null;
  }

  @Override
  final public byte[] getRawContent() throws IOException {
    return rawContent;
  }

  final public byte[] setRawContent(byte[] content) {
    if (rawContent == null) {
      rawContent = content;
    }
    return rawContent;
  }

  @Override
  public String getRemoteAddr() {

    return null;
  }

  @Override
  public int getRemotePort() {

    return 0;
  }

  @Override
  public String getRemoteUser() {

    return null;
  }

  @Override
  final public SipSession getSession() {
    return session;
  }

  @Override
  final public SipSession getSession(boolean flag) {
    return session;
  }

  final public void setSession(MockSipSession theSession) {
    session = theSession;
  }

  @Override
  public Address getTo() {

    return to;
  }

  @Override
  public String getTransport() {

    return null;
  }

  @Override
  public Principal getUserPrincipal() {

    return null;
  }

  @Override
  public boolean isCommitted() {

    return false;
  }

  @Override
  public boolean isSecure() {

    return false;
  }

  @Override
  public boolean isUserInRole(String s) {

    return false;
  }

  @Override
  final public void removeAttribute(String s) {
    _attributes.remove(s);
  }

  @Override
  public void removeHeader(String s) {

  }

  @Override
  public void send() throws IOException {

  }

  @Override
  public void setAcceptLanguage(Locale locale) {

  }

  @Override
  public void setAddressHeader(String s, Address address) {

  }

  @Override
  final public void setAttribute(String s, Object obj) {
    if (_attributes == null) {
      _attributes = new ConcurrentHashMap<String, Object>();
    }
    _attributes.put(s, obj);
  }

  @Override
  public void setCharacterEncoding(String s) throws UnsupportedEncodingException {

  }

  @Override
  public void setContent(Object obj, String s) throws UnsupportedEncodingException {

  }

  @Override
  public void setContentLanguage(Locale locale) {

  }

  @Override
  public void setContentLength(int i) {

  }

  @Override
  final public void setContentType(String s) {
    contentType = s;
  }

  @Override
  final public void setExpires(int i) {
    expires = i;
  }

  @Override
  public void setHeader(String s, String s1) {

  }

  @Override
  public void setHeaderForm(HeaderForm headerform) {

  }

  @Override
  public void setParameterableHeader(String s, Parameterable parameterable) {

  }
  
    public void setFrom(Address from) {

	    this.from = from;
    }

    public void setTo(Address to) {
	
	    this.to = to;
    }
}
