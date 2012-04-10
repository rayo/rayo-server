package com.rayo.provisioning;

import com.rayo.storage.model.Application;
import com.tropo.provisioning.rest.model.ApplicationDto;
import com.tropo.provisioning.rest.model.UserFeatureDto;
import com.voxeo.logging.Loggerf;

/**
 * <p>This class is in charge of querying the provisioning service.</p>
 *  
 * @author martin
 *
 */
public class ProvisioningServiceClient extends HttpRestClient {

	private static final Loggerf logger = Loggerf.getLogger(ProvisioningServiceClient.class);
	
	private String endpoint;
	
	private HttpRestClient httpClient;
	
	/**
	 * Instantiates the provisioning service client
	 * 
	 * @param endpoint Endpoint where the provisioning server is located at
	 */
	public ProvisioningServiceClient(String endpoint) {
		
		if (endpoint == null) {
			throw new IllegalStateException("Could not create provisioning service client. Endpoint is null");
		}
		this.endpoint = endpoint;
		this.httpClient = new HttpRestClient();
	}
	
	/**
	 * Queries the provisioning server for the given application id
	 * 
	 * @param accountId Id of the account that owns the app
	 * @param appId Id of the application
	 * @return {@link Application} corresponding to the given id or <code>null</code> if no 
	 * application can be found
	 */
	public Application getApplication(String accountId, String appId) {
		
		try {
			ApplicationDto dto = httpClient.get(endpoint + "/applications/" + appId, ApplicationDto.class);
			Application application = new Application(dto.getId(), 
					dto.getVoiceUrl().toString(), dto.getPlatform().toString());
			application.setName(dto.getName());
			application.setPermissions(getFeatures(accountId));
			application.setAccountId(accountId);
			
			return application;
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			return null;
		}
	}	
	
	/**
	 * Returns the set of features also known as ODF for a given account
	 * 
	 * @param accountId Id of the account
	 * @return String features
	 */
	public String getFeatures(String accountId) {
		
		try {
			UserFeatureDto[] features = httpClient.get(endpoint + "/users/" + accountId + "/features", UserFeatureDto[].class);
			StringBuilder flags = new StringBuilder();
			for(UserFeatureDto feature: features) {
				flags.append(feature.getFeatureFlag());
			}
			
			return flags.toString();
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			return null;
		}			
	}
}
