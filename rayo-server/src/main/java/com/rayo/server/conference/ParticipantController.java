package com.rayo.server.conference;

public interface ParticipantController {

    public boolean isHold();

    public void setHold(boolean hold);

    public boolean isMute();

    public void setMute(boolean mute);

    public boolean isTonePassthrough();

    public void setTonePassthrough(boolean tonePassthrough);

    public Character getTerminator();

    public void setTerminator(Character terminator);

    public void kick(String reason);
    
    public boolean isModerator();

}