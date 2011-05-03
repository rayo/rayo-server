package com.tropo.core.verb;

import java.net.URI;

import org.hibernate.validator.constraints.NotEmpty;

import com.tropo.core.validation.Messages;
import com.voxeo.utils.Networks;

public class SsmlItem implements PromptItem {

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

    @Override
    public URI toUri() {
        return URI.create("data:" + Networks.urlEncode("application/ssml+xml," + getText()));
    }

}
