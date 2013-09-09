package com.rayo.server.cdr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang.math.RandomUtils;
import org.apache.commons.lang.time.DateUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.rayo.core.cdr.Cdr;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations="/rayo-context-activemq-cdr.xml")
public class FileCdrStorageStrategyTest {

	@Autowired
	FileCdrStorageStrategy storage;
	
	@Test
	public void testFolderIsCreatedOnInit() throws Exception {
		
		storage.setBaseFolder("target/test" + RandomUtils.nextInt(10000));
		File folder = new File(storage.getBaseFolder());
		folder.deleteOnExit();
		assertFalse(folder.exists());
		storage.init();
		assertTrue(folder.exists());
	}
	
	
	@Test
	public void testCdrFileIsCreated() throws Exception {
		
		storage.setBaseFolder("target/test" + RandomUtils.nextInt(10000));
		File folder = new File(storage.getBaseFolder());
		folder.deleteOnExit();
		storage.init();
		assertEquals(folder.listFiles().length, 0);
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		Date now = new Date();
		Cdr cdr = new Cdr();
		cdr.setStartTime(now.getTime());
		storage.store(cdr);

		String expectedFile = sdf.format(now) + ".xml"; 
		assertEquals(folder.listFiles().length, 1);
		assertTrue(folder.listFiles()[0].getName().endsWith(expectedFile));
	}
	
	
	@Test
	public void testCdrFilesRollOver() throws Exception {
		
		storage.setBaseFolder("target/test" + RandomUtils.nextInt(10000));
		File folder = new File(storage.getBaseFolder());
		folder.deleteOnExit();
		assertFalse(folder.exists());
		storage.init();
		assertEquals(folder.listFiles().length, 0);		
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		Date now = new Date();
		Cdr cdr = new Cdr();
		cdr.setStartTime(now.getTime());
		storage.store(cdr);
		String expectedFile = sdf.format(now) + ".xml"; 
		assertEquals(folder.listFiles().length, 1);
		assertTrue(folder.listFiles()[0].getName().endsWith(expectedFile));

		Date tomorrow = DateUtils.addDays(now, 1);
		cdr = new Cdr();
		cdr.setStartTime(tomorrow.getTime());
		storage.store(cdr);
		expectedFile = sdf.format(tomorrow) + ".xml"; 
		assertEquals(folder.listFiles().length, 2);
		assertTrue(folder.listFiles()[0].getName().endsWith(expectedFile) ||
				   folder.listFiles()[1].getName().endsWith(expectedFile));
	}
	
	@Test
	public void testAppendCdrs() throws Exception {
		
		storage.setBaseFolder("target/test" + RandomUtils.nextInt(10000));
		File folder = new File(storage.getBaseFolder());
		folder.deleteOnExit();
		storage.init();
		
		Date now = new Date();
		Cdr cdr = new Cdr();
		cdr.setStartTime(now.getTime());
		storage.store(cdr);
		assertEquals(folder.listFiles().length, 1);
		long length = folder.listFiles()[0].length();

		cdr = new Cdr();
		cdr.setStartTime(now.getTime());
		storage.store(cdr);
		assertEquals(folder.listFiles().length, 1);
		assertTrue(folder.listFiles()[0].length() > length);
	}
}
