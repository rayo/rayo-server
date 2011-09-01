package com.rayo.core;

import javax.validation.constraints.NotNull;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import com.rayo.core.validation.Messages;

public class UnjoinedEvent extends AbstractCallEvent {

	@NotNull(message=Messages.MISSING_JOIN_ID)
	private String from;
	
	private JoinDestinationType type; 

	public UnjoinedEvent(String callId, String from, JoinDestinationType type) {
		super(callId);
		
		this.from = from;
		this.type = type;
	}

	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	public JoinDestinationType getType() {
		return type;
	}

	public void setType(JoinDestinationType type) {
		this.type = type;
	}

	@Override
	public String toString() {

		return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
				.append("callId", getCallId())
				.append("from", from)
				.append("type", type).toString();
	}
}
