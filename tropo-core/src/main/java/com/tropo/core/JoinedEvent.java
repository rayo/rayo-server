package com.tropo.core;

import javax.validation.constraints.NotNull;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import com.tropo.core.validation.Messages;

public class JoinedEvent extends AbstractCallEvent {

	@NotNull(message=Messages.MISSING_TO)
	private String to;

	public JoinedEvent(String callId, String to) {
		super(callId);
		
		this.to = to;
	}

	public String getTo() {
		return to;
	}

	public void setTo(String to) {
		this.to = to;
	}

	@Override
	public String toString() {

		return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
				.append("callId", getCallId()).append("to", to).toString();
	}
}
