package com.tropo.server;

import java.util.ArrayList;
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

	public Cdr create(Call call) {

		Cdr cdr = new Cdr();
		cdr.setStartTime(System.currentTimeMillis());
		cdr.setCallId(call.getId());
		cdr.setFrom(call.getInvitor().toString());
		cdr.setTo(call.getInvitee().toString());
		
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
		cdr.setState(call.getCallState().toString());
	}
	
	public void append(String callId, String element) {
		
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
		
		return new ArrayList<Cdr>(cdrs.values());
	}
	
	public Cdr getCdr(String callId) {
		
		return cdrs.get(callId);
	}
	
	public void reset() {
		
		cdrs.clear();
	}
}
