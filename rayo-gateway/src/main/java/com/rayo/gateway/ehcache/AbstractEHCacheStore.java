package com.rayo.gateway.ehcache;

import com.rayo.gateway.store.Store;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.event.CacheEventListenerAdapter;

/**
 * Base class for an EHCache-based store implementation. This {@link Store} 
 * instance will be backed by an EHCache {@link Cache}. 
 * The {@link AbstractEHCacheStore#init()} initializes this store and uses 
 * the {@link CacheManager} instance to load the Cache defined by the cache name
 * attribute.  
 * 
 * @author martin
 *
 */
public abstract class AbstractEHCacheStore extends CacheEventListenerAdapter {

	private String name;	
	private CacheManager cacheManager;
	
	protected Cache cache;
		
	/**
	 * Initializes this Store implementation. It will try to load an EHCache 
	 * cache using the name field as cache's name.
	 */
	public void init() {
		
		cache = cacheManager.getCache(name);
		cache.getCacheEventNotificationService().registerListener(this);
	}
	
	/**
	 * Sets the cache manager for this cache
	 * 
	 * @param cacheManager Cache Manager
	 */
	public void setCacheManager(CacheManager cacheManager) {
		
		this.cacheManager = cacheManager;
	}
	
	/**
	 * Sets the name of the EHCache cache that will back this Store
	 * 
	 * @param name Name of the cache
	 */
	public void setName(String name) {
		
		this.name = name;
	}
	
	/**
	 * Returns the name of the EHCache cache that backs up this store
	 * 
	 * @return String name of the cache
	 */
	public String getName() {
		
		return name;
	}
}
