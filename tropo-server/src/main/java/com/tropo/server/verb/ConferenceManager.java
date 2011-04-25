package com.tropo.server.verb;

import com.voxeo.moho.ApplicationContext;

public interface ConferenceManager {

  public void conferenceRoomClosed(String name);

  public ConferenceRoom getConferenceRoom(String name, boolean create, ApplicationContext applicationContext);
}
