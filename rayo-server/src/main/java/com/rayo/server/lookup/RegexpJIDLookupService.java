package com.rayo.server.lookup;

import java.io.IOException;
import java.net.URI;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.core.io.Resource;

import com.rayo.core.OfferEvent;
import com.rayo.server.util.LinkedProperties;
import com.voxeo.logging.Loggerf;

public class RegexpJIDLookupService implements RayoJIDLookupService<OfferEvent> {

	private static final Loggerf logger = Loggerf.getLogger(RegexpJIDLookupService.class);
	
	private Map<Pattern, String> patterns = new LinkedHashMap<Pattern, String>();
	
	private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	
	public RegexpJIDLookupService(final Resource properties) throws IOException {
		
		TimerTask readTask = new TimerTask() {
			
			@Override
			public void run() {

				Lock lock = RegexpJIDLookupService.this.lock.writeLock();
				try {
					lock.lock();
					read(properties);
				} catch (IOException e) {
					logger.error(e.getMessage(),e);
				} finally {
					lock.unlock();
				}
			}
		};
		new Timer().schedule(readTask, 0, 60000);
	}
	
	private void read(Resource properties) throws IOException {
		
		if (logger.isDebugEnabled()) {
			logger.debug("Reading JID Lookup Service configuration from disk [%s]", properties.getFilename());
		}
		if (properties.exists()) {
			Properties props = new LinkedProperties();
			props.load(properties.getInputStream());
			@SuppressWarnings("rawtypes")
			Enumeration en = props.keys();
			while(en.hasMoreElements()) {
				String key = en.nextElement().toString().trim();
				try {					
					Pattern p = Pattern.compile(key);
					patterns.put(p,props.getProperty(key).trim());
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
		
		return lookup(event.getTo());
	}
	

	@Override
	public String lookup(URI uri) {
		
		Lock lock = RegexpJIDLookupService.this.lock.readLock();
		try {
			lock.lock();
			for (Pattern pattern: patterns.keySet()) {
				Matcher matcher = pattern.matcher(uri.toString());
				if (matcher.matches()) {
					String value = patterns.get(pattern);
					if (logger.isDebugEnabled()) {
						logger.debug("Found a match for %s : %s", uri.toString(), value);
					}
					return value;
				}
			}
			if (logger.isDebugEnabled()) {
				logger.debug("We didn't find any Regexp match for %s", uri.toString());
			}
			return null;
		} finally {
			lock.unlock();
		}
	}
}
