package com.tropo.server.verb;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;

import javax.media.mscontrol.Value;
import javax.media.mscontrol.mediagroup.CodecConstants;
import javax.media.mscontrol.mediagroup.FileFormatConstants;

import com.tropo.core.recording.StorageService;
import com.tropo.core.verb.Output;
import com.tropo.core.verb.Record;
import com.tropo.core.verb.RecordCompleteEvent;
import com.tropo.core.verb.RecordPauseCommand;
import com.tropo.core.verb.RecordResumeCommand;
import com.tropo.core.verb.VerbCommand;
import com.tropo.core.verb.VerbCompleteEvent;
import com.tropo.core.verb.VerbCompleteReason;
import com.voxeo.logging.Loggerf;
import com.voxeo.moho.MediaException;
import com.voxeo.moho.Participant;
import com.voxeo.moho.State;
import com.voxeo.moho.media.Recording;
import com.voxeo.moho.media.record.RecordCommand;

public class RecordHandler extends AbstractLocalVerbHandler<Record, Participant> {

	private static final Loggerf log = Loggerf.getLogger(RecordHandler.class);

	private Recording<Participant> recording;
	
	private List<StorageService> storageServices;

	private File tempFile;
	
	@Override
	public void start() {

		if (model.getTo() == null) {
			try {
				tempFile = File.createTempFile("rayo", getExtensionFromFormat(model));
				model.setTo(tempFile.toURI());
			} catch (IOException e) {
				log.error(e.getMessage(),e);
			}
		}
		
		RecordCommand command = new RecordCommand(model.getTo());
        if (model.getStartBeep() != null) {
        	command.setStartBeep(model.getStartBeep());
        }
        if (model.getStopBeep() != null) {
        	//TODO: https://evolution.voxeo.com/ticket/1506906
        }
        if (model.getStartPaused() != null) {
        	command.setStartInPausedMode(model.getStartPaused());
        }
        if (model.getFinalTimeout() != null) {
        	command.setFinalTimeout(model.getFinalTimeout().getMillis());
        }
        if (model.getFormat() != null) {
        	command.setFileFormat(Output.toFileFormat(model.getFormat()));
        	if (command.getFileFormat().equals(FileFormatConstants.RAW)) {
        		command.setAudioCODEC(CodecConstants.LINEAR_16BIT_128K);
        	}
        } else {
        	command.setFileFormat(FileFormatConstants.WAV);
        }
        if (model.getInitialTimeout() != null) {
        	command.setInitialTimeout(model.getInitialTimeout().getMillis());
        }
        if (model.getMaxDuration() != null) {
        	command.setMaxDuration(model.getMaxDuration().getMillis());
        }
        
		recording = getMediaService().record(command);
	}

	private String getExtensionFromFormat(Record model) {

		if (model.getFormat() != null) {
			Value format = Output.toFileFormat(model.getFormat());
			if (format.equals(FileFormatConstants.FORMAT_3G2)) {
				return ".3g2";
			} else if (format.equals(FileFormatConstants.FORMAT_3GP)) {
				return ".3gp";
			} else if (format.equals(FileFormatConstants.GSM)) {
				return ".gsm";
			} else if (format.equals(FileFormatConstants.INFERRED)) {
				return ".mp3";
			} else if (format.equals(FileFormatConstants.RAW)) {
				return ".raw";
			} else if (format.equals(FileFormatConstants.WAV)) {
				return ".wav";
			}
		}
		return ".wav";
	}

	@Override
	public void stop(boolean hangup) {

        recording.stop();
        if(hangup) {
            complete(new RecordCompleteEvent(model, VerbCompleteEvent.Reason.HANGUP));
        } else {
        	//complete(new RecordCompleteEvent(model, Reason.SUCCESS));
        }

	}
	
    @Override
    public void onCommand(VerbCommand command) {
    	
        if (command instanceof RecordPauseCommand) {
        	pause();
        } else if (command instanceof RecordResumeCommand) {
        	resume();
        }
    }
	
    private void pause() {
    	
    	recording.pause();
	}
    
    private void resume() {
	
    	recording.resume();
    }
    
	@State
	public synchronized void onRecordComplete(com.voxeo.moho.event.RecordCompleteEvent<Participant> event) {
		
		switch(event.getCause()) {
			case ERROR:
			case UNKNOWN:
				log.error("Error while recording conversation");
				try {
					recording.get();
					complete(VerbCompleteEvent.Reason.ERROR);
				} catch (Exception e) {
					if (e.getCause() instanceof MediaException) {
						complete(VerbCompleteEvent.Reason.ERROR,e.getCause().getMessage());
					}
				}
				break;
			case TIMEOUT:
			case INI_TIMEOUT:
				complete(VerbCompleteEvent.Reason.STOP);
				break;
			case DISCONNECT:
				complete(VerbCompleteEvent.Reason.HANGUP);
				break;
			case CANCEL:
			case SILENCE:
				complete(VerbCompleteEvent.Reason.STOP);
				break;				
		}
	}
	
	private void complete(VerbCompleteReason reason) {
	
		complete(reason,null);
	}
	
	private void complete(VerbCompleteReason reason, String errorText) {

		RecordCompleteEvent event;
		
		// When temp file is null the user has provided a to URL (right now an undocumented feature). In such cases
		// no storage service policies will be applied.
		if (tempFile != null) {
			URI fileUri = tempFile.toURI();
			//TODO: Should we change this and add multiple URIs? Right now only the last URI will make it to the xml
			for (Object storageService: storageServices) {
				StorageService ss = (StorageService)storageService;
				try {
					URI result = ss.store(tempFile, getParticipant());
					if (!result.equals(fileUri)) {
						log.debug("Setting record's URI to %s", result);
						model.setTo(result);
					}
				} catch (IOException ioe) {
					event = createRecordCompleteEvent(VerbCompleteEvent.Reason.ERROR);
					event.setErrorText("Could not store the recording file");
					return;
				}
			}
		}
		event = createRecordCompleteEvent(reason, errorText);		
		complete(event);
	}

	private RecordCompleteEvent createRecordCompleteEvent(VerbCompleteReason reason) {
	
		return createRecordCompleteEvent(reason);
	}
	
	private RecordCompleteEvent createRecordCompleteEvent(VerbCompleteReason reason, String errorText) {
		
		if (errorText != null) {
			return new RecordCompleteEvent(model, errorText);
		} else {
			return new RecordCompleteEvent(model, reason);
		}
	}

	public void setStorageServices(List<StorageService> storageServices) {
		this.storageServices = storageServices;
	}
}
