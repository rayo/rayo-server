package com.rayo.storage.riak;

import org.junit.After;
import org.junit.Before;

import com.rayo.storage.BaseDatastoreTest;

public class RiakDatastoreTest extends BaseDatastoreTest {

	@After
	public void shutdown() throws Exception {
		
		((RiakDatastore)store).removeAllData();
	}
	
	@Before
	public void setup() throws Exception {
		
		store = new RiakDatastore();
		((RiakDatastore)store).init();
	}
	
}
