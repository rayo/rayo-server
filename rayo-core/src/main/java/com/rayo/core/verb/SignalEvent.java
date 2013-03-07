package com.rayo.core.verb;


public class SignalEvent extends AbstractVerbEvent {

	private String source;
	private Long duration;
	private String type;
	private String tone;
   
    public SignalEvent() {
        
    	super();
    }

    public SignalEvent(Input input, String type, Long duration, String source) {
        
    	super(input);
    	this.type = type;
    	this.source = source;
    	this.duration = duration;
    }

    public SignalEvent(Input source, String type, String tone) {
        
    	super(source);
    	this.type = type;
    	this.tone = tone;
    }
    
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Long getDuration() {
		return duration;
	}

	public void setDuration(Long duration) {
		this.duration = duration;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String mode) {
		this.source = mode;
	}

	public String getTone() {
		return tone;
	}

	public void setTone(String tone) {
		this.tone = tone;
	}
}
