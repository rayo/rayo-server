package com.tropo.core.validation;

public class Messages {

	public static final String MISSING_PROMPT_ITEMS = "You need to send at least an audio or SSML item.";
	public static final String MISSING_CHOICES = "You need to provide at least a valid choice element.";
	public static final String INVALID_RECOGNIZER = "Invalid recognizer.";
	public static final String MISSING_TO = "Missing required attribute 'to'";
    public static final String MISSING_FROM = "Missing required attribute 'from'";
	public static final String MISSING_ROOM_NAME = "Missing room name.";
	public static final String MISSING_CHOICES_CONTENT_OR_URL = "For choices, either 'url' or inline choices text is required (not both)";
	public static final String MISSING_CHOICES_CONTENT_TYPE = "'content-type' is required when specifying choices contents inline";
	public static final String MISSING_SSML = "Missing SSML content.";
	public static final String MISSING_DESTINATION = "Missing destination.";
    public static final String MISSING_COMPLETE_REASON = "Complete event with no reason!";

    public static final String INVALID_INPUT_MODE = "Invalid input mode.";
	public static final String INVALID_URI = "You have submitted an invalid URI.";
	public static final String INVALID_BOOLEAN = "You have submitted an invalid boolean value.";
	public static final String INVALID_TIMEOUT = "Invalid timeout specified.";
	public static final String INVALID_CONFIDENCE = "Invalid confidence specified.";
	public static final String INVALID_TERMINATOR = "Invalid terminator character.";
	public static final String INVALID_CONFIDENCE_RANGE = "Confidence must be a value between 0 and 1.";
	public static final String INVALID_REASON = "Invalid reason";
	
	public static final String UNKNOWN_NAMESPACE_ELEMENT = "Could not find the element on namespace.";

}
