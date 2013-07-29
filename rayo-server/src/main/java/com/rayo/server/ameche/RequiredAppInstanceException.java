package com.rayo.server.ameche;

public class RequiredAppInstanceException extends Exception {

    private static final long serialVersionUID = -8096746567312906036L;
	private AppInstance appInstance;

    public RequiredAppInstanceException(AppInstance appInstance, Exception reason) {
        
    	super("A required AppInstance has failed", reason);
        this.appInstance = appInstance;
    }

	public AppInstance getAppInstance() {
		return appInstance;
	}
}
