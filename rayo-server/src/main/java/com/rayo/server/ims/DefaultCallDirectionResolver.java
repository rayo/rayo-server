package com.rayo.server.ims;

import java.net.URI;
import java.util.ListIterator;

import com.rayo.core.CallDirection;
import com.rayo.core.sip.SipURI;
import com.voxeo.logging.Loggerf;
import com.voxeo.moho.Call;

public class DefaultCallDirectionResolver implements CallDirectionResolver {

	private static Loggerf logger = Loggerf.getLogger(DefaultCallDirectionResolver.class);
	
    public CallDirection resolveDirection(Call call) {

    	// 1st try to guess direction from route headers
    	ListIterator<String> routes = call.getHeaders("Route");
    	if (routes != null) {
    		while (routes.hasNext()) {
    			String route = routes.next();
    			CallDirection routeDir = guessDirectionFromUri(route);
    			if (routeDir != null) {
    		    	logger.debug("Found direction on Route header: [%s]", routeDir);
    				return routeDir;
    			}
    		}
    	}
    	
    	// 2nd. Try to find role parameter on Invitee URI
    	if (call.getInvitee() != null) {
	    	URI inviteeUri = call.getInvitee().getURI();
			if (inviteeUri != null) {
				CallDirection inviteeDirection = guessDirectionFromUri(inviteeUri.toString());
				if (inviteeDirection != null) {
			    	logger.debug("Found direction on Invitee URI: [%s]", inviteeDirection);
					return inviteeDirection;
				}
			}
    	}
		
		// 3rd. Try to find it from sescase and p-served-user
		String pHeader = call.getHeader("P-Served-User");
		if (pHeader != null) {
			try {
				pHeader = removeBrackets(pHeader);
				SipURI uri = new SipURI(pHeader);
				CallDirection direction = extractDirectionFromParameter(uri,"sescase");
				if (direction != null) {
			    	logger.debug("Found direction on P-Served-User header: [%s]", direction);
					return direction;
				}
			}
			catch(RuntimeException e) {
				logger.error(e);
			}
		}
		
		// 4th. Give up. Assume term
    	logger.debug("Could not resolve direction. Defaulting to 'term'.");
		return CallDirection.IN;
	}

	private CallDirection extractDirectionFromParameter(SipURI uri, String p) {
		
    	String parameter = uri.getParameter(p);

    	if (parameter != null) {
    		if (parameter.toLowerCase().equalsIgnoreCase("term")) {
    			return CallDirection.IN;    			
    		} else if (parameter.toLowerCase().equalsIgnoreCase("orig")) {
    			return CallDirection.OUT;
    		}
    	}
    	return null;
	}

	private CallDirection guessDirectionFromUri(String route) {

		route = removeBrackets(route);
		if (route == null) {
			return null;
		} else if (route.startsWith("sip:orig")) {
			return CallDirection.OUT;
		} else if (route.startsWith("sip:term")) {
			return CallDirection.IN;
		} else if (route.startsWith("<sip:") || route.startsWith("sip:")) {
			// strip down <> symbols
	    	SipURI uri = new SipURI(route);
	    	return extractDirectionFromParameter(uri, "role");
		}
		return null;
	}

	private String removeBrackets(String route) {

		int bracket = route.indexOf("<");
		if (bracket != -1) {
			route = route.substring(bracket)
				.replaceAll("<", "")
				.replaceAll(">", "");
		}
		return route;
	}
	
}