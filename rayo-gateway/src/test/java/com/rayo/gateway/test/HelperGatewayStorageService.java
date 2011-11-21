package com.rayo.gateway.test;

import com.rayo.gateway.GatewayStorageService;
import com.rayo.gateway.memory.InMemoryStore;
import com.rayo.gateway.model.GatewayCall;
import com.rayo.gateway.model.GatewayClient;
import com.rayo.gateway.model.RayoNode;
import com.rayo.gateway.store.ApplicationsDatastore;
import com.rayo.gateway.store.CallsDatastore;

public class HelperGatewayStorageService extends GatewayStorageService {
	
	public HelperGatewayStorageService() {
		
		HelperNodesDatastore nodesDatastore = new HelperNodesDatastore();
		nodesDatastore.setStore(new InMemoryStore<RayoNode>());;
		setNodesDatastore(nodesDatastore);
		
		ApplicationsDatastore applicationsDatastore = new ApplicationsDatastore();
		applicationsDatastore.setStore(new InMemoryStore<GatewayClient>());
		setApplicationsDatastore(applicationsDatastore);
		
		CallsDatastore callsDatastore = new CallsDatastore();
		callsDatastore.setStore(new InMemoryStore<GatewayCall>());
		setCallsDatastore(callsDatastore);
	}
}
