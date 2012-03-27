package com.rayo.gateway.jmx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;

import com.rayo.storage.GatewayStorageService;
import com.rayo.storage.model.Application;
import com.rayo.storage.model.GatewayMixer;
import com.rayo.storage.model.GatewayVerb;
import com.rayo.storage.model.RayoNode;
import com.voxeo.logging.Loggerf;

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
	
	private static final Loggerf log = Loggerf.getLogger(Gateway.class);
	
	private GatewayStorageService gatewayStorageService;

	@Override
	@ManagedAttribute(description="Platforms")
	public List<Platform> getPlatforms() {
		
		List<Platform> platforms = new ArrayList<Platform>();
		for(String platform: gatewayStorageService.getRegisteredPlatforms()) {
			platforms.add(new Platform(platform));
		}
		Collections.sort(platforms);
		return platforms;
	}	

	@Override
	@ManagedAttribute(description="Nodes")
	public List<Node> getRayoNodes() {
		
		List<Node> nodes = new ArrayList<Node>();
		for(String platform: gatewayStorageService.getRegisteredPlatforms()) {
			nodes.addAll(buildNodesList(gatewayStorageService.getRayoNodes(platform)));
		}
		Collections.sort(nodes);
		return nodes;
	}	
	
	@Override
	@ManagedAttribute(description="ClientApplications")
	public List<ClientApplication> getClientApplications() {
		
		List<ClientApplication> applications = new ArrayList<ClientApplication>();

		for(Application application: gatewayStorageService.getApplications()) {
			ClientApplication app = new ClientApplication(application);
			applications.add(app);
		}

		Collections.sort(applications);
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
	@ManagedAttribute(description="Returns a list with all active clients")
	public List<String> getActiveClients() {
		
		List<String> clients = gatewayStorageService.getClients();
		Collections.sort(clients);
		return clients;
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
	public List<String> getResourcesForJid(String jid) {

		return gatewayStorageService.getResourcesForClient(jid);
	}
	
	@Override
	public List<String> getAddressesForAppId(String appId) {

		ClientApplication application = getClientApplication(appId);
		if (application != null) {		
			return gatewayStorageService.getAddressesForApplication(application.getJID());
		}
		return null;
	}
	
	
	@Override
	public List<String> getAddressesForJid(String jid) {

		return gatewayStorageService.getAddressesForApplication(jid);
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

	@Override
	@ManagedOperation(description = "Returns call information")
	public Call callInfo(String callId) {
		
		String rayoNode = gatewayStorageService.getRayoNode(callId);
		String clientJID = gatewayStorageService.getclientJID(callId);
		
		return new Call(callId, rayoNode, clientJID);
	}

	@Override
	@ManagedAttribute(description="Mixers")
	public List<Mixer> getActiveMixers() {
		
		Map<String, Integer> verbsMap = new HashMap<String, Integer>();
		for(GatewayVerb verb: gatewayStorageService.getVerbs()) {
			Integer i = verbsMap.get(verb.getMixerName());
			if (i == null) {
				i = new Integer(0);
			}
			verbsMap.put(verb.getMixerName(), i+1);
		}
		
		List<Mixer> mixers = new ArrayList<Mixer>();
		for(GatewayMixer it: gatewayStorageService.getMixers()) {
			mixers.add(new Mixer(it.getName(), it.getNodeJid(), it.getParticipants(), verbsMap.get(it.getName())));
		}

		return mixers;
	}	

	@Override
	@ManagedOperation(description = "Returns mixer information")
	public Mixer mixerInfo(String mixerName) {
		
		GatewayMixer mixer = gatewayStorageService.getMixer(mixerName);
		if (mixer != null) {
			List<String> verbs = new ArrayList<String>();
			try {
				for(GatewayVerb verb: gatewayStorageService.getVerbs(mixerName)) {
					verbs.add(verb.getVerbId());
				}
			} catch (Exception e) {
				log.error(e.getMessage(),e);
			}
			return new Mixer(mixer.getName(), mixer.getNodeJid(), 
					mixer.getParticipants(), gatewayStorageService.getVerbs(mixerName).size());
		}
		return null;
	}
	
	@Override
	@ManagedOperation(description = "Returns verbs active for a mixer")
	public List<Verb> activeVerbs(String mixerName) {
		
		List<Verb> verbs = new ArrayList<Verb>();
		for(GatewayVerb it: gatewayStorageService.getVerbs(mixerName)) {
			verbs.add(new Verb(it.getMixerName(), it.getVerbId(), it.getAppJid()));
		}

		return verbs;
	}	
	
	public void setGatewayStorageService(GatewayStorageService gatewayStorageService) {
		this.gatewayStorageService = gatewayStorageService;
	}
}
