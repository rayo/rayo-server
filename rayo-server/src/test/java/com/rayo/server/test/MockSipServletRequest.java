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

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.sip.Address;
import javax.servlet.sip.AuthInfo;
import javax.servlet.sip.B2buaHelper;
import javax.servlet.sip.Proxy;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipURI;
import javax.servlet.sip.TooManyHopsException;
import javax.servlet.sip.URI;
import javax.servlet.sip.ar.SipApplicationRoutingDirective;
import javax.servlet.sip.ar.SipApplicationRoutingRegion;

public class MockSipServletRequest extends MockSipServletMessage implements SipServletRequest {

  @SuppressWarnings("unused")
  private MockSipServletResponse response;

  private boolean isInitial;

  private String method;

  @Override
  public void addAuthHeader(SipServletResponse sipservletresponse, AuthInfo authinfo) {

  }

  @Override
  public void addAuthHeader(SipServletResponse sipservletresponse, String s, String s1) {

  }

  @Override
  final public String getMethod() {
    return method;
  }

  final public void setMethod(String theMethod) {
    method = theMethod;
  }

  @Override
  public SipServletRequest createCancel() {

    return null;
  }

  @Override
  public SipServletResponse createResponse(int i) {
    return null;
  }

  final public void setResponse(MockSipServletResponse resp) {
    response = resp;
  }

  @Override
  final public SipServletResponse createResponse(int i, String s) {
    MockSipServletResponse ret = (MockSipServletResponse) createResponse(i);
    ret.setReasonPhrase(s);
    return ret;
  }

  @Override
  public B2buaHelper getB2buaHelper() {

    return null;
  }

  @Override
  public Address getInitialPoppedRoute() {

    return null;
  }

  @Override
  public ServletInputStream getInputStream() throws IOException {

    return null;
  }

  @Override
  public int getMaxForwards() {

    return 0;
  }

  @Override
  public Address getPoppedRoute() {

    return null;
  }

  @Override
  public Proxy getProxy() throws TooManyHopsException {

    return null;
  }

  @Override
  public Proxy getProxy(boolean flag) throws TooManyHopsException {

    return null;
  }

  @Override
  public BufferedReader getReader() throws IOException {

    return null;
  }

  @Override
  public SipApplicationRoutingRegion getRegion() {

    return null;
  }

  @Override
  public URI getRequestURI() {

    return null;
  }

  @Override
  public SipApplicationRoutingDirective getRoutingDirective() throws IllegalStateException {

    return null;
  }

  @Override
  public URI getSubscriberURI() {

    return null;
  }

  @Override
  final public boolean isInitial() {
    return isInitial;
  }

  final public void setIsInitial(boolean initial) {
    isInitial = initial;
  }

  @Override
  public void pushPath(Address address) {

  }

  @Override
  public void pushRoute(SipURI sipuri) {

  }

  @Override
  public void pushRoute(Address address) {

  }

  @Override
  public void setMaxForwards(int i) {

  }

  @Override
  public void setRequestURI(URI uri) {

  }

  @Override
  public void setRoutingDirective(SipApplicationRoutingDirective sipapplicationroutingdirective,
      SipServletRequest sipservletrequest) throws IllegalStateException {

  }

  @Override
  public String getLocalName() {

    return null;
  }

  @Override
  public Locale getLocale() {

    return null;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Enumeration getLocales() {

    return null;
  }

  @Override
  public String getParameter(String name) {

    return null;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Map getParameterMap() {

    return null;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Enumeration getParameterNames() {

    return null;
  }

  @Override
  public String[] getParameterValues(String name) {

    return null;
  }

  @Override
  public String getRealPath(String path) {

    return null;
  }

  @Override
  public String getRemoteHost() {

    return null;
  }

  @Override
  public RequestDispatcher getRequestDispatcher(String path) {

    return null;
  }

  @Override
  public String getScheme() {

    return null;
  }

  @Override
  public String getServerName() {

    return null;
  }

  @Override
  public int getServerPort() {

    return 0;
  }

}
