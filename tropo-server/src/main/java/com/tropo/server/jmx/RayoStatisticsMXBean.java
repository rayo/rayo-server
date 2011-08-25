package com.tropo.server.jmx;

import java.util.Map;


public interface RayoStatisticsMXBean {

	public long getCallsReceived();
	public long getCallEventsProcessed();
	public long getIQsReceived();
	public Map<String, Long> getCommandsCount();
	public long getIQResponsesHandled();
	public long getIQErrorsSent();
	public long getIQResultsSent();
	public long getValidationErrors();
	public long getMessageStanzasReceived();
	public long getPresenceStanzasReceived();
	public long getTotalCommands();
}
