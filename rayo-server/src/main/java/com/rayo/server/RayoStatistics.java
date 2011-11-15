package com.rayo.server;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

import com.rayo.server.jmx.RayoStatisticsMXBean;

@ManagedResource(objectName="com.rayo:Type=Rayo", description="Rayo Statistics")
public class RayoStatistics implements RayoStatisticsMXBean {

	private AtomicLong callsReceived = new AtomicLong(0);
	private AtomicLong callEventsProcessed = new AtomicLong(0);
	private AtomicLong iqsReceived = new AtomicLong(0);
	private AtomicLong iqResponsesHandled = new AtomicLong(0);
	private AtomicLong iqErrorsSent = new AtomicLong(0);
	private AtomicLong iqResultsSent = new AtomicLong(0);
	private AtomicLong validationErrors = new AtomicLong(0);
	private AtomicLong messagesReceived = new AtomicLong(0);
	private AtomicLong presencesReceived = new AtomicLong(0);
	private AtomicLong presenceErrorsReceived = new AtomicLong(0);
	
	private Map<String, AtomicLong> commands = new ConcurrentHashMap<String, AtomicLong>();
	private AtomicLong totalCommands = new AtomicLong(0);
	
	@ManagedAttribute(description="Calls Received Count")
	public long getCallsReceived() {
		
		return callsReceived.longValue();
	}
	
	public void callReceived() {
		
		callsReceived.incrementAndGet();
	}
	
	@ManagedAttribute(description="Processed Call Events Count")
	public long getCallEventsProcessed() {
		
		return callEventsProcessed.longValue();
	}
	
	public void callEventProcessed() {
		
		callEventsProcessed.incrementAndGet();
	}
	
	@ManagedAttribute(description="IQs Received Count")
	public long getIQsReceived() {
		
		return iqsReceived.longValue();
	}
	
	public void iqReceived() {
		
		iqsReceived.incrementAndGet();
	}
	
	@ManagedAttribute(description="Handled IQ Responses Count")
	public long getIQResponsesHandled() {
		
		return iqResponsesHandled.longValue();
	}
	
	public void iqResponse() {
		
		iqResponsesHandled.incrementAndGet();
	}
	
	@ManagedAttribute(description="Sent IQ Errors Count")
	public long getIQErrorsSent() {
		
		return iqErrorsSent.longValue();
	}
	
	public void iqError() {
		
		iqErrorsSent.incrementAndGet();
	}
	
	@ManagedAttribute(description="Sent IQ Results Count")
	public long getIQResultsSent() {
		
		return iqResultsSent.longValue();
	}
	
	public void iqResult() {
		
		iqResultsSent.incrementAndGet();
	}	
	
	@ManagedAttribute(description="Message Stanzas Received Count")
	public long getMessageStanzasReceived() {
		
		return messagesReceived.longValue();
	}
	
	public void messageStanzaReceived() {
		
		messagesReceived.incrementAndGet();
	}	
	
	@ManagedAttribute(description="Presence Stanzas Received Count")
	public long getPresenceStanzasReceived() {
		
		return presencesReceived.longValue();
	}
	
	public void presenceStanzaReceived() {
		
		presencesReceived.incrementAndGet();
	}
	
	@ManagedAttribute(description="Presence Errors Received Count")
	public long getPresenceErrorsReceived() {
		
		return presenceErrorsReceived.longValue();
	}
	
	public void presenceErrorReceived() {
		
		presenceErrorsReceived.incrementAndGet();
	}
	
	@ManagedAttribute(description="Validation Errors Count")
	public long getValidationErrors() {
		
		return validationErrors.longValue();
	}
	
	public void validationError() {
		
		validationErrors.incrementAndGet();
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
		totalCommands.incrementAndGet();
	}

	@ManagedAttribute(description="Total Commands Count")
	public long getTotalCommands() {
		
		return totalCommands.longValue();
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
