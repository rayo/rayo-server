package com.rayo.server.listener;

/**
 * Lets other services to listen for Quiesce events on the admin server
 * 
 * @author martin
 *
 */
public interface AdminListener {

	/**
	 * This method is called when a Rayo Server enters Quiesce mode 
	 */
	public void onQuiesceModeEntered();
	
	/**
	 * This method is called when a Rayo Server exits Quiesce mode
	 */
	public void onQuiesceModeExited();
	
	/**
	 * This method is invoked when Rayo Server is being shut down
	 */
	public void onShutdown();
	
	/**
	 * Notifies about a property change
	 * 
	 * @param property Property that has changed
	 * @param newValue New value for the property
	 */
	public void onPropertyChanged(String property, String newValue);
}
