package com.rayo.sbcrouter;

import java.util.List;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.URI;

import org.apache.log4j.Logger;

import com.micromethod.sipservices.proxy.router.Destination;
import com.micromethod.sipservices.proxy.router.Router;
import com.micromethod.sipservices.proxy.router.RouterHelper;
import com.rayo.storage.DefaultGatewayStorageService;
import com.rayo.storage.cassandra.CassandraDatastore;
import com.rayo.storage.lb.BlacklistingLoadBalancer;
import com.rayo.storage.lb.PriorityBasedLoadBalancer;
import com.rayo.storage.model.Application;
import com.rayo.storage.model.RayoNode;

public class RayoRouter implements Router {
	private static Logger LOG = Logger.getLogger(RayoRouter.class);

	protected RouterHelper _helper;

	private CassandraDatastore dataStore;
	private DefaultGatewayStorageService storageService;
	private BlacklistingLoadBalancer loadBalancer;

	public void init(Properties props, RouterHelper helper) {

		_helper = helper;
		String hostName = props.getProperty("CassandraHost");
		String port = props.getProperty("CassandraPort");

		if (hostName == null || port == null) {
			throw new IllegalArgumentException(
					"Please configure properties: CassandraHost and CassandraPort.");
		}

		dataStore = new CassandraDatastore();
		dataStore.setHostname(hostName);
		dataStore.setPort(port);
		dataStore.setCreateSampleApplication(false);
		dataStore.setOverrideExistingSchema(false);

		try {
			dataStore.init();
		} catch (Exception ex) {
			LOG.error(
					"Exception when initializing Cassandra connection, the properties:"
							+ props, ex);
			throw new RuntimeException(ex);
		}

		storageService = new DefaultGatewayStorageService();
		storageService.setStore(dataStore);
		storageService.setDefaultPlatform("staging");
		loadBalancer = new PriorityBasedLoadBalancer();
		loadBalancer.setStorageService(storageService);
	}

	public void route(SipServletRequest req, List<Destination> dests)
			throws ServletException {
		
		URI uri = req.getTo().getURI();

		Application application = dataStore.getApplicationForAddress(uri.toString());
		if (application == null) {
			LOG.error("Can't find application for request:" + req);
			return;
		} else {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Found application " + application + " for request:" + req);
			}
		}
		
		RayoNode node = loadBalancer.pickRayoNode(application.getPlatform());
		if (node == null) {
			LOG.error("Could not find available node for request " + req);
			return;
		} else {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Found node " + node + " for request:" + req);
			}
		}
		
		Destination destination = _helper.createDestination("sip:" + node.getIpAddress());
		dests.add(destination);
		
		//TODO: Failover and blacklisting
	}

	public void destroy() {

	}
}
