package com.rayo.server.storage;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.media.mscontrol.mediagroup.FileFormatConstants;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.rayo.core.verb.Record;
import com.rayo.server.recording.LocalTemporaryStore;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations="/rayo-context-activemq-cdr.xml")
public class LocalTemporaryStoreTest {
	
	private final static SimpleDateFormat formatter = new SimpleDateFormat("yyMMdd/hh/mm/");

    @Autowired
    private LocalTemporaryStore store;

	@Test
	public void testFileCreatedDefault() throws Exception {
		
		Record record = new Record();
		File f = store.createRecording(record);
		assertTrue(f.exists());
		assertTrue(f.getAbsolutePath().endsWith(".wav"));
		assertTrue(f.getAbsolutePath().startsWith("/tmp/recordings"));
	}
	
	@Test
	public void testFileCreatedCustom() throws Exception {
		
		String datePath = formatter.format(new Date());
		Record record = new Record();
		record.setFormat("MP3");
		store.setBaseFolder("/tmp/recordings2");
		File f = store.createRecording(record);
		
		assertTrue(f.getAbsolutePath().endsWith(".mp3"));
		System.out.println("Path: " + f.getAbsolutePath() + " vs. " + "/tmp/recordings2/"+datePath);
		assertTrue(f.getAbsolutePath().startsWith("/tmp/recordings2/"+datePath));
	}
	
	@Test
	public void testFileCleanup() throws Exception {
		
		Record record = new Record();
		store.setBaseFolder("/tmp/recordings");
		store.setDeleteAfter(1); // delete after 1 second
		store.setCleanupInterval(2);
		store.init();
		
		File f = store.createRecording(record);
		
		FileOutputStream fos = new FileOutputStream(f);
		fos.write("test".getBytes());
		fos.flush();
		fos.close();		
		assertTrue(f.exists());
		
		Thread.sleep(3000);
		
		assertFalse(f.exists());
	}

	public void setStore(LocalTemporaryStore store) {
		this.store = store;
	}
	
	
}
