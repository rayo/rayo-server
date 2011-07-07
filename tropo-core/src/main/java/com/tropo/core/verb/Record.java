package com.tropo.core.verb;

import java.net.URI;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import com.tropo.core.validation.ValidCodec;
import com.tropo.core.validation.ValidFileFormat;

public class Record extends BaseVerb {

	private String voice;
	
	private Boolean bargein = Boolean.TRUE;
	
	private URI to;
	
	private Boolean append;
	
	private Integer sampleRate;
		
	@ValidCodec
	private String codec;

	private String codecParameters;

	@ValidFileFormat
	private String format;
	
	private Integer maxDuration;
	
	private Integer minDuration;
	
	private Boolean dtmfTruncate;
	
	private Boolean silenceTerminate;
	
	private Boolean startBeep;
	
	private Boolean startPauseMode;
	
	private Integer initialTimeout;
	
	private Integer finalTimeout;
	
	public URI getTo() {
		return to;
	}

	public void setTo(URI to) {
		this.to = to;
	}

	
	
	public Boolean getAppend() {
		return append;
	}

	public void setAppend(Boolean append) {
		this.append = append;
	}

	public Integer getSampleRate() {
		return sampleRate;
	}

	public void setSampleRate(Integer sampleRate) {
		this.sampleRate = sampleRate;
	}

	public String getCodec() {
		return codec;
	}

	public void setCodec(String codec) {
		this.codec = codec;
	}

	public String getCodecParameters() {
		return codecParameters;
	}

	public void setCodecParameters(String codecParameters) {
		this.codecParameters = codecParameters;
	}

	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	public Integer getMaxDuration() {
		return maxDuration;
	}

	public void setMaxDuration(Integer maxDuration) {
		this.maxDuration = maxDuration;
	}

	public Integer getMinDuration() {
		return minDuration;
	}

	public void setMinDuration(Integer minDuration) {
		this.minDuration = minDuration;
	}

	public Boolean getDtmfTruncate() {
		return dtmfTruncate;
	}

	public void setDtmfTruncate(Boolean dtmfTruncate) {
		this.dtmfTruncate = dtmfTruncate;
	}

	public Boolean getSilenceTerminate() {
		return silenceTerminate;
	}

	public void setSilenceTerminate(Boolean silenceTerminate) {
		this.silenceTerminate = silenceTerminate;
	}

	public Boolean getStartBeep() {
		return startBeep;
	}

	public void setStartBeep(Boolean startBeep) {
		this.startBeep = startBeep;
	}

	public Boolean getStartPauseMode() {
		return startPauseMode;
	}

	public void setStartPauseMode(Boolean startPauseMode) {
		this.startPauseMode = startPauseMode;
	}

	public Integer getInitialTimeout() {
		return initialTimeout;
	}

	public void setInitialTimeout(Integer initialTimeout) {
		this.initialTimeout = initialTimeout;
	}

	public Integer getFinalTimeout() {
		return finalTimeout;
	}

	public void setFinalTimeout(Integer finalTimeout) {
		this.finalTimeout = finalTimeout;
	}
	
	public String getVoice() {
		return voice;
	}

	public void setVoice(String voice) {
		this.voice = voice;
	}
	
	public Boolean isBargein() {
		return bargein;
	}

	public void setBargein(Boolean bargein) {
		this.bargein = bargein;
	}

	@Override
	public String toString() {

		return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
				.append("callId", getCallId())
				.append("verbId", getVerbId())
				.append("to", getTo())
				.append("append", getAppend())
				.append("codec", getCodec())
				.append("codec-params", getCodecParameters())
				.append("dtmf-truncate", getDtmfTruncate())
				.append("final-timeout", getFinalTimeout())
				.append("format", getFormat())
				.append("initial-timeout", getInitialTimeout())
				.append("max-duration", getMaxDuration())
				.append("min-duration", getMinDuration())
				.append("sample-rate", getSampleRate())
				.append("silence-terminate", getSilenceTerminate())
				.append("start-beep", getStartBeep())
				.append("start-pause-mode", getStartPauseMode())				
				.toString();

	}
}
