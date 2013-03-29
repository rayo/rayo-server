package com.rayo.core;

import javax.validation.constraints.NotNull;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import com.rayo.core.validation.Messages;

public class JoiningEvent extends AbstractCallEvent {

	@NotNull(message=Messages.MISSING_JOIN_ID)
	private String peerCallId;

	@NotNull(message=Messages.MISSING_TARGET_ADDRESS)
	private String to;
	
	private JoinDestinationType type; 

	public JoiningEvent(String callId, String peerCallId, String to) {
		super(callId);
		
		this.peerCallId = peerCallId;
		this.to = to;
		this.type = JoinDestinationType.CALL;
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

	public String getPeerCallId() {
		return peerCallId;
	}

	public void setPeerCallId(String peerCallId) {
		this.peerCallId = peerCallId;
	}

	@Override
	public String toString() {

		return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
				.append("callId", getCallId())
				.append("to", to)
				.append("type", type).toString();
	}
}
