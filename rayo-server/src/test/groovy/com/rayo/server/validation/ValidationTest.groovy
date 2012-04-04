package com.rayo.server.validation;

import static org.junit.Assert.*

import java.io.StringReader

import org.dom4j.Element
import org.dom4j.io.SAXReader
import org.junit.Before
import org.junit.Test

import com.rayo.core.JoinCommand;
import com.rayo.core.validation.Messages
import com.rayo.core.validation.ValidationException
import com.rayo.core.validation.Validator
import com.rayo.core.xml.DefaultXmlProviderManager;
import com.rayo.core.xml.providers.AskProvider
import com.rayo.core.xml.providers.InputProvider;
import com.rayo.core.xml.providers.OutputProvider;
import com.rayo.core.xml.providers.RayoProvider;
import com.rayo.core.xml.providers.RecordProvider;
import com.rayo.core.xml.providers.SayProvider
import com.rayo.core.xml.providers.TransferProvider
import com.rayo.server.exception.ExceptionMapper
import com.voxeo.servlet.xmpp.StanzaError;

class ValidationTest {

	def providers
	def mapper
	def manager
	
	@Before
	public void init() {
		
		def validator = new Validator()
		providers = [new RayoProvider(validator:validator,namespaces:['urn:xmpp:rayo:1']),
					 new SayProvider(validator:validator,namespaces:['urn:xmpp:tropo:say:1']),
					 new AskProvider(validator:validator,namespaces:['urn:xmpp:tropo:ask:1']),
					 new TransferProvider(validator:validator,namespaces:['urn:xmpp:tropo:transfer:1']),
					 new RecordProvider(validator:validator,namespaces:['urn:xmpp:rayo:record:1']),
					 new OutputProvider(validator:validator,namespaces:['urn:xmpp:rayo:output:1']),
					 new InputProvider(validator:validator,namespaces:['urn:xmpp:rayo:input:1'])
					]
		
		manager = new DefaultXmlProviderManager();
		providers.each {
			manager.register(it)
		}
		mapper = new ExceptionMapper()
	}
	
	// Say
	// ====================================================================================
	
	@Test
	public void validateSayEmptyPromptItems() {
				
		def say = parseXml("""<say xmlns=\"urn:xmpp:tropo:say:1\" voice=\"allison\"></say>""")
		
		def errorMapping = assertValidationException(say)
		assertNotNull errorMapping
		assertEquals errorMapping.type, StanzaError.Type.MODIFY.toString().toString()
		assertEquals errorMapping.condition, ExceptionMapper.toString(StanzaError.Condition.BAD_REQUEST)
		assertEquals errorMapping.text, Messages.MISSING_SSML
	}
		
	@Test
	public void validateSayValid() {
				
		def say = parseXml("""<say xmlns=\"urn:xmpp:tropo:say:1\" voice=\"allison\"><speak>Hello World</speak></say>""")
		assertNotNull fromXML(say)
	}
	
	
	// Ask
	// ====================================================================================
	
	@Test
	public void validateAskChoicesNull() {
				
		def ask = parseXml("""<ask xmlns=\"urn:xmpp:tropo:ask:1\" voice=\"allison\"><prompt><speak xmlns=\"\">Hello World.</speak></prompt></ask>""")
		
		def errorMapping = assertValidationException(ask)
		assertNotNull errorMapping
		assertEquals errorMapping.type, StanzaError.Type.MODIFY.toString()
		assertEquals errorMapping.condition, ExceptionMapper.toString(StanzaError.Condition.BAD_REQUEST)
		assertEquals errorMapping.text, Messages.MISSING_CHOICES
	}
	
	@Test
	public void validateAskMissingChoice() {
				
		def ask = parseXml("""<ask xmlns=\"urn:xmpp:tropo:ask:1\" voice=\"allison\"><prompt><speak xmlns=\"\">Hello World.</speak></prompt></ask>""")
		
		def errorMapping = assertValidationException(ask)
		assertNotNull errorMapping
		assertEquals errorMapping.type, StanzaError.Type.MODIFY.toString()
		assertEquals errorMapping.condition, ExceptionMapper.toString(StanzaError.Condition.BAD_REQUEST)
		assertEquals errorMapping.text, Messages.MISSING_CHOICES
	}

    @Test
    public void validateAskInvalidChoice() {
                
        def ask = parseXml("""<ask xmlns=\"urn:xmpp:tropo:ask:1\" voice=\"allison\"><choices/><prompt><speak xmlns=\"\">Hello World.</speak></prompt></ask>""")
        
        def errorMapping = assertValidationException(ask)
        assertNotNull errorMapping
        assertEquals errorMapping.type, StanzaError.Type.MODIFY.toString()
        assertEquals errorMapping.condition, ExceptionMapper.toString(StanzaError.Condition.BAD_REQUEST)
        assertEquals errorMapping.text, Messages.MISSING_CHOICES_CONTENT_OR_URL
    }

    @Test
    public void validateAskMissingContentType() {
                
        def ask = parseXml("""<ask xmlns=\"urn:xmpp:tropo:ask:1\" voice=\"allison\"><choices>bling</choices><prompt><speak xmlns=\"\">Hello World.</speak></prompt></ask>""")
        
        def errorMapping = assertValidationException(ask)
        assertNotNull errorMapping
        assertEquals errorMapping.type, StanzaError.Type.MODIFY.toString()
        assertEquals errorMapping.condition, ExceptionMapper.toString(StanzaError.Condition.BAD_REQUEST)
        assertEquals errorMapping.text, Messages.MISSING_CHOICES_CONTENT_TYPE
    }

	@Test
	public void validateAskInvalidChoicesURI() {
				
		def ask = parseXml("""<ask xmlns=\"urn:xmpp:tropo:ask:1\" recognizer="ar-oo" voice=\"allison\"><choices url="\$?\\.com">sales,support</choices><prompt><speak xmlns=\"\">Hello World.</speak></prompt></ask>""")
		
		def errorMapping = assertValidationException(ask)
		assertNotNull errorMapping
		assertEquals errorMapping.type, StanzaError.Type.MODIFY.toString()
		assertEquals errorMapping.condition, ExceptionMapper.toString(StanzaError.Condition.BAD_REQUEST)
		assertEquals errorMapping.text, Messages.INVALID_URI
	}

	@Test
	public void validateAskInvalidInputMode() {
				
		def ask = parseXml("""<ask xmlns=\"urn:xmpp:tropo:ask:1\" mode="aaaa" voice=\"allison\"><choices>sales,support</choices><prompt><speak xmlns=\"\">Hello World.</speak></prompt></ask>""")
		
		def errorMapping = assertValidationException(ask)
		assertNotNull errorMapping
		assertEquals errorMapping.type, StanzaError.Type.MODIFY.toString()
		assertEquals errorMapping.condition, ExceptionMapper.toString(StanzaError.Condition.BAD_REQUEST)
		assertEquals errorMapping.text, Messages.INVALID_INPUT_MODE
	}
	
	@Test
	public void validateAskInvalidTimeout() {
				
		def ask = parseXml("""<ask xmlns=\"urn:xmpp:tropo:ask:1\" mode="any" timeout="aaaa" voice=\"allison\"><choices>sales,support</choices><prompt><speak xmlns=\"\">Hello World.</speak></prompt></ask>""")
		
		def errorMapping = assertValidationException(ask)
		assertNotNull errorMapping
		assertEquals errorMapping.type, StanzaError.Type.MODIFY.toString()
		assertEquals errorMapping.condition, ExceptionMapper.toString(StanzaError.Condition.BAD_REQUEST)
		assertEquals errorMapping.text, Messages.INVALID_TIMEOUT
	}
	
	@Test
	public void validateAskInvalidConfidence() {
				
		def ask = parseXml("""<ask xmlns=\"urn:xmpp:tropo:ask:1\" min-confidence="aaa" voice=\"allison\"><choices>sales,support</choices><prompt><speak xmlns=\"\">Hello World.</speak></prompt></ask>""")
		
		def errorMapping = assertValidationException(ask)
		assertNotNull errorMapping
		assertEquals errorMapping.type, StanzaError.Type.MODIFY.toString()
		assertEquals errorMapping.condition, ExceptionMapper.toString(StanzaError.Condition.BAD_REQUEST)
		assertEquals errorMapping.text, Messages.INVALID_CONFIDENCE
	}
	
	@Test
	public void validateAskInvalidConfidenceRange() {
				
		def ask = parseXml("""<ask xmlns=\"urn:xmpp:tropo:ask:1\" min-confidence="1.2" voice=\"allison\"><choices content-type="application/grammar+voxeo">sales,support</choices><prompt><speak xmlns=\"\">Hello World.</speak></prompt></ask>""")
		
		def errorMapping = assertValidationException(ask)
		assertNotNull errorMapping
		assertEquals StanzaError.Type.MODIFY.toString(), errorMapping.type
		assertEquals ExceptionMapper.toString(StanzaError.Condition.BAD_REQUEST), errorMapping.condition
		assertEquals Messages.INVALID_CONFIDENCE_RANGE, errorMapping.text
	}
	
	@Test
	public void validateAskInvalidConfidenceNegative() {
				
		def ask = parseXml("""<ask xmlns=\"urn:xmpp:tropo:ask:1\" min-confidence="-1.0" voice=\"allison\"><choices content-type="application/grammar+voxeo">sales,support</choices><prompt><speak xmlns=\"\">Hello World.</speak></prompt></ask>""")
		
		def errorMapping = assertValidationException(ask)
		assertNotNull errorMapping
        assertEquals StanzaError.Type.MODIFY.toString(), errorMapping.type
        assertEquals ExceptionMapper.toString(StanzaError.Condition.BAD_REQUEST), errorMapping.condition
        assertEquals Messages.INVALID_CONFIDENCE_RANGE, errorMapping.text
	}
	
	@Test
	public void validateAskInvalidTerminator() {
				
		def ask = parseXml("""<ask xmlns=\"urn:xmpp:tropo:ask:1\" terminator="abcd" voice=\"allison\"><choices content-type="application/grammar+voxeo">sales,support</choices><prompt><speak xmlns=\"\">Hello World.</speak></prompt></ask>""")
		
		def errorMapping = assertValidationException(ask)
		assertNotNull errorMapping
		assertEquals StanzaError.Type.MODIFY.toString(), errorMapping.type
		assertEquals ExceptionMapper.toString(StanzaError.Condition.BAD_REQUEST), errorMapping.condition
		assertEquals Messages.INVALID_TERMINATOR, errorMapping.text
	}
	
	@Test
	public void validateAskValid() {
				
		def ask = parseXml("""<ask xmlns=\"urn:xmpp:tropo:ask:1\" recognizer="en-us" voice=\"allison\"><choices content-type="application/grammar+voxeo">sales,support</choices><prompt><speak xmlns=\"\">Choose your department.</speak></prompt></ask>""")		
		assertNotNull fromXML(ask)
	}
	
	// Reject
	// ====================================================================================
	@Test
	public void validateRejectMissingReason() {
				
		def reject = parseXml("""<reject xmlns="urn:xmpp:rayo:1"/>""")
		
		def errorMapping = assertValidationException(reject)
		assertNotNull errorMapping
		assertEquals errorMapping.type, StanzaError.Type.MODIFY.toString()
		assertEquals errorMapping.condition, ExceptionMapper.toString(StanzaError.Condition.BAD_REQUEST)
		assertEquals errorMapping.text, Messages.MISSING_REASON
	}
	
	
	// Transfer
	// ====================================================================================
	
	@Test
	public void validateTransferNullTo() {
				
		def transfer = parseXml("""<transfer xmlns=\"urn:xmpp:tropo:transfer:1\"></transfer>""")
		
		def errorMapping = assertValidationException(transfer)
		assertNotNull errorMapping
		assertEquals errorMapping.type, StanzaError.Type.MODIFY.toString()
		assertEquals errorMapping.condition, ExceptionMapper.toString(StanzaError.Condition.BAD_REQUEST)
		assertEquals errorMapping.text, Messages.MISSING_TO
	}
	
	@Test
	public void validateTransferEmptyTo() {
				
		def transfer = parseXml("""<transfer xmlns=\"urn:xmpp:tropo:transfer:1\"><to/></transfer>""")
		
		def errorMapping = assertValidationException(transfer)
		assertNotNull errorMapping
		assertEquals errorMapping.type, StanzaError.Type.MODIFY.toString()
		assertEquals errorMapping.condition, ExceptionMapper.toString(StanzaError.Condition.BAD_REQUEST)
		assertEquals errorMapping.text, Messages.MISSING_TO
	}
	
	@Test
	public void validateTransferEmptyToAttribute() {
				
		def transfer = parseXml("""<transfer xmlns=\"urn:xmpp:tropo:transfer:1\" to=""></transfer>""")
		
		def errorMapping = assertValidationException(transfer)
		assertNotNull errorMapping
		assertEquals errorMapping.type, StanzaError.Type.MODIFY.toString()
		assertEquals errorMapping.condition, ExceptionMapper.toString(StanzaError.Condition.BAD_REQUEST)
		assertEquals errorMapping.text, Messages.MISSING_TO
	}
	
	@Test
	public void validateTransferEmptyElementButNotEmptyToAttribute() {
				
		def transfer = parseXml("""<transfer xmlns=\"urn:xmpp:tropo:transfer:1\" to="tel:123456"><to/></transfer>""")
		assertNotNull fromXML(transfer)
	}
	
	@Test
	public void validateTransferInvalidToURI() {
				
		def transfer = parseXml("""<transfer xmlns=\"urn:xmpp:tropo:transfer:1\"><to>\$?\\.com</to></transfer>""")
		
		def errorMapping = assertValidationException(transfer)
		assertNotNull errorMapping
		assertEquals errorMapping.type, StanzaError.Type.MODIFY.toString()
		assertEquals errorMapping.condition, ExceptionMapper.toString(StanzaError.Condition.BAD_REQUEST)
		assertEquals errorMapping.text, Messages.INVALID_URI
	}
	
	@Test
	public void validateTransferInvalidFromURI() {
				
		def transfer = parseXml("""<transfer xmlns=\"urn:xmpp:tropo:transfer:1\" from="\$?\\.com"><to>tel:123456789</to></transfer>""")
		
		def errorMapping = assertValidationException(transfer)
		assertNotNull errorMapping
		assertEquals errorMapping.type, StanzaError.Type.MODIFY.toString()
		assertEquals errorMapping.condition, ExceptionMapper.toString(StanzaError.Condition.BAD_REQUEST)
		assertEquals errorMapping.text, Messages.INVALID_URI
	}
	
	@Test
	public void validateTransferInvalidAnswerOnMedia() {
				
		def transfer = parseXml("""<transfer xmlns=\"urn:xmpp:tropo:transfer:1\" from="tel:12345666" answer-on-media="111"><to>tel:123456789</to></transfer>""")
		
		def errorMapping = assertValidationException(transfer)
		assertNotNull errorMapping
		assertEquals errorMapping.type, StanzaError.Type.MODIFY.toString()
		assertEquals errorMapping.condition, ExceptionMapper.toString(StanzaError.Condition.BAD_REQUEST)
		assertEquals errorMapping.text, String.format(Messages.INVALID_BOOLEAN,"answer-on-media")
	}
	
	@Test
	public void validateTransferInvalidTimeout() {
				
		def transfer = parseXml("""<transfer xmlns=\"urn:xmpp:tropo:transfer:1\" from="tel:12345666" timeout="abc"><to>tel:123456789</to></transfer>""")
		
		def errorMapping = assertValidationException(transfer)
		assertNotNull errorMapping
		assertEquals errorMapping.type, StanzaError.Type.MODIFY.toString()
		assertEquals errorMapping.condition, ExceptionMapper.toString(StanzaError.Condition.BAD_REQUEST)
		assertEquals errorMapping.text, Messages.INVALID_TIMEOUT
	}
	
	@Test
	public void validateTransferInvalidTerminator() {
				
		def transfer = parseXml("""<transfer xmlns=\"urn:xmpp:tropo:transfer:1\" from="tel:12345666" terminator="abc"><to>tel:123456789</to></transfer>""")
		
		def errorMapping = assertValidationException(transfer)
		assertNotNull errorMapping
		assertEquals errorMapping.type, StanzaError.Type.MODIFY.toString()
		assertEquals errorMapping.condition, ExceptionMapper.toString(StanzaError.Condition.BAD_REQUEST)
		assertEquals errorMapping.text, Messages.INVALID_TERMINATOR
	}
	
	@Test
	public void validateTransferInvalidMedia() {
				
		def transfer = parseXml("""<transfer xmlns=\"urn:xmpp:tropo:transfer:1\" from="tel:12345666" media="aaa" terminator="#"><to>tel:123456789</to></transfer>""")
		
		def errorMapping = assertValidationException(transfer)
		assertNotNull errorMapping
		assertEquals errorMapping.type, StanzaError.Type.MODIFY.toString()
		assertEquals errorMapping.condition, ExceptionMapper.toString(StanzaError.Condition.BAD_REQUEST)
		assertEquals errorMapping.text, Messages.INVALID_MEDIA
	}
	
	@Test
	public void validateTransferValid() {
				
		def transfer = parseXml("""<transfer xmlns=\"urn:xmpp:tropo:transfer:1\" from="tel:12345666"><to>tel:123456789</to></transfer>""")
		assertNotNull fromXML(transfer)
	}
	
	@Test
	public void validateTransferOneToElementValid() {
		
		def transfer = parseXml("""<transfer xmlns="urn:xmpp:tropo:transfer:1" from="sip:name@connfu.com"><to>sip:8517c60c-39a6-4bce-8d21-9df2b6b1ad8c@gw113.phono.com</to></transfer>""")
		assertNotNull fromXML(transfer)
	}

	// Redirect
	// ====================================================================================
	
	@Test
	public void validateRedirectMissingDestination() {
				
		def redirect = parseXml("""<redirect xmlns="urn:xmpp:rayo:1"/>""")
		
		def errorMapping = assertValidationException(redirect)
		assertNotNull errorMapping
		assertEquals errorMapping.type, StanzaError.Type.MODIFY.toString()
		assertEquals errorMapping.condition, ExceptionMapper.toString(StanzaError.Condition.BAD_REQUEST)
		assertEquals errorMapping.text, Messages.MISSING_DESTINATION
	}
	
	@Test
	public void validateRedirectInvalidURI() {
				
		def redirect = parseXml("""<redirect xmlns="urn:xmpp:rayo:1" to="\$?\\.com"/>""")
		
		def errorMapping = assertValidationException(redirect)
		assertNotNull errorMapping
		assertEquals errorMapping.type, StanzaError.Type.MODIFY.toString()
		assertEquals errorMapping.condition, ExceptionMapper.toString(StanzaError.Condition.BAD_REQUEST)
		assertEquals errorMapping.text, Messages.INVALID_URI
	}
	
	// Dial
	// ====================================================================================
	
	@Test
	public void validateDialMissingTo() {
				
		def dial = parseXml("""<dial xmlns=\"urn:xmpp:rayo:1\" from=\"tel:34637710708\"><header name=\"test\" value=\"atest\"/></dial>""")
		
		def errorMapping = assertValidationException(dial)
		assertNotNull errorMapping
		assertEquals errorMapping.type, StanzaError.Type.MODIFY.toString()
		assertEquals errorMapping.condition, ExceptionMapper.toString(StanzaError.Condition.BAD_REQUEST)
		assertEquals errorMapping.text, Messages.MISSING_TO
	}
	
	@Test
	public void validateEmptyDialIsOk() {
				
		def dial = parseXml("""<dial xmlns=\"urn:xmpp:rayo:1\" to=\"tel:34637710701\" from=\"tel:34637710708\"></dial>""")
		assertNotNull fromXML(dial)
	}
	
	@Test
	public void validateEmptyDialEmptyHeaderIsOk() {
				
		def dial = parseXml("""<dial xmlns=\"urn:xmpp:rayo:1\" to=\"tel:34637710701\" from=\"tel:34637710708\"><header/></dial>""")
		assertNotNull fromXML(dial)
	}
	
	@Test
	public void validateDialInvalidToURI() {
				
		def dial = parseXml("""<dial xmlns=\"urn:xmpp:rayo:1\" to=\"tel:\$?\\.com\" from=\"tel:34637710708\"><header/></dial>""")
		
		def errorMapping = assertValidationException(dial)
		assertNotNull errorMapping
		assertEquals errorMapping.type, StanzaError.Type.MODIFY.toString()
		assertEquals errorMapping.condition, ExceptionMapper.toString(StanzaError.Condition.BAD_REQUEST)
		assertEquals errorMapping.text, Messages.INVALID_URI
	}
	
	@Test
	public void validateDialInvalidFromURI() {
				
		def dial = parseXml("""<dial xmlns=\"urn:xmpp:rayo:1\" to=\"tel:34637710708\" from=\"tel:\$?\\.com\"><header/></dial>""")
		
		def errorMapping = assertValidationException(dial)
		assertNotNull errorMapping
		assertEquals errorMapping.type, StanzaError.Type.MODIFY.toString()
		assertEquals errorMapping.condition, ExceptionMapper.toString(StanzaError.Condition.BAD_REQUEST)
		assertEquals errorMapping.text, Messages.INVALID_URI
	}
	
	@Test
	public void validateDialValidTelURIWithPlusSign() {
				
		def dial = parseXml("""<dial xmlns="urn:xmpp:rayo:1" to="tel:+447976224017 " from="sip:442035149248@173.255.241.49"></dial>""")
		assertNotNull fromXML(dial)
	}	
	
	@Test
	public void validateNestedJoinInvalidDirection() {
				
		def dial = parseXml("""<dial xmlns=\"urn:xmpp:rayo:1\" to="tel:34637710708" from="tel:34637710708"><join xmlns="urn:xmpp:rayo:join:1" call-id="abcd" direction="abcd"/></dial>""")
		
		def errorMapping = assertValidationException(dial)
		assertNotNull errorMapping
		assertEquals errorMapping.type, StanzaError.Type.MODIFY.toString()
		assertEquals errorMapping.condition, ExceptionMapper.toString(StanzaError.Condition.BAD_REQUEST)
		assertEquals errorMapping.text, String.format(Messages.INVALID_ENUM, 'direction')
	}
	
	
	@Test
	public void validateNestedJoinInvalidMedia() {
				
		def dial = parseXml("""<dial xmlns=\"urn:xmpp:rayo:1\" to=\"tel:34637710708\" from="tel:34637710708"><join xmlns="urn:xmpp:rayo:join:1" call-id="abcd" direction="duplex" media="abcd"/></dial>""")
		
		def errorMapping = assertValidationException(dial)
		assertNotNull errorMapping
		assertEquals errorMapping.type, StanzaError.Type.MODIFY.toString()
		assertEquals errorMapping.condition, ExceptionMapper.toString(StanzaError.Condition.BAD_REQUEST)
		assertEquals errorMapping.text, String.format(Messages.INVALID_ENUM, 'media')
	}
	
	@Test
	public void validateNestedJoinInvalidForce() {
				
		def dial = parseXml("""<dial xmlns=\"urn:xmpp:rayo:1\" to=\"tel:34637710708\" from="tel:34637710708"><join xmlns="urn:xmpp:rayo:join:1" call-id="abcd" force="123" direction="duplex" media="bridge"/></dial>""")
		
		def errorMapping = assertValidationException(dial)
		assertNotNull errorMapping
		assertEquals errorMapping.type, StanzaError.Type.MODIFY.toString()
		assertEquals errorMapping.condition, ExceptionMapper.toString(StanzaError.Condition.BAD_REQUEST)
		assertEquals errorMapping.text, String.format(Messages.INVALID_BOOLEAN, 'force')
	}
	
	@Test
	public void validateNestedJoinMissingId() {
				
		def dial = parseXml("""<dial xmlns=\"urn:xmpp:rayo:1\" to=\"tel:34637710708\" from="tel:34637710708"><join xmlns="urn:xmpp:rayo:join:1" direction="duplex" media="bridge"/></dial>""")
		
		def errorMapping = assertValidationException(dial)
		assertNotNull errorMapping
		assertEquals errorMapping.type, StanzaError.Type.MODIFY.toString()
		assertEquals errorMapping.condition, ExceptionMapper.toString(StanzaError.Condition.BAD_REQUEST)
		assertEquals errorMapping.text, Messages.MISSING_JOIN_ID
	}
	
	@Test
	public void validateJoinedEmptyTo() {
				
		def joined = parseXml("""<joined xmlns="urn:xmpp:rayo:1"/> """)
		
		def errorMapping = assertValidationException(joined)
		assertNotNull errorMapping
		assertEquals errorMapping.type, StanzaError.Type.MODIFY.toString()
		assertEquals errorMapping.condition, ExceptionMapper.toString(StanzaError.Condition.BAD_REQUEST)
		assertEquals errorMapping.text, Messages.MISSING_JOIN_ID
	}
	
	@Test
	public void validateUnjoinedEmptyFrom() {
				
		def unjoined = parseXml("""<unjoined xmlns="urn:xmpp:rayo:1"/> """)
		
		def errorMapping = assertValidationException(unjoined)
		assertNotNull errorMapping
		assertEquals errorMapping.type, StanzaError.Type.MODIFY.toString()
		assertEquals errorMapping.condition, ExceptionMapper.toString(StanzaError.Condition.BAD_REQUEST)
		assertEquals errorMapping.text, Messages.MISSING_JOIN_ID
	}

	// Join
	// ====================================================================================
	
	@Test
	public void validateJoinInvalidDirection() {
				
		def join = parseXml("""<join xmlns="urn:xmpp:rayo:1" call-id="abcd" direction="abcd"/>""")
		
		def errorMapping = assertValidationException(join)
		assertNotNull errorMapping
		assertEquals errorMapping.type, StanzaError.Type.MODIFY.toString()
		assertEquals errorMapping.condition, ExceptionMapper.toString(StanzaError.Condition.BAD_REQUEST)
		assertEquals errorMapping.text, String.format(Messages.INVALID_ENUM, 'direction')
	}
	
	
	@Test
	public void validateJoinInvalidMedia() {
				
		def join = parseXml("""<join xmlns="urn:xmpp:rayo:1" call-id="abcd" direction="duplex" media="abcd"/>""")
		
		def errorMapping = assertValidationException(join)
		assertNotNull errorMapping
		assertEquals errorMapping.type, StanzaError.Type.MODIFY.toString()
		assertEquals errorMapping.condition, ExceptionMapper.toString(StanzaError.Condition.BAD_REQUEST)
		assertEquals errorMapping.text, String.format(Messages.INVALID_ENUM, 'media')
	}
	
	@Test
	public void validateJoinInvalidForce() {
				
		def join = parseXml("""<join xmlns="urn:xmpp:rayo:1" call-id="abcd" direction="duplex" media="bridge" force="123"/>""")
		
		def errorMapping = assertValidationException(join)
		assertNotNull errorMapping
		assertEquals errorMapping.type, StanzaError.Type.MODIFY.toString()
		assertEquals errorMapping.condition, ExceptionMapper.toString(StanzaError.Condition.BAD_REQUEST)
		assertEquals errorMapping.text, String.format(Messages.INVALID_BOOLEAN, 'force')
	}
	
	@Test
	public void validateJoinMissingId() {
				
		def join = parseXml("""<join xmlns="urn:xmpp:rayo:1" direction="duplex" media="bridge"/>""")
		
		def errorMapping = assertValidationException(join)
		assertNotNull errorMapping
		assertEquals errorMapping.type, StanzaError.Type.MODIFY.toString()
		assertEquals errorMapping.condition, ExceptionMapper.toString(StanzaError.Condition.BAD_REQUEST)
		assertEquals errorMapping.text, Messages.MISSING_JOIN_ID
	}
	
	// Record
	// ====================================================================================
	
	@Test
	public void validateRecordValid() {
				
		def record = parseXml("""<record xmlns=\"urn:xmpp:rayo:record:1\"></record>""")
		assertNotNull fromXML(record)
	}
	
	@Test
	public void validateRecordInvalidURI() {
				
		def record = parseXml("""<record xmlns="urn:xmpp:rayo:record:1" to="\$?\\.com"/>""")
		
		def errorMapping = assertValidationException(record)
		assertNotNull errorMapping
		assertEquals errorMapping.type, StanzaError.Type.MODIFY.toString()
		assertEquals errorMapping.condition, ExceptionMapper.toString(StanzaError.Condition.BAD_REQUEST)
		assertEquals errorMapping.text, Messages.INVALID_URI
	}
	
	@Test
	public void validateRecordInvalidFileFormat() {
				
		def record = parseXml("""<record xmlns="urn:xmpp:rayo:record:1" format="abcd"/>""")
		
		def errorMapping = assertValidationException(record)
		assertNotNull errorMapping
		assertEquals errorMapping.type, StanzaError.Type.MODIFY.toString()
		assertEquals errorMapping.condition, ExceptionMapper.toString(StanzaError.Condition.BAD_REQUEST)
		assertEquals errorMapping.text, Messages.INVALID_FILE_FORMAT
	}
		
	@Test
	public void validateRecordInvalidStartBeep() {
				
		def record = parseXml("""<record xmlns="urn:xmpp:rayo:record:1" start-beep="abcd"/>""")
		
		def errorMapping = assertValidationException(record)
		assertNotNull errorMapping
		assertEquals errorMapping.type, StanzaError.Type.MODIFY.toString()
		assertEquals errorMapping.condition, ExceptionMapper.toString(StanzaError.Condition.BAD_REQUEST)
		assertEquals errorMapping.text, String.format(Messages.INVALID_BOOLEAN, 'start-beep')
	}
	
	@Test
	public void validateRecordInvalidStopBeep() {
				
		def record = parseXml("""<record xmlns="urn:xmpp:rayo:record:1" stop-beep="abcd"/>""")
		
		def errorMapping = assertValidationException(record)
		assertNotNull errorMapping
		assertEquals errorMapping.type, StanzaError.Type.MODIFY.toString()
		assertEquals errorMapping.condition, ExceptionMapper.toString(StanzaError.Condition.BAD_REQUEST)
		assertEquals errorMapping.text, String.format(Messages.INVALID_BOOLEAN, 'stop-beep')
	}
	
	@Test
	public void validateRecordInvalidStartPaused() {
				
		def record = parseXml("""<record xmlns="urn:xmpp:rayo:record:1" start-paused="abcd"/>""")
		
		def errorMapping = assertValidationException(record)
		assertNotNull errorMapping
		assertEquals errorMapping.type, StanzaError.Type.MODIFY.toString()
		assertEquals errorMapping.condition, ExceptionMapper.toString(StanzaError.Condition.BAD_REQUEST)
		assertEquals errorMapping.text, String.format(Messages.INVALID_BOOLEAN, 'start-paused')
	}
	
	@Test
	public void validateRecordInvalidInitialTimeout() {
				
		def record = parseXml("""<record xmlns="urn:xmpp:rayo:record:1" initial-timeout="abcd"/>""")
		
		def errorMapping = assertValidationException(record)
		assertNotNull errorMapping
		assertEquals errorMapping.type, StanzaError.Type.MODIFY.toString()
		assertEquals errorMapping.condition, ExceptionMapper.toString(StanzaError.Condition.BAD_REQUEST)
		assertEquals errorMapping.text, String.format(Messages.INVALID_DURATION, 'initial-timeout')
	}
	
	@Test
	public void validateRecordInvalidFinalTimeout() {
				
		def record = parseXml("""<record xmlns="urn:xmpp:rayo:record:1" final-timeout="abcd"/>""")
		
		def errorMapping = assertValidationException(record)
		assertNotNull errorMapping
		assertEquals errorMapping.type, StanzaError.Type.MODIFY.toString()
		assertEquals errorMapping.condition, ExceptionMapper.toString(StanzaError.Condition.BAD_REQUEST)
		assertEquals errorMapping.text, String.format(Messages.INVALID_DURATION, 'final-timeout')
	}
	
	@Test
	public void validateRecordInvalidMaxDuration() {
				
		def record = parseXml("""<record xmlns="urn:xmpp:rayo:record:1" max-duration="abcd"/>""")
		
		def errorMapping = assertValidationException(record)
		assertNotNull errorMapping
		assertEquals errorMapping.type, StanzaError.Type.MODIFY.toString()
		assertEquals errorMapping.condition, ExceptionMapper.toString(StanzaError.Condition.BAD_REQUEST)
		assertEquals errorMapping.text, String.format(Messages.INVALID_DURATION, 'max-duration')
	}
	
	// Mixed tests
	// ====================================================================================
	
	@Test
	public void validateInvalidNamespace() {
				
		def say = parseXml("""<say xmlns=\"urn:xmpp:tropo:conference:1\" voice=\"allison\"><speak>Hello World</speak></say>""")
		
		def errorMapping = assertValidationException(say)
		assertNotNull errorMapping
		assertEquals errorMapping.type, StanzaError.Type.MODIFY.toString()
		assertEquals errorMapping.condition, ExceptionMapper.toString(StanzaError.Condition.BAD_REQUEST)
		assertEquals errorMapping.text, Messages.UNKNOWN_NAMESPACE_ELEMENT
	}
	
	// Output
	// ====================================================================================
	
	@Test
	public void validateOutputInvalidInterrupt() {
				
		def output = parseXml("""<output xmlns="urn:xmpp:rayo:output:1" interrupt-on="aaaa">say hello</output>""")
		
		def errorMapping = assertValidationException(output)
		assertNotNull errorMapping
		assertEquals errorMapping.condition, ExceptionMapper.toString(StanzaError.Condition.BAD_REQUEST)
		assertEquals errorMapping.text, Messages.INVALID_BARGEIN_TYPE
	}
	
	@Test
	public void validateOutputInvalidStartOffset() {
				
		def output = parseXml("""<output xmlns="urn:xmpp:rayo:output:1" start-offset="aaaa">say hello</output>""")
		
		def errorMapping = assertValidationException(output)
		assertNotNull errorMapping
		assertEquals errorMapping.condition, ExceptionMapper.toString(StanzaError.Condition.BAD_REQUEST)
		assertEquals errorMapping.text, String.format(Messages.INVALID_DURATION, 'start-offset')
	}
	
	@Test
	public void validateOutputInvalidRepeatInterval() {
				
		def output = parseXml("""<output xmlns="urn:xmpp:rayo:output:1" repeat-interval="aaaa">say hello</output>""")
		
		def errorMapping = assertValidationException(output)
		assertNotNull errorMapping
		assertEquals errorMapping.condition, ExceptionMapper.toString(StanzaError.Condition.BAD_REQUEST)
		assertEquals errorMapping.text, String.format(Messages.INVALID_DURATION, 'repeat-interval')
	}
	
	@Test
	public void validateOutputInvalidMaxTime() {
				
		def output = parseXml("""<output xmlns="urn:xmpp:rayo:output:1" max-time="aaaa">say hello</output>""")
		
		def errorMapping = assertValidationException(output)
		assertNotNull errorMapping
		assertEquals errorMapping.condition, ExceptionMapper.toString(StanzaError.Condition.BAD_REQUEST)
		assertEquals errorMapping.text, String.format(Messages.INVALID_DURATION, 'max-time')
	}
	
	@Test
	public void validateOutputInvalidStartPaused() {
				
		def output = parseXml("""<output xmlns="urn:xmpp:rayo:output:1" start-paused="aaaa">say hello</output>""")
		
		def errorMapping = assertValidationException(output)
		assertNotNull errorMapping
		assertEquals errorMapping.condition, ExceptionMapper.toString(StanzaError.Condition.BAD_REQUEST)
		assertEquals errorMapping.text, String.format(Messages.INVALID_BOOLEAN, 'start-paused')
	}
	
	@Test
	public void validateOutputInvalidRepeatTimes() {
				
		def output = parseXml("""<output xmlns="urn:xmpp:rayo:output:1" repeat-times="aaaa">say hello</output>""")
		
		def errorMapping = assertValidationException(output)
		assertNotNull errorMapping
		assertEquals errorMapping.condition, ExceptionMapper.toString(StanzaError.Condition.BAD_REQUEST)
		assertEquals errorMapping.text, String.format(Messages.INVALID_INTEGER, 'repeat-times')
	}
	
	@Test
	public void validateOutputEmptyPromptItems() {
				
		def output = parseXml("""<output xmlns=\"urn:xmpp:rayo:output:1\" voice=\"allison\"></output>""")
		
		def errorMapping = assertValidationException(output)
		assertNotNull errorMapping
		assertEquals errorMapping.type, StanzaError.Type.MODIFY.toString()
		assertEquals errorMapping.condition, ExceptionMapper.toString(StanzaError.Condition.BAD_REQUEST)
		assertEquals errorMapping.text, Messages.MISSING_SSML
	}
	
	@Test
	public void validateSeekInvalidDirection() {
				
		def output = parseXml("""<seek xmlns="urn:xmpp:rayo:output:1" amount="10000" direction="aaa"/>""")
		
		def errorMapping = assertValidationException(output)
		assertNotNull errorMapping
		assertEquals errorMapping.type, StanzaError.Type.MODIFY.toString()
		assertEquals errorMapping.condition, ExceptionMapper.toString(StanzaError.Condition.BAD_REQUEST)
		assertEquals errorMapping.text, String.format(Messages.INVALID_ENUM, 'direction')
	}
	
	@Test
	public void validateSeekInvalidAmount() {
				
		def output = parseXml("""<seek xmlns="urn:xmpp:rayo:output:1" amount="dfdf" direction="forward"/>""")
		
		def errorMapping = assertValidationException(output)
		assertNotNull errorMapping
		assertEquals errorMapping.type, StanzaError.Type.MODIFY.toString()
		assertEquals errorMapping.condition, ExceptionMapper.toString(StanzaError.Condition.BAD_REQUEST)
		assertEquals errorMapping.text, String.format(Messages.INVALID_INTEGER, 'amount')
	}
	
	@Test
	public void validateSeekMissingAmount() {
				
		def output = parseXml("""<seek xmlns="urn:xmpp:rayo:output:1" direction="forward"/>""")
		
		def errorMapping = assertValidationException(output)
		assertNotNull errorMapping
		assertEquals errorMapping.type, StanzaError.Type.MODIFY.toString()
		assertEquals errorMapping.condition, ExceptionMapper.toString(StanzaError.Condition.BAD_REQUEST)
		assertEquals errorMapping.text, Messages.MISSING_AMOUNT
	}
	
	@Test
	public void validateSeekMissingDirection() {
				
		def output = parseXml("""<seek xmlns="urn:xmpp:rayo:output:1" amount="10000"/>""")
		
		def errorMapping = assertValidationException(output)
		assertNotNull errorMapping
		assertEquals errorMapping.type, StanzaError.Type.MODIFY.toString()
		assertEquals errorMapping.condition, ExceptionMapper.toString(StanzaError.Condition.BAD_REQUEST)
		assertEquals errorMapping.text, Messages.MISSING_DIRECTION
	}
	
	@Test
	public void validateOutputValid() {
				
		def output = parseXml("""<output xmlns=\"urn:xmpp:rayo:output:1\" voice=\"allison\"><speak>Hello World</speak></output>""")
		assertNotNull fromXML(output)
	}
	
	@Test
	public void validateOutputValidSpeak() {
				
		def output = parseXml("""<output xmlns=\"urn:xmpp:rayo:output:1\"><speak xmlns=\"http://www.w3.org/2001/10/synthesis\" version=\"1.0\" xml:lang=\"en-US\"><audio src=\"digits/3\"/></speak></output>""")
		assertNotNull fromXML(output)
	}
	
	// Input
	// ====================================================================================
	
	@Test
	public void validateInputGrammarsNull() {
				
		def input = parseXml("""<input xmlns=\"urn:xmpp:rayo:input:1\"></input>""")
		
		def errorMapping = assertValidationException(input)
		assertNotNull errorMapping
		assertEquals errorMapping.type, StanzaError.Type.MODIFY.toString()
		assertEquals errorMapping.condition, ExceptionMapper.toString(StanzaError.Condition.BAD_REQUEST)
		assertEquals errorMapping.text, Messages.MISSING_CHOICES
	}

	@Test
	public void validateInputInvalidGrammar() {
				
		def input = parseXml("""<input xmlns=\"urn:xmpp:rayo:input:1\"><grammar/></input>""")
		
		def errorMapping = assertValidationException(input)
		assertNotNull errorMapping
		assertEquals errorMapping.type, StanzaError.Type.MODIFY.toString()
		assertEquals errorMapping.condition, ExceptionMapper.toString(StanzaError.Condition.BAD_REQUEST)
		assertEquals errorMapping.text, Messages.MISSING_CHOICES_CONTENT_OR_URL
	}

	@Test
	public void validateInputMissingContentType() {
				
		def input = parseXml("""<input xmlns=\"urn:xmpp:rayo:input:1\"><grammar>bling</grammar></input>""")
		
		def errorMapping = assertValidationException(input)
		assertNotNull errorMapping
		assertEquals errorMapping.type, StanzaError.Type.MODIFY.toString()
		assertEquals errorMapping.condition, ExceptionMapper.toString(StanzaError.Condition.BAD_REQUEST)
		assertEquals errorMapping.text, Messages.MISSING_CHOICES_CONTENT_TYPE
	}

	@Test
	public void validateInputInvalidGrammarURI() {
				
		def input = parseXml("""<input xmlns=\"urn:xmpp:rayo:input:1\" recognizer="ar-oo"><grammar url="\$?\\.com">sales,support</grammar></input>""")
		
		def errorMapping = assertValidationException(input)
		assertNotNull errorMapping
		assertEquals errorMapping.type, StanzaError.Type.MODIFY.toString()
		assertEquals errorMapping.condition, ExceptionMapper.toString(StanzaError.Condition.BAD_REQUEST)
		assertEquals errorMapping.text, Messages.INVALID_URI
	}
	
	
	@Test
	public void validateInputInvalidRecognizer() {
				
		def input = parseXml("""<input xmlns=\"urn:xmpp:rayo:input:1\" recognizer="test"><grammar content-type="vxml">sales,support</grammar></input>""")
		
		def errorMapping = assertValidationException(input)
		assertNotNull errorMapping
		assertEquals errorMapping.type, StanzaError.Type.MODIFY.toString()
		assertEquals errorMapping.condition, ExceptionMapper.toString(StanzaError.Condition.BAD_REQUEST)
		assertEquals errorMapping.text, Messages.INVALID_RECOGNIZER
	}

	@Test
	public void validateInputInvalidInputMode() {
				
		def input = parseXml("""<input xmlns=\"urn:xmpp:rayo:input:1\" mode="aaaa"><grammar>sales,support</grammar></input>""")
		
		def errorMapping = assertValidationException(input)
		assertNotNull errorMapping
		assertEquals errorMapping.type, StanzaError.Type.MODIFY.toString()
		assertEquals errorMapping.condition, ExceptionMapper.toString(StanzaError.Condition.BAD_REQUEST)
		assertEquals errorMapping.text, Messages.INVALID_INPUT_MODE
	}
	
	@Test
	public void validateInputInvalidInitialTimeout() {
				
		def input = parseXml("""<input xmlns=\"urn:xmpp:rayo:input:1\" mode="any" initial-timeout="aaaa"><grammar>sales,support</grammar></input>""")
		
		def errorMapping = assertValidationException(input)
		assertNotNull errorMapping
		assertEquals errorMapping.type, StanzaError.Type.MODIFY.toString()
		assertEquals errorMapping.condition, ExceptionMapper.toString(StanzaError.Condition.BAD_REQUEST)
		assertEquals errorMapping.text, String.format(Messages.INVALID_DURATION, 'initial-timeout')
	}
	
	@Test
	public void validateInputInvalidInterDigitTimeout() {
				
		def input = parseXml("""<input xmlns=\"urn:xmpp:rayo:input:1\" mode="any" inter-digit-timeout="aaaa"><grammar>sales,support</grammar></input>""")
		
		def errorMapping = assertValidationException(input)
		assertNotNull errorMapping
		assertEquals errorMapping.type, StanzaError.Type.MODIFY.toString()
		assertEquals errorMapping.condition, ExceptionMapper.toString(StanzaError.Condition.BAD_REQUEST)
		assertEquals errorMapping.text, String.format(Messages.INVALID_DURATION, 'inter-digit-timeout')
	}
	
	@Test
	public void validateInputInvalidConfidence() {
				
		def input = parseXml("""<input xmlns=\"urn:xmpp:rayo:input:1\" min-confidence="aaa"><grammar>sales,support</grammar></input>""")
		
		def errorMapping = assertValidationException(input)
		assertNotNull errorMapping
		assertEquals errorMapping.type, StanzaError.Type.MODIFY.toString()
		assertEquals errorMapping.condition, ExceptionMapper.toString(StanzaError.Condition.BAD_REQUEST)
		assertEquals errorMapping.text, Messages.INVALID_CONFIDENCE
	}
	
	@Test
	public void validateInputInvalidSensitivity() {
				
		def input = parseXml("""<input xmlns=\"urn:xmpp:rayo:input:1\" sensitivity="aaa"><grammar>sales,support</grammar></input>""")
		
		def errorMapping = assertValidationException(input)
		assertNotNull errorMapping
		assertEquals errorMapping.type, StanzaError.Type.MODIFY.toString()
		assertEquals errorMapping.condition, ExceptionMapper.toString(StanzaError.Condition.BAD_REQUEST)
		assertEquals errorMapping.text, String.format(Messages.INVALID_FLOAT, 'sensitivity')
	}
	
	@Test
	public void validateInputInvalidConfidenceRange() {
				
		def input = parseXml("""<input xmlns=\"urn:xmpp:rayo:input:1\" min-confidence="1.2"><grammar content-type="vxml">sales,support</grammar></input>""")
		
		def errorMapping = assertValidationException(input)
		assertNotNull errorMapping
		assertEquals StanzaError.Type.MODIFY.toString(), errorMapping.type
		assertEquals ExceptionMapper.toString(StanzaError.Condition.BAD_REQUEST), errorMapping.condition
		assertEquals Messages.INVALID_CONFIDENCE_RANGE, errorMapping.text
	}
	
	@Test
	public void validateInputInvalidConfidenceNegative() {
				
		def input = parseXml("""<input xmlns=\"urn:xmpp:rayo:input:1\" min-confidence="-1.0"><grammar content-type="vxml">sales,support</grammar></input>""")
		
		def errorMapping = assertValidationException(input)
		assertNotNull errorMapping
		assertEquals StanzaError.Type.MODIFY.toString(), errorMapping.type
		assertEquals ExceptionMapper.toString(StanzaError.Condition.BAD_REQUEST), errorMapping.condition
		assertEquals Messages.INVALID_CONFIDENCE_RANGE, errorMapping.text
	}
	
	
	@Test
	public void validateInputInvalidSensitivityRange() {
				
		def input = parseXml("""<input xmlns=\"urn:xmpp:rayo:input:1\" sensitivity="1.2"><grammar content-type="vxml">sales,support</grammar></input>""")
		
		def errorMapping = assertValidationException(input)
		assertNotNull errorMapping
		assertEquals StanzaError.Type.MODIFY.toString(), errorMapping.type
		assertEquals ExceptionMapper.toString(StanzaError.Condition.BAD_REQUEST), errorMapping.condition
		assertEquals Messages.INVALID_SENSITIVITY_RANGE, errorMapping.text
	}
	
	@Test
	public void validateInputInvalidSensitivityNegative() {
				
		def input = parseXml("""<input xmlns=\"urn:xmpp:rayo:input:1\" sensitivity="-1.0"><grammar content-type="vxml">sales,support</grammar></input>""")
		
		def errorMapping = assertValidationException(input)
		assertNotNull errorMapping
		assertEquals StanzaError.Type.MODIFY.toString(), errorMapping.type
		assertEquals ExceptionMapper.toString(StanzaError.Condition.BAD_REQUEST), errorMapping.condition
		assertEquals Messages.INVALID_SENSITIVITY_RANGE, errorMapping.text
	}
	
	@Test
	public void validateInvalidMaxSilence() {
				
		def input = parseXml("""<input xmlns=\"urn:xmpp:rayo:input:1\" max-silence="-1"><grammar content-type="vxml">sales,support</grammar></input>""")
		
		def errorMapping = assertValidationException(input)
		assertNotNull errorMapping
		assertEquals StanzaError.Type.MODIFY.toString(), errorMapping.type
		assertEquals ExceptionMapper.toString(StanzaError.Condition.BAD_REQUEST), errorMapping.condition
		assertEquals Messages.INVALID_MAX_SILENCE, errorMapping.text
	}
	
	@Test
	public void validateInputInvalidTerminator() {
				
		def input = parseXml("""<input xmlns=\"urn:xmpp:rayo:input:1\" terminator="abcd"><grammar content-type="vxml">sales,support</grammar></input>""")
		
		def errorMapping = assertValidationException(input)
		assertNotNull errorMapping
		assertEquals StanzaError.Type.MODIFY.toString(), errorMapping.type
		assertEquals ExceptionMapper.toString(StanzaError.Condition.BAD_REQUEST), errorMapping.condition
		assertEquals Messages.INVALID_TERMINATOR, errorMapping.text
	}
	
	// DTMF
	// ====================================================================================

	@Test
	public void validateDtmfMissingKey() {
				
		def output = parseXml("""<dtmf xmlns="urn:xmpp:rayo:1"/>""")
		
		def errorMapping = assertValidationException(output)
		assertNotNull errorMapping
		assertEquals errorMapping.type, StanzaError.Type.MODIFY.toString()
		assertEquals errorMapping.condition, ExceptionMapper.toString(StanzaError.Condition.BAD_REQUEST)
		assertEquals errorMapping.text, Messages.INVALID_DTMF_KEY
	}
	
	@Test
	public void validateDtmfEmptyKey() {
				
		def output = parseXml("""<dtmf xmlns="urn:xmpp:rayo:1" tones=""/>""")
		
		def errorMapping = assertValidationException(output)
		assertNotNull errorMapping
		assertEquals errorMapping.type, StanzaError.Type.MODIFY.toString()
		assertEquals errorMapping.condition, ExceptionMapper.toString(StanzaError.Condition.BAD_REQUEST)
		assertEquals errorMapping.text, Messages.INVALID_DTMF_KEY
	}
	
	@Test
	public void validateDtmfLongKey() {
				
		def output = parseXml("""<dtmf xmlns="urn:xmpp:rayo:1" tones="12"/>""")
		assertNotNull fromXML(output)
	}
	
	@Test
	public void validateDtmfValid() {
				
		def chars = ['0','1','2','3','4','5','6','7','8','9','#','*','A','B','C','D'] as char[]
		chars.each {
			def dtmf = parseXml("""<dtmf xmlns=\"urn:xmpp:rayo:1\" tones="${it}"/>""")
			assertEquals fromXML(dtmf).tones, String.valueOf(it)
		}
	}

	// Active Speakier
	// ====================================================================================

	@Test
	public void validateSpeakingMissingCallId() {
				
		def output = parseXml("""<started-speaking xmlns="urn:xmpp:rayo:1"/>""")
		
		def errorMapping = assertValidationException(output)
		assertNotNull errorMapping
		assertEquals errorMapping.type, StanzaError.Type.MODIFY.toString()
		assertEquals errorMapping.condition, ExceptionMapper.toString(StanzaError.Condition.BAD_REQUEST)
		assertEquals errorMapping.text, Messages.MISSING_SPEAKER_ID
	}
	
	@Test
	public void validateFinishedSpeakingMissingCallId() {
				
		def output = parseXml("""<stopped-speaking xmlns="urn:xmpp:rayo:1"/>""")
		
		def errorMapping = assertValidationException(output)
		assertNotNull errorMapping
		assertEquals errorMapping.type, StanzaError.Type.MODIFY.toString()
		assertEquals errorMapping.condition, ExceptionMapper.toString(StanzaError.Condition.BAD_REQUEST)
		assertEquals errorMapping.text, Messages.MISSING_SPEAKER_ID
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

		manager.fromXML(element)
	}
	
	private Element parseXml(String string) {
		
		new SAXReader().read(new StringReader(string)).rootElement;
	}
}
