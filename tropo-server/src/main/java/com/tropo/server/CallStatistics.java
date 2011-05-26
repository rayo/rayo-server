package com.tropo.server;

import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

import com.tropo.server.jmx.CallStatisticsMXBean;

@ManagedResource(objectName="com.tropo:Type=Call Statistics", description="Call Statistics")
public class CallStatistics implements CallStatisticsMXBean {

	private long callsAccepted;
	private long callsAnswered;
	private long callsBusy;
	private long callsHangedUp;
	private long callsFailed;
	private long callsRejected;
	private long callsTimedout;
	private long callsEndedUnknownReason;
	private long callsRedirected;
	private long callsIncoming;
	private long callsOutgoing;
	private long verbsCreated;
	
	@ManagedAttribute(description="Busy Calls Count")
	public long getCallsBusy() {
		return callsBusy;
	}

	public void callBusy() {

		callsBusy++;
	}

	public void callAccepted() {
		
		callsAccepted++;
	}
	
	public void callAnswered() {
		
		callsAnswered++;
	}
	
	@ManagedAttribute(description="Answered Calls Count")
	public long getCallsAnswered() {
		
		return callsAnswered;
	}

	@ManagedAttribute(description="Hanged Up Calls Count")
	public long getCallsHangedUp() {
		
		return callsHangedUp;
	}
	
	public void callHangedUp() {
		
		callsHangedUp++;
	}

	public void callRejected() {
		
		callsRejected++;
	}
	
	public void callTimedout() {
		
		callsTimedout++;
	}
	
	public void callFailed() {
		
		callsFailed++;
	}

	@ManagedAttribute(description="Accepted Calls Count")
	public long getCallsAccepted() {
		return callsAccepted;
	}

	@ManagedAttribute(description="Failed Calls Count")
	public long getCallsFailed() {
		return callsFailed;
	}

	@ManagedAttribute(description="Rejected Calls Count")
	public long getCallsRejected() {
		return callsRejected;
	}

	@ManagedAttribute(description="Timed out Calls Count")
	public long getCallsTimedout() {
		return callsTimedout;
	}

	@ManagedAttribute(description="Busy Calls Count")
	public void callEndedUnknownReason() {
		
		callsEndedUnknownReason++;
	}
	
	@ManagedAttribute(description="Calls Ended Unknown Reason")
	public long getCallsEndedUnknownReason() {
		
		return callsEndedUnknownReason;
	}

	public void callRedirected() {
		
		callsRedirected++;
	}
	
	@ManagedAttribute(description="Redirected Calls")
	public long getCallsRedirected() {
		
		return callsRedirected;
	}

	public void incomingCall() {
		
		callsIncoming++;
	}
	
	@ManagedAttribute(description="Total Incoming Calls")
	public long getIncomingCalls() {
		
		return callsIncoming;
	}	

	public void outgoingCall() {
		
		callsOutgoing++;
	}
	
	@ManagedAttribute(description="Total Outgoing Calls")
	public long getOutgoingCalls() {
		
		return callsOutgoing;
	}	
	
	@ManagedAttribute(description="Total Calls")
	public long getTotalCalls() {
		
		return callsIncoming + callsOutgoing;
	}

	@ManagedAttribute(description="Total Verbs")
	public long getTotalVerbs() {
		return verbsCreated;
	}

	public void verbCreated() {

		verbsCreated++;
	}
}
