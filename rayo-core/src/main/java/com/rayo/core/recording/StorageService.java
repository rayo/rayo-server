package com.rayo.core.recording;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import com.voxeo.moho.Call;
import com.voxeo.moho.Participant;
import com.voxeo.moho.conference.Conference;

public interface StorageService {

	/**
	 * <p>This method receives a file and stores it somewhere else. 
	 * Finally it returns an URI to the resource that has been created.</p>
	 * <p>Possible implementations of storage services are file systems, Web Services, 
	 * Amazon S3, etc.</p>
	 * 
	 * @param file File that has the binary content that needs to be stored
	 * @param participant The target of the record command. Commonly it should be either
	 * an instance of {@link Call} or {@link Conference}. You can use this object to grab
	 * useful information about the context of the recording. 
	 * 
	 * @return URI that can be used for accessing to the resource that has been created 
	 * 
	 * @throws IOException If there is any error while storing the file
	 */
	public URI store(File file, Participant participant) throws IOException;
}
