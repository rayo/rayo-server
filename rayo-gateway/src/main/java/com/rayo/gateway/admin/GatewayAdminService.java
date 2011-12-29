package com.rayo.gateway.admin;

import java.util.List;

import com.rayo.gateway.GatewayStorageService;
import com.rayo.gateway.exception.GatewayException;
import com.rayo.gateway.model.RayoNode;
import com.rayo.server.admin.AdminService;
import com.voxeo.logging.Loggerf;

public class GatewayAdminService extends AdminService {

	private GatewayStorageService storageService;
	
	private static final Loggerf log = Loggerf.getLogger(GatewayAdminService.class);
	
	private int maxDialRetries = 3;
	
	public void blacklist(String platformId, String hostname, boolean blacklisted) {
		
		log.debug("Setting Node [%s]'s blacklist status to [%s] on platform [%s]", hostname, blacklisted, platformId);
		List<RayoNode> nodes = storageService.getRayoNodes(platformId);
		for(RayoNode node: nodes) {
			if(node.getHostname().equals(hostname)) {
				node.setBlackListed(blacklisted);
				try {
					storageService.updateRayoNode(node);
				} catch (GatewayException e) {}
				break;
			}
		}
	}
	
	@Override
	public String getServerName() {

		return "Rayo Gateway";
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
