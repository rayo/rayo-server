package com.rayo.server.lookup;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.core.io.Resource;

import com.rayo.core.OfferEvent;
import com.voxeo.logging.Loggerf;

public class RegexpJIDLookupService implements RayoJIDLookupService<OfferEvent> {

	private static final Loggerf logger = Loggerf.getLogger(RegexpJIDLookupService.class);
	
	private Map<Pattern, String> patterns = new LinkedHashMap<Pattern, String>();
	
	public RegexpJIDLookupService(Resource properties) throws IOException {
		
		read(properties);
	}
	
	private void read(Resource properties) throws IOException {
		
		if (properties.exists()) {
			Properties props = new Properties();
			props.load(properties.getInputStream());
			for(Entry<Object, Object> entry: props.entrySet()) {
				try {
					Pattern p = Pattern.compile(entry.getKey().toString());
					patterns.put(p,entry.getValue().toString());
				} catch (Exception e) {
					logger.error("Could not parse Regexp pattern: " + entry.getKey());
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
				return patterns.get(pattern);
			}
		}
		return null;
	}
	
	public static void main(String[] args) {
		
		 Pattern p = Pattern.compile(".*@localhost");
		 Matcher m = p.matcher("usera@localhost");
		 boolean b = m.matches();
		 System.out.println(b);
	}
}
