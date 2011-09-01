package com.rayo.server.validation

import static org.junit.Assert.*

import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner

import com.rayo.server.validation.SsmlValidator;
import com.rayo.core.validation.ValidationException;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations=["/rayo-context-activemq-cdr.xml"])
public class SsmlValidationTest {

    @Autowired
    private SsmlValidator ssmlValidator
	
    @Test
    public void testSsmlValidator() throws InterruptedException {

		assertNotNull ssmlValidator
		ssmlValidator.validateSsml("<say-as interpret-as=\"number:cardinal\">12345</say-as>")
		
		try {
			ssmlValidator.validateSsml("<output-as interpret-as=\"number:cardinal\">12345</output-as>")
			fail "Expected Ssml validation exception"
		} catch (ValidationException ve) {
			assertEquals ve.getMessage(), "Invalid SSML: cvc-elt.1: Cannot find the declaration of element 'output-as'."
		}
		
	}
}