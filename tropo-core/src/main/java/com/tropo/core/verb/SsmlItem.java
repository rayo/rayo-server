package com.tropo.core.verb;

import java.net.URI;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.hibernate.validator.constraints.NotEmpty;

import com.tropo.core.validation.Messages;
import com.voxeo.utils.Networks;

public class SsmlItem {

	@NotEmpty(message=Messages.MISSING_SSML)
    private String ssml;

    public SsmlItem(String ssml) {
        this.ssml = ssml;
    }

    public String getText() {
        return ssml;
    }

    public void setText(String text) {
        this.ssml = text;
    }

    public URI toUri() {
        return URI.create("data:" + Networks.urlEncode("application/ssml+xml,<speak>" + getText() + "</speak>"));
    }

    @Override
    public String toString() {

    	return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)    		
			.append("ssml",ssml)
    		.append("uri",toUri())
    		.toString();
    }    
}
