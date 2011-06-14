package com.tropo.server.cdr;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;

import com.tropo.core.cdr.Cdr;
import com.tropo.core.cdr.CdrException;
import com.tropo.server.jmx.FileCdrMXBean;
import com.voxeo.logging.Loggerf;

@ManagedResource(objectName = "com.tropo:Type=Admin,name=File CDR", description = "Filebased CDR storage")
public class FileCdrStorageStrategy implements CdrStorageStrategy, FileCdrMXBean {

	private Loggerf logger = Loggerf.getLogger(FileCdrStorageStrategy.class);
	
	private String path;
	private boolean append = true;
	
	private OutputStream out;

	// Lock to enable File settings hot replacement
	private final ReadWriteLock lock = new ReentrantReadWriteLock();
	
	public void init() throws IOException {
	
		logger.info("Initializing Filebased CDR storage");
		
		if (path == null) {
			throw new IllegalStateException("Don't know where to write CDRs. You need to set the path variable.");
		}
		
		File file = new File(path);
		if (!file.exists()) {
			File parent = file.getParentFile();
			if (!parent.exists()) {
				parent.mkdirs();
			}
		}
		
		out = new BufferedOutputStream(new FileOutputStream(file,append));
		
	}
	
	public void shutdown() {
		
		logger.info("Shutting down filebased CDR storage");
		if (out != null) {
			try {
				out.close();
			} catch (IOException e) {
				logger.error(e.getMessage(),e);
			}
		}
	}
	
	@Override
	public void store(Cdr cdr) throws CdrException {
		
		try {
			lock.readLock().lock();
			out.write(cdr.toString().getBytes());
			out.flush();
		} catch (IOException e) {
			throw new CdrException(e);
		} finally {
			lock.readLock().unlock();
		}
	}
	
	@Override
	@ManagedOperation(description = "Change CDRs audit file")
	public void changeFile(String filename) {
		
		logger.info("Changing CDRs audit file to %s", filename);
		
		String oldPath = this.path;
		try {
			// We do not let the system to do any logging while we are changing the config
			lock.writeLock().lock();
			this.path = filename;
			init();
		} catch (IOException ioe) {
			logger.error("Could not replace File storage configuration settings. Rolling back to previous setup");
			this.path = oldPath;
		} finally {
			lock.writeLock().unlock();
		}
	}

	public void setPath(String path) {
		this.path = path;
	}
	
	public String getPath() {
		
		return path;
	}

	public void setAppend(boolean append) {
		this.append = append;
	}
}
