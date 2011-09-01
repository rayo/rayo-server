package com.rayo.server.jmx;

import java.io.Serializable;
import java.util.List;

import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

import com.rayo.server.CdrManager;
import com.rayo.core.cdr.Cdr;

@ManagedResource(objectName="com.rayo:Type=Cdrs", description="Active CDRs")
public class Cdrs implements Serializable, CdrsMXBean {

	private static final long serialVersionUID = 1L;

	private CdrManager cdrManager;

	@ManagedAttribute(description="Active CDRs")
	public List<Cdr> getActiveCDRs() {
		
		return cdrManager.getActiveCdrs();
	}
	
	public void setCdrManager(CdrManager cdrManager) {
		
		this.cdrManager = cdrManager;
	}
}
