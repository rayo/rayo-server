package com.rayo.provisioning.model;

import java.net.URI;

public class UserFeatureDto extends BaseDto {
    private URI feature;

    private String featureName;
    
    private String featureFlag;
    
    public URI getFeature() {
        return feature;
    }

    public void setFeature(URI feature) {
        this.feature = feature;
    }

    public String getFeatureName() {
        return featureName;
    }

    public void setFeatureName(String featureName) {
        this.featureName = featureName;
    }

	public String getFeatureFlag() {
		return featureFlag;
	}

	public void setFeatureFlag(String featureFlag) {
		this.featureFlag = featureFlag;
	}
}
