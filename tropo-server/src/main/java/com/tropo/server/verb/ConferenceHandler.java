package com.tropo.server.verb;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.media.mscontrol.join.Joinable.Direction;

import com.tropo.core.verb.Conference;
import com.tropo.core.verb.ConferenceCompleteEvent;
import com.tropo.core.verb.ConferenceCompleteEvent.Reason;
import com.tropo.core.verb.KickCommand;
import com.tropo.core.verb.MuteCommand;
import com.tropo.core.verb.OffHoldEvent;
import com.tropo.core.verb.OnHoldEvent;
import com.tropo.core.verb.SsmlItem;
import com.tropo.core.verb.UnmuteCommand;
import com.tropo.core.verb.VerbCommand;
import com.tropo.server.MohoUtil;
import com.tropo.server.conference.ParticipantController;
import com.voxeo.moho.Participant;
import com.voxeo.moho.Participant.JoinType;
import com.voxeo.moho.State;
import com.voxeo.moho.conference.ConferenceController;
import com.voxeo.moho.event.InputCompleteEvent;
import com.voxeo.moho.media.Input;
import com.voxeo.moho.media.Output;
import com.voxeo.moho.media.input.DigitInputCommand;
import com.voxeo.moho.media.output.OutputCommand;
import com.voxeo.moho.media.output.OutputCommand.BehaviorIfBusy;

public class ConferenceHandler extends AbstractLocalVerbHandler<Conference> implements ParticipantController {

    public static final String PARTICIPANT_KEY = "com.tropo.conference.participant.";
    private static final String WAIT_LIST_KEY = "com.tropo.conference.waitList";

    private ConferenceController mohoConferenceController;

    private boolean hold;
    private boolean joined;
    private Output holdMusic;
    private Input hotwordListener;
    private com.voxeo.moho.conference.Conference mohoConference;

    // Represent the current state of the participant
    private boolean mute;
    private Character terminator;
    private boolean tonePassthrough;

    @Override
    public void start() {

        // Init local state from verb request
        this.mute = model.isMute();
        this.terminator = model.getTerminator();
        this.tonePassthrough = model.isTonePassthrough();

        // Create or get Moho Conference
        mohoConference = call.getApplicationContext().getConferenceManager().createConference(model.getRoomName(), Integer.MAX_VALUE, null, null);

        synchronized (mohoConference) {

            // If this is the first participant then we should configure the controller
            if (mohoConference.getController() == null) {
                mohoConference.setController(mohoConferenceController);
            }

            // Register Tropo Participant object as an attribute of the Mogo Call
            call.setAttribute(getMohoParticipantAttributeKey(), this);

            // Determine if the conference is 'open' by checking if there are any moderators
            Boolean open = isConferenceOpen();
            
            // If the conference was not open and we're a moderator then open it up
            if (!open && model.isModerator()) {
                // Join all waiting participant to the mixer
                for (com.voxeo.moho.Participant mixerParticipant : getWaitList()) {
                    ParticipantController controller = (ParticipantController) mixerParticipant.getAttribute(getMohoParticipantAttributeKey());
                    controller.setHold(false);
                }
                open = true;
            }

            // Join the conference if it's open or play music if it's not
            if (open) {
                setHold(false);
                playAnnouncement();
            }
            else {
                setHold(true);
            }
        }

    }

    public void stop(boolean hangup) {
        try {
            preStop(hangup);
            complete(new ConferenceCompleteEvent(model, hangup ? Reason.HANGUP : Reason.STOP));
        }
        catch (RuntimeException e) {
            complete(new ConferenceCompleteEvent(model, e.getMessage()));
            throw e;
        }
    }

    @Override
    public void onCommand(VerbCommand command) {
        if (command instanceof KickCommand) {
            kick(((KickCommand)command).getReason());
        }
        else if (command instanceof MuteCommand) {
            setMute(true);
        }
        else if (command instanceof UnmuteCommand) {
            setMute(false);
        }
    }

    // Commands
    // ================================================================================

    public synchronized boolean isHold() {
        return hold;
    }

    public synchronized void setHold(boolean hold) {

        synchronized (mohoConference) {
            if (hold) {
                stopMixing();
                startMusic();
                startHotwordListener();
                addCallToWaitList();
                fire(new OnHoldEvent());
            }
            else {
                stopMusic();
                startMixing();
                startHotwordListener();
                removeCallFromWaitList();
                fire(new OffHoldEvent());
            }
        }

        this.hold = hold;
    }

    private List<Participant> getWaitList() {
        synchronized (mohoConference) {
            List<Participant> waitList = mohoConference.getAttribute(WAIT_LIST_KEY);
            if(waitList == null) {
                waitList = new CopyOnWriteArrayList<Participant>();
                mohoConference.setAttribute(WAIT_LIST_KEY, waitList);
            }
            return waitList;
        }
    }

    private void addCallToWaitList() {
        synchronized (mohoConference) {
            getWaitList().add(call);
        }
    }

    private void removeCallFromWaitList() {
        synchronized (mohoConference) {
            List<Participant> waitList = mohoConference.getAttribute(WAIT_LIST_KEY);
            if(waitList != null) {
                waitList.remove(call);
            }
        }
    }

    public synchronized boolean isMute() {
        return mute;
    }

    public synchronized void setMute(boolean mute) {
        this.mute = mute;
        startMixing();
    }

    public synchronized boolean isTonePassthrough() {
        return tonePassthrough;
    }

    public synchronized void setTonePassthrough(boolean tonePassthrough) {
        this.tonePassthrough = tonePassthrough;
        startMixing();
    }

    public synchronized Character getTerminator() {
        return terminator;
    }

    public synchronized void setTerminator(Character terminator) {
        this.terminator = terminator;
        startMixing();
    }

    public synchronized void kick(String reason) {
        complete(Reason.KICK, reason);
    }
    
    public boolean isModerator() {
        return model.isModerator();
    }

    // Util
    // ================================================================================

    /**
     * @param hangup True if we're baing called as the result of a hangup event
     */
    private void preStop(boolean hangup) {
        
        synchronized (mohoConference) {
            
            call.setAttribute(getMohoParticipantAttributeKey(), null);
            removeCallFromWaitList();

            // Free up media resources if the call is still active
            if(!hangup) {
                stopMixing();
                stopMusic();
                stopHotwordListener();
            }
            
            // Check if we're the last moderator and if so put the remaining participants on hold
            if(model.isModerator() && isLastModerator()) {
                // Put remaining participants on hold
                for (com.voxeo.moho.Participant mixerParticipant : mohoConference.getParticipants()) {
                    ParticipantController controller = (ParticipantController) mixerParticipant.getAttribute(getMohoParticipantAttributeKey());
                    controller.setHold(true);
                }
            }
        }
        
    }
    
    private boolean isConferenceOpen() {
        return mohoConference.getParticipants().length > 0;
    }    

    private boolean isLastModerator() {
        synchronized (mohoConference) {
            for (com.voxeo.moho.Participant mixerParticipant : mohoConference.getParticipants()) {
                ParticipantController controller = (ParticipantController) mixerParticipant.getAttribute(getMohoParticipantAttributeKey());
                if(controller.isModerator()) {
                    return false;
                }
            }
            return true;
        }
    }    

    private String getMohoParticipantAttributeKey() {
        return (PARTICIPANT_KEY + model.getRoomName());
    }

    private void startMixing() {
        if (!joined) {
            Properties props = new Properties();
            props.setProperty("playTones", Boolean.toString(tonePassthrough));
            // TODO: Added property for 'beep' when it becomes available
            // https://evolution.voxeo.com/ticket/1430585
            try {
                mohoConference.join(call, JoinType.BRIDGE, mute ? Direction.RECV : Direction.DUPLEX, props).get();
            }
            catch (Exception e) {
                throw new IllegalStateException(e);
            }
            joined = true;
        }
    }

    private void stopMixing() {
        if (joined) {
            call.unjoin(mohoConference);
            // Rejoin the media server
            call.getMediaService(true);
            joined = false;
        }
    }

    private void startHotwordListener() {
        if (hotwordListener == null || hotwordListener.isDone()) {
            // Re-enable terminator because recognition stops after we join the mixer
            if (terminator != null) {
                DigitInputCommand inputCommand = new DigitInputCommand(terminator);
                inputCommand.setDtmfHotword(true);
                hotwordListener = media.input(inputCommand);
            }
        }
    }

    private void stopHotwordListener() {
        if (hotwordListener != null && !hotwordListener.isDone()) {
            hotwordListener.stop();
        }
    }

    private void startMusic() {
        SsmlItem holdMusicPrompt = model.getHoldMusic();
        if (holdMusicPrompt != null) {
            OutputCommand outputCommand = MohoUtil.output(holdMusicPrompt);
            outputCommand.setRepeatTimes(1000);
            holdMusic = media.output(outputCommand);
        }
    }

    private void playAnnouncement() {
        SsmlItem announcementPrompt = model.getAnnouncement();
        if (announcementPrompt != null) {
            OutputCommand outputCommand = MohoUtil.output(announcementPrompt);
            outputCommand.setBahavior(BehaviorIfBusy.QUEUE);
            mohoConference.getMediaService().output(outputCommand);
        }
    }

    private void stopMusic() {
        if (holdMusic != null && !holdMusic.isDone()) {
            holdMusic.stop();
        }
    }

    private void complete(Reason reason) {
        complete(reason, null);
    }

    private void complete(Reason reason, String kickReason) {
        try {
            preStop(false);
            ConferenceCompleteEvent event = new ConferenceCompleteEvent(model, reason);
            event.setKickReason(kickReason);
            complete(event);
        }
        catch (RuntimeException e) {
            complete(new ConferenceCompleteEvent(model, e.getMessage()));
            throw e;
        }
    }

    // Config
    // ================================================================================
    
    public ConferenceController getMohoConferenceController() {
        return mohoConferenceController;
    }
    
    public void setMohoConferenceController(ConferenceController mohoConferenceController) {
        this.mohoConferenceController = mohoConferenceController;
    }

    // Moho Events
    // ================================================================================

    @State
    public void onTermChar(InputCompleteEvent event) {
        // This event can come from another verb so we first check that
        if(hotwordListener != null && event.hasMatch() && event.source == call) {
            complete(Reason.TERMINATOR);
        }
    }


}
