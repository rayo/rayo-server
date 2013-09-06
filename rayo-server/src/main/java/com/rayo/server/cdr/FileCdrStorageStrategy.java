package com.rayo.server.cdr;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;

import com.rayo.core.cdr.Cdr;
import com.rayo.core.cdr.CdrException;
import com.rayo.server.jmx.FileCdrMXBean;
import com.voxeo.logging.Loggerf;

@ManagedResource(objectName = "com.rayo:Type=Admin,name=File CDR", description = "Filebased CDR storage")
public class FileCdrStorageStrategy implements CdrStorageStrategy, FileCdrMXBean {

	private Loggerf logger = Loggerf.getLogger(FileCdrStorageStrategy.class);
	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
	
	// will keep this one for legacy reasons
	private String path;
	private String baseFolder = "/tmp/cdrs";
	
	private boolean append = true;
	private String lastUsedPath;
	
	private OutputStream out;

	// Lock to enable File settings hot replacement
	private final ReadWriteLock lock = new ReentrantReadWriteLock();
	
	public void init() throws IOException {
	
		logger.info("Initializing Filebased CDR storage");
		
		if (baseFolder == null && path == null) {
			throw new IllegalStateException("Don't know where to write CDRs. You need to set the baseFolder variable.");
		}
		
		if (baseFolder != null) {
			File f = new File(baseFolder);
			if (!f.exists()) {
				boolean result = f.mkdirs();
				if (!result) {
					throw new IllegalStateException(String.format("Could not create CDRs folder %s", f.getAbsolutePath()));
				}
			}
			// unset legacy stuff
			path = null;
		} else {
			File file = new File(path);
			if (!file.exists()) {
				File parent = file.getParentFile();
				if (!parent.exists()) {
					parent.mkdirs();
				}
			}
			out = new BufferedOutputStream(new FileOutputStream(file,append));
		}	
		lastUsedPath = null;
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
			if (baseFolder != null) {
				out = getFileStream(cdr);
			}
			lock.readLock().lock();
			out.write(cdr.toString().getBytes());
			out.flush();
		} catch (IOException e) {
			throw new CdrException(e);
		} finally {
			lock.readLock().unlock();
		}
	}
	
	private synchronized OutputStream getFileStream(Cdr cdr) {

		OutputStream oldOut = out;
		path = sdf.format(new Date(cdr.getStartTime()));
		if (!path.equals(lastUsedPath)) {
			File f = new File(baseFolder + "/" + path + ".xml");
			if (!f.exists()) {
				if (out != null) {
					try {
						out.close();
					} catch (IOException ioe) {
						logger.error(ioe.getMessage(), ioe);
					}
				}
				try {
					out = new BufferedOutputStream(new FileOutputStream(f));
				} catch (IOException ioe) {
					logger.error(ioe.getMessage(), ioe);
					out = oldOut;
				}
			}
		} 
		lastUsedPath = path;
		return out;
		
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

	public String getBaseFolder() {
		return baseFolder;
	}

	public void setBaseFolder(String baseFolder) {
		
		if (baseFolder.endsWith("/")) {
			baseFolder = baseFolder.substring(0, baseFolder.length()-1);
		}
		this.baseFolder = baseFolder;
	}
}
