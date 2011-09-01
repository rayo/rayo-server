package com.tropo.server.validation;

import java.io.StringReader;

import javax.annotation.PostConstruct;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.springframework.core.io.Resource;

import com.tropo.core.validation.ValidationException;
import com.voxeo.logging.Loggerf;

public class SsmlValidator {

	private static final Loggerf logger = Loggerf.getLogger(SsmlValidator.class);	
	private Schema schema;
	Resource resource;

	@PostConstruct
	public void init() {

		final SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

		try {
			schema = sf.newSchema(resource.getFile());
		} catch (Exception e) {
			logger.error("Could not initialize SSML Validation. SSML Validation will be turned off");
			e.printStackTrace();
		}
	}
	public void validateSsml(String ssml) throws ValidationException {
				
		Validator validator = schema.newValidator();
		try {
			validator.validate(new StreamSource( new StringReader(ssml)));
		} catch (Exception e) {
			throw new ValidationException("Invalid SSML: " + e.getMessage());
		}
	}
	
	public void setResource(Resource resource) {
		this.resource = resource;
	}
}
