package com.tropo.core.cdr;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Cdr implements Serializable {

	private static final long serialVersionUID = -1948067531358803733L;
	
	private String callId;
	private List<String> transcript = new ArrayList<String>();
	
	public String getCallId() {
		return callId;
	}
	public void setCallId(String callId) {
		this.callId = callId;
	}
	
	public void add(String element) {
		
		transcript.add(element);
	}
	
	public List<String> getTranscript() {
		
		return transcript;
	}
}
