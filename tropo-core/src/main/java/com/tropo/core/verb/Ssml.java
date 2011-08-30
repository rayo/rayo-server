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
        this.ssml = cleanupSpeakTag(ssml);
    }

    private String cleanupSpeakTag(String ssml) {

    	// Moho will wrap text with speak tags. 
    	// The client app may have sent speak tags, so we get rid of it
    	if (ssml.startsWith("<speak")) {
    		ssml = ssml.substring(ssml.indexOf('>') +1);
    		ssml = ssml.replaceAll("</speak>","");
    	}
    	return ssml;
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
        return URI.create("data:" + Networks.urlEncode("application/ssml+xml,<speak>" + getText() + "</speak>"));
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
