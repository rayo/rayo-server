package com.tropo.core.verb;

public class Conference extends BaseVerb {

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

}
