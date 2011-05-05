package com.tropo.server.conference;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.media.mscontrol.Parameters;
import javax.media.mscontrol.join.Joinable.Direction;

import com.tropo.core.verb.PromptItems;
import com.tropo.server.MohoUtil;
import com.voxeo.moho.ApplicationContext;
import com.voxeo.moho.Call;
import com.voxeo.moho.Participant.JoinType;
import com.voxeo.moho.conference.Conference;
import com.voxeo.moho.media.Output;
import com.voxeo.moho.media.input.DigitInputCommand;
import com.voxeo.moho.media.output.OutputCommand;
import com.voxeo.mscontrol.VoxeoParameter;
import com.voxeo.utils.Pair;

public class ConferenceRoom {

    private enum State {
        INITIAL, STARTED, ENDED
    }

    // Config
    // ================================================================================
    
    private String name;
    private Conference conference;
    private ConferenceManager conferenceManager;
    private ApplicationContext applicationContext;
    
    private Output holdMusic;

    // State
    // ================================================================================
    
    private State state = State.INITIAL;
    private Map<Call, Pair<Participant, com.tropo.core.verb.Conference>> participants = new HashMap<Call, Pair<Participant, com.tropo.core.verb.Conference>>();

    public ConferenceRoom(String name, ConferenceManager conferenceManager, ApplicationContext appContext) {
        this.name = name;
        this.conferenceManager = conferenceManager;
        this.applicationContext = appContext;
    }

    // Commands
    // ================================================================================
    
    public synchronized Participant enter(Call call, com.tropo.core.verb.Conference model) {

        Participant participant = new Participant(this, call, model.isMute(), model.isTonePassthrough(), model.getTerminator());

        switch(state) {
        
            case INITIAL:
                
                // Play hold music if rookm is empty
                if (participants.isEmpty()) {
                    
                    PromptItems holdMusicPrompt = model.getHoldMusic();
                    if(holdMusicPrompt != null) {
                        OutputCommand outputCommand = MohoUtil.output(holdMusicPrompt);
                        outputCommand.setRepeatTimes(Integer.MAX_VALUE);
                        holdMusic = call.getMediaService().output(outputCommand);
                    }
                    
                }
                else {
                    
                    // Create the Moho Conference
                    final Parameters params = call.getMediaObject().createParameters();
                    params.put(VoxeoParameter.VOXEO_JOIN_ENTER_TONE, model.isBeep());
                    params.put(VoxeoParameter.VOXEO_JOIN_EXIT_TONE, model.isBeep());
                    conference = applicationContext.getConferenceManager().createConference(name, Integer.MAX_VALUE, params);
                    
                    join(call, model.isMute(), model.isTonePassthrough(), model.getTerminator());

                    if(holdMusic != null) {
                        holdMusic.stop();
                    }

                    // Join the waiting calls
                    for (Call activeCall : participants.keySet()) {
                        
                        Pair<Participant, com.tropo.core.verb.Conference> pair = participants.get(activeCall);
                        com.tropo.core.verb.Conference participantModel = pair.second;
                        
                        join(activeCall, 
                            participantModel.isMute(), 
                            participantModel.isTonePassthrough(), 
                            participantModel.getTerminator()
                        );
                    }

                    state = State.STARTED;                    
                    
                }
                break;
                
            case STARTED:
                join(call, model.isMute(), model.isTonePassthrough(), model.getTerminator());
                break;
            default:
                throw new IllegalStateException("ConferenceRoom ended.");
        }

        participants.put(call, new Pair<Participant, com.tropo.core.verb.Conference>(participant, model));
        
        return participant;
    }

    public synchronized void leave(Call call) {
        if (participants.remove(call) != null) {
            try {
                if (state == State.STARTED) {
                    call.unjoin(conference);
                }
                else if (state == State.INITIAL) {
                    if (holdMusic != null) {
                        holdMusic.stop();
                    }
                }
            }
            finally {
                if (participants.isEmpty()) {
                    this.close();
                }
            }
        }
    }    
    
    public synchronized void close() {
        
        if (state == State.ENDED) {
            return;
        }

        if (state == State.STARTED) {
            try {
                for (Call call : participants.keySet()) {
                    call.unjoin(conference);
                }
            }
            finally {
                applicationContext.getConferenceManager().removeConference(name);
            }
        }
        else if (state == State.INITIAL) {
            if (holdMusic != null && !holdMusic.isDone()) {
                holdMusic.stop();
            }
        }

        participants.clear();
        conferenceManager.conferenceRoomClosed(name);
        state = State.ENDED;
    }

    protected void join(Call call, boolean muted, boolean tonePassthrough, Character terminator) {

        Properties props = new Properties();
        props.setProperty("playTones", Boolean.toString(tonePassthrough));

        if (muted) {
            conference.join(call, JoinType.BRIDGE, Direction.RECV, props);
        }
        else {
            conference.join(call, JoinType.BRIDGE, Direction.DUPLEX, props);
        }
        
        // Re-enable terminator because recognition stops after we join the mixer
        if(terminator != null) {
            DigitInputCommand inputCommand = new DigitInputCommand(terminator);
            inputCommand.setDtmfHotword(true);
            call.getMediaService().input(inputCommand);
        }
        
    }

    // Properties
    // ================================================================================

    public Conference getConference() {
        return conference;
    }

    public String getName() {
        return name;
    }
    
}
