package com.rayo.storage.memory;

import org.junit.Before;

import com.rayo.storage.BaseDatastoreTest;

public class InMemoryDatastoreTest extends BaseDatastoreTest {

	@Before
	public void setup() {
		
		store = new InMemoryDatastore();
	}
}
