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

import javax.servlet.sip.SipURI;
import javax.servlet.sip.URI;

public class MockSipURI implements SipURI {

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
    return null;
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

    return null;
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

  }

  @Override
  public void setUserParam(String arg0) {

  }

  @Override
  public void setUserPassword(String arg0) {

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

    return "sip";
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

}
