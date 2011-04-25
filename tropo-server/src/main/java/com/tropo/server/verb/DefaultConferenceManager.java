package com.tropo.server.verb;

import java.util.HashMap;
import java.util.Map;

import com.voxeo.moho.ApplicationContext;

public class DefaultConferenceManager implements ConferenceManager {

    private Map<String, ConferenceRoom> conferenceRooms;

    public synchronized ConferenceRoom getConferenceRoom(String name, boolean create, ApplicationContext applicationContext) {
        if (conferenceRooms == null) {
            conferenceRooms = new HashMap<String, ConferenceRoom>();
        }
        ConferenceRoom room = conferenceRooms.get(name);
        if (room == null && create) {
            room = new ConferenceRoom(name, this, applicationContext);
            conferenceRooms.put(name, room);
        }

        return room;
    }

    public void conferenceRoomClosed(String name) {
        conferenceRooms.remove(name);
    }

}
