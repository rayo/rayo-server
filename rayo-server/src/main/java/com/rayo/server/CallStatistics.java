package com.rayo.server;

import java.util.concurrent.atomic.AtomicLong;

import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

import com.rayo.server.jmx.CallStatisticsMXBean;

@ManagedResource(objectName="com.rayo:Type=Call Statistics", description="Call Statistics")
public class CallStatistics implements CallStatisticsMXBean {

	private AtomicLong callsAccepted = new AtomicLong(0);
	private AtomicLong callsAnswered = new AtomicLong(0);
	private AtomicLong callsBusy = new AtomicLong(0);
	private AtomicLong callsHungUp = new AtomicLong(0);
	private AtomicLong callsFailed = new AtomicLong(0);
	private AtomicLong callsRejected = new AtomicLong(0);
	private AtomicLong callsTimedout = new AtomicLong(0);
	private AtomicLong callsEndedUnknownReason = new AtomicLong(0);
	private AtomicLong callsRedirected = new AtomicLong(0);
	private AtomicLong callsIncoming = new AtomicLong(0);
	private AtomicLong callsOutgoing = new AtomicLong(0);
	private AtomicLong verbsCreated = new AtomicLong(0);
	
	@ManagedAttribute(description="Busy Calls Count")
	public long getCallsBusy() {
		return callsBusy.longValue();
	}

	public void callBusy() {

		callsBusy.incrementAndGet();
	}

	public void callAccepted() {
		
		callsAccepted.incrementAndGet();
	}
	
	public void callAnswered() {
		
		callsAnswered.incrementAndGet();
	}
	
	@ManagedAttribute(description="Answered Calls Count")
	public long getCallsAnswered() {
		
		return callsAnswered.longValue();
	}

	@ManagedAttribute(description="Hung Up Calls Count")
	public long getCallsHungUp() {
		
		return callsHungUp.longValue();
	}
	
	public void callHangedUp() {
		
		callsHungUp.incrementAndGet();
	}

	public void callRejected() {
		
		callsRejected.incrementAndGet();
	}
	
	public void callTimedout() {
		
		callsTimedout.incrementAndGet();
	}
	
	public void callFailed() {
		
		callsFailed.incrementAndGet();
	}

	@ManagedAttribute(description="Accepted Calls Count")
	public long getCallsAccepted() {
		return callsAccepted.longValue();
	}

	@ManagedAttribute(description="Failed Calls Count")
	public long getCallsFailed() {
		return callsFailed.longValue();
	}

	@ManagedAttribute(description="Rejected Calls Count")
	public long getCallsRejected() {
		return callsRejected.longValue();
	}

	@ManagedAttribute(description="Timed out Calls Count")
	public long getCallsTimedout() {
		return callsTimedout.longValue();
	}

	@ManagedAttribute(description="Busy Calls Count")
	public void callEndedUnknownReason() {
		
		callsEndedUnknownReason.incrementAndGet();
	}
	
	@ManagedAttribute(description="Calls Ended Unknown Reason")
	public long getCallsEndedUnknownReason() {
		
		return callsEndedUnknownReason.longValue();
	}

	public void callRedirected() {
		
		callsRedirected.incrementAndGet();
	}
	
	@ManagedAttribute(description="Redirected Calls")
	public long getCallsRedirected() {
		
		return callsRedirected.longValue();
	}

	public void incomingCall() {
		
		callsIncoming.incrementAndGet();
	}
	
	@ManagedAttribute(description="Total Incoming Calls")
	public long getIncomingCalls() {
		
		return callsIncoming.longValue();
	}	

	public void outgoingCall() {
		
		callsOutgoing.incrementAndGet();
	}
	
	@ManagedAttribute(description="Total Outgoing Calls")
	public long getOutgoingCalls() {
		
		return callsOutgoing.longValue();
	}	
	
	@ManagedAttribute(description="Total Calls")
	public long getTotalCalls() {
		
		return callsIncoming.longValue() + callsOutgoing.longValue();
	}

	@ManagedAttribute(description="Total Verbs")
	public long getTotalVerbs() {
		return verbsCreated.longValue();
	}

	public void verbCreated() {

		verbsCreated.incrementAndGet();
	}
}
