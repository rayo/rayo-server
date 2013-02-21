package com.rayo.server.ameche;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.regex.Pattern;

import org.dom4j.Element;

/**
 * <p>Resolves Ameche routing rules defined on a external file, typically WEB-INF/ameche-routing.properties. 
 * Routing rules will have into consideration both to/from fields. All instances matching the given offer will 
 * be dispatched.</p>
 * 
 * @author martin
 *
 */
public class SimpleRegexpAppInstanceResolver implements AppInstanceResolver {

	class RoutingRule {
		
		Pattern pattern;
		AppInstance instance;
	}
	
    private List<RoutingRule> rules = new ArrayList<SimpleRegexpAppInstanceResolver.RoutingRule>();

    @Override
    public List<AppInstance> lookup(Element offer) {
    	
    	List<AppInstance> instances = new ArrayList<AppInstance>();
    	String from = offer.attributeValue("from");
    	String to = offer.attributeValue("to");
    	for(RoutingRule rule: rules) {
    		if (rule.pattern.matcher(to).matches() || rule.pattern.matcher(from).matches()) {
    			instances.add(rule.instance);
    		}
    	}
    	return instances;
    }

	public void setRoutingRules(Properties routingRules) {

		int id = 0;
		for(Entry<Object,Object> entry: routingRules.entrySet()) {
			RoutingRule rule = new RoutingRule(); 			
			rule.pattern = Pattern.compile((String)entry.getKey());			
			try {
				rule.instance = new AppInstance(String.valueOf(++id), new URI((String)entry.getValue()));
				rules.add(rule);
			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
