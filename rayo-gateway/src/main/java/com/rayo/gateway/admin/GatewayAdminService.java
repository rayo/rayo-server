package com.rayo.gateway.admin;

import com.rayo.server.admin.AdminService;

public class GatewayAdminService extends AdminService {

	@Override
	public String getServerName() {

		return "Rayo Gateway";
	}	
}
