package com.rayo.provisioning.model;


public class VstackVersionDto {
    private String version;
    private String revision;
    private String build;
    
    
    
    @Override
    public String toString() {
        return String.format("[version: %s   revision: %s]",version,revision);
    }



    public String getVersion() {
        return version;
    }



    public void setVersion(String version) {
        this.version = version;
    }



    public String getRevision() {
        return revision;
    }



    public void setRevision(String revision) {
        this.revision = revision;
    }



	public String getBuild() {
		return build;
	}



	public void setBuild(String build) {
		this.build = build;
	}
    
    
}
