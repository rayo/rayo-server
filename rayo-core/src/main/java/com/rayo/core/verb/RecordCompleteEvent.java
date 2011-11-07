package com.rayo.core.verb;

import java.net.URI;

import org.joda.time.Duration;

public class RecordCompleteEvent extends VerbCompleteEvent {

    public enum Reason implements VerbCompleteReason {
        SUCCESS, INI_TIMEOUT, TIMEOUT
    }

    private URI uri;
    private Duration duration;
    private long size;
    
    public RecordCompleteEvent() {}
    
    public RecordCompleteEvent(Verb verb) {
        super(verb);
        if (verb instanceof Record) {
        	setUri((Record)verb);
        }
    }

    public RecordCompleteEvent(VerbCompleteReason reason) {
        super(reason);
    }

    public RecordCompleteEvent(Record verb, VerbCompleteReason reason) {
        super(verb, reason);
        setUri(verb);
    }

    public RecordCompleteEvent(Record verb, String errorText) {
        super(verb, errorText);
        setUri(verb);
    }

	private void setUri(Record verb) {
		
		this.uri = verb.getTo();
	}

	public URI getUri() {
		return uri;
	}

	public void setUri(URI uri) {
		this.uri = uri;
	}

	public Duration getDuration() {
		return duration;
	}

	public void setDuration(Duration duration) {
		this.duration = duration;
	}

	public long getSize() {
		return size;
	}

	public void setSize(long size) {
		this.size = size;
	}	
}
