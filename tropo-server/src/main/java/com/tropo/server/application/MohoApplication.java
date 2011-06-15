package com.tropo.server.application;

import java.util.Set;

import com.tropo.core.application.TokenApplication;
import com.voxeo.moho.Endpoint;

public interface MohoApplication extends TokenApplication
{
	public Set<Endpoint> getEndpoints ();
}
