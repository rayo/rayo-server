package com.rayo.storage.cassandra;


/**
 * Cassandra priming interface. 
 * 
 * @author martin
 *
 */
public interface CassandraPrimer {

	public void prime(CassandraDatastore datastore) throws Exception;

}
