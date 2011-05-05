package com.tropo.server.conference;

import com.voxeo.moho.Call;

public class Participant {

    private Call call;
    private boolean muted;
    private boolean tonePassthrough;
    private Character terminator;
    private ConferenceRoom room;

    public Participant(ConferenceRoom room, Call call, boolean muted, boolean tonePassthrough, Character terminator) {
        this.call = call;
        this.muted = muted;
        this.room = room;
        this.tonePassthrough = tonePassthrough;
        this.terminator = terminator;
    }

    public synchronized boolean isMuted() {
        return muted;
    }

    public synchronized void setMuted(boolean muted) {
        this.muted = muted;
        rejoin();
    }
    
    public synchronized boolean isTonePassthrough() {
        return tonePassthrough;
    }

    public synchronized void setTonePassthrough(boolean tonePassthrough) {
        this.tonePassthrough = tonePassthrough;
        rejoin();
    }

    public synchronized Character getTerminator() {
        return terminator;
    }

    public synchronized void setTerminator(Character terminator) {
        this.terminator = terminator;
        rejoin();
    }

    public Call getCall() {
        return call;
    }

    public ConferenceRoom getRoom() {
        return room;
    }

    private void rejoin() {
        room.join(call, muted, muted, terminator);
    }

}
