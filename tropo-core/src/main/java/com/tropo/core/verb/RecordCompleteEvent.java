package com.tropo.core.verb;

import java.net.URI;

public class RecordCompleteEvent extends VerbCompleteEvent {

    public enum Reason implements VerbCompleteReason {
        SUCCESS
    }

    private URI uri;
    
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
}
