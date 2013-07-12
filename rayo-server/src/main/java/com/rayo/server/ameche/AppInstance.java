package com.rayo.server.ameche;

import java.net.URI;
import java.util.EnumSet;
import java.util.Set;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import com.ameche.repo.RuntimePermission;

public class AppInstance {

    private String id;
    private URI endpoint;
    private Integer priority;
    private Integer permissions;

    public AppInstance(String id, URI endpoint, Integer priority, Integer permissions) {
        this.setId(id);
        this.setEndpoint(endpoint);
        this.setPriority(priority);
        this.setPermissions(permissions);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public URI getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(URI endpoint) {
        this.endpoint = endpoint;
    }

    @Override
    public boolean equals(Object obj) {

    	if (!(obj instanceof AppInstance)) return false;
    	
    	return id.equals(((AppInstance)obj).id);
    }
    
	public Integer getPriority() {
		return priority;
	}

	public void setPriority(Integer priority) {
		this.priority = priority;
	}

	public Integer getPermissions() {
		return permissions;
	}

	public void setPermissions(Integer permissions) {
		this.permissions = permissions;
	}

	private Set<RuntimePermission> getRuntimePermissions() {
		
		Set<RuntimePermission> result = EnumSet.noneOf(RuntimePermission.class);
		if (permissions != null) {
			for (RuntimePermission perm : RuntimePermission.values()) {
				if (hasPermission(perm)) {
					result.add(perm);
				}
			}
		}
		return result;
	}
	
	public boolean hasPermission(RuntimePermission permission) {
		
		return ((permissions & (1 << permission.ordinal())) > 0);		
	}
	
	@Override
    public String toString() {

    	return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)    		
    		.append("id", getId())
    		.append("endpoint", getEndpoint())
    		.append("priority", getPriority())
    		.append("permissions", getRuntimePermissions())
    		.toString();
    }
}
