package com.tropo.server.jmx;


public interface CallStatisticsMXBean {

	public long getCallsBusy();
	public long getCallsAnswered();
	public long getCallsHangedUp();
	public long getCallsAccepted();
	public long getCallsFailed();
	public long getCallsRejected();
	public long getCallsTimedout();
	public long getCallsEndedUnknownReason();
	public long getCallsRedirected();
	public long getIncomingCalls();
	public long getOutgoingCalls();
	public long getTotalCalls();
	public long getTotalVerbs();
}
