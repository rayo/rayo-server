package com.rayo.core;

public enum CallRejectReason {

    DECLINE(603), 
    BUSY(486), 
    ERROR(500);

    private int code;

    private CallRejectReason(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
    
    public static CallRejectReason valueOf(int code) {
    	
    	for (CallRejectReason reason: values()) {
    		if (reason.getCode() == code) {
    			return reason;
    		}
    	}
    	throw new IllegalArgumentException(String.format("Could not found reject reason for code [%s]",code));
    }
}
