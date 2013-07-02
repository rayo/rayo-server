package com.rayo.server.ameche;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.context.support.XmlWebApplicationContext;

import com.rayo.core.CallDirection;
import com.rayo.core.recording.StorageService;
import com.rayo.server.CallActor;
import com.rayo.server.CallRegistry;
import com.rayo.server.CommandHandler;
import com.rayo.server.Server;
import com.rayo.server.Transport;
import com.rayo.server.exception.ErrorMapping;
import com.rayo.server.exception.ExceptionMapper;
import com.rayo.server.ims.CallDirectionResolver;
import com.voxeo.logging.Loggerf;

@SuppressWarnings("serial")
public class AmecheServlet extends HttpServlet implements Transport {

    private static final Loggerf log = Loggerf.getLogger(AmecheServlet.class);
    
    // Config
    private CommandHandler commandHandler;
    private AppInstanceResolver appInstanceResolver;
    private AppInstanceEventDispatcher appInstanceEventDispatcher;
    private AmecheCallRegistry amecheCallRegistry;
    private AmecheMixerRegistry amecheMixerRegistry;
    private AmecheAuthenticationService amecheAuthenticationService;
    private CallRegistry callRegistry;
    private AmecheStorageService amecheStorageService;
    private CallDirectionResolver callDirectionResolver;
    private ExceptionMapper exceptionMapper;
    
    @Override
    public void init() throws ServletException {
        
        WebApplicationContext wac = WebApplicationContextUtils.getRequiredWebApplicationContext(getServletContext());

        XmlWebApplicationContext httpTransportContext = new XmlWebApplicationContext();
        httpTransportContext.setServletContext(getServletContext());
        httpTransportContext.setParent(wac);
        httpTransportContext.setConfigLocation("/WEB-INF/" + getServletName() + ".xml");
        httpTransportContext.refresh();

        appInstanceEventDispatcher = (AppInstanceEventDispatcher) httpTransportContext.getBean("appInstanceEventDispatcher");
        appInstanceResolver = (AppInstanceResolver) httpTransportContext.getBean("appInstanceResolver");
        amecheCallRegistry = (AmecheCallRegistry) httpTransportContext.getBean("amecheCallRegistry");
        amecheMixerRegistry = (AmecheMixerRegistry) httpTransportContext.getBean("amecheMixerRegistry");
        amecheAuthenticationService = (AmecheAuthenticationService) httpTransportContext.getBean("amecheAuthenticationService");
        callRegistry = (CallRegistry) httpTransportContext.getBean("callRegistry");
        amecheStorageService = (AmecheStorageService)httpTransportContext.getBean("amecheStorageService");
        callDirectionResolver = (CallDirectionResolver)httpTransportContext.getBean("callDirectionResolver");
        exceptionMapper = (ExceptionMapper)httpTransportContext.getBean("exceptionMapper");
                
        // Replace Rayo's default Storage service with Ameche's one
        @SuppressWarnings("unchecked")
		Collection<StorageService> storageServices = (Collection<StorageService>)httpTransportContext.getBean("storageServices");
        storageServices.clear();
        storageServices.add(amecheStorageService);
        
        Server server = (Server) httpTransportContext.getBean("rayoServer");
        server.addTransport(this);
        this.commandHandler = server;
        
        String rayoUrl = getServletConfig().getInitParameter("rayoUrl");
        appInstanceEventDispatcher.setRayoUrl(rayoUrl);
    }

    @Override
    public boolean callEvent(String callId, String componentId, Element event) throws Exception {

        AmecheCall machine = null;
        boolean result = false;
        log.debug("Handling event: %s", event.getName());
        // New Call
        if (event.getName().equals("offer")) {

            // Lookup App Instance Endpoints
            List<AppInstance> apps = appInstanceResolver.lookup(event, resolveDirection(callId));
            if (apps.size() != 0) {
                // Make and register a new ameche call handler 
            	machine = createAmecheCall(callId, event, apps);
                amecheCallRegistry.registerCall(callId, machine);
                machine.offer();
                result = true;
            }
            else {
            	log.debug("There is no Ameche instances interested on call [%s]", callId);
            }
        } else {
            // Existing Call
        	try {
	            // Lookup ameche call handler
	            machine = amecheCallRegistry.getCall(callId);
	            if (machine != null) { 
	                machine.onEvent(event, callId, componentId);
	                result = true;
	            } else {
	            	// is it a mixer?
	            	AmecheMixer mixer = amecheMixerRegistry.getMixer(callId);
	            	if (mixer != null) {
	            		mixer.onEvent(event, callId, componentId);
	            		result = true;
	            	} else {
	            		log.warn("Could not find an Ameche Call registered for callId %s", callId);
	            	}
	            }
        	} finally {
	            // Clean up event machine when the call ends
	            if (event.getName().equals("end")) {
	                amecheCallRegistry.unregisterCall(callId);
	                amecheMixerRegistry.unregisterMixerIfNecessary(callId);
	                amecheAuthenticationService.unregisterCall(callId);
	            }
        	}
        }

        return result;
    }
    
    private AmecheCall createAmecheCall(String callId, Element event, List<AppInstance> apps) {
    	
    	String token = amecheAuthenticationService.generateToken(callId);
    	AmecheCall call = new AmecheCall(callId, token, event, apps);
    	call.setAmecheCallRegistry(amecheCallRegistry);
    	call.setAmecheAuthenticationService(amecheAuthenticationService);
    	call.setAppInstanceEventDispatcher(appInstanceEventDispatcher);
    	call.setCommandHandler(commandHandler);
    	return call;
	}
    
    private AmecheMixer createAmecheMixer(String mixerName, String callId) {
    
		AmecheMixer mixer = new AmecheMixer(mixerName);
		mixer = amecheMixerRegistry.registerMixer(callId, mixer);
		mixer.setAmecheCallRegistry(amecheCallRegistry);
		mixer.setCommandHandler(commandHandler);
		return mixer;
    }
    
	@Override
    public boolean mixerEvent(String mixerId, Collection<String> participants, Element body) throws Exception {
        
    	return false;
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        
        ServletInputStream is = req.getInputStream();
        
        String body = IOUtils.toString(is);
        log.debug("(i) %s", body);
        log.debug("Processing Ameche request");
        
        SAXReader reader = new SAXReader();
        
        try {
            
            Element command = reader.read(new StringReader(body)).getRootElement();
            
            String callId = req.getHeader("call-id");
            String componentId = req.getHeader("component-id");
            String appInstanceId = req.getHeader("app-instance-id");
            String mixerName = req.getHeader("mixer-name");
            
            if(mixerName != null) {
            	log.debug(mixerName);
            }
            
            if(callId == null) {
                log.warn("Missing call-id header");
                resp.setStatus(400, "Missing call-id header");
                return;
            }
            
            if (amecheAuthenticationService.isTokenAuthEnabled()) {
            	String authToken = req.getHeader("auth-token");
            	if (!amecheAuthenticationService.isValidToken(callId, authToken)) {
            		log.error("Invalid auth token: [%s] for call id [%s] ", authToken, callId);
            		resp.setStatus(404, "Invalid auth token");
            		return;
            	}
            }
            
            if(appInstanceId == null) {
                log.warn("Missing app-instance-id header");
                resp.setStatus(400, "Missing app-instance-id header");
                return;
            }
            
            Element result = null;
            if (mixerName == null) { 
            	AmecheCall call = amecheCallRegistry.getCall(callId);
            
	            if(call == null) {
	                log.warn("Received command for unknown call: %s", command.asXML());
	                resp.setStatus(404);
	                return;
	            }
	            
	            if (command.getName().equals("ping")) {
	            	// was just pinging
	                resp.setContentType("application/xml; charset=utf-8");
	            	String response = "<ok/>";
	            	log.debug("Sending response to Ameche: [%s]", response);
	                byte[] bytes = response.getBytes("utf-8");
	                resp.setStatus(200);
	                resp.setContentLength(bytes.length);
	                resp.getOutputStream().write(bytes);
	            	return;
	            }
            
	            // Send command to call's event machine
	            result = call.onCommand(appInstanceId, callId, componentId, command).get();
            } else {
            	AmecheMixer mixer = amecheMixerRegistry.getMixer(mixerName);
            	if (mixer == null) {
            		mixer = createAmecheMixer(mixerName, callId);
            	}
            	result = mixer.onCommand(appInstanceId, mixerName, callId, componentId, command).get();
            }
	            
            resp.setContentType("application/xml; charset=utf-8");
            
            if(result != null) {
            	String response = ((Element)result).asXML();
            	log.debug("Sending response to Ameche: [%s]", response);
                byte[] bytes = response.getBytes("utf-8");
                resp.setStatus(200);
                resp.setContentLength(bytes.length);
                resp.getOutputStream().write(bytes);
            }
            else {
            	log.debug("Sending 203 back to Ameche.");
                resp.setStatus(203);
            }
            
        }
        catch (DocumentException e) {
            log.error("Failed to parse Rayo command", e);
            resp.setStatus(500);
        } catch (ExecutionException ee) {
            log.error("Failed to process command", ee);
            ErrorMapping error = exceptionMapper.toXmppError((Exception)ee.getCause());
            resp.setHeader("rayo-error", error.getText());
            resp.sendError(error.getHttpCode(), error.getText());        	
        } catch (Exception e) {
            log.error("Failed to process command", e);
            resp.setHeader("rayo-error", e.getMessage());
            resp.sendError(500, e.getMessage());
        }
    }

	@Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
    		throws ServletException, IOException {

    	String uri = req.getRequestURI();
    	int lastSlash = uri.lastIndexOf("/");
    	if (lastSlash != -1 && lastSlash != uri.length()-1) {
    		if (uri.substring(0, lastSlash).endsWith("/recordings")) {
    			String key = uri.substring(lastSlash+1);
        		File file = amecheStorageService.getFile(key);
        		doStream(resp, file);    			
    		}
    	}
    }
    
    private void doStream(HttpServletResponse resp, File file) throws IOException {
    	
    	FileInputStream in = null;
    	try {
    		in = new FileInputStream(file);
	    	String mimeType = getMimeType(file.getAbsolutePath());
	    	byte[] bytes = new byte[4096];
	    	int bytesRead;
	
	    	resp.setContentType(mimeType);
	
	    	while ((bytesRead = in.read(bytes)) != -1) {
	    	    resp.getOutputStream().write(bytes, 0, bytesRead);
	    	}
    	} finally {	    
    		if (in != null) {
    			in.close();
    		}
	    	resp.getOutputStream().close();
    	}
	}

	private CallDirection resolveDirection(String callId) {
    	
    	CallActor<?> actor = callRegistry.get(callId);
    	if (actor != null) {
    		return callDirectionResolver.resolveDirection(actor.getCall());
    	} else {
    		log.error("Could not resolve direction for call %s. Setting to term.", callId);
    		return CallDirection.IN;
    	}
    }
	
	public static String getMimeType(String fileUrl) throws java.io.IOException {

		FileNameMap fileNameMap = URLConnection.getFileNameMap();
		String type = fileNameMap.getContentTypeFor(fileUrl);

		return type;
	}
    
    public AppInstanceResolver getAppInstanceResolver() {
        return appInstanceResolver;
    }

    public void setAppInstanceResolver(AppInstanceResolver endpointResolver) {
        this.appInstanceResolver = endpointResolver;
    }

    public CommandHandler getCommandHandler() {
        return commandHandler;
    }

    public void setCommandHandler(CommandHandler commandHandler) {
        this.commandHandler = commandHandler;
    }

    public AppInstanceEventDispatcher getAppInstanceEventDispatcher() {
        return appInstanceEventDispatcher;
    }
    
    public void setAppInstanceEventDispatcher(AppInstanceEventDispatcher appInstanceEventDispatcher) {
        this.appInstanceEventDispatcher = appInstanceEventDispatcher;
    }

	public AmecheCallRegistry getAmecheCallRegistry() {
		return amecheCallRegistry;
	}

	public void setAmecheCallRegistry(AmecheCallRegistry amecheCallRegistry) {
		this.amecheCallRegistry = amecheCallRegistry;
	}
}
