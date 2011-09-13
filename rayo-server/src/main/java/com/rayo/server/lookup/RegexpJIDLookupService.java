package com.rayo.server.lookup;

import java.io.IOException;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.core.io.Resource;

import com.rayo.core.OfferEvent;
import com.rayo.server.util.LinkedProperties;
import com.voxeo.logging.Loggerf;

public class RegexpJIDLookupService implements RayoJIDLookupService<OfferEvent> {

	private static final Loggerf logger = Loggerf.getLogger(RegexpJIDLookupService.class);
	
	private Map<Pattern, String> patterns = new LinkedHashMap<Pattern, String>();
	
	public RegexpJIDLookupService(Resource properties) throws IOException {
		
		read(properties);
	}
	
	private void read(Resource properties) throws IOException {
		
		if (properties.exists()) {
			Properties props = new LinkedProperties();
			props.load(properties.getInputStream());
			@SuppressWarnings("rawtypes")
			Enumeration en = props.keys();
			while(en.hasMoreElements()) {
				String key = en.nextElement().toString();
				try {					
					Pattern p = Pattern.compile(key);
					patterns.put(p,props.getProperty(key));
				} catch (Exception e) {
					logger.error(String.format("Could not parse Regexp pattern: '%s'",key));
				}
			}
		} else {
			logger.warn("Could not find JID lookup service configuration file");
		}
	}

	@Override
	public String lookup(OfferEvent event) {
		
		for (Pattern pattern: patterns.keySet()) {
			Matcher matcher = pattern.matcher(event.getTo().toString());
			if (matcher.matches()) {
				String value = patterns.get(pattern);
				if (logger.isDebugEnabled()) {
					logger.debug("Found a match for %s : %s", event.getTo().toString(), value);
				}
				return value;
			}
		}
		if (logger.isDebugEnabled()) {
			logger.debug("We didn't find any Regexp match for %s", event.getTo().toString());
		}
		return null;
	}
}
