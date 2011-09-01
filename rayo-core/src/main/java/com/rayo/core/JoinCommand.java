package com.rayo.core;

import javax.media.mscontrol.join.Joinable.Direction;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import com.rayo.core.validation.Messages;
import com.voxeo.moho.Participant.JoinType;

public class JoinCommand extends AbstractCallCommand {
	
	public static final String MEDIA_TYPE = "MEDIA_TYPE";	
	public static final String DIRECTION = "DIRECTION";
	public static final String TO = "TO";
	public static final String TYPE = "TYPE";
	
	private Direction direction = Direction.DUPLEX;

	private JoinType media = JoinType.BRIDGE;

	@NotNull(message=Messages.MISSING_JOIN_ID)
	private String to;
	
	private JoinDestinationType type; 

	public Direction getDirection() {
		return direction;
	}

	public void setDirection(Direction direction) {
		this.direction = direction;
	}

	public JoinType getMedia() {
		return media;
	}

	public void setMedia(JoinType media) {
		this.media = media;
	}

	public String getTo() {
		return to;
	}

	public void setTo(String to) {
		this.to = to;
	}

	
	public void setType(JoinDestinationType type) {
		this.type = type;
	}

	public JoinDestinationType getType() {
		return type;
	}

	@Override
	public String toString() {

		return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
				.append("callId", getCallId())
				.append("direction", direction).append("media", media)
				.append("to",to)
				.append("type", type).toString();

	}
}
