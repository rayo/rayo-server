package com.rayo.gateway.memory;

import org.junit.Before;

import com.rayo.gateway.BaseDatastoreTest;

public class InMemoryDatastoreTest extends BaseDatastoreTest {

	@Before
	public void setup() {
		
		store = new InMemoryDatastore();
	}
}
