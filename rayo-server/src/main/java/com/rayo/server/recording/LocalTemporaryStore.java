package com.rayo.server.recording;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.media.mscontrol.Value;
import javax.media.mscontrol.mediagroup.FileFormatConstants;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.math.RandomUtils;

import com.rayo.core.verb.Output;
import com.rayo.core.verb.Record;
import com.voxeo.logging.Loggerf;

/**
 * <p>A local temporary Store to store recordings. This implementation will 
 * store the recordings in the form /BASE_FOLDER/day/hour/second/rayo12345.wav</p>
 * 
 * <p>This store is not persistent. Recordings will be self-deleted after the 
 * number of seconds specified in the deleteAfter attribute.</p> 
 * 
 * @author martin
 *
 */
public class LocalTemporaryStore implements LocalStore {

	private final static Loggerf log = Loggerf.getLogger(LocalTemporaryStore.class);
	private final static SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd/hh/mm/");
	
	private long deleteAfter = 60*60*24*1000; // By default, delete files after one day
	private String baseFolder = "/tmp/recordings"; // By deafult store recordings in /tmp/recordings
	private long cleanupInterval = 60*60*24; // Interval to check if files should be deleted. By default wait 1 day.
	private int maxAttempts = 20; // Max attempts to get a valid file name
	
	private ScheduledExecutorService scheduledExecutor;
	private ScheduledFuture<?> future;
	
	public void init() {
		
		if (future != null) {
			future.cancel(true);
		}
		
		future = scheduledExecutor.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				cleanup();
			}
		}, cleanupInterval, cleanupInterval, TimeUnit.SECONDS);
	}
	
	private void cleanup() {
	
		log.info("Cleaning up task started.");
		File root = new File(baseFolder);
		checkToClean(root, System.currentTimeMillis());
	}
	
	private void checkToClean(File file, long now) {

		log.trace("Checking to clean: " + file.getAbsolutePath());
		if (file.isFile()) {
			if (now - file.lastModified() > deleteAfter) {
				log.trace("Deleting: " + file.getAbsolutePath());
				try {
					FileUtils.forceDelete(file);
				} catch (IOException e) {
					log.error("Could not delete file %s.", file.getAbsolutePath());
				}
			}
		} else {
			for(File child: file.listFiles()) {
				checkToClean(child, now);
			}
		}
	}
	
	@Override
	public File createRecording(Record recording) throws IOException {

		File f = null;
		int i = 0;
		do {
			f = new File(getTentativePath(recording));
			
		} while(f.exists() && i < maxAttempts);
		if (i > maxAttempts) {
			throw new IOException("Could not create file at " + 
					baseFolder + ". Max attempts (" + maxAttempts + 
					") to create recording exceeded.");
		}
		if (!f.getParentFile().exists()) {
			if (!f.getParentFile().mkdirs()) {
				throw new IOException("Could not create directory structure for " + f.getAbsolutePath());
			}
		}
		if (!f.createNewFile()) {
			throw new IOException("Could not create file " + f.getAbsolutePath());
		}
		return f;
	}
	
	private String getTentativePath(Record recording) {
		
		String path = baseFolder;
		if (!baseFolder.endsWith("/")) path+="/";
		return path + formatter.format(new Date()) + "rayo" + 
			RandomUtils.nextInt(900000)+getExtensionFromFormat(recording);
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
	
	public long getDeleteAfter() {
		return deleteAfter;
	}

	public void setDeleteAfter(long deleteAfter) {
		this.deleteAfter = deleteAfter * 1000;
	}

	public String getBaseFolder() {
		return baseFolder;
	}

	public void setBaseFolder(String baseFolder) {
		this.baseFolder = baseFolder;
	}

	public void setScheduledExecutor(ScheduledExecutorService scheduledExecutor) {
		this.scheduledExecutor = scheduledExecutor;
	}

	public long getCleanupInterval() {
		return cleanupInterval;
	}

	public void setCleanupInterval(long cleanupInterval) {
		this.cleanupInterval = cleanupInterval;
	}
}
