package com.tropo.server.cdr;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.tropo.core.cdr.Cdr;
import com.tropo.core.cdr.CdrException;
import com.voxeo.logging.Loggerf;

public class FileCdrStorageStrategy implements CdrStorageStrategy {

	private Loggerf logger = Loggerf.getLogger(FileCdrStorageStrategy.class);
	
	private String path;
	private boolean append = true;
	
	private OutputStream out;

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
			StringBuilder builder = new StringBuilder(String.format("<cdr callId=\"%s\">", cdr.getCallId()));
			for (String element: cdr.getTranscript()) {
				builder.append(element);
			}
			builder.append("</xml>\n");
			out.write(builder.toString().getBytes());
			out.flush();
		} catch (IOException e) {
			throw new CdrException(e);
		}
	}

	public void setPath(String path) {
		this.path = path;
	}

	public void setAppend(boolean append) {
		this.append = append;
	}
}
