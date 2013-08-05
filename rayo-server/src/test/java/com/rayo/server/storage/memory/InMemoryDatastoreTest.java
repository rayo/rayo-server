package com.rayo.server.storage.memory;

import org.junit.Before;

import com.rayo.server.storage.BaseDatastoreTest;

public class InMemoryDatastoreTest extends BaseDatastoreTest {

	@Before
	public void setup() {
		
		store = new InMemoryDatastore();
	}
}
