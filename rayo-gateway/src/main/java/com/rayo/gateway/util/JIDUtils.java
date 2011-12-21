package com.rayo.gateway.util;

public class JIDUtils {

	public static String getDomain(String jid) {
		
		if (jid.contains("@")) {
			jid = jid.substring(jid.indexOf("@")+1, jid.length());
		}
		if (jid.contains("/")) {
			jid = jid.substring(0, jid.indexOf("@"));
		}
		return jid;
	}
	
	public static String getBareJid(String jid) {
		
		if (jid.contains("/")) {
			jid = jid.substring(0, jid.indexOf("/"));
		}
		return jid;
	}
	
	public static String getResource(String jid) {
		
		if (jid.contains("/")) {
			return jid.substring(jid.indexOf("/")+1);
		}
		return null;
	}
}
