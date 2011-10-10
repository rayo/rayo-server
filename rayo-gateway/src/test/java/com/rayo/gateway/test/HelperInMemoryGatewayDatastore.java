package com.rayo.gateway.test;

import com.rayo.gateway.InMemoryGatewayDatastore;
import com.rayo.gateway.RayoNode;

public class HelperInMemoryGatewayDatastore extends InMemoryGatewayDatastore {

	public void addIpAddressMapping(String ipAddress, RayoNode node) {
		
		addressMap.put(ipAddress, node);
	}
}
