package com.tropo.core.recording;

import java.io.File;
import java.io.IOException;
import java.net.URI;

public interface StorageService {

	/**
	 * <p>This method receives a file and stores it somewhere else. 
	 * Finally it returns an URI to the resource that has been created.</p>
	 * <p>Possible implementations of storage services are file systems, Web Services, 
	 * Amazon S3, etc.</p>
	 * 
	 * @param file File that has the binary content that needs to be stored
	 * 
	 * @return URI that can be used for accessing to the resource that has been created 
	 * 
	 * @throws IOException If there is any error while storing the file
	 */
	public URI store(File file) throws IOException;
}
