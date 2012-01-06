package com.rayo.sbcrouter;

import java.util.List;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.URI;

import org.apache.log4j.Logger;
import org.mortbay.log.Log;

import com.micromethod.sipservices.proxy.router.CallbackableRouter;
import com.micromethod.sipservices.proxy.router.Destination;
import com.micromethod.sipservices.proxy.router.RouterHelper;
import com.rayo.storage.DefaultGatewayStorageService;
import com.rayo.storage.cassandra.CassandraDatastore;
import com.rayo.storage.lb.BlacklistingLoadBalancer;
import com.rayo.storage.lb.PriorityBasedLoadBalancer;
import com.rayo.storage.model.Application;
import com.rayo.storage.model.RayoNode;

public class RayoRouter implements CallbackableRouter {

  private static Logger LOG = Logger.getLogger(RayoRouter.class);

  protected RouterHelper _helper;

  private CassandraDatastore dataStore;

  private DefaultGatewayStorageService storageService;

  private BlacklistingLoadBalancer loadBalancer;

  @Override
  public void init(Properties props, RouterHelper helper) {

    _helper = helper;
    String hostName = props.getProperty("CassandraHost");
    String port = props.getProperty("CassandraPort");

    if (hostName == null || port == null) {
      throw new IllegalArgumentException("Please configure properties: CassandraHost and CassandraPort.");
    }

    dataStore = new CassandraDatastore();
    dataStore.setHostname(hostName);
    dataStore.setPort(port);
    dataStore.setCreateSampleApplication(false);
    dataStore.setOverrideExistingSchema(false);

    try {
      dataStore.init();
    }
    catch (Exception ex) {
      LOG.error("Exception when initializing Cassandra connection, the properties:" + props, ex);
      throw new RuntimeException(ex);
    }

    storageService = new DefaultGatewayStorageService();
    storageService.setStore(dataStore);
    storageService.setDefaultPlatform("staging");
    loadBalancer = new PriorityBasedLoadBalancer();
    loadBalancer.setStorageService(storageService);
  }

  @Override
  public void route(SipServletRequest req, List<Destination> dests) throws ServletException {
    proxyTo(req, dests);

    // TODO: Failover and blacklisting
  }

  @Override
  public void callback(SipServletResponse resp, Destination dest, List<Destination> dests) {
    SipServletRequest req = resp.getProxy().getOriginalRequest();
    RayoNode oldNode = (RayoNode) req.getAttribute("CurrentTryingRayoNode");

    if (resp.getStatus() >= 500 && resp.getStatus() < 600) {
      Log.warn("Node operation failed:" + oldNode + " for request:" + req);
      loadBalancer.nodeOperationFailed(oldNode);

      proxyTo(req, dests);
    }
    else {
      loadBalancer.nodeOperationSuceeded(oldNode);
    }
  }

  private void proxyTo(SipServletRequest req, List<Destination> dests) {
    URI uri = req.getTo().getURI();

    Application application = dataStore.getApplicationForAddress(uri.toString());
    if (application == null) {
      LOG.error("Can't find application for request:" + req);
      return;
    }
    else {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Found application " + application + " for request:" + req);
      }
    }

    RayoNode node = loadBalancer.pickRayoNode(application.getPlatform());
    if (node == null) {
      LOG.error("Could not find available node for request " + req);
      return;
    }
    else {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Found node " + node + " for request:" + req);
      }
      req.setAttribute("CurrentTryingRayoNode", node);
    }

    Destination destination = _helper.createDestination("sip:" + node.getIpAddress());
    dests.add(destination);
  }

  @Override
  public void destroy() {

  }
}
