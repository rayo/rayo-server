package com.tropo.core.verb;

import javax.media.mscontrol.Value;
import javax.media.mscontrol.mediagroup.CodecConstants;
import javax.media.mscontrol.mediagroup.FileFormatConstants;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.joda.time.Duration;

import com.voxeo.moho.media.output.OutputCommand.BargeinType;

public class Output extends BaseVerb {
	
    public static final String MISSING_PROMPT = "Nothing to do";
    
    private BargeinType bargeinType;
    private Duration startOffset;
    private Boolean startPaused;
    private Duration repeatInterval;
    private Integer repeatTimes;
    private Duration maxTime;
    private String voice;
    
	@Valid
    @NotNull(message=Output.MISSING_PROMPT)
    private Ssml prompt;
    

    public Ssml getPrompt() {
		return prompt;
	}

	public void setPrompt(Ssml prompt) {
		this.prompt = prompt;
	}

	public BargeinType getBargeinType() {
		return bargeinType;
	}

	public void setBargeinType(BargeinType type) {
		this.bargeinType = type;
	}

	public String getVoice() {
		return voice;
	}

	public void setVoice(String voice) {
		this.voice = voice;
	}

	public Duration getStartOffset() {
		return startOffset;
	}

	public void setStartOffset(Duration offset) {
		this.startOffset = offset;
	}

	public Integer getRepeatTimes() {
		return repeatTimes;
	}

	public void setRepeatTimes(Integer repeatTimes) {
		this.repeatTimes = repeatTimes;
	}

	public Boolean isStartPaused() {
		return startPaused;
	}

	public void setStartPaused(Boolean startInPauseMode) {
		this.startPaused = startInPauseMode;
	}

	public static Value toFileFormat(String format) {
		
		// Adapt the input value to FileFormatConstants' toString() format
		format = "FORMAT_" + format;
		
		if (FileFormatConstants.FORMAT_3G2.toString().equalsIgnoreCase(format)) {
			return FileFormatConstants.FORMAT_3G2;
		} else if (FileFormatConstants.FORMAT_3GP.toString().equalsIgnoreCase(format)) {
			return FileFormatConstants.FORMAT_3GP;
		} else if (FileFormatConstants.GSM.toString().equalsIgnoreCase(format)) {
			return FileFormatConstants.GSM;
		} else if (FileFormatConstants.INFERRED.toString().equalsIgnoreCase(format)) {
			return FileFormatConstants.INFERRED;
		} else if (FileFormatConstants.RAW.toString().equalsIgnoreCase(format)) {
			return FileFormatConstants.RAW;
		} else if (FileFormatConstants.WAV.toString().equalsIgnoreCase(format)) {
			return FileFormatConstants.WAV;
		} else {
			// Manual assignments as FileFormatConstants does not include all supported file types
			if (format.equalsIgnoreCase("FORMAT_MP3")) {
				return FileFormatConstants.INFERRED;
			}
		}
		
		return null;
	}
	
    public static Value toCodecValue(String codec) {

    	if (CodecConstants.ADPCM_16K_G726.toString().equalsIgnoreCase(codec)) {
    		return CodecConstants.ADPCM_16K_G726;
    	} else if (CodecConstants.ADPCM_32K.toString().equalsIgnoreCase(codec)) {
    		return CodecConstants.ADPCM_32K;
    	} else if (CodecConstants.ADPCM_32K_G726.toString().equalsIgnoreCase(codec)) {
    		return CodecConstants.ADPCM_32K_G726;
    	} else if (CodecConstants.ADPCM_32K_OKI.toString().equalsIgnoreCase(codec)) {
    		return CodecConstants.ADPCM_32K_OKI;
    	} else if (CodecConstants.ALAW_PCM_48K.toString().equalsIgnoreCase(codec)) {
    		return CodecConstants.ALAW_PCM_48K;
    	} else if (CodecConstants.ALAW_PCM_64K.toString().equalsIgnoreCase(codec)) {
    		return CodecConstants.ALAW_PCM_64K;
    	} else if (CodecConstants.AMR.toString().equalsIgnoreCase(codec)) {
    		return CodecConstants.AMR;
    	} else if (CodecConstants.AMR_WB.toString().equalsIgnoreCase(codec)) {
    		return CodecConstants.AMR_WB;
    	} else if (CodecConstants.EVRC.toString().equalsIgnoreCase(codec)) {
    		return CodecConstants.EVRC;
    	} else if (CodecConstants.G723_1B.toString().equalsIgnoreCase(codec)) {
    		return CodecConstants.G723_1B;
    	} else if (CodecConstants.G729_A.toString().equalsIgnoreCase(codec)) {
    		return CodecConstants.G729_A;
    	} else if (CodecConstants.GSM.toString().equalsIgnoreCase(codec)) {
    		return CodecConstants.GSM;
    	} else if (CodecConstants.H263.toString().equalsIgnoreCase(codec)) {
    		return CodecConstants.H263;
    	} else if (CodecConstants.H263_1998.toString().equalsIgnoreCase(codec)) {
    		return CodecConstants.H263_1998;
    	} else if (CodecConstants.H264.toString().equalsIgnoreCase(codec)) {
    		return CodecConstants.H264;
    	} else if (CodecConstants.INFERRED.toString().equalsIgnoreCase("CODEC_" + codec)) {
    		return CodecConstants.INFERRED;
    	} else if (CodecConstants.LINEAR_16BIT_128K.toString().equalsIgnoreCase(codec)) {
    		return CodecConstants.LINEAR_16BIT_128K;
    	} else if (CodecConstants.LINEAR_16BIT_256K.toString().equalsIgnoreCase(codec)) {
    		return CodecConstants.LINEAR_16BIT_256K;
    	} else if (CodecConstants.LINEAR_8BIT_64K.toString().equalsIgnoreCase(codec)) {
    		return CodecConstants.LINEAR_8BIT_64K;
    	} else if (CodecConstants.MP4V_ES.toString().equalsIgnoreCase(codec)) {
    		return CodecConstants.MP4V_ES;
    	} else if (CodecConstants.MULAW_PCM_64K.toString().equalsIgnoreCase(codec)) {
    		return CodecConstants.MULAW_PCM_64K;
    	}
    	return null;
	}
	
    public Duration getRepeatInterval() {
        return repeatInterval;
    }

    public void setRepeatInterval(Duration repeatInterval) {
        this.repeatInterval = repeatInterval;
    }
    
    public Duration getMaxTime() {
        return maxTime;
    }

    public void setMaxTime(Duration maxTime) {
        this.maxTime = maxTime;
    }

	@Override
    public String toString() {

    	return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)    		
    		.append("callId", getCallId())
    		.append("verbId", getVerbId())
    		.append("interrupt-on", getBargeinType())
    		.append("start-offset", getStartOffset() != null ? getStartOffset().getMillis() : null)
    		.append("start-paused", isStartPaused())
            .append("repeatInterval", getRepeatInterval() != null ? getRepeatInterval().getMillis() : null)
    		.append("repeatTimes", getRepeatTimes())
            .append("maxTime", getMaxTime() != null ? getMaxTime().getMillis() : null)
            .append("voice", getVoice())
    		.append("prompt", getPrompt())
    		.toString();
    }



}
