package com.rayo.gateway.jmx;

import java.util.ArrayList;
import java.util.List;

import org.springframework.jmx.export.annotation.ManagedResource;

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
	public boolean equals(Object obj) {
		
		if (!(obj instanceof Node)) return false;
		return (((Node)obj).jid.toString().equals(jid.toString()));
	}
	
	@Override
	public int hashCode() {

		return jid.toString().hashCode();
	}
}
