package com.rayo.server.verb;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import javax.media.mscontrol.mediagroup.CodecConstants;
import javax.media.mscontrol.mediagroup.FileFormatConstants;
import javax.validation.ConstraintValidatorContext;

import org.joda.time.Duration;

import com.rayo.core.recording.StorageService;
import com.rayo.core.verb.Output;
import com.rayo.core.verb.Record;
import com.rayo.core.verb.RecordCompleteEvent;
import com.rayo.core.verb.RecordCompleteEvent.Reason;
import com.rayo.core.verb.RecordPauseCommand;
import com.rayo.core.verb.RecordResumeCommand;
import com.rayo.core.verb.VerbCommand;
import com.rayo.core.verb.VerbCompleteEvent;
import com.rayo.core.verb.VerbCompleteReason;
import com.rayo.server.exception.ExceptionMapper;
import com.rayo.server.recording.LocalStore;
import com.voxeo.logging.Loggerf;
import com.voxeo.moho.MediaException;
import com.voxeo.moho.Participant;
import com.voxeo.moho.State;
import com.voxeo.moho.media.Recording;
import com.voxeo.moho.media.record.RecordCommand;
import com.voxeo.servlet.xmpp.StanzaError;

public class RecordHandler extends AbstractLocalVerbHandler<Record, Participant> {

	private static final Loggerf log = Loggerf.getLogger(RecordHandler.class);

	private Recording<Participant> recording;
	
	private List<StorageService> storageServices;
	private LocalStore localStore;

	private File file;
	
	@Override
	public void start() {

		if (model.getTo() == null) {
			try {
				file = localStore.createRecording(model);
				
				if (model.getDuplex() != null && model.getDuplex()) {
					// Hack to workaround a VCS issue with URIs ( call record has to be file:/// and normal record file:/
					URI hackedURI = null;
					try {
						hackedURI = new URI("file://" + file.getAbsolutePath());
					} catch (URISyntaxException e) {}
					model.setTo(hackedURI);
				} else {
					model.setTo(file.toURI());
				}
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
        } else {
            command.setFinalTimeout(10000);        	
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
        } else {
            command.setInitialTimeout(10000);        	
        }
        if (model.getMaxDuration() != null) {
        	command.setMaxDuration(model.getMaxDuration().getMillis());
        }
        
        command.setSilenceTerminationOn(false);
        if (model.getDuplex() != null) {
        	command.setDuplex(model.getDuplex());
        } else {
        	command.setDuplex(false);
        }
        
		recording = getMediaService().record(command);
	}



	@Override
	public void stop(boolean hangup) {

        recording.stop();
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
    
	@Override
	public boolean isStateValid(ConstraintValidatorContext context) {

    	if (!isReady(participant)) {
            context.buildConstraintViolationWithTemplate("Call is not ready yet.")
        		.addNode(ExceptionMapper.toString(StanzaError.Condition.RESOURCE_CONSTRAINT))
        		.addConstraintViolation();
            return false;
    		
    	}
    	
        if (!canManipulateMedia()) {
            context.buildConstraintViolationWithTemplate("Media operations are not allowed in the current call status.")
            	.addNode(ExceptionMapper.toString(StanzaError.Condition.RESOURCE_CONSTRAINT))
            		.addConstraintViolation();
            return false;
        }
        return true;
	}
    
	@State
	public synchronized void onRecordComplete(com.voxeo.moho.event.RecordCompleteEvent<Participant> event) {
		
    	if (event.getMediaOperation() != null && !event.getMediaOperation().equals(recording)) {
    		log.debug("Ignoring complete event as it is targeted to a different media operation");
    		return;
    	}

		switch(event.getCause()) {
			case ERROR:
			case UNKNOWN:
				log.error("Error while recording conversation");
				try {
					recording.get();
					complete(VerbCompleteEvent.Reason.ERROR, event.getErrorText(), event.getDuration());
				} catch (Exception e) {
					if (e.getCause() instanceof MediaException) {
						complete(VerbCompleteEvent.Reason.ERROR,e.getCause().getMessage(), event.getDuration());
					}
				}
				break;
			case TIMEOUT:
				complete(Reason.TIMEOUT, event.getDuration());
				break;
			case INI_TIMEOUT:
				complete(Reason.INI_TIMEOUT, event.getDuration());
				break;
			case DISCONNECT:
				complete(VerbCompleteEvent.Reason.HANGUP, event.getDuration());
				break;
			case CANCEL:
			case SILENCE:
				complete(VerbCompleteEvent.Reason.STOP, event.getDuration());
				break;				
		}
	}
	
	private void complete(VerbCompleteReason reason, long duration) {
	
		complete(reason,null, duration);
	}
	
	private void complete(VerbCompleteReason reason, String errorText, long duration) {

		RecordCompleteEvent event;
		long size = 0;
		
		// When temp file is null the user has provided a to URL (right now an undocumented feature). In such cases
		// no storage service policies will be applied.
		if (file != null) {
			
			try {
				size = file.length();
			} catch (Exception e) {
				log.error(e.getMessage(),e);
			}
			
			URI fileUri = file.toURI();
			//TODO: Should we change this and add multiple URIs? Right now only the last URI will make it to the xml
			for (Object storageService: storageServices) {
				StorageService ss = (StorageService)storageService;
				try {
					URI result = ss.store(file, getParticipant());
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
		event.setDuration(new Duration(duration));
		event.setSize(size);
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

	public void setLocalStore(LocalStore localStore) {
		this.localStore = localStore;
	}	
}
