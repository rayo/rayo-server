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
import com.rayo.gateway.cassandra.CassandraDatastore;
import com.rayo.gateway.model.Application;
import com.rayo.gateway.model.RayoNode;

public class RayoRouter implements Router {
  private static Logger LOG = Logger.getLogger(RayoRouter.class);

  protected RouterHelper _helper;

  protected CassandraDatastore _store;

  public void init(Properties props, RouterHelper helper) {
    _helper = helper;
    String hostName = props.getProperty("CassandraHost");
    String port = props.getProperty("CassandraPort");

    if (hostName == null || port == null) {
      throw new IllegalArgumentException("Please configure properties: CassandraHost and CassandraPort.");
    }
    _store = new CassandraDatastore();
    _store.setHostname(hostName);
    _store.setPort(port);
    _store.setCreateSampleApplication(false);
    _store.setOverrideExistingSchema(false);

    try {
      _store.init();
    }
    catch (Exception ex) {
      LOG.error("Exception when initializing Cassandra connection, the properties:" + props, ex);
      throw new RuntimeException(ex);
    }
  }

  public void route(SipServletRequest req, List<Destination> dests) throws ServletException {
    URI uri = req.getTo().getURI();

    Application application = _store.getApplicationForAddress(uri.toString());
    if (application == null) {
      LOG.error("Can't find application for request:" + req);
      return;
    }
    List<RayoNode> nodes = _store.getRayoNodesForPlatform(application.getPlatform());
    if (nodes == null || nodes.size() == 0) {
      LOG.error("Can't find nodes for request:" + req);
      return;
    }
    if (LOG.isDebugEnabled()) {
      LOG.debug("Found nodes for request:" + req);
    }

    StringBuffer sb = new StringBuffer();
    sb.append("Found nodes for request:");
    for (RayoNode node : nodes) {
      dests.add(_helper.createDestination("sip:"+node.getIpAddress()));
      sb.append(node).append("|");
    }
    sb.append(" ").append(req.toString());
    LOG.debug(sb.toString());
  }

  public void destroy() {

  }
}
