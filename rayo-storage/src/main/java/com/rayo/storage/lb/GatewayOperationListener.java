package com.rayo.storage.lb;

import com.rayo.storage.model.RayoNode;

/**
 * Callback interface for operations on rayo nodes and client resources.
 * 
 * @author martin
 *
 */
public interface GatewayOperationListener {

	/**
	 * <p>Reports a failure on a Rayo Node to the load balancer system. This method gives a 
	 * chance to load balancer systems to adjust their strategy. Like for example, a load 
	 * balancer could disable for 30 seconds a rayo node that has produced two consecutive 
	 * failures.</p>
	 * 
	 * <p>This interface offers this method solely as a way to report failures. Bear in mind 
	 * that implementations of this interface are not obliged to provide any failure protection 
	 * mechanisms.</p> 
	 * 
	 * @param node Rayo node
	 */
	void nodeOperationFailed(RayoNode node);
	
	/**
	 * <p>Reports a successful operation on a Rayo Node. Commonly this method serves as a 
	 * callback to disable any blacklisting operations that may have been done on the rayo 
	 * node as result of previous failures.</p>
	 *  
	 * @param node Rayo node
	 */
	void nodeOperationSuceeded(RayoNode node);
	
	
	/**
	 * <p>Reports a failure on a Client resource to the load balancer system. This method gives a 
	 * chance to load balancer systems to adjust their strategy. Like for example, a load 
	 * balancer could disable for 30 seconds a concrete cliente resource that has produced a few
	 * consecutive failures.</p>
	 * 
	 * <p>This interface offers this method solely as a way to report failures. Bear in mind 
	 * that implementations of this interface are not obliged to provide any failure protection 
	 * mechanisms.</p> 
	 * 
	 * @param fullJid JID of the client resource
	 */
	void clientOperationFailed(String fullJid);
	
	/**
	 * <p>Reports a successful operation on a Client resource. Commonly this method serves as a 
	 * callback to disable any blacklisting operations that may have been done on the client 
	 * resource as result of previous failures.</p>
	 *  
	 * @param fullJid JID of the client resource
	 */
	void clientOperationSuceeded(String fullJid);
}
