package com.rayo.server.recording;

import java.io.File;
import java.io.IOException;

import com.rayo.core.verb.Record;

/**
 * <p>An interface to define how recordings will be stored locally.</p>
 * 
 * @author martin
 *
 */
public interface LocalStore {

	/**
	 * Creates a recording file.
	 * 
	 * @param recording Recording object
	 * 
	 * @return File The file where the recording will be stored.
	 * @throws IOException If there is any issue while creating the recording
	 */
	File createRecording(Record recording) throws IOException;
}
