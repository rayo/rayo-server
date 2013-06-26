package com.rayo.server.ims;

import com.rayo.core.CallDirection;
import com.voxeo.moho.Call;

/**
 * <p>A call direction resolver would receive a call object and return the 
 * directionality for that call. This interface can be overwritten to provide 
 * different strategies for different IMS implementations.</p>
 * 
 * @author martin
 *
 */
public interface CallDirectionResolver {

	/**
	 * Resolves the directionality for a call on an IMS scenario.
	 * 
	 * @param call Call that we want to get the directionality of.
	 * 
	 * @return {@link CallDirection} Directionality
	 */
    CallDirection resolveDirection(Call call);
}
