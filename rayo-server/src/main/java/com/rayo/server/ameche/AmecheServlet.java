package com.rayo.server.ameche;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParamBean;
import org.apache.http.util.EntityUtils;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.context.support.XmlWebApplicationContext;

import com.rayo.core.DialCommand;
import com.rayo.core.HangupCommand;
import com.rayo.core.JoinCommand;
import com.rayo.server.CommandHandler;
import com.rayo.server.Server;
import com.rayo.server.Transport;
import com.rayo.server.TransportCallback;
import com.voxeo.logging.Loggerf;
import com.voxeo.moho.Participant.JoinType;
import com.voxeo.moho.common.util.SettableResultFuture;

@SuppressWarnings("serial")
public class AmecheServlet extends HttpServlet implements Transport {

    private static final Loggerf log = Loggerf.getLogger(AmecheServlet.class);
    
    // Config
    private HttpClient http; 
    private CommandHandler commandHandler;
    private AppInstanceResolver endpointResolver;
    
    // Internal State
    private Map<String, AmecheCall> calls = new ConcurrentHashMap<String, AmecheCall>();
    
    @Override
    public void init() throws ServletException {
        
        WebApplicationContext wac = WebApplicationContextUtils.getRequiredWebApplicationContext(getServletContext());

        XmlWebApplicationContext httpTransportContext = new XmlWebApplicationContext();
        httpTransportContext.setServletContext(getServletContext());
        httpTransportContext.setParent(wac);
        httpTransportContext.setConfigLocation("/WEB-INF/" + getServletName() + ".xml");
        httpTransportContext.refresh();

        http = (HttpClient) httpTransportContext.getBean("httpClient");
        
        Server server = (Server) httpTransportContext.getBean("rayoServer");
        server.addTransport(this);
        this.commandHandler = server;
        
    }

    @Override
    public void callEvent(String callId, String componentId, Element event) throws Exception {

        AmecheCall machine = null;
        
        // New Call
        if(event.getName().equals("offer")) {
            
            // Lookup App Instance Endpoints
            List<AppInstance> apps = endpointResolver.lookup(event);
            
            // Make and register a new ameche call handler 
            machine = new AmecheCall(callId, apps);
            calls.put(callId, machine);
            
        }
        // Existing Call
        else {
            
            // Lookup ameche call handler
            machine = calls.get(callId);
            
            // Clean up event machine when the call ends
            if(event.getName().equals("end")) {
                calls.remove(callId);
            }
        }

        machine.callEvent(callId, componentId, event);
        
    }
    
    @Override
    public void mixerEvent(String mixerId, Collection<String> participants, Element body) throws Exception {
        
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        
        ServletInputStream is = req.getInputStream();
        SAXReader reader = new SAXReader();
        
        try {
            
            Element command = reader.read(is).getRootElement();
            
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
            Element result = call.handleCommand(appInstanceId, callId, componentId, command).get();
            
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
        catch (InterruptedException e) {
            log.error("Failed to get command result", e);
            resp.setStatus(500);
        }
        catch (ExecutionException e) {
            log.error("Failed to get command result", e);
            resp.setStatus(500);
        }
        
    }
    
    private class AmecheCall {

        // Config
        private String parentCallId;
        private Map<String, AppInstance> apps;
        private Element offer;
        
        // Internal
        private HttpRequest offerRequest;
        private Iterator<AppInstance> appIterator;
        private Set<String> childCallIds;
        private Map<String, AppInstance> componentToAppMapping;

        // Constructor
        public AmecheCall(String callId, List<AppInstance> apps) {
            
            this.parentCallId = callId;
            
            this.apps = new ConcurrentHashMap<String, AppInstance>();
            for(AppInstance appInstance : apps) {
                this.apps.put(appInstance.getId(), appInstance);
            }

            this.childCallIds = Collections.synchronizedSet(new HashSet<String>());
            this.componentToAppMapping = new ConcurrentHashMap<String, AppInstance>();
        }

        /**
         * Do some pre-processing on the call event and then dispatch to the appropriate app instances
         *  
         * @param callId
         * @param componentId
         * @param event
         * @throws Exception
         */
        public void callEvent(String callId, String componentId, Element event) throws Exception {
            
            // New Call
            if(event.getName().equals("offer")) {
                this.offer = event;
                this.appIterator = apps.values().iterator();
                this.offerRequest = buildRequest(offer, callId, null);
                nextOffer();
            }
            
            // Call Event
            else {
                
                if(event.getName().equals("end")) {
                    hangupCalls(callId);
                }

                HttpRequest request = buildRequest(event, callId, componentId);
                
                if(this.parentCallId != callId) {
                    request.addHeader("parent-call-id", parentCallId);
                }
                
                if(componentId != null) {
                    
                    AppInstance appInstance = null;
                    
                    if(event.getName().equals("complete")) {
                        appInstance = componentToAppMapping.remove(componentId);
                    }
                    else {
                        appInstance = componentToAppMapping.get(componentId);
                    }
                    
                    if(appInstance == null) {
                        log.error("Received event for an unmapped component. Ignoring. [callId=%s, componentId=%s]", callId, componentId);
                        return;
                    }
                    
                    dispatchEvent(request, appInstance);
                    
                }
                else {
                    for(AppInstance appInstance : apps.values()) {
                        dispatchEvent(request, appInstance);
                    }
                }
            }

        }

        /**
         * Synchronous method that checks for any internal Ameche commands and forward the rest to the {@link Server} for processing
         * 
         * TODO: If/when PRISM supports async http we can just respond in the TransportCallback thread
         * 
         * @param sourceAppAddress
         * @param callId
         * @param componentId
         * @param command
         * @param callback
         */
        public Future<Element> handleCommand(final String appInstanceId, String callId, String componentId, Element command) {

            // Used to synchronize return with the result of the command handler
            final SettableResultFuture<Element> resultFuture = new SettableResultFuture<Element>();

            if(command.getName().equals("continue")) {
                // FIXME: The caller will block until the next offer is dispatched
                // Consider doing offers in a thread pool (JdC)
                nextOffer();
                resultFuture.setResult(null);
            }
            else {
                // Send command to call's event machine
                commandHandler.handleCommand(callId, componentId, command, new TransportCallback() {
                    public void handle(Element result, Exception err) {
                        if(err != null) {
                            resultFuture.setException((Exception)err);                        
                        }
                        else {
                            // If the command resulteded in a new compionent being created we need
                            // to assocociate it with the app insatnce that created it since that
                            // should be the only app to receive its events
                            if(result != null && result.getName().equals("ref")) {
                                AppInstance appInstance = apps.get(appInstanceId);
                                String newComponentId = result.attributeValue("id");
                                componentToAppMapping.put(newComponentId, appInstance);
                            }
                            resultFuture.setResult(result);
                        }
                    }
                });
            }
            
            return resultFuture;
            
        }

        /**
         * Send the &lt;offer&gt; to the next app instance in the apps list
         */
        private void nextOffer() {
            if(appIterator.hasNext()) {
                AppInstance appInstance = appIterator.next();
                dispatchEvent(offerRequest, appInstance);
            }
            else {
                completeCall();
            }
        }
        
        private void completeCall() {
            
            DialCommand dial = new DialCommand();
            dial.setTo(URI.create(offer.attributeValue("to")));
            dial.setFrom(URI.create(offer.attributeValue("from")));
            
            JoinCommand join = new JoinCommand();
            join.setCallId(parentCallId);
            // FIXME: This should eventually be DIRECT and then have the system upgrade to BRIDGE when needed
            join.setMedia(JoinType.BRIDGE_EXCLUSIVE);
            dial.setJoin(join);
            
            commandHandler.handleCommand(null, null, dial, new TransportCallback() {
                public void handle(Element result, Exception err) {
                    if(err != null) {
                        hangupCalls(null);
                    }
                    else {
                        String childCallId = result.attributeValue("id");
                        // Register call with outer AmecheServlet's registry
                        calls.put(childCallId, AmecheCall.this);
                        // Register call internally so we know what to clean up later
                        childCallIds.add(childCallId);
                    }
                }
            });
        }
        
        /**
         * Hangs up all related calls
         * 
         * @param sourceCall If specified, this is the call that that triggered the hangup so it should be skipped 
         */
        private void hangupCalls(String sourceCall) {
            
            // Clean up AmecheServlet mappings
            calls.remove(parentCallId);
            for(String childCallId : childCallIds) {
                calls.remove(childCallId);
            }

            // Build removal list
            Set<String> targetCalls = new HashSet<String>(childCallIds);
            targetCalls.add(parentCallId);
            
            if(sourceCall != null) {
                targetCalls.remove(sourceCall);
            }
            
            for(String targetCall : targetCalls) {
                HangupCommand hangupCommand = new HangupCommand(targetCall);
                commandHandler.handleCommand(targetCall, null, hangupCommand, null);
            }
        }

        /**
         * Util method used to build an HTTP Client {@link HttpRequest} with the appropriate headers
         * and timeout values.
         * 
         * @param event
         * @param componentId
         * @return
         */
        private HttpRequest buildRequest(Element event, String callId, String componentId) {
            
            // Build HTTP request
            HttpPost request = new HttpPost(URI.create("http://dummy.com")); // A default uri is required
            
            request.setHeader("call-id", callId);
            
            if(componentId != null) {
                request.setHeader("component-id", componentId);
            }
            
            request.setEntity(new StringEntity(event.asXML(), ContentType.APPLICATION_XML));
            
            // Request Properties
            HttpParams params = request.getParams();
            HttpProtocolParamBean pbean = new HttpProtocolParamBean(params);
            pbean.setContentCharset("utf-8");
            pbean.setUseExpectContinue(false);
            
            // Connections Properties
            params.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 1000)
                  .setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 1000)
                  //.setBooleanParameter(CoreConnectionPNames.STALE_CONNECTION_CHECK, false)
                  .setBooleanParameter(CoreConnectionPNames.TCP_NODELAY, true);        
            
            return request;
        }

        
        /**
         * Sendth HTTP request to a single app instance
         * 
         * @param request
         * @param appEndpoint
         */
        private void dispatchEvent(HttpRequest request, AppInstance appInstance) {
            
            URI appEndpoint = appInstance.getEndpoint();
            
            try {
                HttpHost target = new HttpHost(appEndpoint.getHost(), appEndpoint.getPort(), appEndpoint.getScheme());
                HttpResponse response = http.execute(target, request);
                
                // We must consume the content to release the connection
                EntityUtils.toString(response.getEntity());
                
                // Check the status code
                int statusCode = response.getStatusLine().getStatusCode();
                
                if(statusCode != 203) {
                    log.error("Non-203 Status Code [appEndpoint=%s, callId=%s, componentId=%s, status=%s]", appEndpoint, parentCallId, statusCode);
                    apps.remove(appInstance.getId());
                }
            }
            catch (IOException e) {
                log.error("Failed to dispatch event [appEndpoint=%s, callId=%s, componentId=%s]", appEndpoint, parentCallId, e);
                apps.remove(appInstance.getId());
            }
            
        }    
    }

    public AppInstanceResolver getEndpointResolver() {
        return endpointResolver;
    }

    public void setEndpointResolver(AppInstanceResolver endpointResolver) {
        this.endpointResolver = endpointResolver;
    }

    public CommandHandler getCommandHandler() {
        return commandHandler;
    }

    public void setCommandHandler(CommandHandler commandHandler) {
        this.commandHandler = commandHandler;
    }

    public HttpClient getHttp() {
        return http;
    }

    public void setHttp(HttpClient http) {
        this.http = http;
    }

}
