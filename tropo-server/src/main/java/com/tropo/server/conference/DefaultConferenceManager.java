package com.tropo.server.conference;

import java.util.HashMap;
import java.util.Map;

import com.voxeo.moho.ApplicationContext;

public class DefaultConferenceManager implements ConferenceManager {

    private Map<String, ConferenceRoom> rooms = new HashMap<String, ConferenceRoom>();

    public synchronized ConferenceRoom getConferenceRoom(String name, boolean create, ApplicationContext applicationContext) {
        ConferenceRoom room = rooms.get(name);
        if (room == null && create) {
            room = new ConferenceRoom(name, this, applicationContext);
            rooms.put(name, room);
        }
        return room;
    }

    public void conferenceRoomClosed(String name) {
        rooms.remove(name);
    }

}
