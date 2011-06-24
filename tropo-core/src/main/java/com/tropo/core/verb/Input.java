package com.tropo.core.verb;

import java.util.List;

import javax.validation.Valid;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.hibernate.validator.constraints.NotEmpty;

import com.tropo.core.validation.Messages;
import com.tropo.core.validation.ValidRecognizer;

public class Input extends BaseVerb {

	@Valid
	@NotEmpty(message = Messages.MISSING_CHOICES)
	private List<Choices> grammars;

	private Integer initialTimeout;
	private Integer interSigTimeout;
	private Float confidence = 0.3f;
	private Float sensitivity;
	private Integer maxTimeout;
	private Boolean buffering;

	@ValidRecognizer
	private String recognizer;

	private Character terminator;
	private InputMode inputMode = InputMode.ANY;
	private Boolean dtmfHotword;
	private Boolean dtmfTypeAhead;
	private Boolean supervised;

	public Integer getInitialTimeout() {
		return initialTimeout;
	}

	public void setInitialTimeout(Integer initialTimeout) {
		this.initialTimeout = initialTimeout;
	}

	public Integer getInterSigTimeout() {
		return interSigTimeout;
	}

	public void setInterSigTimeout(Integer interSigTimeout) {

		this.interSigTimeout = interSigTimeout;
	}

	public Float getConfidence() {
		return confidence;
	}

	public void setConfidence(Float confidence) {
		this.confidence = confidence;
	}

	public Float getSensitivity() {
		return sensitivity;
	}

	public void setSensitivity(Float sensitivity) {
		this.sensitivity = sensitivity;
	}

	public Integer getMaxTimeout() {
		return maxTimeout;
	}

	public void setMaxTimeout(Integer maxTimeout) {
		this.maxTimeout = maxTimeout;
	}

	public Boolean getBuffering() {
		return buffering;
	}

	public void setBuffering(Boolean buffering) {
		this.buffering = buffering;
	}

	public String getRecognizer() {
		return recognizer;
	}

	public void setRecognizer(String recognizer) {
		this.recognizer = recognizer;
	}

	public Character getTerminator() {
		return terminator;
	}

	public void setTerminator(Character terminator) {
		this.terminator = terminator;
	}

	public InputMode getInputMode() {
		return inputMode;
	}

	public void setInputMode(InputMode inputMode) {
		this.inputMode = inputMode;
	}

	public Boolean getDtmfHotword() {
		return dtmfHotword;
	}

	public void setDtmfHotword(Boolean dtmfHotword) {
		this.dtmfHotword = dtmfHotword;
	}

	public Boolean getDtmfTypeAhead() {
		return dtmfTypeAhead;
	}

	public void setDtmfTypeAhead(Boolean dtmfTypeAhead) {
		this.dtmfTypeAhead = dtmfTypeAhead;
	}

	public Boolean getSupervised() {
		return supervised;
	}

	public void setSupervised(Boolean supervised) {
		this.supervised = supervised;
	}

	public List<Choices> getGrammars() {
		return grammars;
	}

	public void setGrammars(List<Choices> grammars) {
		this.grammars = grammars;
	}

	@Override
	public String toString() {

		return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
				.append("callId", getCallId()).append("verbId", getVerbId())
				.append("buffering", getBuffering())
				.append("confidence", getConfidence())
				.append("dtmfHotword", getDtmfHotword())
				.append("dtmfTypeAhead", getDtmfTypeAhead())
				.append("initialTimeout", getInitialTimeout())
				.append("inputMode", getInputMode())
				.append("integerSigTimeout", getInterSigTimeout())
				.append("language", getRecognizer())
				.append("maxTimeout", getMaxTimeout())
				.append("sensitivity", getSensitivity())
				.append("supervised", getSupervised())
				.append("terminator", getTerminator())
				.append("grammars", grammars).toString();
	}
}
