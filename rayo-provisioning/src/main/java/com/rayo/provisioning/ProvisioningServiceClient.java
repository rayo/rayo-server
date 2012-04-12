package com.rayo.provisioning;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.rayo.storage.model.Application;
import com.tropo.provisioning.rest.model.AddressDto;
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
			if (dto == null) {
				return null;
			}
			String features = getFeatures(accountId);
			return toApplication(dto, accountId, features);
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			return null;
		}
	}

	private Application toApplication(ApplicationDto dto, String accountId, String features) {
		
		Application application = new Application(dto.getId(), 
				dto.getVoiceUrl().toString(), dto.getPlatform().toString());
		application.setName(dto.getName());
		application.setPermissions(features);
		application.setAccountId(accountId);
		
		return application;
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
	
	/**
	 * Returns the list of addresses associated with the given application id
	 *  
	 * @param appId Id of the application
	 * @return List<String> List of addresses for the given application or an empty 
	 * list if no addresses can be found
	 */
	@SuppressWarnings("unchecked")
	public List<String> getAddresses(String appId) {
		
		try {			
			AddressDto[] dtos = httpClient.get(endpoint + "/applications/" + appId + "/addresses", AddressDto[].class);
			if (dtos == null) {
				return Collections.EMPTY_LIST;
			}
			return fillAddresses(dtos);
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			return Collections.EMPTY_LIST;
		}	
	}

	private List<String> fillAddresses(AddressDto[] dtos) {
		
		List<String> addresses = new ArrayList<String>();
		for (AddressDto dto: dtos) {
			String value = dto.getValue();
			if (dto.getType().equalsIgnoreCase("sip")) {
				value = "sip:" + value;
			}
			addresses.add(value);
		}
		return addresses;
	}
	
	@SuppressWarnings("unchecked")
	/**
	 * Returns the list of addresses owned by the given account id
	 * 
	 * @param accountId Id of the account
	 * @return List<String> List of addresses for the given account id or an empty 
	 * list if no addresses can be found
	 */
	public List<String> getAddressesForAccount(String accountId) {
		
		try {			
			AddressDto[] dtos = httpClient.get(endpoint + "/users/" + accountId + "/addresses", AddressDto[].class);
			if (dtos == null) {
				return Collections.EMPTY_LIST;
			}
			return fillAddresses(dtos);
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			return Collections.EMPTY_LIST;
		}	
	}
	
	
	/**
	 * Returns the list of applications owned by the given account id
	 *  
	 * @param accountId Id of the account
	 * @return List<String> List of applications owned by the given application or an empty 
	 * list if no applications can be found
	 */
	@SuppressWarnings("unchecked")
	public List<Application> getApplications(String accountId) {
		
		try {
			List<Application> applications = new ArrayList<Application>();
			ApplicationDto[] dtos = httpClient.get(
					endpoint + "/users/" + accountId + "/applications", ApplicationDto[].class);
			if (dtos == null) {
				return Collections.EMPTY_LIST;
			}
			String features = getFeatures(accountId);
			for(ApplicationDto dto: dtos) {
				applications.add(toApplication(dto, accountId, features));
			}
			return applications;
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			return Collections.EMPTY_LIST;
		}	
	}
}
