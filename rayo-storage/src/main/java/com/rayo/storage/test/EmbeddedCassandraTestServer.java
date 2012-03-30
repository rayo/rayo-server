package com.rayo.storage.test;

import org.junit.BeforeClass;

public class EmbeddedCassandraTestServer {

	private static EmbeddedServerHelper embeddedCassandraServer;

    @BeforeClass
    public static void start() throws Exception {

    	if (embeddedCassandraServer == null) {
    		embeddedCassandraServer = new EmbeddedServerHelper();
    		embeddedCassandraServer.setup();
    	}
    }  
}
