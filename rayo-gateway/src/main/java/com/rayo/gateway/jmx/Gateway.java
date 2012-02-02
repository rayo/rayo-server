package com.rayo.gateway.jmx;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;

import com.rayo.storage.GatewayStorageService;
import com.rayo.storage.model.Application;
import com.rayo.storage.model.RayoNode;
import com.rayo.storage.util.JIDUtils;

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
	
	private GatewayStorageService gatewayStorageService;

	@Override
	@ManagedAttribute(description="Platforms")
	public List<Platform> getPlatforms() {
		
		List<Platform> platforms = new ArrayList<Platform>();
		for(String platform: gatewayStorageService.getRegisteredPlatforms()) {
			platforms.add(new Platform(platform));
		}
		return platforms;
	}	

	@Override
	@ManagedAttribute(description="Nodes")
	public List<Node> getRayoNodes() {
		
		List<Node> nodes = new ArrayList<Node>();
		for(String platform: gatewayStorageService.getRegisteredPlatforms()) {
			nodes.addAll(buildNodesList(gatewayStorageService.getRayoNodes(platform)));
		}

		return nodes;
	}	
	
	@Override
	@ManagedAttribute(description="ClientApplications")
	public List<ClientApplication> getClientApplications() {
		
		List<ClientApplication> applications = new ArrayList<ClientApplication>();

		for(Application application: gatewayStorageService.getApplications()) {
			String bareJid = JIDUtils.getBareJid(application.getJid());			
			ClientApplication app = new ClientApplication(application);
			if (!applications.contains(app)) {
				app.addResources(gatewayStorageService.getResourcesForClient(bareJid));
				List<String> addresses = gatewayStorageService.getAddressesForApplication(application.getBareJid());
				if (addresses !=  null) {
					app.setAddresses(StringUtils.join(addresses,','));
				}
				applications.add(app);
			}
		}

		return applications;
	}	
	
	@Override
	@ManagedAttribute(description="Returns a client application by id")
	public ClientApplication getClientApplication(String appId) {
		
		for(ClientApplication application: getClientApplications()) {
			if (application.getAppId().equals(appId)) {
				return application;
			}
		}
		return null;
	}
	
	@Override
	public List<String> getResourcesForAppId(String appId) {

		ClientApplication application = getClientApplication(appId);
		if (application != null) {		
			return gatewayStorageService.getResourcesForClient(application.getJID());
		}
		return null;
	}
	

	@Override
	@ManagedOperation(description = "Returns Rayo Nodes for a given platform")
	public List<Node> getRayoNodes(String platformId) {
		
		return buildNodesList(gatewayStorageService.getRayoNodes(platformId));
	}	
	
	private List<Node> buildNodesList(List<RayoNode> rayoNodes) {

		Set<Node> nodes = new HashSet<Node>();
		for (RayoNode rayoNode: rayoNodes) {
			Node node = new Node(rayoNode);
			node.setGatewayStorageService(gatewayStorageService);
			nodes.add(node);
		}
		return new ArrayList<Node>(nodes);
	}

	@ManagedOperation(description = "Returns call information")
	public Call callInfo(String callId) {
		
		String rayoNode = gatewayStorageService.getRayoNode(callId);
		String clientJID = gatewayStorageService.getclientJID(callId);
		
		return new Call(callId, rayoNode, clientJID);
	}
	
	public void setGatewayStorageService(GatewayStorageService gatewayStorageService) {
		this.gatewayStorageService = gatewayStorageService;
	}
}
