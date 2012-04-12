package com.rayo.provisioning.rest;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Mock class for rest testing
 * 
 * @author martin
 *
 */
@SuppressWarnings("serial")
public class RestServlet extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest request, 
			HttpServletResponse response) throws ServletException, IOException {

		String restKey = getRestKey(request);
		response.setContentType("application/json");
		String json = RestTestStore.getJson(restKey);
		if (json !=  null) {
			response.getOutputStream().write(json.getBytes());
		} else {
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
		}

		response.getOutputStream().flush();
	}

	private String getRestKey(HttpServletRequest request) {

		String url = request.getRequestURL().toString();
		return url.substring(url.indexOf("/rest")+5);
	}
}
