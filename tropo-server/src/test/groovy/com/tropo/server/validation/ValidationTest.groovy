package com.tropo.server.validation;

import static org.junit.Assert.*

import java.io.StringReader

import org.dom4j.Element
import org.dom4j.io.SAXReader
import org.junit.Before
import org.junit.Test

import com.tropo.core.validation.Messages
import com.tropo.core.validation.ValidationException
import com.tropo.core.validation.Validator
import com.tropo.core.xml.OzoneProvider
import com.tropo.core.xml.providers.AskProvider
import com.tropo.core.xml.providers.ConferenceProvider
import com.tropo.core.xml.providers.SayProvider
import com.tropo.core.xml.providers.TransferProvider
import com.tropo.server.exception.ExceptionMapper
import com.voxeo.servlet.xmpp.XmppStanzaError


class ValidationTest {

	def providers
	def mapper
	
	@Before
	public void init() {
		
		def validator = new Validator()
		providers = [new OzoneProvider(validator:validator),
					 new SayProvider(validator:validator),
					 new AskProvider(validator:validator),
					 new TransferProvider(validator:validator),
					 new ConferenceProvider(validator:validator)]

		mapper = new ExceptionMapper()
	}
	
	// Say
	// ====================================================================================
	
	@Test
	public void validateSayEmptyPromptItems() {
				
		def say = parseXml("""<say xmlns=\"urn:xmpp:ozone:say:1\" voice=\"allison\"></say>""")
		
		def errorMapping = assertValidationException(say)
		assertNotNull errorMapping
		assertEquals errorMapping.type, XmppStanzaError.Type_MODIFY
		assertEquals errorMapping.condition, XmppStanzaError.BAD_REQUEST_CONDITION
		assertEquals errorMapping.text, Messages.MISSING_PROMPT_ITEMS
	}
	
	@Test
	public void validateSayInvalidPromptItemsURI() {
				
		def say = parseXml("""<say xmlns=\"urn:xmpp:ozone:say:1\" voice=\"allison\"><audio url="\$?\\.com"/></say>""")
		
		def errorMapping = assertValidationException(say)
		assertNotNull errorMapping
		assertEquals errorMapping.type, XmppStanzaError.Type_MODIFY
		assertEquals errorMapping.condition, XmppStanzaError.BAD_REQUEST_CONDITION
		assertEquals errorMapping.text, Messages.INVALID_URI
	}
	
	@Test
	public void validateSayInvalidPromptEmptyURI() {
				
		def say = parseXml("""<say xmlns=\"urn:xmpp:ozone:say:1\" voice=\"allison\"><audio url=""/></say>""")
		
		def errorMapping = assertValidationException(say)
		assertNotNull errorMapping
		assertEquals errorMapping.type, XmppStanzaError.Type_MODIFY
		assertEquals errorMapping.condition, XmppStanzaError.BAD_REQUEST_CONDITION
		assertEquals errorMapping.text, Messages.MISSING_URI
	}

	@Test
	public void validateSayInvalidMissingURI() {
				
		def say = parseXml("""<say xmlns=\"urn:xmpp:ozone:say:1\" voice=\"allison\"><audio/></say>""")
		
		def errorMapping = assertValidationException(say)
		assertNotNull errorMapping
		assertEquals errorMapping.type, XmppStanzaError.Type_MODIFY
		assertEquals errorMapping.condition, XmppStanzaError.BAD_REQUEST_CONDITION
		assertEquals errorMapping.text, Messages.MISSING_URI
	}
	
	@Test
	public void validateSayValid() {
				
		def say = parseXml("""<say xmlns=\"urn:xmpp:ozone:say:1\" voice=\"allison\"><speak>Hello World</speak></say>""")
		assertNotNull fromXML(say)
	}
	
	
	// Ask
	// ====================================================================================
	
	@Test
	public void validateAskPromptItems() {
				
		def ask = parseXml("""<ask xmlns=\"urn:xmpp:ozone:ask:1\" voice=\"allison\"><choices>sales,support</choices></ask>""")
		
		def errorMapping = assertValidationException(ask)
		assertNotNull errorMapping
		assertEquals errorMapping.type, XmppStanzaError.Type_MODIFY
		assertEquals errorMapping.condition, XmppStanzaError.BAD_REQUEST_CONDITION
		assertEquals errorMapping.text, Messages.MISSING_PROMPT_ITEMS
	}
	
	@Test
	public void validateAskInvalidPromptItemsInvalidURI() {
				
		def ask = parseXml("""<ask xmlns=\"urn:xmpp:ozone:ask:1\" voice=\"allison\"><prompt><audio url="\$?\\.com"/></prompt><choices>sales,support</choices></ask>""")
		
		def errorMapping = assertValidationException(ask)
		assertNotNull errorMapping
		assertEquals errorMapping.type, XmppStanzaError.Type_MODIFY
		assertEquals errorMapping.condition, XmppStanzaError.BAD_REQUEST_CONDITION
		assertEquals errorMapping.text, Messages.INVALID_URI
	}
		
	@Test
	public void validateAskInvalidPromptItemsEmptyURI() {
				
		def ask = parseXml("""<ask xmlns=\"urn:xmpp:ozone:ask:1\" voice=\"allison\"><prompt><audio url=""/></prompt><choices>sales,support</choices></ask>""")
		
		def errorMapping = assertValidationException(ask)
		assertNotNull errorMapping
		assertEquals errorMapping.type, XmppStanzaError.Type_MODIFY
		assertEquals errorMapping.condition, XmppStanzaError.BAD_REQUEST_CONDITION
		assertEquals errorMapping.text, Messages.MISSING_URI
	}
	
	@Test
	public void validateAskChoicesNull() {
				
		def ask = parseXml("""<ask xmlns=\"urn:xmpp:ozone:ask:1\" voice=\"allison\"><prompt><speak xmlns=\"\">Hello World.</speak></prompt></ask>""")
		
		def errorMapping = assertValidationException(ask)
		assertNotNull errorMapping
		assertEquals errorMapping.type, XmppStanzaError.Type_MODIFY
		assertEquals errorMapping.condition, XmppStanzaError.BAD_REQUEST_CONDITION
		assertEquals errorMapping.text, Messages.MISSING_CHOICES
	}
	
	@Test
	public void validateAskChoicesEmpty() {
				
		def ask = parseXml("""<ask xmlns=\"urn:xmpp:ozone:ask:1\" voice=\"allison\"><choices/><prompt><speak xmlns=\"\">Hello World.</speak></prompt></ask>""")
		
		def errorMapping = assertValidationException(ask)
		assertNotNull errorMapping
		assertEquals errorMapping.type, XmppStanzaError.Type_MODIFY
		assertEquals errorMapping.condition, XmppStanzaError.BAD_REQUEST_CONDITION
		assertEquals errorMapping.text, Messages.MISSING_CHOICES
	}
	
	@Test
	public void validateAskChoicesInvalidUri() {
				
		def ask = parseXml("""<ask xmlns=\"urn:xmpp:ozone:ask:1\" voice=\"allison\"><choices/><prompt><speak xmlns=\"\">Hello World.</speak></prompt></ask>""")
		
		def errorMapping = assertValidationException(ask)
		assertNotNull errorMapping
		assertEquals errorMapping.type, XmppStanzaError.Type_MODIFY
		assertEquals errorMapping.condition, XmppStanzaError.BAD_REQUEST_CONDITION
		assertEquals errorMapping.text, Messages.MISSING_CHOICES
	}
	
	@Test
	public void validateAskInvalidChoicesURI() {
				
		def ask = parseXml("""<ask xmlns=\"urn:xmpp:ozone:ask:1\" recognizer="ar-oo" voice=\"allison\"><choices url="\$?\\.com">sales,support</choices><prompt><speak xmlns=\"\">Hello World.</speak></prompt></ask>""")
		
		def errorMapping = assertValidationException(ask)
		assertNotNull errorMapping
		assertEquals errorMapping.type, XmppStanzaError.Type_MODIFY
		assertEquals errorMapping.condition, XmppStanzaError.BAD_REQUEST_CONDITION
		assertEquals errorMapping.text, Messages.INVALID_URI
	}

	@Test
	public void validateAskInvalidInputMode() {
				
		def ask = parseXml("""<ask xmlns=\"urn:xmpp:ozone:ask:1\" mode="aaaa" voice=\"allison\"><choices>sales,support</choices><prompt><speak xmlns=\"\">Hello World.</speak></prompt></ask>""")
		
		def errorMapping = assertValidationException(ask)
		assertNotNull errorMapping
		assertEquals errorMapping.type, XmppStanzaError.Type_MODIFY
		assertEquals errorMapping.condition, XmppStanzaError.BAD_REQUEST_CONDITION
		assertEquals errorMapping.text, Messages.INVALID_INPUT_MODE
	}
	
	@Test
	public void validateAskInvalidTimeout() {
				
		def ask = parseXml("""<ask xmlns=\"urn:xmpp:ozone:ask:1\" mode="both" timeout="aaaa" voice=\"allison\"><choices>sales,support</choices><prompt><speak xmlns=\"\">Hello World.</speak></prompt></ask>""")
		
		def errorMapping = assertValidationException(ask)
		assertNotNull errorMapping
		assertEquals errorMapping.type, XmppStanzaError.Type_MODIFY
		assertEquals errorMapping.condition, XmppStanzaError.BAD_REQUEST_CONDITION
		assertEquals errorMapping.text, Messages.INVALID_TIMEOUT
	}
	
	@Test
	public void validateAskInvalidConfidence() {
				
		def ask = parseXml("""<ask xmlns=\"urn:xmpp:ozone:ask:1\" min-confidence="aaa" voice=\"allison\"><choices>sales,support</choices><prompt><speak xmlns=\"\">Hello World.</speak></prompt></ask>""")
		
		def errorMapping = assertValidationException(ask)
		assertNotNull errorMapping
		assertEquals errorMapping.type, XmppStanzaError.Type_MODIFY
		assertEquals errorMapping.condition, XmppStanzaError.BAD_REQUEST_CONDITION
		assertEquals errorMapping.text, Messages.INVALID_CONFIDENCE
	}
	
	@Test
	public void validateAskInvalidConfidenceRange() {
				
		def ask = parseXml("""<ask xmlns=\"urn:xmpp:ozone:ask:1\" min-confidence="1.2" voice=\"allison\"><choices>sales,support</choices><prompt><speak xmlns=\"\">Hello World.</speak></prompt></ask>""")
		
		def errorMapping = assertValidationException(ask)
		assertNotNull errorMapping
		assertEquals XmppStanzaError.Type_MODIFY, errorMapping.type
		assertEquals XmppStanzaError.BAD_REQUEST_CONDITION, errorMapping.condition
		assertEquals Messages.INVALID_CONFIDENCE_RANGE, errorMapping.text
	}
	
	@Test
	public void validateAskInvalidConfidenceNegative() {
				
		def ask = parseXml("""<ask xmlns=\"urn:xmpp:ozone:ask:1\" min-confidence="-1.0" voice=\"allison\"><choices>sales,support</choices><prompt><speak xmlns=\"\">Hello World.</speak></prompt></ask>""")
		
		def errorMapping = assertValidationException(ask)
		assertNotNull errorMapping
        assertEquals XmppStanzaError.Type_MODIFY, errorMapping.type
        assertEquals XmppStanzaError.BAD_REQUEST_CONDITION, errorMapping.condition
        assertEquals Messages.INVALID_CONFIDENCE_RANGE, errorMapping.text
	}
	
	@Test
	public void validateAskInvalidTerminator() {
				
		def ask = parseXml("""<ask xmlns=\"urn:xmpp:ozone:ask:1\" terminator="abcd" voice=\"allison\"><choices>sales,support</choices><prompt><speak xmlns=\"\">Hello World.</speak></prompt></ask>""")
		
		def errorMapping = assertValidationException(ask)
		assertNotNull errorMapping
		assertEquals XmppStanzaError.Type_MODIFY, errorMapping.type
		assertEquals XmppStanzaError.BAD_REQUEST_CONDITION, errorMapping.condition
		assertEquals Messages.INVALID_TERMINATOR, errorMapping.text
	}
	
	@Test
	public void validateAskValid() {
				
		def ask = parseXml("""<ask xmlns=\"urn:xmpp:ozone:ask:1\" recognizer="en-us" voice=\"allison\"><choices>sales,support</choices><prompt><speak xmlns=\"\">Choose your department.</speak></prompt></ask>""")		
		assertNotNull fromXML(ask)
	}
	
	// Transfer
	// ====================================================================================
	
	@Test
	public void validateTransferNullTo() {
				
		def transfer = parseXml("""<transfer xmlns=\"urn:xmpp:ozone:transfer:1\"></transfer>""")
		
		def errorMapping = assertValidationException(transfer)
		assertNotNull errorMapping
		assertEquals errorMapping.type, XmppStanzaError.Type_MODIFY
		assertEquals errorMapping.condition, XmppStanzaError.BAD_REQUEST_CONDITION
		assertEquals errorMapping.text, Messages.MISSING_TO
	}
	
	@Test
	public void validateTransferEmptyTo() {
				
		def transfer = parseXml("""<transfer xmlns=\"urn:xmpp:ozone:transfer:1\"><to/></transfer>""")
		
		def errorMapping = assertValidationException(transfer)
		assertNotNull errorMapping
		assertEquals errorMapping.type, XmppStanzaError.Type_MODIFY
		assertEquals errorMapping.condition, XmppStanzaError.BAD_REQUEST_CONDITION
		assertEquals errorMapping.text, Messages.MISSING_TO
	}
	
	@Test
	public void validateTransferEmptyToAttribute() {
				
		def transfer = parseXml("""<transfer xmlns=\"urn:xmpp:ozone:transfer:1\" to=""></transfer>""")
		
		def errorMapping = assertValidationException(transfer)
		assertNotNull errorMapping
		assertEquals errorMapping.type, XmppStanzaError.Type_MODIFY
		assertEquals errorMapping.condition, XmppStanzaError.BAD_REQUEST_CONDITION
		assertEquals errorMapping.text, Messages.MISSING_TO
	}
	
	@Test
	public void validateTransferEmptyElementButNotEmptyToAttribute() {
				
		def transfer = parseXml("""<transfer xmlns=\"urn:xmpp:ozone:transfer:1\" to="tel:123456"><to/></transfer>""")
		assertNotNull fromXML(transfer)
	}
	
	@Test
	public void validateTransferInvalidToURI() {
				
		def transfer = parseXml("""<transfer xmlns=\"urn:xmpp:ozone:transfer:1\"><to>\$?\\.com</to></transfer>""")
		
		def errorMapping = assertValidationException(transfer)
		assertNotNull errorMapping
		assertEquals errorMapping.type, XmppStanzaError.Type_MODIFY
		assertEquals errorMapping.condition, XmppStanzaError.BAD_REQUEST_CONDITION
		assertEquals errorMapping.text, Messages.INVALID_URI
	}
	
	@Test
	public void validateTransferInvalidFromURI() {
				
		def transfer = parseXml("""<transfer xmlns=\"urn:xmpp:ozone:transfer:1\" from="\$?\\.com"><to>tel:123456789</to></transfer>""")
		
		def errorMapping = assertValidationException(transfer)
		assertNotNull errorMapping
		assertEquals errorMapping.type, XmppStanzaError.Type_MODIFY
		assertEquals errorMapping.condition, XmppStanzaError.BAD_REQUEST_CONDITION
		assertEquals errorMapping.text, Messages.INVALID_URI
	}
	
	@Test
	public void validateTransferInvalidAnswerOnMedia() {
				
		def transfer = parseXml("""<transfer xmlns=\"urn:xmpp:ozone:transfer:1\" from="tel:12345666" answer-on-media="111"><to>tel:123456789</to></transfer>""")
		
		def errorMapping = assertValidationException(transfer)
		assertNotNull errorMapping
		assertEquals errorMapping.type, XmppStanzaError.Type_MODIFY
		assertEquals errorMapping.condition, XmppStanzaError.BAD_REQUEST_CONDITION
		assertEquals errorMapping.text, Messages.INVALID_BOOLEAN
	}
	
	@Test
	public void validateTransferInvalidTimeout() {
				
		def transfer = parseXml("""<transfer xmlns=\"urn:xmpp:ozone:transfer:1\" from="tel:12345666" timeout="abc"><to>tel:123456789</to></transfer>""")
		
		def errorMapping = assertValidationException(transfer)
		assertNotNull errorMapping
		assertEquals errorMapping.type, XmppStanzaError.Type_MODIFY
		assertEquals errorMapping.condition, XmppStanzaError.BAD_REQUEST_CONDITION
		assertEquals errorMapping.text, Messages.INVALID_TIMEOUT
	}
	
	@Test
	public void validateTransferInvalidTerminator() {
				
		def transfer = parseXml("""<transfer xmlns=\"urn:xmpp:ozone:transfer:1\" from="tel:12345666" terminator="abc"><to>tel:123456789</to></transfer>""")
		
		def errorMapping = assertValidationException(transfer)
		assertNotNull errorMapping
		assertEquals errorMapping.type, XmppStanzaError.Type_MODIFY
		assertEquals errorMapping.condition, XmppStanzaError.BAD_REQUEST_CONDITION
		assertEquals errorMapping.text, Messages.INVALID_TERMINATOR
	}
	
	@Test
	public void validateTransferValid() {
				
		def transfer = parseXml("""<transfer xmlns=\"urn:xmpp:ozone:transfer:1\" from="tel:12345666"><to>tel:123456789</to></transfer>""")
		assertNotNull fromXML(transfer)
	}
	
	// Conference
	// ====================================================================================
	
	@Test
	public void validateConferenceNullRoomName() {
				
		def conference = parseXml("""<conference xmlns=\"urn:xmpp:ozone:conference:1\"></conference>""")
		
		def errorMapping = assertValidationException(conference)
		assertNotNull errorMapping
		assertEquals errorMapping.type, XmppStanzaError.Type_MODIFY
		assertEquals errorMapping.condition, XmppStanzaError.BAD_REQUEST_CONDITION
		assertEquals errorMapping.text, Messages.MISSING_ROOM_NAME
	}
	
	@Test
	public void validateConferenceInvalidBeep() {
				
		def conference = parseXml("""<conference xmlns=\"urn:xmpp:ozone:conference:1\" id="1" beep="123"></conference>""")
		
		def errorMapping = assertValidationException(conference)
		assertNotNull errorMapping
		assertEquals errorMapping.type, XmppStanzaError.Type_MODIFY
		assertEquals errorMapping.condition, XmppStanzaError.BAD_REQUEST_CONDITION
		assertEquals errorMapping.text, Messages.INVALID_BOOLEAN
	}
	
	@Test
	public void validateConferenceInvalidMute() {
				
		def conference = parseXml("""<conference xmlns=\"urn:xmpp:ozone:conference:1\" id="1" mute="123"></conference>""")
		
		def errorMapping = assertValidationException(conference)
		assertNotNull errorMapping
		assertEquals errorMapping.type, XmppStanzaError.Type_MODIFY
		assertEquals errorMapping.condition, XmppStanzaError.BAD_REQUEST_CONDITION
		assertEquals errorMapping.text, Messages.INVALID_BOOLEAN
	}
	
	@Test
	public void validateConferenceInvalidTonePassThrough() {
				
		def conference = parseXml("""<conference xmlns=\"urn:xmpp:ozone:conference:1\" id="1" tone-passthrough="123"></conference>""")
		
		def errorMapping = assertValidationException(conference)
		assertNotNull errorMapping
		assertEquals errorMapping.type, XmppStanzaError.Type_MODIFY
		assertEquals errorMapping.condition, XmppStanzaError.BAD_REQUEST_CONDITION
		assertEquals errorMapping.text, Messages.INVALID_BOOLEAN
	}
	
	@Test
	public void validateConferenceInvalidTerminator() {
				
		def conference = parseXml("""<conference xmlns=\"urn:xmpp:ozone:conference:1\" id="1" terminator="123"></conference>""")
		
		def errorMapping = assertValidationException(conference)
		assertNotNull errorMapping
		assertEquals errorMapping.type, XmppStanzaError.Type_MODIFY
		assertEquals errorMapping.condition, XmppStanzaError.BAD_REQUEST_CONDITION
		assertEquals errorMapping.text, Messages.INVALID_TERMINATOR
	}
	
	@Test
	public void validateConferenceValid() {
				
		def conference = parseXml("""<conference xmlns=\"urn:xmpp:ozone:conference:1\" mute="false" beep="false" tone-passthrough="true" id="123456"/>""")
		assertNotNull fromXML(conference)
	}

	// Redirect
	// ====================================================================================
	
	@Test
	public void validateRedirectMissingDestination() {
				
		def conference = parseXml("""<redirect xmlns="urn:xmpp:ozone:1"/>""")
		
		def errorMapping = assertValidationException(conference)
		assertNotNull errorMapping
		assertEquals errorMapping.type, XmppStanzaError.Type_MODIFY
		assertEquals errorMapping.condition, XmppStanzaError.BAD_REQUEST_CONDITION
		assertEquals errorMapping.text, Messages.MISSING_DESTINATION
	}
	
	@Test
	public void validateRedirectInvalidURI() {
				
		def conference = parseXml("""<redirect xmlns="urn:xmpp:ozone:1" to="\$?\\.com"/>""")
		
		def errorMapping = assertValidationException(conference)
		assertNotNull errorMapping
		assertEquals errorMapping.type, XmppStanzaError.Type_MODIFY
		assertEquals errorMapping.condition, XmppStanzaError.BAD_REQUEST_CONDITION
		assertEquals errorMapping.text, Messages.INVALID_URI
	}
	
	// Mixed tests
	// ====================================================================================
	
	@Test
	public void validateInvalidNamespace() {
				
		def say = parseXml("""<say xmlns=\"urn:xmpp:ozone:conference:1\" voice=\"allison\"><speak>Hello World</speak></say>""")
		
		def errorMapping = assertValidationException(say)
		assertNotNull errorMapping
		assertEquals errorMapping.type, XmppStanzaError.Type_MODIFY
		assertEquals errorMapping.condition, XmppStanzaError.BAD_REQUEST_CONDITION
		assertEquals errorMapping.text, Messages.UNKNOWN_NAMESPACE_ELEMENT
	}
	
	// Invalid jids
	// ====================================================================================
	
	def assertValidationException(def element) {
		
		try {
			fromXML(element)
		} catch (Exception e) {
			assertTrue e instanceof ValidationException
			def errorMapping = mapper.toXmppError(e)
			return errorMapping
		}
		fail "Expected validation exception"
	}
	
	def fromXML(def element) {

		for(def provider: providers) {
			if (provider.handles(element)) {
				return provider.fromXML(element)
			}
		}
	}
	
	private Element parseXml(String string) {
		
		return new SAXReader().read(new StringReader(string)).rootElement;
	}
}
