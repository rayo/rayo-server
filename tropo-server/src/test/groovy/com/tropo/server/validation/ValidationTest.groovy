package com.tropo.server.validation;

import static org.junit.Assert.*

import java.io.StringReader;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.hibernate.validator.constraints.NotEmpty;
import org.junit.Before;
import org.junit.Test

import com.tropo.core.validation.Messages;
import com.tropo.core.validation.ValidationException
import com.tropo.core.validation.Validator
import com.tropo.core.verb.Say
import com.tropo.core.xml.OzoneProvider;
import com.tropo.server.exception.ExceptionMapper
import com.voxeo.servlet.xmpp.XmppStanzaError;


class ValidationTest {

	def provider
	def mapper
	
	@Before
	public void init() {
		
		provider = new OzoneProvider(validator: new Validator())
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
	public void validateSayValid() {
				
		def say = parseXml("""<say xmlns=\"urn:xmpp:ozone:say:1\" voice=\"allison\"><speak>Hello World</speak></say>""")
		assertNotNull provider.fromXML(say)
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
	public void validateAskInvalidRecognizer() {
				
		def ask = parseXml("""<ask xmlns=\"urn:xmpp:ozone:ask:1\" recognizer="ar-oo" voice=\"allison\"><choices>sales,support</choices><prompt><speak xmlns=\"\">Hello World.</speak></prompt></ask>""")
		
		def errorMapping = assertValidationException(ask)
		assertNotNull errorMapping
		assertEquals errorMapping.type, XmppStanzaError.Type_MODIFY
		assertEquals errorMapping.condition, XmppStanzaError.BAD_REQUEST_CONDITION
		assertEquals errorMapping.text, Messages.INVALID_RECOGNIZER
	}

	@Test
	public void validateAskInvalidInputMode() {
				
		def ask = parseXml("""<ask xmlns=\"urn:xmpp:ozone:ask:1\" mode="aaaa" recognizer="ar-oo" voice=\"allison\"><choices>sales,support</choices><prompt><speak xmlns=\"\">Hello World.</speak></prompt></ask>""")
		
		def errorMapping = assertValidationException(ask)
		assertNotNull errorMapping
		assertEquals errorMapping.type, XmppStanzaError.Type_MODIFY
		assertEquals errorMapping.condition, XmppStanzaError.BAD_REQUEST_CONDITION
		assertEquals errorMapping.text, Messages.INVALID_INPUT_MODE
	}
	
	@Test
	public void validateAskValid() {
				
		def ask = parseXml("""<ask xmlns=\"urn:xmpp:ozone:ask:1\" recognizer="en-us" voice=\"allison\"><choices>sales,support</choices><prompt><speak xmlns=\"\">Choose your department.</speak></prompt></ask>""")		
		assertNotNull provider.fromXML(ask)
	}
	
	def assertValidationException(def object) {
		
		try {
			provider.fromXML(object)
		} catch (Exception e) {
			e.printStackTrace()
			assertTrue e instanceof ValidationException
			def errorMapping = mapper.toXmppError(e)
			return errorMapping
		}
		fail "Expected validation exception"
	}
	
	private Element parseXml(String string) {
		
		return new SAXReader().read(new StringReader(string)).rootElement;
	}
}
