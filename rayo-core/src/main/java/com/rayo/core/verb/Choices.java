package com.rayo.core.verb;

import java.net.URI;

import javax.validation.constraints.AssertTrue;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import com.rayo.core.validation.Messages;

public class Choices {

    private URI uri;
    private String content;
    private String contentType;
    
    public static final String VOXEO_GRAMMAR = "application/grammar+voxeo";
    public static final String GRXML_GRAMMAR = "application/grammar+grxml";

    public Choices() {}
    
    public Choices(URI uri) {
        this.uri = uri;
    }

    public Choices(String contentType, String content) {
        this.contentType = contentType;
        this.content = content;
    }

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String text) {
        this.content = text;
    }

    @AssertTrue(message=Messages.MISSING_CHOICES_CONTENT_TYPE)
    public boolean isContentsTypeSpecifiedWithInlineContents() {
        return (content == null) || (content != null && contentType != null);
    }

    @AssertTrue(message=Messages.MISSING_CHOICES_CONTENT_OR_URL)
    public boolean isContentsOrUrlSpecified() {
        return (content != null  && uri==null) || 
               (uri != null && content == null);
    }

    @Override
    public String toString() {

    	return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)    		
    		.append("uri",uri)
    		.append("contentType",contentType)
    		.append("content",content)
    		.toString();
    }    
}
