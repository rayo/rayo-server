package com.rayo.server.gateway.test;

import com.rayo.server.gateway.InMemoryGatewayDatastore;
import com.rayo.server.gateway.RayoNode;

public class HelperInMemoryGatewayDatastore extends InMemoryGatewayDatastore {

	public void addIpAddressMapping(String ipAddress, RayoNode node) {
		
		addressMap.put(ipAddress, node);
	}
}
