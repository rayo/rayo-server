package com.rayo.core;


/**
 * <p>Special type of commands that is only send to Rayo Nodes from 
 * Rayo Gateways when a Rayo Gateway wants to dispose a mixer resource 
 * from a Rayo Node.</p>
 * 
 * @author martin
 *
 */
public class DestroyMixerCommand extends AbstractCallCommand {

	public DestroyMixerCommand() {

		super();
	}
}
