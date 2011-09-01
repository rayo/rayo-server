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

package com.rayo.core.sip;

public class SipCharset 
{
	private long[] _charset = new long[4];
	
	public SipCharset() {}
	
	public SipCharset(String charset) 
	{
		set(charset);
	}
	
	public void set(int c)
	{
		_charset[c >> 6] |= bit(c);
	}
	
	public void set(int start, int end) 
	{
		for (int i = start; i <= end; i++)
			set(i);
	}
	
	public void set(String s)
	{
		for (int i = 0; i < s.length(); i++)
		{
			set(s.charAt(i));
		}
	}
	
	private final long bit(int c)
	{
		return 1L << (c & 0x3f);
	}
	
	public final boolean contains(int c)
	{
		int index = c >> 6;
		if (index > 3) return false;
		return ((_charset[index] & bit(c)) != 0);
	}
	
	public final boolean containsAll(String s)
	{
		for (int i = 0; i < s.length(); i++)
		{
			if (!contains(s.charAt(i))) return false;
		}
		return true;
	}
}
