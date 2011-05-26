package com.tropo.server;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

import com.tropo.server.jmx.OzoneStatisticsMXBean;

@ManagedResource(objectName="com.tropo:Type=Ozone", description="Ozone Statistics")
public class OzoneStatistics implements OzoneStatisticsMXBean {

	private long callsReceived;
	private long callEventsProcessed;
	private long iqsReceived;
	private long iqResponsesHandled;
	private long iqErrorsSent;
	private long iqResultsSent;
	private long validationErrors;
	
	private Map<String, AtomicLong> commands = new ConcurrentHashMap<String, AtomicLong>();
	
	@ManagedAttribute(description="Calls Received Count")
	public long getCallsReceived() {
		
		return callsReceived;
	}
	
	public void callReceived() {
		
		callsReceived++;
	}
	
	@ManagedAttribute(description="Processed Call Events Count")
	public long getCallEventsProcessed() {
		
		return callEventsProcessed;
	}
	
	public void callEventProcessed() {
		
		callEventsProcessed++;
	}
	
	@ManagedAttribute(description="IQs Received Count")
	public long getIQsReceived() {
		
		return iqsReceived;
	}
	
	public void iqReceived() {
		
		iqsReceived++;
	}
	
	@ManagedAttribute(description="Handled IQ Responses Count")
	public long getIQResponsesHandled() {
		
		return iqResponsesHandled;
	}
	
	public void iqResponse() {
		
		iqResponsesHandled++;
	}
	
	@ManagedAttribute(description="Sent IQ Errors Count")
	public long getIQErrorsSent() {
		
		return iqErrorsSent;
	}
	
	public void iqError() {
		
		iqErrorsSent++;
	}
	
	@ManagedAttribute(description="Sent IQ Results Count")
	public long getIQResultsSent() {
		
		return iqResultsSent;
	}
	
	public void iqResult() {
		
		iqResultsSent++;
	}	
	
	@ManagedAttribute(description="Validation Errors Count")
	public long getValidationErrors() {
		
		return validationErrors;
	}
	
	public void validationError() {
		
		validationErrors++;
	}	
	
	public void commandReceived(Object command) {
		
		String className = command.getClass().getName();
		className = className.substring(className.lastIndexOf('.') + 1);
		if (className.endsWith("Command")) {
			className = className.replaceAll("Command", "");
		}
		AtomicLong count = commands.get(className);
		if (count == null) {
			count = new AtomicLong();
			commands.put(className, count);
		}
		count.incrementAndGet();
	}

	@ManagedAttribute(description="Commands Count")
	public Map<String, Long> getCommandsCount() {
		
		Map<String,Long> commandCount = new HashMap<String, Long>();
		for(Map.Entry<String, AtomicLong> entry: commands.entrySet()) {
			commandCount.put(entry.getKey(), entry.getValue().longValue());
		}
		return commandCount;
	}
}
