package com.tropo.core;

import javax.validation.constraints.NotNull;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import com.tropo.core.validation.Messages;

public class UnjoinedEvent extends AbstractCallEvent {

	@NotNull(message=Messages.MISSING_FROM)
	private String from;

	public UnjoinedEvent(String callId, String from) {
		super(callId);
		
		this.from = from;
	}

	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	@Override
	public String toString() {

		return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
				.append("callId", getCallId()).append("to", from).toString();
	}
}
