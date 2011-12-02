package com.rayo.server.lookup

import static org.junit.Assert.*
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ByteArrayResource;

import com.rayo.core.OfferEvent;

class RegexpJIDLookupServiceTest {

	def regexpJIDLookupService
		
	@Test
	public void testInit() {
	
		buildLookupService("""
			.*@localhost.*=rayo-test@conference.jabber.org
		""")
		
		
		
		assertNotNull regexpJIDLookupService
	}
		
	@Test
	public void testLookup() {
		
		buildLookupService("""
			.*@localhost.*=rayo-test@conference.jabber.org
		""")
		
		def uri = new URI("sip:usera@localhost")
		def offer = new OfferEvent()
		offer.setTo(uri)
		def domain = regexpJIDLookupService.lookup(offer)
		assertEquals domain, "rayo-test@conference.jabber.org"
	}
	
	@Test
	public void testMultipleMatchingRulesReturnTheFirstOne() {
		
		buildLookupService("""
			.*usera@localhost.*=usera@conference.jabber.org
			.*@localhost.*=others@conference.jabber.org
		""")

		def offer = new OfferEvent()
		offer.setTo(new URI("sip:usera@localhost"))
		def domain = regexpJIDLookupService.lookup(offer)
		assertEquals domain, "usera@conference.jabber.org"
		
		
		offer.setTo(new URI("sip:userb@localhost"))
		domain = regexpJIDLookupService.lookup(offer)
		assertEquals domain, "others@conference.jabber.org"
	}
	
	
	@Test
	public void testCatchAllRule() {
		
		buildLookupService("""
			.*=all@conference.jabber.org
		""")

		def offer = new OfferEvent()
		offer.setTo(new URI("sip:all@whatever"))
		def domain = regexpJIDLookupService.lookup(offer)
		assertEquals domain, "all@conference.jabber.org"
	}
	
	
	@Test
	public void testIndividualUserRules() {
		
		buildLookupService("""
			.*user1@localhost=user1@conference.jabber.org
			.*user2@localhost=user2@conference.jabber.org
			.*user3@localhost=user3@conference.jabber.org
		""")

		def offer = new OfferEvent()
		offer.setTo(new URI("sip:user1@localhost"))
		assertEquals regexpJIDLookupService.lookup(offer), "user1@conference.jabber.org"
		
		offer.setTo(new URI("sip:user2@localhost"))
		assertEquals regexpJIDLookupService.lookup(offer), "user2@conference.jabber.org"
		
		offer.setTo(new URI("sip:user3@localhost"))
		assertEquals regexpJIDLookupService.lookup(offer), "user3@conference.jabber.org"
	}
	
	@Test
	public void testLookupNotFound() {
		
		buildLookupService("""
			.*@localhost.*=rayo-test@conference.jabber.org
		""")
		def uri = new URI("sip:usera@unknowndomain")
		def offer = new OfferEvent()
		offer.setTo(uri)
		def domain = regexpJIDLookupService.lookup(offer)
		assertNull regexpJIDLookupService.lookup(offer)
	}
	
	
	@Test
	public void testSpacesAreTrimmed() {
		
		buildLookupService("""
			.*@localhost=all@conference.jabber.org     
			
			.*@anotherone=all@anotherone.jabber.org     
			
			
		""")

		def offer = new OfferEvent()
		offer.setTo(new URI("sip:usera@localhost"))
		def domain = regexpJIDLookupService.lookup(offer)
		assertEquals domain, "all@conference.jabber.org"

		offer.setTo(new URI("sip:usera@anotherone"))
		domain = regexpJIDLookupService.lookup(offer)
		assertEquals domain, "all@anotherone.jabber.org"
	}
	
	@Test
	public void testNonStandardPort() {
		
		buildLookupService("""
			.*=all@conference.jabber.org		
		""")

		def offer = new OfferEvent()
		offer.setTo(new URI("sip:usera@localhost:5061"))
		def domain = regexpJIDLookupService.lookup(offer)
		assertEquals domain, "all@conference.jabber.org"
	}

	@Test
	public void testReload() {
		
		buildLookupService("""
			.*=domain1.com
		""")

		def uri = new URI("sip:usera@localhost")
		def offer = new OfferEvent()
		offer.setTo(uri)
		def domain = regexpJIDLookupService.lookup(offer)
		assertEquals domain, "domain1.com"

		def resource = new ByteArrayResource("""
			.*=domain2.com
		""".getBytes())
		regexpJIDLookupService.read(resource)

		domain = regexpJIDLookupService.lookup(offer)
		assertEquals domain, "domain2.com"
	}

	def buildLookupService(String config) {
		
		def resource = new ByteArrayResource(config.getBytes())
		regexpJIDLookupService = new RegexpJIDLookupService(resource)
	}
}
