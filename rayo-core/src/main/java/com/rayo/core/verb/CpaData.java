package com.rayo.core.verb;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

public class CpaData {

	private Long maxTime;
	private Long finalSilence;
	private Long minSpeechDuration;
	private Long minVolume;
	private Boolean terminate = Boolean.FALSE;
	private String[] signals = new String[]{};
	
	public CpaData(String... signals) {
		
		this.signals = signals;
	}

	public String[] getSignals() {
		return signals;
	}


	public void setSignals(String[] signals) {
		this.signals = signals;
	}


	public Long getMaxTime() {
		return maxTime;
	}

	public void setMaxTime(Long maxTime) {
		this.maxTime = maxTime;
	}

	public Long getFinalSilence() {
		return finalSilence;
	}

	public void setFinalSilence(Long finalSilence) {
		this.finalSilence = finalSilence;
	}

	public Long getMinSpeechDuration() {
		return minSpeechDuration;
	}

	public void setMinSpeechDuration(Long minSpeechDuration) {
		this.minSpeechDuration = minSpeechDuration;
	}

	public Long getMinVolume() {
		return minVolume;
	}

	public void setMinVolume(Long minVolume) {
		this.minVolume = minVolume;
	}

	public Boolean isTerminate() {
		return terminate;
	}

	public void setTerminate(Boolean terminate) {
		this.terminate = terminate;
	}

	@Override
	public String toString() {

		return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
				.append("maxTime", getMaxTime())
				.append("finalSilence", getFinalSilence())
				.append("minSpeechDuration", getMinSpeechDuration())
				.append("minVolume", getMinVolume())
				.append("terminate", isTerminate())
				.append("signals", getSignals()).toString();
	}
}
