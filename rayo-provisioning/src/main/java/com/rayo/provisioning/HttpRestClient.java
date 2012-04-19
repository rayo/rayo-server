package com.rayo.provisioning;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;

import com.google.gson.GsonBuilder;
import com.voxeo.logging.Loggerf;

/**
 * <p>A very basic rest client based on Http ComonsComponents.</p>
 * @author martin
 *
 */
public class HttpRestClient {

	private DefaultHttpClient httpClient = new DefaultHttpClient();
	
	private static final Loggerf logger = Loggerf.getLogger(HttpRestClient.class);
	
	private String username;
	private String password;
	
	public HttpRestClient(String username, String password) {
		
		this.username = username;
		this.password = password;
	}
	
	/**
	 * Sends a get request to the given endpoint and returns the response body.
	 * 
	 * @param endpoint Endpoint that has to be fetched
	 * @return String body of the response
	 * @throws RestException If the response code is other than 200
	 */
	public <T> T get(String endpoint, Class<T> clazz) throws RestException, IOException {
		
		try {
			HttpGet getRequest = new HttpGet(endpoint);
			getRequest.addHeader("accept", "application/json");
			
			AuthScope scope = new AuthScope(getRequest.getURI().getHost(), getRequest.getURI().getPort());
			
			httpClient.getCredentialsProvider().setCredentials(
					scope, new UsernamePasswordCredentials(username, password)); 
			httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 3000);
			httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, 3000);
			String result = httpClient.execute(getRequest, new ResponseHandler<String>() {
				
				@Override
				public String handleResponse(HttpResponse response)
						throws ClientProtocolException, IOException {
	
					if (response.getStatusLine().getStatusCode() == 404) {				
						return null;
					}
					if (response.getStatusLine().getStatusCode() != 200) {				
						throw new HttpResponseException(response.getStatusLine().getStatusCode(), "Request error");
					}	
					String result = IOUtils.toString(response.getEntity().getContent());
					
					return result;
				}
			});
			if (result == null) {
				return null;
			}
				
			GsonBuilder builder = new GsonBuilder();
			return builder.create().fromJson(result, clazz);
		} catch (ClientProtocolException e) {
			logger.error(e.getMessage(), e);
			throw new IOException(e);
		}
	}
	
	/**
	 * Disposes all the resources of this client
	 */
	public void shutdown() {
		
		if (httpClient != null) {
			httpClient.getConnectionManager().shutdown();
		}
	}
}
