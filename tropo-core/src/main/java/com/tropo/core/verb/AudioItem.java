package com.tropo.core.verb;

import java.net.URI;

import javax.validation.constraints.NotNull;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import com.tropo.core.validation.Messages;

public class AudioItem implements PromptItem {

	@NotNull(message=Messages.MISSING_URI)
    private URI uri;

    public AudioItem() {}

    public AudioItem(URI uri) {
        this.uri = uri;
    }

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    @Override
    public URI toUri() {
        return uri;
    }

    @Override
    public String toString() {

    	return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)    		
    		.append("uri",uri)
    		.toString();
    }     
}
