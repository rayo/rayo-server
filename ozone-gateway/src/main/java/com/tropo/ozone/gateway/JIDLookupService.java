package com.tropo.ozone.gateway;

import java.util.Map;

public interface JIDLookupService
{
	String lookupJID (String from, String to, Map<String, String> headers);
}
