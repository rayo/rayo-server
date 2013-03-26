package com.rayo.core.verb;

import java.net.URI;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.joda.time.Duration;

import com.rayo.core.validation.ValidFileFormat;

public class Record extends BaseVerb {

	private URI to;
	
	@ValidFileFormat
	private String format;
	
	private Duration maxDuration;
	
	private Boolean startBeep;
	
	private Boolean stopBeep;
	
	private Boolean startPaused;
	
	private Duration initialTimeout;
	
	private Duration finalTimeout;
	
	private Boolean duplex;
	
	public URI getTo() {
		return to;
	}

	public void setTo(URI to) {
		this.to = to;
	}

	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	public Duration getMaxDuration() {
		return maxDuration;
	}

	public void setMaxDuration(Duration maxDuration) {
		this.maxDuration = maxDuration;
	}

	public Boolean getStartBeep() {
		return startBeep;
	}

	public void setStartBeep(Boolean startBeep) {
		this.startBeep = startBeep;
	}

	public Boolean getStartPaused() {
		return startPaused;
	}

	public void setStartPaused(Boolean startPaused) {
		this.startPaused = startPaused;
	}

	public Duration getInitialTimeout() {
		return initialTimeout;
	}

	public void setInitialTimeout(Duration initialTimeout) {
		this.initialTimeout = initialTimeout;
	}

	public Duration getFinalTimeout() {
		return finalTimeout;
	}

	public void setFinalTimeout(Duration finalTimeout) {
		this.finalTimeout = finalTimeout;
	}

	public Boolean getStopBeep() {
		return stopBeep;
	}

	public void setStopBeep(Boolean stopBeep) {
		this.stopBeep = stopBeep;
	}

	public Boolean getDuplex() {
		return duplex;
	}

	public void setDuplex(Boolean duplex) {
		this.duplex = duplex;
	}

	@Override
	public String toString() {

		return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
				.append("callId", getCallId())
				.append("verbId", getVerbId())
				.append("to", getTo())
				.append("final-timeout", getFinalTimeout())
				.append("format", getFormat())
				.append("initial-timeout", getInitialTimeout())
				.append("max-duration", getMaxDuration())
				.append("start-beep", getStartBeep())
				.append("start-pause-mode", getStartPaused())
				.append("duplex", getDuplex())
				.toString();

	}
}
