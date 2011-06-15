package com.tropo.server;

import javax.media.mscontrol.Parameters;

import com.voxeo.moho.media.InputMode;
import com.voxeo.moho.media.dialect.MediaDialect;
import com.voxeo.mscontrol.VoxeoParameter;

public class PrismDialect implements MediaDialect {

    @Override
    public void setBeepOnConferenceEnter(Parameters parameters, Boolean value) {
        parameters.put(VoxeoParameter.VOXEO_JOIN_ENTER_TONE, value);
    }

    @Override
    public void setBeepOnConferenceExit(Parameters parameters, Boolean value) {
        parameters.put(VoxeoParameter.VOXEO_JOIN_EXIT_TONE, value);
    }

    @Override
    public void setSpeechInputMode(Parameters parameters, InputMode value) {
        parameters.put(VoxeoParameter.VOXEO_INPUT_MODE, value);
    }

    @Override
    public void setSpeechLanguage(Parameters parameters, String value) {
        parameters.put(VoxeoParameter.SPEECH_LANGUAGE, value);
    }

    @Override
    public void setSpeechTermChar(Parameters parameters, Character value) {
        parameters.put(VoxeoParameter.DTMF_TERM_CHAR, value);
    }

    @Override
    public void setTextToSpeechVoice(Parameters parameters, String value) {
        parameters.put(VoxeoParameter.VOICE_NAME, value);
    }

    @Override
    public void setDtmfHotwordEnabled(Parameters parameters, Boolean value) {
        parameters.put(VoxeoParameter.DTMF_HOTWORD_DETECTION_ENABLED, value);
    }
    
    @Override
    public void setDtmfTypeaheadEnabled(Parameters parameters, Boolean value) {
        parameters.put(VoxeoParameter.DTMF_TYPE_AHEAD_ENABLED, value);
    }

    @Override
    public void setConfidence(Parameters parameters, float value) {
    	// Uncomment when the parameter gets into maven repo
    	parameters.put(VoxeoParameter.VOXEO_CONFIDENCE_THRESHOLD, value);
    }
}
