package com.rayo.core;

import javax.validation.constraints.NotNull;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import com.rayo.core.validation.Messages;

public class JoinedEvent extends AbstractCallEvent {

	@NotNull(message=Messages.MISSING_JOIN_ID)
	private String to;
	
	private JoinDestinationType type; 

	public JoinedEvent(String callId, String to, JoinDestinationType type) {
		super(callId);
		
		this.to = to;
		this.type = type;
	}

	public String getTo() {
		return to;
	}

	public void setTo(String to) {
		this.to = to;
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
				.append("to", to)
				.append("type", type).toString();
	}
}
