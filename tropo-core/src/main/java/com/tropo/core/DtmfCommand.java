package com.tropo.core;

import javax.validation.constraints.NotNull;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.hibernate.validator.constraints.NotEmpty;

import com.tropo.core.validation.Messages;
import com.tropo.core.validation.ValidDtmf;

public class DtmfCommand implements ServerCommand {

	@NotNull(message = Messages.MISSING_DTMF_KEY)
	@NotEmpty(message = Messages.MISSING_DTMF_KEY)
	@ValidDtmf
	private String key;

	public DtmfCommand(String key) {
		
		this.key = key;
	}
	
	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
				.append("key", key).toString();
	}
}
