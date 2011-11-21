package com.rayo.gateway.test;

import com.rayo.gateway.model.RayoNode;
import com.rayo.gateway.store.NodesDatastore;

public class HelperNodesDatastore extends NodesDatastore {

	public void addIpAddressMapping(String ipAddress, RayoNode node) {
		
		addressMap.put(ipAddress, node);
	}
}
