package com.rayo.core.cdr;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.time.DateFormatUtils;

public class Cdr implements Serializable {

	private static final long serialVersionUID = -1948067531358803733L;
	
	private String callId;
	private long startTime;
	private long endTime;
	private String from;
	private String to;
	private String state;
	
	private List<String> transcript = new ArrayList<String>();
	
	public String getCallId() {
		return callId;
	}
	public void setCallId(String callId) {
		this.callId = callId;
	}
	
	public void add(String element) {
		
		transcript.add(timestamp(element));
	}
	
	public List<String> getTranscript() {
		
		return transcript;
	}
	public long getStartTime() {
		return startTime;
	}
	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}
	public long getEndTime() {
		return endTime;
	}
	public void setEndTime(long endTime) {
		this.endTime = endTime;
	}
	public String getFrom() {
		return from;
	}
	public void setFrom(String from) {
		this.from = from;
	}
	public String getTo() {
		return to;
	}
	public void setTo(String to) {
		this.to = to;
	}
	public String getState() {
		return state;
	}
	public void setState(String state) {
		this.state = state;
	}
	public void setTranscript(List<String> transcript) {
		this.transcript = transcript;
	}
	
	public String toString() {
		
		StringBuilder builder = new StringBuilder(String.format(
				"<cdr callId=\"%s\" from=\"%s\" to=\"%s\" start=\"%s\" end=\"%s\" state=\"%s\" xmlns=\"http://tropo.com/schema/cdr\"  xmlns:cdr=\"http://tropo.com/schema/cdr\">", 
				getCallId(), getFrom(), getTo(), 
				formatDate(new Date(getStartTime())), formatDate(new Date(getEndTime())), getState()
		));
		for (String element: getTranscript()) {
			builder.append(element);
		}
		builder.append("</cdr>\n");	
		
		return builder.toString();
	}
	
	private String timestamp(String element) {
		
		int i = element.indexOf('>');
		if (i == element.length()-1) {
			i--;
		}
		StringBuffer builder = new StringBuffer(element.substring(0,i));
		if (! (builder.charAt(builder.length()-1) == ' ')) {
			builder.append(' ');
		}
		builder.append(String.format("cdr:ts=\"%s\"", formatDate(new Date())));
		builder.append(element.substring(i));
		
		return builder.toString();
	}
	
	private String formatDate(Date date) {
		
		return DateFormatUtils.format(new Date(), "yyyy-dd-MM hh:mm:ss.SZ");
	}
}
