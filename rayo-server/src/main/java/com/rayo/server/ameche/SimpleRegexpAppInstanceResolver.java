package com.rayo.server.ameche;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Pattern;

import org.dom4j.Element;
import org.springframework.core.io.Resource;

import com.voxeo.logging.Loggerf;

/**
 * <p>Resolves Ameche routing rules defined on a external file, typically WEB-INF/ameche-routing.properties. 
 * Routing rules will have into consideration both to/from fields. All instances matching the given offer will 
 * be dispatched.</p>
 * 
 * @author martin
 *
 */
public class SimpleRegexpAppInstanceResolver implements AppInstanceResolver {

	private static final Loggerf logger = Loggerf.getLogger(SimpleRegexpAppInstanceResolver.class);
	
	class RoutingRule {
		
		Pattern pattern;
		AppInstance instance;
	}
	
    private List<RoutingRule> rules = new ArrayList<SimpleRegexpAppInstanceResolver.RoutingRule>();
    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    
	public SimpleRegexpAppInstanceResolver(final Resource properties) throws IOException {
		
		this(properties, 60000);
	}
	
	public SimpleRegexpAppInstanceResolver(final Resource properties, int delay) throws IOException {
		
		read(properties);
		
		TimerTask readTask = new TimerTask() {
			
			@Override
			public void run() {

				try {
					read(properties);
				} catch (IOException e) {
					logger.error(e.getMessage(),e);
				}
			}
		};
		new Timer().schedule(readTask, delay, delay);
	}
	
    @Override
    public List<AppInstance> lookup(Element offer) {
    	
    	List<AppInstance> instances = new ArrayList<AppInstance>();
    	List<RoutingRule> rules = null;
    	Lock lock = SimpleRegexpAppInstanceResolver.this.lock.readLock();    	
    	try {
    		lock.lock();
    		rules = new ArrayList<SimpleRegexpAppInstanceResolver.RoutingRule>(this.rules);
    	} finally {
    		lock.unlock();
    	}
    	String from = offer.attributeValue("from");
    	String to = offer.attributeValue("to");
    	for(RoutingRule rule: rules) {
    		if (rule.pattern.matcher(to).matches() || rule.pattern.matcher(from).matches()) {
    			instances.add(rule.instance);
    		}
    	}
    	return instances;
    }
	
	void read(Resource properties) throws IOException {
		
		try {
			logger.debug("Reading Ameche routes from disk [%s]", properties.getFilename());
		} catch (IllegalStateException ise) {
			// Ignore. On testing a byte array does not have a filename property and throws an exception
		}
		Lock lock = SimpleRegexpAppInstanceResolver.this.lock.writeLock();
		try {
			lock.lock();
			rules.clear();
			if (properties.isReadable()) {
	
				InputStream is = null;
				try {
					File file = properties.getFile();
					if (file.exists()) {
						is = new FileInputStream(file);
					}			
				} catch (IOException e) {
					is = properties.getInputStream();
				}
				int id = 0;
				Scanner scanner = new Scanner(is);
				while(scanner.hasNextLine()) {
					String line = scanner.nextLine();
					if (line.trim().length() > 0 && !line.trim().startsWith("#")) {
						String[] elements = line.trim().split("=");
						if (!(elements.length == 2)) {
							logger.error("Could not parse line %s", line);
							continue;
						}
						String pattern = elements[0].trim();
						String uri = elements[1].trim();
						RoutingRule rule = new RoutingRule(); 			
						rule.pattern = Pattern.compile(pattern);			
						try {
							rule.instance = new AppInstance(String.valueOf(++id), new URI(uri));
							rules.add(rule);
						} catch (URISyntaxException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			} else {
				logger.warn("Could not find AppInstance resolver configuration file [%s]", properties);
			}
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			throw new IOException(e);
		} finally {
			lock.unlock();
		}			
	}

}
