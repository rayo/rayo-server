package com.tropo.core.verb;

import javax.validation.constraints.NotNull;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import com.tropo.core.validation.Messages;

public class Conference extends BaseVerb {

	@NotNull(message=Messages.MISSING_ROOM_NAME)
    private String roomName;
    private boolean mute;
    private boolean beep;
    private boolean tonePassthrough;
    private Character terminator;

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public boolean isMute() {
        return mute;
    }

    public void setMute(boolean mute) {
        this.mute = mute;
    }

    public boolean isTonePassthrough() {
        return tonePassthrough;
    }

    public void setTonePassthrough(boolean tonePassthrough) {
        this.tonePassthrough = tonePassthrough;
    }

    public Character getTerminator() {
        return terminator;
    }

    public void setTerminator(Character terminator) {
        this.terminator = terminator;
    }

    public boolean isBeep() {
        return beep;
    }

    public void setBeep(boolean beep) {
        this.beep = beep;
    }

    @Override
    public String toString() {

    	return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)    		
    		.append("callId", getCallId())
    		.append("verbId", getVerbId())
    		.append("roomName",roomName)
    		.append("mute",mute)
    		.append("beep",beep)
    		.append("tonePassthrough",tonePassthrough)
    		.append("terminator",terminator)
    		.toString();
    }
}
