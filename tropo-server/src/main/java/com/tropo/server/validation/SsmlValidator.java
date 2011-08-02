package com.tropo.server.validation;

import java.io.InputStream;
import java.io.StringReader;

import javax.annotation.PostConstruct;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.commons.io.IOUtils;

import com.tropo.core.validation.ValidationException;
import com.voxeo.logging.Loggerf;

public class SsmlValidator {

	private static final Loggerf logger = Loggerf.getLogger(SsmlValidator.class);
	private static int TIMEOUT = 10000;
	
	private Schema schema;
	private Thread initThread;

	@PostConstruct
	public void init() {

		final SchemaFactory sf = SchemaFactory
				.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		logger.debug("Using schema factory", sf);
		InputStream is = Thread.currentThread().getContextClassLoader()
				.getResourceAsStream("synthesis-core.xsd");
		if (is == null) {
			logger.error("Could not find the SSML Schema in the classpath. SSML validation will be turned off");
			return;
		}
		try {
			final String strSchema = IOUtils.toString(is);

			// XML compilation is a very slow operation. 
			initThread = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						schema = sf.newSchema(new StreamSource(new StringReader(strSchema)));
					} catch (Exception e) {
						logger.error("Could not initialize SSML Validation. SSML Validation will be turned off");
						e.printStackTrace();
					}					
				}
			});
			initThread.start();
		} catch (Exception e) {
			logger.error("Could not initialize SSML Validation. SSML Validaiton will be turned off");
			e.printStackTrace();
		}
	}
	
	void waitToInitialize(long timeout) {
		
		try {
			initThread.join(timeout);
		} catch (InterruptedException e) {
			throw new IllegalStateException(e.getMessage());
		}
	}
	
	public void validateSsml(String ssml) throws ValidationException {
		
		if (schema == null) {
			if (initThread.isAlive()) {
				try {
					initThread.join(TIMEOUT);
				} catch (InterruptedException e) {
					if (initThread.isAlive()) {
						logger.debug("Could not validate SSML. Schema validation is off");
					}
				}
			} else {
				logger.debug("Could not validate SSML. Schema validation is off");
				return;
			}
		}
		
		Validator validator = schema.newValidator();
		try {
			validator.validate(new StreamSource( new StringReader(ssml)));
		} catch (Exception e) {
			throw new ValidationException("Invalid SSML: " + e.getMessage());
		}
	}
}
