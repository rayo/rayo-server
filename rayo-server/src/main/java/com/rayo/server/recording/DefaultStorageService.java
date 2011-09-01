package com.rayo.server.recording;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import com.rayo.core.recording.StorageService;
import com.voxeo.moho.Participant;

/**
 * Dummy storage service. It will just return an URI to the actual file
 * 
 * @author martin
 *
 */
public class DefaultStorageService implements StorageService {

	@Override
	public URI store(File file, Participant participant) throws IOException {
		
		return file.toURI();
	}
}
