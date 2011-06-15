package com.tropo.core.application;


public interface ApplicationLookupService<T extends Application> {
	public T lookup (Object key);
}
