package com.rayo.gateway.jmx;

import java.util.ArrayList;
import java.util.List;

import org.springframework.jmx.export.annotation.ManagedResource;

import com.rayo.gateway.GatewayDatastore;
import com.voxeo.servlet.xmpp.JID;

/**
 * <p>This MBean represents each of the Rayo Nodes.</p>
 * 
 * @author martin
 *
 */
@ManagedResource(objectName="com.rayo.gateway:Type=Platform", description="Platform")
public class Node implements RayoNodeMXBean {

	private JID jid;
	private List<String> platforms = new ArrayList<String>();
	private GatewayDatastore gatewayDatastore;

	public Node(JID jid) {
		this.jid = jid;
	}
	
	@Override
	public String getJID() {

		return jid.toString();
	}
	
	@Override
	public List<String> getPlatforms() {

		return platforms;
	}
	
	public void addPlatform(String platform) {
		
		platforms.add(platform);
	}
	
	@Override
	public List<Call> getCalls() {

		List<Call> calls = new ArrayList<Call>();
		for(String callId : gatewayDatastore.getCallsForRayoNode(jid)) {
			Call call = new Call(callId, jid, gatewayDatastore.getclientJID(callId));
			calls.add(call);
		}
		return calls;
	}
	
	@Override
	public boolean equals(Object obj) {
		
		if (!(obj instanceof Node)) return false;
		return (((Node)obj).jid.toString().equals(jid.toString()));
	}
	
	@Override
	public int hashCode() {

		return jid.toString().hashCode();
	}

	public void setGatewayDatastore(GatewayDatastore gatewayDatastore) {

		this.gatewayDatastore = gatewayDatastore;
	}
}
