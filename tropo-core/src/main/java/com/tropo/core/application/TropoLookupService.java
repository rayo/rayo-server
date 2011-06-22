package com.tropo.core.application;

import java.util.Map;

public interface TropoLookupService {
	Map<String,String> lookup (Object key);
}
