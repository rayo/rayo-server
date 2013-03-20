package com.rayo.server

import org.apache.commons.collections.iterators.ListIteratorWrapper;

import com.rayo.core.CallDirection;
import com.voxeo.moho.Call
import com.voxeo.moho.CallableEndpoint;
import com.rayo.core.sip.SipURI

import static org.junit.Assert.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

class ResolveDirectionTest {

	@Test
	public void resolveDefaultsToTerm() {
		
		def call = [getHeaders:{new ListIteratorWrapper([].iterator())}, 
					getInvitee:{null}, 
					getHeader:{null}] as Call
		def actor = new IncomingCallActor(null);
		
		assertEquals actor.resolveDirection(call), CallDirection.TERM
	}
	
	@Test
	public void resolveOrigFromSingleRouteHeader() {
		
		def call = [getHeaders:{new ListIteratorWrapper(["<sip:orig@scscf.open-ims.test:6060;lr>"].iterator())},
					getInvitee:{null},
					getHeader:{null}] as Call
		def actor = new IncomingCallActor(null);
		
		assertEquals actor.resolveDirection(call), CallDirection.ORIG
	}
	
	@Test
	public void resolveOrigFromMultipleRouteHeaders() {
		def call = [getHeaders:{new ListIteratorWrapper(["<sip:pcscf.open-ims.test:4060;lr>",
														  "<sip:orig@scscf.open-ims.test:6060;lr>"].iterator())},
					getInvitee:{null},
					getHeader:{null}] as Call
		def actor = new IncomingCallActor(null);
		
		assertEquals actor.resolveDirection(call), CallDirection.ORIG
	}
	
	@Test
	public void resolveOrigFromSingleRouteHeaderAndRoleParameter() {
		
		def call = [getHeaders:{new ListIteratorWrapper(["<sip:scscf@scscf.open-ims.test:6060; role=orig; lr>"].iterator())},
					getInvitee:{null},
					getHeader:{null}] as Call
		def actor = new IncomingCallActor(null);
		
		assertEquals actor.resolveDirection(call), CallDirection.ORIG
	}
	
	@Test
	public void resolveOrigFromMultipleRouteHeadersAndRoleParameter() {
		def call = [getHeaders:{new ListIteratorWrapper(["<sip:pcscf.open-ims.test:4060>",
														  "<sip:scsfs@scscf.open-ims.test:6060; role=orig; lr>"].iterator())},
					getInvitee:{null},
					getHeader:{null}] as Call
		def actor = new IncomingCallActor(null);
		
		assertEquals actor.resolveDirection(call), CallDirection.ORIG
	}
	
	@Test
	public void resolveOrigFromInvitee() {
		
		def uri = new URI("sip:serviceFoo@Bish.msf.org;role=orig;lr")
		def callableEndpoint = [getURI:{uri}] as CallableEndpoint
		def call = [getHeaders:{new ListIteratorWrapper([].iterator())},
					getInvitee:{callableEndpoint},
					getHeader:{null}] as Call
		def actor = new IncomingCallActor(null);
		
		assertEquals actor.resolveDirection(call), CallDirection.ORIG
	}
	
	@Test
	public void resolveOrigFromPHeader() {

		def call = [getHeaders:{new ListIteratorWrapper([].iterator())},
					getInvitee:{null},
					getHeader:{"<sip:user@example.com>; sescase=orig; regstate=reg"}] as Call
		def actor = new IncomingCallActor(null);
		
		assertEquals actor.resolveDirection(call), CallDirection.ORIG
	}
	
	@Test
	public void resolveTermFromSingleRouteHeader() {
		
		// We add an orig on invitee so this also tests rule precedence
		
		def uri = new URI("sip:serviceFoo@Bish.msf.org;role=orig;lr")
		def callableEndpoint = [getURI:{uri}] as CallableEndpoint
		def call = [getHeaders:{new ListIteratorWrapper(["<sip:term@scscf.open-ims.test:6060;lr>"].iterator())},
					getInvitee:{callableEndpoint},
					getHeader:{null}] as Call
		def actor = new IncomingCallActor(null);
		
		assertEquals actor.resolveDirection(call), CallDirection.TERM
	}
	
	@Test
	public void resolveTermFromMultipleRouteHeaders() {
		
		// We add an orig on invitee so this also tests rule precedence
		
		def uri = new URI("sip:serviceFoo@Bish.msf.org;role=orig;lr")
		def callableEndpoint = [getURI:{uri}] as CallableEndpoint
		def call = [getHeaders:{new ListIteratorWrapper(["<sip:pcscf.open-ims.test:4060;lr>",
														  "<sip:term@scscf.open-ims.test:6060;lr>"].iterator())},
					getInvitee:{callableEndpoint},
					getHeader:{null}] as Call
		def actor = new IncomingCallActor(null);
		
		assertEquals actor.resolveDirection(call), CallDirection.TERM
	}
	
	@Test
	public void resolveTermFromInvitee() {
		
		// We add an orig on p-served-user so this also tests rule precedence
		
		def uri = new URI("sip:serviceFoo@Bish.msf.org;role=term;lr")
		def callableEndpoint = [getURI:{uri}] as CallableEndpoint
		def call = [getHeaders:{new ListIteratorWrapper([].iterator())},
					getInvitee:{callableEndpoint},
					getHeader:{"<sip:user@example.com>; sescase=orig; regstate=reg"}] as Call
		def actor = new IncomingCallActor(null);
		
		assertEquals actor.resolveDirection(call), CallDirection.TERM
	}
	
	
	@Test
	public void resolveTermFromPHeader() {

		def call = [getHeaders:{new ListIteratorWrapper([].iterator())},
					getInvitee:{null},
					getHeader:{"<sip:user@example.com>; sescase=term; regstate=reg"}] as Call
		def actor = new IncomingCallActor(null);
		
		assertEquals actor.resolveDirection(call), CallDirection.TERM
	}
}
