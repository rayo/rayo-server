package com.tropo.core.verb;

import java.net.URI;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.hibernate.validator.constraints.NotEmpty;

import com.tropo.core.validation.Messages;
import com.voxeo.utils.Networks;

public class Ssml {

	@NotEmpty(message=Messages.MISSING_SSML)
    private String ssml;

    private String voice;

    public Ssml(String ssml) {
        this.ssml = ssml;
    }

	public String getText() {
        return ssml;
    }

    public void setText(String text) {
        this.ssml = text;
    }

    public void setVoice(String voice) {
        this.voice = voice;
    }

    public String getVoice() {
        return voice;
    }    

    public URI toUri() {
    	String uriText = ssml.trim();
    	if (ssml.startsWith("<speak")) {
    		return URI.create("data:" + Networks.urlEncode("application/ssml+xml," + uriText));    		
    	} else {
    		return URI.create("data:" + Networks.urlEncode("application/ssml+xml,<speak>" + uriText + "</speak>"));
    	}
    }

    @Override
    public String toString() {

    	return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)    		
			.append("ssml",ssml)
			.append("voice",getVoice())
    		.append("uri",toUri())
    		.toString();
    }

}
