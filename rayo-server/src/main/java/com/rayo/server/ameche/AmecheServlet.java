package com.rayo.server.ameche;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.List;

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

import com.rayo.server.CommandHandler;
import com.rayo.server.Server;
import com.rayo.server.Transport;
import com.voxeo.logging.Loggerf;

@SuppressWarnings("serial")
public class AmecheServlet extends HttpServlet implements Transport {

    private static final Loggerf log = Loggerf.getLogger(AmecheServlet.class);
    
    // Config
    private CommandHandler commandHandler;
    private AppInstanceResolver appInstanceResolver;
    private AppInstanceEventDispatcher appInstanceEventDispatcher;
    private AmecheCallRegistry amecheCallRegistry;
    
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
        
        Server server = (Server) httpTransportContext.getBean("rayoServer");
        server.addTransport(this);
        this.commandHandler = server;
        
    }

    @Override
    public boolean callEvent(String callId, String componentId, Element event) throws Exception {

        AmecheCall machine = null;

        // New Call
        if (event.getName().equals("offer")) {

            // Lookup App Instance Endpoints
            List<AppInstance> apps = appInstanceResolver.lookup(event);
            if (apps.size() != 0) {
                // Make and register a new ameche call handler 
            	machine = createAmecheCall(callId, event, apps);
                amecheCallRegistry.registerCall(callId, machine);
                machine.offer();
                return true;
            }
            else {
                return false;
            }
        }
        // Existing Call
        else {

            // Lookup ameche call handler
            machine = amecheCallRegistry.getCall(callId);
            // Clean up event machine when the call ends
            if (event.getName().equals("end")) {
                amecheCallRegistry.unregisterCall(callId);
            }
            
            if (machine != null) {
                machine.onEvent(event, callId, componentId);
                return true;
            }
        }

        return false;
    }
    
    private AmecheCall createAmecheCall(String callId, Element event, List<AppInstance> apps) {
    	
    	AmecheCall call = new AmecheCall(callId, event, apps);
    	call.setAmecheCallRegistry(amecheCallRegistry);
    	call.setAppInstanceEventDispatcher(appInstanceEventDispatcher);
    	call.setCommandHandler(commandHandler);
    	return call;
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
        
        SAXReader reader = new SAXReader();
        
        try {
            
            Element command = reader.read(new StringReader(body)).getRootElement();
            
            String callId = req.getHeader("call-id");
            String componentId = req.getHeader("component-id");
            String appInstanceId = req.getHeader("app-instance-id");
            
            if(callId == null) {
                log.warn("Missing call-id header");
                resp.setStatus(400, "Missing call-id header");
                return;
            }
            
            if(appInstanceId == null) {
                log.warn("Missing app-instance-id header");
                resp.setStatus(400, "Missing app-instance-id header");
                return;
            }
            
            AmecheCall call = amecheCallRegistry.getCall(callId);
            
            if(call == null) {
                log.warn("Received command for unknown call: %s", command.asXML());
                resp.setStatus(404);
                return;
            }
            
            // Send command to call's event machine
            Element result = call.onCommand(appInstanceId, callId, componentId, command).get();
            
            resp.setContentType("application/xml; charset=utf-8");
            
            if(result != null) {
                byte[] bytes = ((Element)result).asXML().getBytes("utf-8");
                resp.setStatus(200);
                resp.setContentLength(bytes.length);
                resp.getOutputStream().write(bytes);
            }
            else {
                resp.setStatus(203);
            }
            
        }
        catch (DocumentException e) {
            log.error("Failed to parse Rayo command", e);
            resp.setStatus(500);
        }
        catch (Exception e) {
            log.error("Failed to process command", e);
            resp.setStatus(500);
        }
        
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
