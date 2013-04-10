package com.rayo.server.ameche;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.lang.RandomStringUtils;

import com.rayo.core.recording.StorageService;
import com.voxeo.moho.Participant;

public class AmecheStorageService implements StorageService {

	private static final String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ123456789";

	private String baseUrl;
	private Map<String, String> recordings = new ConcurrentHashMap<String, String>();
	
	private ReadWriteLock recordingsLock = new ReentrantReadWriteLock();
	
	@Override
	public URI store(File file, Participant participant) throws IOException {
		
		Lock lock = recordingsLock.writeLock();
		lock.lock();
		try {
			String key = shorten(file);
			recordings.put(key, file.getAbsolutePath());
			return new URI(baseUrl + "/" + key);
		} catch (URISyntaxException e) {
			throw new IOException(e);
		} finally {
			lock.unlock();
		}
	}

	public void setBaseUrl(String url) {
		
		baseUrl = url;
	}
	
	public File getFile(String key) throws IOException {
		
		Lock lock = recordingsLock.readLock();
		lock.lock();
		try {
			String path = recordings.get(key);
			File file = new File(path);
			if (!file.exists()) {
				throw new FileNotFoundException("Could not find any recording for key " + key);				
			}
			return file;
		} finally {
			lock.unlock();
		}
	}
	
	private String shorten(File file) {
		
		String key = null;
		int size = 4;
		do {
			key = RandomStringUtils.random(size, chars);
		} while(recordings.get(key) != null);
		return key;
	}
}
