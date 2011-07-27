package com.tropo.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.tropo.core.cdr.Cdr;
import com.tropo.server.cdr.CdrErrorHandler;
import com.tropo.server.cdr.CdrStorageStrategy;
import com.voxeo.logging.Loggerf;
import com.voxeo.moho.Call;

public class CdrManager {

	private static final Loggerf logger = Loggerf.getLogger(CdrManager.class);
	
	private CdrErrorHandler errorHandler;
	private List<CdrStorageStrategy> storageStrategies = new ArrayList<CdrStorageStrategy>();

	private Map<String, Cdr> cdrs = new ConcurrentHashMap<String, Cdr>();

	private Comparator<Cdr> comparator = new Comparator<Cdr>() {
		
		public int compare(Cdr cdr1, Cdr cdr2) {
			
			return (int)(cdr1.getStartTime() - cdr2.getStartTime());
		};
	};
	
	public Cdr create(Call call) {

		Cdr cdr = new Cdr();
		cdr.setStartTime(System.currentTimeMillis());
		cdr.setCallId(call.getId());
		if (call.getInvitor() != null && call.getInvitor().getURI() != null) {
			cdr.setFrom(call.getInvitor().getURI().toString());
		}
		if (call.getInvitee() != null && call.getInvitee().getURI() != null) {
			cdr.setTo(call.getInvitee().getURI().toString());
		}
		cdrs.put(call.getId(),cdr);
		
		return cdr;
	}
	
	public void end(Call call) {
		
		Cdr cdr = cdrs.get(call.getId());
		if (cdr == null) {
			logger.error("Could not find CDR for call id %s", call.getId());
			return;
		}
		cdr.setEndTime(System.currentTimeMillis());
		if (call.getCallState() != null) {
			cdr.setState(call.getCallState().toString());
		}
	}
	
	public void append(String callId, String element) {
		
		if (callId == null) {
			logger.error("Received null call id from element %s", element);
			return;
		}
		Cdr cdr = cdrs.get(callId);
		if (cdr == null) {
			logger.error("Could not find CDR for call id %s", callId);
			return;
		}
		cdr.add(element);
	}
	
	public void store(String callId) {
		
		Cdr cdr = cdrs.get(callId);
		if (cdr == null) {
			logger.error("Could not find CDR for call id %s", callId);
			return;
		}

		for(CdrStorageStrategy storageStrategy: storageStrategies) {
			try {
				storageStrategy.store(cdr);
			} catch (Exception e) {
				errorHandler.handleException(e);
			}
		}
		
		cdrs.remove(callId);
	}
	
	public void setErrorHandler(CdrErrorHandler errorHandler) {
		
		this.errorHandler = errorHandler;
	}

	public void setStorageStrategies(List<CdrStorageStrategy> strategies) {
		
		this.storageStrategies = strategies;
	}	
	
	public List<Cdr> getActiveCdrs() {
		
		return sort(cdrs.values());
	}
	
	private List<Cdr> sort(Collection<Cdr> values) {

		List<Cdr> cdrs = new ArrayList<Cdr>(values);
		Collections.sort(cdrs, comparator);
		return cdrs;
	}

	public Cdr getCdr(String callId) {
		
		return cdrs.get(callId);
	}
	
	public void reset() {
		
		cdrs.clear();
	}

	public void setSpiStorageStrategies(List<CdrStorageStrategy> spiStorageStrategies) {
		
		// These are SPI storage strategies that the user has provided. 
		// We just copy them to the standard storage strategies list
		if (spiStorageStrategies != null) {
			storageStrategies.addAll(spiStorageStrategies);
		}
	}
}
