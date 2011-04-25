package com.tropo.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ExecutionContext {

    public static final String KEY = "com.tropo.ExecutionContext";

    public static final String ACCOUNT_ID = "com.tropo.accountId";
    public static final String APPLICATION_ID = "com.tropo.applicationId";
    public static final String APPLICATION_NAME = "com.tropo.applicationName";
    public static final String PERMISSIONS = "com.tropo.permissions";

    private Map<String, Object> properties = new HashMap<String, Object>();

    // Property Accesors
    // ================================================================================

    public Object set(String propertyName, Object value) {
        return properties.put(propertyName, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String propertyName) {
        return (T)properties.get(propertyName);
    }

    public String getString(String propertyName) {
        return (String) properties.get(propertyName);
    }

    public String getString(String propertyName, String def) {
        Object value = properties.get(propertyName);
        if (value == null || !(value instanceof String)) {
            return def;
        }
        return (String) value;
    }

    public boolean contains(String propertyName) {
        return properties.containsKey(propertyName);
    }
    
    // Properties
    // ================================================================================

    public String getAccountId() {
        return getString(ACCOUNT_ID);
    }

    public void setAccountId(String accountId) {
        set(ACCOUNT_ID, accountId);
    }

    public String getApplicationId() {
        return getString(APPLICATION_ID);
    }

    public void setApplicationId(String applicationId) {
        set(APPLICATION_ID, applicationId);
    }

    public String getApplicationName() {
        return getString(APPLICATION_NAME);
    }

    public void setApplicationName(String applicationName) {
        set(APPLICATION_NAME, applicationName);
    }

    @SuppressWarnings("unchecked")
    public Set<Permissions> getPremissions() {
        return (Set<Permissions>) get(PERMISSIONS);
    }

    public void setPremissions(Set<Permissions> premissions) {
        set(PERMISSIONS, premissions);
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

}
