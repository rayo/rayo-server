package com.rayo.server.lookup;

import java.io.IOException;
import java.net.URI;

import org.springframework.core.io.Resource;

import com.rayo.core.OfferEvent;
import com.rayo.server.exception.RayoProtocolException;
import com.rayo.server.storage.PropertiesBasedDatastore;
import com.voxeo.logging.Loggerf;

/**
 * <p>Regexp based implementation of the {@link RayoJIDLookupService} interface.</p>
 * 
 * <p>This implementation uses a file named rayo-routing.properties to map incoming
 * calls (offer events) to the actual client applications that will handle the calls.
 * The format of the mapping file is very simple and basically matches regular expressions
 * with hardcoded client applications:</p>
 * <ul>
 * <li>.*+13457800.*=usera@localhost</li>
 * <li>.*sipusername.*=userb@localhost</li>
 * <li>.*=userc@localhost</li>
 * </ul>
 * <p>The mappings on the rayo-routing.properties are reloaded each minute.</p>
 * <p>Although you can use this implementation on any setup (staging, production) but 
 * mainly it has been created for testing and as a reference implementation.</p>  
 * 
 * @author martin
 *
 */
public class RegexpJIDLookupService implements RayoJIDLookupService<OfferEvent> {

	private static final Loggerf logger = Loggerf.getLogger(RegexpJIDLookupService.class);

	private PropertiesBasedDatastore datastore;

	@Override
	public String lookup(OfferEvent event) throws RayoProtocolException {
		
		return lookup(event.getTo());
	}

	@Override
	public String lookup(URI uri) throws RayoProtocolException {
		
		logger.debug("Trying to find a match for URI [%s]", uri);
		return datastore.lookup(uri);
	}


	public void setDatastore(PropertiesBasedDatastore datastore) {
		this.datastore = datastore;
	}
}
