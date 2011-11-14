package com.rayo.gateway.jmx;

import java.util.ArrayList;
import java.util.List;

import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

import com.rayo.gateway.GatewayDatastore;
import com.voxeo.servlet.xmpp.JID;

/**
 * <p>This Mbean exposes relevant information on the Distributed hash table. It 
 * can be used to find calls being managed by the gateway, registered client 
 * resources, registered rayo nodes, etc. </p>
 * 
 * @author martin
 *
 */
@ManagedResource(objectName="com.rayo.gateway:Type=Gateway", description="Rayo Gateway")
public class Gateway implements GatewayMXBean {

	private GatewayDatastore gatewayDatastore;

	@ManagedAttribute(description="Platforms")
	public List<Platform> getPlatforms() {
		
		List<Platform> platforms = new ArrayList<Platform>();
		for(String platform: gatewayDatastore.getRegisteredPlatforms()) {
			platforms.add(new Platform(platform));
		}
		return platforms;
	}	

	@ManagedAttribute(description="Nodes")
	public List<Node> getRayoNodes() {
		
		List<Node> nodes = new ArrayList<Node>();
		for(String platform: gatewayDatastore.getRegisteredPlatforms()) {
			for (JID jid: gatewayDatastore.getRayoNodes(platform)) {
				Node node = new Node(jid);
				if (!nodes.contains(node)) {
					nodes.add(node);
					node.addPlatform(platform);
				} else {
					nodes.get(nodes.indexOf(node)).addPlatform(platform);
				}
			}
		}

		return nodes;
	}	
	
	@ManagedAttribute(description="ClientApplications")
	public List<ClientApplication> getClientApplications() {
		
		List<ClientApplication> clients = new ArrayList<ClientApplication>();

		for(JID jid: gatewayDatastore.getClientResources()) {
			ClientApplication client = new ClientApplication(jid);
			client.addResources(gatewayDatastore.getResourcesForClient(jid));
			clients.add(client);
		}

		return clients;
	}	
	
	public void setGatewayDatastore(GatewayDatastore gatewayDatastore) {
		this.gatewayDatastore = gatewayDatastore;
	}	
}
