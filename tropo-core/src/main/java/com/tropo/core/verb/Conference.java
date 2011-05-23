package com.tropo.core.verb;

import java.util.Map;

import javax.validation.constraints.NotNull;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.joda.time.Duration;

import com.tropo.core.validation.Messages;

public class Conference extends BaseVerb {

    @NotNull(message = Messages.MISSING_ROOM_NAME)
    private String roomName;

    private boolean mute = false;
    private Character terminator = '#';
    private boolean tonePassthrough = true;
    private Duration maxTime; // Unlimited
    private boolean beep = true;
    private boolean moderator = true;
    private SsmlItem holdMusic;
    private SsmlItem announcement;
    private Map<String, String> metaData;

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

    public void setHoldMusic(SsmlItem holdMusic) {
        this.holdMusic = holdMusic;
    }

    public SsmlItem getHoldMusic() {
        return holdMusic;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
            .append("callId", getCallId())
            .append("verbId", getVerbId())
            .append("roomName", roomName)
            .append("mute", mute)
            .append("beep", beep)
            .append("tonePassthrough", tonePassthrough)
            .append("terminator", terminator)
            .toString();
    }

    public void setMaxTime(Duration maxTime) {
        this.maxTime = maxTime;
    }

    public Duration getMaxTime() {
        return maxTime;
    }

    public void setModerator(boolean openOnEnter) {
        this.moderator = openOnEnter;
    }

    public boolean isModerator() {
        return moderator;
    }

    public void setMetaData(Map<String, String> metaData) {
        this.metaData = metaData;
    }

    public Map<String, String> getMetaData() {
        return metaData;
    }

    public void setAnnouncement(SsmlItem announcement) {
        this.announcement = announcement;
    }

    public SsmlItem getAnnouncement() {
        return announcement;
    }

}
