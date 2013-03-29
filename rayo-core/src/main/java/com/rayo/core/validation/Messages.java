package com.rayo.core.validation;

public class Messages {

	public static final String MISSING_PROMPT_ITEMS = "You need to send at least an audio or SSML item.";
	public static final String MISSING_CHOICES = "You need to provide at least a valid choice element or CPA data.";
	public static final String MISSING_GRAMMARS = "You need to provide at least a valid grammar element.";
	public static final String INVALID_RECOGNIZER = "Invalid recognizer.";
	public static final String MISSING_TO = "Missing required attribute 'to'";
    public static final String MISSING_FROM = "Missing required attribute 'from'";
	public static final String MISSING_ROOM_NAME = "Missing room name.";
	public static final String MISSING_CHOICES_CONTENT_OR_URL = "For choices, either 'url' or inline choices text is required (not both)";
	public static final String MISSING_CHOICES_CONTENT_TYPE = "'content-type' is required when specifying choices contents inline";
	public static final String MISSING_SSML = "Missing SSML content.";
	public static final String MISSING_DESTINATION = "Missing destination.";
    public static final String MISSING_COMPLETE_REASON = "Complete event with no reason!";
    public static final String MISSING_JOIN_ID = "Missing Join id. You need to specify either a valid call-id or mixer-name";
	public static final String MISSING_AMOUNT = "Amount is a mandatory field.";
	public static final String MISSING_DIRECTION = "Direction is a mandatory field.";
	public static final String MISSING_DTMF_KEY = "DTMF key is a mandatory field.";
	public static final String MISSING_SPEAKER_ID = "Call id is a mandatory field.";
	public static final String MISSING_REASON = "Missing mandatory reason element";
    public static final String MISSING_CALL_EVENT = "Missing call event.";
    public static final String MISSING_TARGET_ADDRESS = "Missing target address";

    public static final String INVALID_DURATION = "Invalid duration for '%s'";
    public static final String INVALID_INPUT_MODE = "Invalid input mode.";
	public static final String INVALID_URI = "You have submitted an invalid URI.";
	public static final String INVALID_BOOLEAN = "You have submitted an invalid boolean value for '%s'";
	public static final String INVALID_INTEGER = "You have submitted an invalid integer value for '%s'";
	public static final String INVALID_LONG = "You have submitted an invalid long value for '%s'";
	public static final String INVALID_FLOAT = "You have submitted an invalid float value for '%s'";
	public static final String INVALID_TIMEOUT = "Invalid timeout specified.";
	public static final String INVALID_CONFIDENCE = "Invalid confidence specified.";
	public static final String INVALID_SENSITIVITY = "Invalid sensitivity specified.";
	public static final String INVALID_TERMINATOR = "Invalid terminator character.";
	public static final String INVALID_CONFIDENCE_RANGE = "Confidence must be a value between 0 and 1.";
	public static final String INVALID_SENSITIVITY_RANGE = "Sensitivity must be a value between 0 and 1.";
	public static final String INVALID_MAX_SILENCE = "Max Silence needs to be greater or equal to 0.";
	public static final String INVALID_REASON = "Invalid reason";
    public static final String INVALID_MEDIA = "Invalid media.";
    public static final String INVALID_DIRECTION = "Invalid direction. Should be either true or false.";
    public static final String INVALID_POSITION = "Invalid position. It needs to be an integer value.";
    public static final String INVALID_TIME = "Invalid time. It needs to be an integer value.";
    public static final String INVALID_SPEED = "Invalid speed. It needs to be a boolean.";
    public static final String INVALID_VOLUME = "Invalid volume. It needs to be a boolean.";
    public static final String INVALID_CODEC = "Invalid codec value.";
    public static final String INVALID_FILE_FORMAT = "Invalid file format.";
    public static final String INVALID_MEDIA_DIRECTION = "Invalid direction. It should be one of DUPLEX|RECV|SEND.";
    public static final String INVALID_JOIN_TYPE = "Invalid join type. It should be one of BRIDGE|DIRECT.";
    public static final String INVALID_BARGEIN_TYPE = "Invalid 'interrupt-on'.";
    public static final String INVALID_ENUM = "Unsupported value for '%s'";
    public static final String INVALID_DTMF_KEY = "Invalid DTMF key. Characters accepted are [0-9], [A,B,C,D], * or #.";
    public static final String INVALID_SIGNAL = "Invalid signal: %s.";
    		
	public static final String UNKNOWN_NAMESPACE_ELEMENT = "Could not find the element on namespace.";

}
