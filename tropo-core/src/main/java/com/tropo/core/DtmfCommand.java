package com.tropo.core;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import com.tropo.core.validation.ValidDtmf;

public class DtmfCommand extends AbstractCallCommand {

	@ValidDtmf
	private String tones;

	public DtmfCommand(String tones) {

		this.tones = tones;
	}

	public String getTones() {
		return tones;
	}

	public void setTones(String tones) {
		this.tones = tones;
	}

	@Override
	public String toString() {
		
		return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
			.append("callId", getCallId())
			.append("tones", tones).toString();
	}
}
