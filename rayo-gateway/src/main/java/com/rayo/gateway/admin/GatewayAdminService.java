package com.rayo.gateway.admin;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.rayo.server.admin.AdminService;
import com.rayo.server.storage.GatewayException;
import com.rayo.server.storage.GatewayStorageService;
import com.rayo.server.storage.model.Application;
import com.rayo.server.storage.model.RayoNode;
import com.voxeo.logging.Loggerf;

public class GatewayAdminService extends AdminService {

	private GatewayStorageService storageService;
	private Set<String> bannedJids = new HashSet<String>();

	private static final Loggerf log = Loggerf.getLogger(GatewayAdminService.class);
	
	private int maxDialRetries = 3;
	
	public void blacklist(String platformId, String hostname, boolean blacklisted) {
		
		log.debug("Setting Node [%s]'s blacklist status to [%s] on platform [%s]", hostname, blacklisted, platformId);
		List<RayoNode> nodes = storageService.getRayoNodes(platformId);
		for(RayoNode node: nodes) {
			if(node.getHostname().equals(hostname)) {
				node.setBlackListed(blacklisted);
				node.setConsecutiveErrors(0);
				try {
					storageService.updateRayoNode(node);
				} catch (GatewayException e) {}
				break;
			}
		}
	}
	
	public void ban(String jid) {
		
		bannedJids.add(jid);
	}
	
	public void unban(String jid) {
		
		bannedJids.remove(jid);
	}
	
	public boolean isBanned(String jid) {
		
		return bannedJids.contains(jid);
	}
	
	@Override
	public String getServerName() {

		return "Rayo Gateway";
	}
	
	public void removeNode(String jid) throws GatewayException {
		
		storageService.unregisterRayoNode(jid);
	}
	
	public void registerApplication(Application application) throws GatewayException {
		
		storageService.registerApplication(application);
	}

	public void registerAddress(String jid, String address) throws GatewayException {
		
		storageService.storeAddress(address, jid);
	}
	
	
	public void unregisterApplication(String jid) throws GatewayException {
		
		storageService.unregisterApplication(jid);
	}

	public void unregisterAddress(String address) throws GatewayException {
		
		storageService.removeAddress(address);
	}
	
	public void setStorageService(GatewayStorageService storageService) {
		this.storageService = storageService;
	}

	public int getMaxDialRetries() {
		return maxDialRetries;
	}

	public void setMaxDialRetries(int maxDialRetries) {
		this.maxDialRetries = maxDialRetries;
	}	
}
