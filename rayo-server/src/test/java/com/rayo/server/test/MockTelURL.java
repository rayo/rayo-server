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

import javax.servlet.sip.TelURL;

public class MockTelURL implements TelURL {

	private String _phoneNumber;

	public TelURL clone() {
		return null;
	}

	final public String getPhoneNumber() {
		return _phoneNumber;
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

}
