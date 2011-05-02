package com.tropo.server.verb;

import com.tropo.core.verb.Conference;
import com.tropo.core.verb.ConferenceCompleteEvent;
import com.tropo.core.verb.MuteCommand;
import com.tropo.core.verb.VerbCommand;
import com.tropo.core.verb.ConferenceCompleteEvent.Reason;
import com.tropo.core.verb.KickCommand;
import com.tropo.core.verb.UnmuteCommand;
import com.voxeo.moho.State;
import com.voxeo.moho.event.DisconnectEvent;
import com.voxeo.moho.event.InputCompleteEvent;
import com.voxeo.moho.event.JoinCompleteEvent;
import com.voxeo.moho.event.JoinCompleteEvent.Cause;

public class LocalConferenceHandler extends AbstractLocalVerbHandler<Conference> {

    private ConferenceManager conferenceManager;

    private boolean joined;
    private ConferenceRoom conferenceRoom;

    @Override
    public void start() {
        conferenceRoom = conferenceManager.getConferenceRoom(model.getRoomName(), true, call.getApplicationContext());
        conferenceRoom.enter(call, model);
        joined = true;
    }

    // Commands
    // ================================================================================

    public void stop() {
        conferenceRoom.leave(call);
        conferenceRoom = null;
    }

    @Override
    public void onCommand(VerbCommand command) {
        if (command instanceof KickCommand) {
            kick();
        } else if (command instanceof MuteCommand) {
            mute();
        } else if (command instanceof UnmuteCommand) {
            unmute();
        }
    }

    public void kick() {
        stop();
        complete(new ConferenceCompleteEvent(model, Reason.KICK));
    }

    public void mute() {
        if (model.isMute() == false) {
            model.setMute(true);
            conferenceRoom.enter(call, model);
        }
    }

    public void unmute() {
        if (model.isMute() == true) {
            model.setMute(false);
            conferenceRoom.enter(call, model);
        }
    }

    // Moho Events
    // ================================================================================

    @State
    public void onJoinComplete(JoinCompleteEvent event) {
        if (event.source == call) {
            if (event.getCause() != Cause.JOINED) {
                complete(Reason.HANGUP);
            }
            joined = false;
        }
    }

    @State
    public void onDisconnect(DisconnectEvent event) {
        if (event.source == call && !joined) {
            complete(Reason.HANGUP);
        }
    }

    @State
    public void onTermChar(InputCompleteEvent event) {
        if (event.hasMatch() && event.source == call) {
            complete(Reason.LEAVE);
        }
    }

    private void complete(Reason reason) {
        ConferenceCompleteEvent completeEvent = new ConferenceCompleteEvent(model, reason);
        try {
            stop();
        } finally {
            complete(completeEvent);
        }
    }

	public ConferenceManager getConferenceManager() {
		return conferenceManager;
	}

	public void setConferenceManager(ConferenceManager conferenceManager) {
		this.conferenceManager = conferenceManager;
	}
}
