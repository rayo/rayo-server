package com.tropo.core.verb;

import javax.media.mscontrol.Value;
import javax.media.mscontrol.mediagroup.CodecConstants;
import javax.media.mscontrol.mediagroup.FileFormatConstants;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import com.tropo.core.validation.ValidCodec;
import com.tropo.core.validation.ValidFileFormat;

public class Output extends BaseVerb {
	
    public static final String MISSING_PROMPT = "Nothing to do";
    
    private Boolean bargein;
    private String voice;
    private Integer timeout;
    private Integer volumeUnit;
    private Integer offset;
    
    @ValidCodec
    private String codec;
    
    @ValidFileFormat
    private String format;
    
    private Integer repeatTimes;
    private Integer jumpPlaylistIncrement;
    private Integer jumpTime;
    private Boolean startInPauseMode;
    
	@Valid
    @NotNull(message=Output.MISSING_PROMPT)
    private Ssml prompt;
    

    public Ssml getPrompt() {
		return prompt;
	}

	public void setPrompt(Ssml prompt) {
		this.prompt = prompt;
	}

	public Boolean isBargein() {
		return bargein;
	}

	public void setBargein(Boolean bargein) {
		this.bargein = bargein;
	}

	public String getVoice() {
		return voice;
	}

	public void setVoice(String voice) {
		this.voice = voice;
	}

	public Integer getTimeout() {
		return timeout;
	}

	public void setTimeout(Integer timeout) {
		this.timeout = timeout;
	}

	public Integer getVolumeUnit() {
		return volumeUnit;
	}

	public void setVolumeUnit(Integer volumeUnit) {
		this.volumeUnit = volumeUnit;
	}

	public Integer getOffset() {
		return offset;
	}

	public void setOffset(Integer offset) {
		this.offset = offset;
	}

	public String getCodec() {
		return codec;
	}

	public void setCodec(String codec) {
		this.codec = codec;
	}

	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	public Integer getRepeatTimes() {
		return repeatTimes;
	}

	public void setRepeatTimes(Integer repeatTimes) {
		this.repeatTimes = repeatTimes;
	}

	public Integer getJumpPlaylistIncrement() {
		return jumpPlaylistIncrement;
	}

	public void setJumpPlaylistIncrement(Integer jumpPlaylistIncrement) {
		this.jumpPlaylistIncrement = jumpPlaylistIncrement;
	}

	public Integer getJumpTime() {
		return jumpTime;
	}

	public void setJumpTime(Integer jumpTime) {
		this.jumpTime = jumpTime;
	}

	public Boolean isStartInPauseMode() {
		return startInPauseMode;
	}

	public void setStartInPauseMode(Boolean startInPauseMode) {
		this.startInPauseMode = startInPauseMode;
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
    	} else if (CodecConstants.INFERRED.toString().equalsIgnoreCase(codec)) {
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
	
	@Override
    public String toString() {

    	return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)    		
    		.append("callId", getCallId())
    		.append("verbId", getVerbId())
    		.append("prompt", getPrompt())
    		.append("jumpTime", getJumpTime())
    		.append("jumpPlaylistIncrement", getJumpPlaylistIncrement())
    		.append("offset", getOffset())
    		.append("repeatTimes", getRepeatTimes())
    		.append("timeout", getTimeout())
    		.append("volumeUnit", getVolumeUnit())
    		.append("codec", getCodec())
    		.append("format", getFormat())
    		.append("voice", getVoice())
    		.toString();
    }
}
