package com.rayo.provisioning.model;

import static com.voxeo.utils.Strings.isEmpty;

import com.voxeo.web.Gsonable;

@Gsonable
public class AddressNotification {
    
    private String type;
    private String address;
    private String appId;
    
    @Override
    public String toString() {
        return String.format("[Notification %s, %s]",type, address);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getAppId() {
		return appId;
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}

	public boolean hasType() {
        return !isEmpty(type);
    }
}
