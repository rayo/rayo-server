package com.rayo.server.ameche;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.context.support.XmlWebApplicationContext;

import com.rayo.core.ConnectCommand;
import com.rayo.server.CommandHandler;
import com.rayo.server.Server;
import com.rayo.server.Transport;
import com.rayo.server.TransportCallback;
import com.voxeo.logging.Loggerf;
import com.voxeo.moho.common.util.SettableResultFuture;
import com.voxeo.utils.Typesafe;

public class AmecheServlet extends HttpServlet implements Transport {

    private static final Loggerf log = Loggerf.getLogger(AmecheServlet.class);
    
    // Config
    private CommandHandler commandHandler;
    private AppInstanceResolver appInstanceResolver;
    private AppInstanceEventDispatcher appInstanceEventDispatcher;
    
    // Internal
    private Map<String, AmecheCall> calls = new ConcurrentHashMap<String, AmecheCall>();
    
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
                machine = new AmecheCall(callId, event, apps);
                calls.put(callId, machine);
                return true;
            }
            else {
                return false;
            }
        }
        // Existing Call
        else {

            // Lookup ameche call handler
            machine = calls.get(callId);

            // Clean up event machine when the call ends
            if (event.getName().equals("end")) {
                calls.remove(callId);
            }
            
            if (machine != null) {
                machine.onEvent(event, callId, componentId);
                return true;
            }
        }

        return false;
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
            
            AmecheCall call = calls.get(callId);
            
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
    
    private class AmecheCall {

        // Config
        private Element offer;
        private String parentCallId;
        
        // Internal
        private Iterator<AppInstance> appIterator;
        private List<URI> offerTargets = new ArrayList<URI>();;
        private Map<String, AppInstance> apps = new ConcurrentHashMap<String, AppInstance>();
        private Map<String, AppInstance> componentToAppMapping = new ConcurrentHashMap<String, AppInstance>();
        
        // Constructor
        public AmecheCall(String callId, Element offer, List<AppInstance> appList) {

            this.offer = offer;
            this.parentCallId = callId;
            
            // Create App Map. Used to track active apps. If an app misbehaves we pull
            // it out of this map so it won't receive any more events.
            for(AppInstance appInstance : appList) {
                this.apps.put(appInstance.getId(), appInstance);
            }
            
            // Iterator used for the offer cycle
            this.appIterator = this.apps.values().iterator();
            
            offer();
            
        }

        /**
         * Do some pre-processing on the call event and then dispatch to the appropriate app instances
         *  
         * @param callId
         * @param componentId
         * @param event
         * @throws Exception
         */
        public synchronized void onEvent(Element event, String callId, String componentId) {
            
            // Send event to the app instance that started the component
            if(componentId != null) {
                AppInstance appInstance = componentToAppMapping.get(componentId);
                
                if(event.getName().equals("complete")) {
                    componentToAppMapping.remove(componentId);
                }
                
                if(appInstance == null) {
                    log.error("Received event for unmapped component. Ignoring. [callId=%s, componentId=%s]", callId, componentId);
                    return;
                }
                
                dispatchEvent(event, callId, componentId, appInstance);
            } else {
                if (event.getName().equals("joining")) {
                    //TODO: Check if proper call ids are being used here
                    // Register call with outer AmecheServlet's registry
                    String peerCallId = event.attributeValue("call-id");
                    calls.put(peerCallId, AmecheCall.this);
                    
                    // Notify apps of new leg.
                    // Send <announce><joining call-id="PARENT_CALL_ID" /></announce/>
                    Element announceElement = DocumentHelper.createElement("announce");
                    //FIXME: no way to know the actual address that was dialed :-(
                    announceElement.addAttribute("to", "foo");
                    announceElement.add(event.createCopy());
                    
                    event = announceElement;
                }
            	// Blast event to all active instances
                for(AppInstance appInstance : apps.values()) {
                    dispatchEvent(event, callId, componentId, appInstance);
                }
            }

        }
        
        /**
         * Synchronous method that checks for any internal Ameche commands and forward the rest to the {@link Server} for processing
         */
        public synchronized Future<Element> onCommand(
        		final String appInstanceId, String callId, String componentId, final Element command) {

            final SettableResultFuture<Element> future = new SettableResultFuture<Element>();

            if(command.getName().equals("continue")) {
                processOfferTargets(command);                
                // FIXME: The caller will block until the next offer is dispatched
                // Consider doing offers in a thread pool (JdC)
                offer();
                future.setResult(null);                
            } else if (command.getName().equals("connect")) {
            	//TODO: CONNECT AND CONTINUE CONFUSION ARGGGGGHHH
            	processOfferTargets(command);
                // Send command to call's event machine
                commandHandler.handleCommand(callId, componentId, command, new TransportCallback() {
                    public void handle(Element result, Exception err) {
                        if(err != null) {
                            future.setException((Exception)err);
                            return;
                        }
                        if (result == null) {
                        	
                        }
                        // If the command resulted in a new component being created we need
                        // to assocociate it with the app that created it since that should 
                        // be the only app to receive events
                        if(result != null && result.getName().equals("ref")) {
                            AppInstance appInstance = apps.get(appInstanceId);
                            String newComponentId = result.attributeValue("id");
                            componentToAppMapping.put(newComponentId, appInstance);
                        }
                        future.setResult(result);
                    }
                });            	
            } else {
                // Send command to call's event machine
                commandHandler.handleCommand(callId, componentId, command, new TransportCallback() {
                    public void handle(Element result, Exception err) {
                        if(err != null) {
                            future.setException((Exception)err);
                            return;
                        }
                        
                        // If the command resulted in a new component being created we need
                        // to assocociate it with the app that created it since that should 
                        // be the only app to receive events
                        if(result != null && result.getName().equals("ref")) {
                            AppInstance appInstance = apps.get(appInstanceId);
                            String newComponentId = result.attributeValue("id");
                            componentToAppMapping.put(newComponentId, appInstance);
                        }
                        future.setResult(result);
                    }
                });
            }
            
            return future;
            
        }

		private void processOfferTargets(final Element command) {
			// Extract targets to ring when offer cycle is complete
			offerTargets.clear();
			for(Element targetElement : Typesafe.list(Element.class, command.elements("target"))) {
			    try {
			        offerTargets.add(new URI(targetElement.getText()));
			    }
			    catch (URISyntaxException e) {
			        log.warn("Received an invalid connect target URI from client");
			    }
			}
		}        

        private void dispatchEvent(Element event, String callId, String componentId, AppInstance appInstance) {
            try {
                appInstanceEventDispatcher.send(event, callId, componentId, appInstance);
            }
            catch (Exception e) {
                apps.remove(appInstance.getId());
            }
        }

        /**
         * Send offer to the next app instance in the apps list 
         * or complete the call once it's been offered to all apps
         */
        private void offer() {
            if(appIterator.hasNext()) {
                AppInstance appInstance = appIterator.next();
                dispatchEvent(offer, parentCallId, null, appInstance);
            }
            else {
                connect();
            }
        }
        

        private void connect() {
            
            ConnectCommand command = new ConnectCommand(parentCallId);
            command.setTargets(offerTargets);
            commandHandler.handleCommand(parentCallId, null, command, null);
            
            /*
            commandHandler.handleCommand(parentCallId, null, command, new TransportCallback() {
                
                public void handle(Element result, Exception e) {
                    if(result != null && result.getName().equals("ref")) {
                        
                        // Register call with outer AmecheServlet's registry
                        String peerCallId = result.attributeValue("id");
                        calls.put(peerCallId, AmecheCall.this);
                        
                        // Notify apps of new leg.
                        // Send <announce><joining call-id="PARENT_CALL_ID" /></announce/>
                        Element announceElement = DocumentHelper.createElement("announce");
                        //FIXME: no way to know the actual address that was dialed :-(
                        announceElement.addAttribute("to", "foo");
                        announceElement.addElement("joining").addAttribute("call-id", parentCallId);
                        
                        for(AppInstance appInstance : apps.values()) {
                            dispatchEvent(announceElement, peerCallId, null, appInstance);
                        }
                        
                    }
                    
                }
            });
            */
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
    
}
