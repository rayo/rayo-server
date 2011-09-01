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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Locale;

import javax.servlet.ServletOutputStream;
import javax.servlet.sip.Proxy;
import javax.servlet.sip.ProxyBranch;
import javax.servlet.sip.Rel100Exception;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

public class MockSipServletResponse extends MockSipServletMessage implements SipServletResponse {

  private MockSipServletRequest request;

  private int status;

  private String reasonPhrase;

  @Override
  public SipServletRequest createAck() {
    return null;
  }

  @Override
  public SipServletRequest createPrack() throws Rel100Exception {

    return null;
  }

  @Override
  public Iterator<String> getChallengeRealms() {

    return null;
  }

  @Override
  public ServletOutputStream getOutputStream() throws IOException {

    return null;
  }

  @Override
  public Proxy getProxy() {

    return null;
  }

  @Override
  public ProxyBranch getProxyBranch() {

    return null;
  }

  @Override
  final public String getReasonPhrase() {
    return reasonPhrase;
  }

  final public void setReasonPhrase(String s) {
    reasonPhrase = s;
  }

  @Override
  final public SipServletRequest getRequest() {
    return request;
  }

  final public void setRequest(MockSipServletRequest req) {
    request = req;
  }

  @Override
  final public String getMethod() {
    return getRequest().getMethod();
  }

  @Override
  final public int getStatus() {
    return status;
  }

  @Override
  public PrintWriter getWriter() throws IOException {

    return null;
  }

  @Override
  public boolean isBranchResponse() {

    return false;
  }

  @Override
  public void sendReliably() throws Rel100Exception {

  }

  @Override
  final public void setStatus(int i) {
    status = i;
  }

  @Override
  public void setStatus(int i, String s) {

  }

  @Override
  public void flushBuffer() throws IOException {

  }

  @Override
  public int getBufferSize() {

    return 0;
  }

  @Override
  public Locale getLocale() {

    return null;
  }

  @Override
  public void reset() {

  }

  @Override
  public void resetBuffer() {

  }

  @Override
  public void setBufferSize(int size) {

  }

  @Override
  public void setLocale(Locale loc) {

  }

  @Override
  public void setCharacterEncoding(String s) {

  }

  @Override
  protected Object clone() throws CloneNotSupportedException {

    return super.clone();
  }

}
