package com.rayo.sbcrouter;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.sip.Proxy;
import javax.servlet.sip.ServletParseException;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServlet;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.TooManyHopsException;
import javax.servlet.sip.URI;

import org.apache.log4j.Logger;

import com.rayo.server.storage.DefaultGatewayStorageService;
import com.rayo.server.storage.model.Application;
import com.rayo.server.storage.model.RayoNode;
import com.rayo.storage.cassandra.CassandraDatastore;
import com.rayo.storage.lb.BlacklistingLoadBalancer;
import com.rayo.storage.lb.PriorityBasedLoadBalancer;

public class SBCRouterServlet extends SipServlet {

  private static final long serialVersionUID = -5565293666294007230L;

  private static Logger LOG = Logger.getLogger(SBCRouterServlet.class);

  private CassandraDatastore dataStore;

  private DefaultGatewayStorageService storageService;

  private BlacklistingLoadBalancer loadBalancer;

  private SipFactory _sipFactory;

  @Override
  public void init() throws ServletException {
    _sipFactory = (SipFactory) getServletContext().getAttribute(SipFactory.class.getName());

    String hostName = getServletConfig().getInitParameter("CassandraHost");
    String port = getServletConfig().getInitParameter("CassandraPort");

    if (hostName == null || port == null) {
      throw new IllegalArgumentException("Please configure servlet parameter: CassandraHost and CassandraPort.");
    }

    dataStore = new CassandraDatastore();
    dataStore.setHostname(hostName);
    dataStore.setPort(port);
    dataStore.setOverrideExistingSchema(false);

    try {
      dataStore.init();
    }
    catch (Exception ex) {
      LOG.error("Exception when initializing Cassandra connection, the properties:" + hostName + ":" + port, ex);
      throw new RuntimeException(ex);
    }

    storageService = new DefaultGatewayStorageService();
    storageService.setStore(dataStore);
    loadBalancer = new PriorityBasedLoadBalancer();
    loadBalancer.setStorageService(storageService);
  }

  @Override
  protected void doRequest(SipServletRequest req) throws ServletException, IOException {
    if (req.isInitial()) {
      proxyTo(req);
    }
  }

  @Override
  protected void doResponse(SipServletResponse resp) throws ServletException, IOException {
    processReponse(resp);
  }

  @Override
  protected void doBranchResponse(SipServletResponse resp) throws ServletException, IOException {
    processReponse(resp);
  }

  private void proxyTo(SipServletRequest req) {
    URI uri = req.getTo().getURI();

    Application application = dataStore.getApplicationForAddress(uri.toString());
    if (application == null) {
      LOG.error("Can't find application for request:" + req);
	  LOG.debug("Trying to fetch default application: 'voxeo'");
      application = dataStore.getApplication("voxeo");
      if (application != null) {
          LOG.debug("Found application " + application + " for request:" + req);
      } else {
    	  return;
      }
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

    try {
      Proxy proxy = req.getProxy(true);
      proxy.setAddToPath(true);
      proxy.setRecordRoute(true);
      proxy.setRecurse(false);

      proxy.setParallel(false);
      proxy.setSupervised(true);

      proxy.proxyTo(_sipFactory.createURI("sip:" + node.getIpAddress()));
    }
    catch (TooManyHopsException e) {
      LOG.error("Can't proxy request:" + req, e);
    }
    catch (ServletParseException e) {
      LOG.error("Can't parse uri " + "sip:" + node.getIpAddress(), e);
    }
  }

  private void processReponse(SipServletResponse resp) {
    if (resp.getStatus() >= 200) {
      SipServletRequest req = resp.getProxy().getOriginalRequest();
      RayoNode oldNode = (RayoNode) req.getAttribute("CurrentTryingRayoNode");

      if ((resp.getStatus() >= 500 && resp.getStatus() < 600)
          || (resp.getStatus() == SipServletResponse.SC_REQUEST_TIMEOUT && resp.getReasonPhrase().equalsIgnoreCase(
              "Request Timeout:Proxy Created Internally"))) {
        LOG.warn("Node operation failed:" + oldNode + " for request:" + req);
        loadBalancer.nodeOperationFailed(oldNode);
        proxyTo(req);
      }
      else {
        loadBalancer.nodeOperationSuceeded(oldNode);
      }
    }
  }
}
