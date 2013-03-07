package com.rayo.server.verb;

import java.util.ArrayList;
import java.util.List;

import javax.validation.ConstraintValidatorContext;

import com.rayo.core.verb.Choices;
import com.rayo.core.verb.Input;
import com.rayo.core.verb.InputCompleteEvent;
import com.rayo.core.verb.InputCompleteEvent.Reason;
import com.rayo.core.verb.InputMode;
import com.rayo.core.verb.SignalEvent;
import com.rayo.core.verb.VerbCompleteEvent;
import com.rayo.server.CallActor;
import com.rayo.server.exception.ExceptionMapper;
import com.voxeo.logging.Loggerf;
import com.voxeo.moho.Call;
import com.voxeo.moho.Participant;
import com.voxeo.moho.State;
import com.voxeo.moho.common.event.MohoCPAEvent;
import com.voxeo.moho.event.CPAEvent;
import com.voxeo.moho.event.CPAEvent.Type;
import com.voxeo.moho.event.InputDetectedEvent;
import com.voxeo.moho.media.input.EnergyGrammar;
import com.voxeo.moho.media.input.Grammar;
import com.voxeo.moho.media.input.InputCommand;
import com.voxeo.moho.media.input.SignalGrammar;
import com.voxeo.moho.media.input.SignalGrammar.Signal;
import com.voxeo.servlet.xmpp.StanzaError;

public class InputHandler extends AbstractLocalVerbHandler<Input, Participant> {

	private com.voxeo.moho.media.Input<Participant> input;
	private List<String> signals = new ArrayList<String>();

	private static final Loggerf logger = Loggerf.getLogger(InputHandler.class);

	/**
	 * <p>
	 * The 'cpa-maxtime' parameter is the "measuring stick" used to determine
	 * 'human' or 'machine' events. If the duration of voice activity is less
	 * than the value of 'cpa-maxtime', the called party is considered to be
	 * 'human.' If voice activity exceeds the 'cpa-maxtime' value, your
	 * application has likely called a 'machine'.
	 * <p>
	 * The recommended value for this parameter is between 4000 and 6000ms.
	 */
	protected long voxeo_cpa_max_time = 4000;

	/**
	 * <p>
	 * The 'cpa-maxsilence' parameter is used to identify the end of voice
	 * activity. When activity begins, CPA will measure the duration until a
	 * period of silence greater than the value of 'cpa-maxsilence' is detected.
	 * Armed with start and end timestamps, CPA can then calculate the total
	 * duration of voice activity.
	 * <p>
	 * A value of 800 to 1200ms is suggested for this parameter.
	 */
	protected long voxeo_cpa_final_silence = 1000;

	/**
	 * <p>
	 * The 'cpa-min-speech-duration' parameter is used to identify the minimum
	 * duration of energy.
	 * <p>
	 * A value of (x)ms to (y)ms is suggested for this parameter.
	 */
	protected long voxeo_cpa_min_speech_duration = 80;

	/**
	 * <p>
	 * The 'cpa-min-volume' parameter is used to identify the threshold of what
	 * is considered to be energy vs silence.
	 * <p>
	 * A value of (x)db to (y)db is suggested for this parameter.
	 */
	protected int voxeo_cpa_min_volume = -24;
	
	protected boolean terminate = false;

	private long _lastStartOfSpeech;
	private long _lastEndOfSpeech;
	private int _retries;
	
	@Override
	public void start() {

		Grammar[] grammars = buildGrammars(model);

		// Is this a CPA request?
		if (getActor() instanceof CallActor) {

			InputCommand inputCommand = new InputCommand(grammars);
			if (model.getCpaData() != null) {
				logger.debug("Starting CPA request with data",
						model.getCpaData());

				if (model.getCpaData().getFinalSilence() != null) {
					voxeo_cpa_final_silence = model.getCpaData()
							.getFinalSilence();
				}
				if (model.getCpaData().getMaxTime() != null) {
					voxeo_cpa_max_time = model.getCpaData().getMaxTime();
				}
				if (model.getCpaData().getMinSpeechDuration() != null) {
					voxeo_cpa_min_speech_duration = model.getCpaData()
							.getMinSpeechDuration();
				}
				if (model.getCpaData().getMinVolume() != null) {
					voxeo_cpa_min_volume = model.getCpaData().getMinVolume()
							.intValue();
				}
				
				terminate = model.getCpaData().isTerminate();

				inputCommand.setAutoRest(false);
				inputCommand.setEnergyParameters(voxeo_cpa_final_silence, null,
						null, voxeo_cpa_min_speech_duration,
						voxeo_cpa_min_volume);
			}
			inputCommand.setInputMode(com.voxeo.moho.media.InputMode.ANY);
			if (model.getMode() != null) {
				inputCommand.setInputMode(getMohoMode(model.getMode()));
			}
			if (model.getMode() == InputMode.DTMF || model.getMode() == InputMode.ANY) {
				inputCommand.setSupervised(true);
			}

			if (model.getInitialTimeout() != null) {
				inputCommand.setInitialTimeout(model.getInitialTimeout()
						.getMillis());
			}
			if (model.getInterDigitTimeout() != null) {
				inputCommand.setInterDigitsTimeout(model.getInterDigitTimeout()
						.getMillis());
			}
			if (model.getMaxSilence() != null) {
				inputCommand.setSpeechIncompleteTimeout(model.getMaxSilence()
						.getMillis());
			}
			if (model.getRecognizer() != null) {
				inputCommand.setRecognizer(model.getRecognizer());
			}
			if (model.getMinConfidence() != null) {
				inputCommand.setMinConfidence(model.getMinConfidence());
			}
			if (model.getSensitivity() != null) {
				inputCommand.setSensitivity(model.getSensitivity());
			}
			if (model.getTerminator() != null) {
				inputCommand.setTerminator(model.getTerminator());
			}

			input = getMediaService().input(inputCommand);
		}
	}

	private Grammar[] buildGrammars(Input model) {

		List<Grammar> grammars = new ArrayList<Grammar>();

		// Is this a CPA request?
		if (getActor() instanceof CallActor) {
			if (model.getCpaData() != null) {
				logger.debug("Starting CPA request with data",
						model.getCpaData());

				grammars.add(new EnergyGrammar(true, false, false));
				grammars.add(new EnergyGrammar(false, true, false));

				for (String it : model.getCpaData().getSignals()) {
					// We ignore DTMF signal at this layer. Moho will broadcast
					// those events as InputDetectedEvent
					// However the rayo protocol exposes the events through the
					// CPA API for consistency reasons
					// So, DTMF events will go through a different path
					if (!it.equalsIgnoreCase("dtmf") &&
						!it.equalsIgnoreCase("speech")) {
						grammars.add(new SignalGrammar(Signal.parse(it), model.getCpaData().isTerminate()));
					}
					this.signals.add(it);
				}
				if (model.getCpaData().getFinalSilence() != null) {
					voxeo_cpa_final_silence = model.getCpaData()
							.getFinalSilence();
				}
				if (model.getCpaData().getMaxTime() != null) {
					voxeo_cpa_max_time = model.getCpaData().getMaxTime();
				}
				if (model.getCpaData().getMinSpeechDuration() != null) {
					voxeo_cpa_min_speech_duration = model.getCpaData()
							.getMinSpeechDuration();
				}
				if (model.getCpaData().getMinVolume() != null) {
					voxeo_cpa_min_volume = model.getCpaData().getMinVolume()
							.intValue();
				}
			}
		}

		for (int i = 0; i < model.getGrammars().size(); i++) {
			Choices choices = model.getGrammars().get(i);
			if (choices != null) {
				Grammar grammar = null;
				if (choices.getUri() != null) {
					grammar = new Grammar(choices.getUri());
				} else {
					grammar = new Grammar(choices.getContentType(),
							choices.getContent());
				}
				grammars.add(grammar);
			}
		}

		return grammars.toArray(new Grammar[] {});
	}

	@Override
	public void stop(boolean hangup) {

		if (hangup) {
			complete(new InputCompleteEvent(model, Reason.NOMATCH));
		} else {
			if (input != null) {
				input.stop();
			}
		}
	}

	@Override
	public boolean isStateValid(ConstraintValidatorContext context) {

		if (!isReady(participant)) {
			context.buildConstraintViolationWithTemplate(
					"Call is not ready yet.")
					.addNode(
							ExceptionMapper
									.toString(StanzaError.Condition.RESOURCE_CONSTRAINT))
					.addConstraintViolation();
			return false;

		}

		if (!canManipulateMedia()) {
			context.buildConstraintViolationWithTemplate(
					"Media operations are not allowed in the current call status.")
					.addNode(
							ExceptionMapper
									.toString(StanzaError.Condition.RESOURCE_CONSTRAINT))
					.addConstraintViolation();
			return false;
		}
		return true;
	}

	@State
	public void onCPAEvent(CPAEvent<Call> event) {

		logger.debug("Received CPA Event: " + event);

		if (event.getSource().equals(participant)) {
			if (event.getSignal() != null) {
				if (signals != null
						&& signals.contains(event.getSignal().toString()
								.toLowerCase())) {
					getEventDispatcher().fire(
							new com.rayo.core.verb.SignalEvent(
									(Input) getModel(), 
									event.getSignal().toString().toLowerCase(), 
									event.getDuration(),
									null));
				}
			} else {
				if (event.getType() != null
						&& (signals != null && signals.contains("speech"))) {
					SignalEvent signalEvent = buildSignalFromCPAEvent(event);
					if (signalEvent != null) {
						getEventDispatcher().fire(signalEvent);
					}
				}
			}
		}
	}
	
	private SignalEvent buildSignalFromCPAEvent(CPAEvent<Call> event) {
		
		SignalEvent signalEvent = null;
		if (event.getType() != null
				&& (signals != null && signals.contains("speech"))) {
			// human vs machine scenario
			switch (event.getType()) {
			case MACHINE_DETECTED:
				signalEvent = new com.rayo.core.verb.SignalEvent(
					(Input) getModel(), "speech", event.getDuration(), "machine");
				break;
			case HUMAN_DETECTED:
				signalEvent = new com.rayo.core.verb.SignalEvent(
					(Input) getModel(), "speech", event.getDuration(), "human");
				break;
			}
		}	
		return signalEvent;
	}

	@com.voxeo.moho.State
	public void onInputDetected(InputDetectedEvent<Call> event) throws Exception {

		logger.debug(event.toString());
		if (event.getInput() != null) {
			if (signals != null && signals.contains("dtmf")) {
				SignalEvent signalEvent = new SignalEvent(
					participant.getId(), "dtmf",event.getInput());
				if (terminate) {
					// This is for compatibility with CPA's terminate tag. Probalby not 
					// much sense for DTMF detection as the same can be achieved via a 
					// [1 DIGITS] grammar, but we should support it anyways
					InputCompleteEvent completeEvent = 
						new InputCompleteEvent(model, Reason.MATCH);
					completeEvent.setSignalEvent(signalEvent);
					try {
						complete(completeEvent);
					} finally {
						stop(false);
					}
				} else {
					fire(signalEvent);
				}
			}			
		} else {
			if (event.isStartOfSpeech()) {
				_lastStartOfSpeech = System.currentTimeMillis();
			} else if (event.isEndOfSpeech()) {
				_lastEndOfSpeech = System.currentTimeMillis();
	
				++_retries;
				long duration = _lastEndOfSpeech - _lastStartOfSpeech;
				Type type;
				if (duration < voxeo_cpa_max_time) {
					type = Type.HUMAN_DETECTED;
				} else {
					type = Type.MACHINE_DETECTED;
				}
				CPAEvent<Call> cpaEvent = new MohoCPAEvent<Call>(
					event.getSource(),type, duration, _retries);
				onCPAEvent(cpaEvent);
				
				_lastStartOfSpeech = 0;
				_lastEndOfSpeech = 0;
			} else if (event.getSignal() != null) {
				onCPAEvent(new MohoCPAEvent<Call>(event.getSource(),
						Type.MACHINE_DETECTED, event.getSignal()));
			}
		}		
	}

	@State
	public void onInputComplete(
			com.voxeo.moho.event.InputCompleteEvent<Participant> event) {

		if (!event.getMediaOperation().equals(input)) {
			logger.debug("Ignoring complete event as it is targeted to a different media operation");
			return;
		}

		InputCompleteEvent completeEvent = null;

		switch (event.getCause()) {
			case MATCH:
				completeEvent = new InputCompleteEvent(model, Reason.MATCH);
				completeEvent.setConcept(event.getConcept());
				completeEvent.setInterpretation(event.getInterpretation());
				completeEvent.setConfidence(event.getConfidence());
				completeEvent.setUtterance(event.getUtterance());
				completeEvent.setNlsml(event.getNlsml());
				completeEvent.setTag(event.getTag());
				processSignalIfAny(event, completeEvent);
				if (event.getInputMode() != null) {
					completeEvent.setMode(getInputMode(event.getInputMode()));
				}
				break;
			case INI_TIMEOUT:
				completeEvent = new InputCompleteEvent(model, Reason.NOINPUT);
				break;
			case IS_TIMEOUT:
			case MAX_TIMEOUT:
				completeEvent = new InputCompleteEvent(model, Reason.TIMEOUT);
				break;
			case NO_MATCH:
				completeEvent = new InputCompleteEvent(model, Reason.NOMATCH);
				break;
			case CANCEL:
				completeEvent = new InputCompleteEvent(model,
						VerbCompleteEvent.Reason.STOP);
				break;
			case DISCONNECT:
				completeEvent = new InputCompleteEvent(model,
						VerbCompleteEvent.Reason.HANGUP);
				break;
			case ERROR:
				String cause = event.getErrorText() == null ? "Internal Server Error"
						: event.getErrorText();
				completeEvent = new InputCompleteEvent(model, cause);
				break;
			case UNKNOWN:
			default:
				if (participant instanceof Call) {
					if (((Call) participant).getCallState() == com.voxeo.moho.Call.State.DISCONNECTED) {
						completeEvent = new InputCompleteEvent(model,
								VerbCompleteEvent.Reason.HANGUP);
						break;
					}
				}
				completeEvent = new InputCompleteEvent(model,
						"Internal Server Error");
		}

		complete(completeEvent);
	}

	private void processSignalIfAny(
			com.voxeo.moho.event.InputCompleteEvent<Participant> event,
			InputCompleteEvent completeEvent) {
		
		if (event.getSignal() != null) {
			if (signals != null
					&& signals.contains(event.getSignal().toString().toLowerCase())) {
				completeEvent.setSignalEvent(
						new com.rayo.core.verb.SignalEvent(
								(Input) getModel(), 
								event.getSignal().toString().toLowerCase(), 
								-1L,
								null));
			}				
		}
	}
}
