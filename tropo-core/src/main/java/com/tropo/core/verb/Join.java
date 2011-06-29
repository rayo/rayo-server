package com.tropo.core.verb;

import java.util.Map;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import com.tropo.core.validation.ValidDirection;
import com.tropo.core.validation.ValidJoinType;

public class Join extends BaseVerb {

	@ValidDirection
	private String direction;

	@ValidJoinType
	private String media;

	private String to;

	private Map<String, String> headers;

	public String getDirection() {
		return direction;
	}

	public void setDirection(String direction) {
		this.direction = direction;
	}

	public String getMedia() {
		return media;
	}

	public void setMedia(String media) {
		this.media = media;
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

	public void setHeaders(Map<String, String> headers) {
		this.headers = headers;
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
				.append("callId", getCallId()).append("verbId", getVerbId())
				.append("direction", direction).append("media", media)
				.append("headers", headers).toString();

	}
}
