package com.rayo.core;

import java.util.Collection;

public interface MixerEvent {

    public String getMixerId();

    public Collection<String> getParticipantIds();
}
