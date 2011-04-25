package com.tropo.server.verb;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.media.mscontrol.Parameters;
import javax.media.mscontrol.join.Joinable.Direction;

import com.voxeo.moho.ApplicationContext;
import com.voxeo.moho.Call;
import com.voxeo.moho.Participant.JoinType;
import com.voxeo.moho.conference.Conference;
import com.voxeo.moho.media.Prompt;
import com.voxeo.moho.media.input.DigitInputCommand;
import com.voxeo.mscontrol.VoxeoParameter;

public class ConferenceRoom {

    public static String defaultBackGround;

    private enum State {
        Initialized, Started, Ended
    }

    private String _name;

    // moho conference
    private Conference _conference;

    // conferenceManager
    private ConferenceManager _conferenceManager;

    // Moho ApplicationContext, used to create Moho conference
    private ApplicationContext _applicationContext;

    private State _state = State.Initialized;

    private Map<Call, EnterReq> _conferenceReqs = new HashMap<Call, EnterReq>();

    public ConferenceRoom(String name, ConferenceManager conferenceManager, ApplicationContext appContext) {
        super();
        this._name = name;
        this._conferenceManager = conferenceManager;
        this._applicationContext = appContext;
    }

    public synchronized void close() {
        if (_state == State.Ended) {
            return;
        }

        if (_state == State.Started) {
            Set<Call> calls = _conferenceReqs.keySet();
            for (Call call : calls) {
                call.unjoin(_conference);
            }
            _applicationContext.getConferenceManager().removeConference(_name);
        }
        else if (_state == State.Initialized) {
            Set<Call> Calls = _conferenceReqs.keySet();
            for (Call Call : Calls) {
                EnterReq enterReq = _conferenceReqs.get(Call);
                if (enterReq != null && enterReq.playMusic != null) {
                    enterReq.playMusic.getOutput().stop();
                }
            }
        }

        _conferenceReqs.clear();
        _conferenceManager.conferenceRoomClosed(_name);
        _state = State.Ended;
    }

    private void start(Call call, com.tropo.core.verb.Conference model) {
        if (_conference == null) {
            final Parameters params = call.getMediaObject().createParameters();
            params.put(VoxeoParameter.VOXEO_JOIN_ENTER_TONE, model.isBeep());
            params.put(VoxeoParameter.VOXEO_JOIN_EXIT_TONE, model.isBeep());
            _conference = _applicationContext.getConferenceManager().createConference(_name, 20, params);
        }

        join(call, model, _conference);

        call.getMediaService().input(new DigitInputCommand(model.getTerminator()));

        Set<Call> activeCalls = _conferenceReqs.keySet();
        for (Call activeCall : activeCalls) {
            EnterReq enterReq = _conferenceReqs.get(activeCall);

            if (enterReq.playMusic != null) {
                enterReq.playMusic.getOutput().stop();
            }

            join(enterReq.call, enterReq.model, _conference);

            // re-enable the hangupOnStar, because the input is invalidated after join
            // to mixer at 309 layer.
            activeCall.getMediaService().input(new DigitInputCommand(enterReq.model.getTerminator()));
        }

        _state = State.Started;
    }

    public synchronized void leave(Call call) {
        EnterReq enterReq = _conferenceReqs.remove(call);
        if (enterReq != null) {
            try {
                if (_state == State.Started) {
                    call.unjoin(_conference);
                }
                else if (_state == State.Initialized) {
                    if (enterReq.playMusic != null) {
                        enterReq.playMusic.getOutput().stop();
                    }
                }
            }
            finally {
                if (_conferenceReqs.size() < 1) {
                    this.close();
                }
            }
        }
    }

    public synchronized void enter(Call call, com.tropo.core.verb.Conference model) {
        if (_conferenceReqs.containsKey(call)) {
            if (_state == State.Started) {
                // rejoin
                join(call, model, _conference);
            }
            return;
        }

        EnterReq enterReq = new EnterReq(call, model);

        if (_state == State.Started) {
            join(call, model, _conference);

            call.getMediaService().input(new DigitInputCommand(model.getTerminator()));
        }
        else if (_state == State.Initialized) {
            // if need start the conference
            if (_conferenceReqs.size() > 0) {
                start(call, model);
            }
        }
        else {
            throw new IllegalStateException("ConferenceRoom ended.");
        }

        _conferenceReqs.put(call, enterReq);
    }

    private void join(Call call, com.tropo.core.verb.Conference model, Conference conference) {
        Properties props = null;

        if (!model.isTonePassthrough()) {
            props = new Properties();
            props.setProperty("playTones", "false");
        }

        if (model.isMute()) {
            conference.join(call, JoinType.BRIDGE, Direction.RECV, props);
        }
        else {
            conference.join(call, JoinType.BRIDGE, Direction.DUPLEX, props);
        }
    }

    public Conference getConference() {
        return _conference;
    }

    public String getName() {
        return _name;
    }

    private class EnterReq {

        public Prompt playMusic;

        private com.tropo.core.verb.Conference model;

        private Call call;

        public EnterReq(Call call, com.tropo.core.verb.Conference model) {
            this.model = model;
            this.call = call;
        }
    }
}
