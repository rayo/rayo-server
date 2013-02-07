package com.rayo.server.servlet;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.apache.xerces.dom.CoreDocumentImpl;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.dom.DOMElement;
import org.dom4j.io.DOMWriter;

import com.rayo.core.HangupCommand;
import com.rayo.core.OfferEvent;
import com.rayo.server.JIDRegistry;
import com.rayo.server.MixerManager;
import com.rayo.server.Server;
import com.rayo.server.Transport;
import com.rayo.server.TransportCallback;
import com.rayo.server.admin.RayoAdminService;
import com.rayo.server.exception.RayoProtocolException;
import com.rayo.server.listener.XmppMessageListenerGroup;
import com.rayo.server.lookup.RayoJIDLookupService;
import com.voxeo.logging.Loggerf;
import com.voxeo.servlet.xmpp.IQRequest;
import com.voxeo.servlet.xmpp.IQResponse;
import com.voxeo.servlet.xmpp.JID;
import com.voxeo.servlet.xmpp.PresenceMessage;

@SuppressWarnings("serial")
public class RayoServlet extends AbstractRayoServlet implements Transport {

    protected static final Loggerf log = Loggerf.getLogger(RayoServlet.class);
    
    /**
     * By default a Rayo Server will retry 5 times in case of any failure 
     * while brodcasting its status
     */
    public static final int BROADCAST_RETRIES = 5; // 5 retries to broadcast node state
    
    /**
     * In case of a brodcast error by default there will be a 30 seconds delay 
     * between broadcasting retries
     */
    public static final int BROADCAST_RETRY_DELAY = 30000; 
    
    private Server server;
    private JIDRegistry jidRegistry;
    private MixerManager mixerManager;
    private RayoJIDLookupService<OfferEvent> rayoLookupService;
    private XmppMessageListenerGroup xmppMessageListenersGroup;

    private int broadcastRetries = BROADCAST_RETRIES;
    private int broadcastRetryDelay = BROADCAST_RETRY_DELAY;
    
    // Setup
    // ================================================================================
    
    @Override
    public void init(ServletConfig config) throws ServletException {
    	
        super.init(config);
        
		RayoAdminService adminService = (RayoAdminService)getAdminService();
        String gatewayDomain = adminService.getGatewayDomain();
        if (gatewayDomain != null) {
        	// It will be the gateway who takes care of disposing mixers
        	mixerManager.setGatewayHandlingMixers(true);
        }
        
        broadcastPresence("chat");
    }
    
    public void start() {
        super.start();
        server.addTransport(this);
    }
        
    // Events: Server -> Client
    // ================================================================================


	@Override
	public void callEvent(String callId, String componentId, Element body) throws Exception {

        JID jid = null;
        JID from = null;

        RayoAdminService adminService = (RayoAdminService) getAdminService();
        String gatewayDomain = adminService.getGatewayDomain();
        if (gatewayDomain == null) {
            // Single server. This code needs a bit of refactoring specially as 
            // the gateway servlet shares some of this stuff. 
            if (body.getName().equals("offer")) {
                URI to = new URI(body.attributeValue("to"));
                JID callTo = getCallDestination(to);
                jidRegistry.put(callId, callTo);
            }
            // Call Cleanup
            else if(body.getName().equals("end")) {
                jidRegistry.remove(callId);
            }

            String callDomain = getLocalDomain();
            jid = jidRegistry.getJID(callId);
            from = getXmppFactory().createJID(callId + "@" + callDomain);
        }
        else {
            // Clustered setup. Everything is forwarded to the gateway
            from = getXmppFactory().createJID(callId + "@" + getLocalDomain());
            jid = getXmppFactory().createJID(gatewayDomain);
        }

        from.setResource(componentId);

        // TODO: ouch
        org.w3c.dom.Element documentElement = toDomElement(body);
        PresenceMessage presence = getXmppFactory().createPresence(from, jid, null, documentElement); 
        presence.send();
        xmppMessageListenersGroup.onPresenceSent(presence);

	}
	
	@Override
	public void mixerEvent(String mixerId, Collection<String> participants, Element body) {
	    
        JID from = getXmppFactory().createJID(mixerId + "@" + getLocalDomain());

        RayoAdminService adminService = (RayoAdminService) getAdminService();
        String gatewayDomain = adminService.getGatewayDomain();

        List<JID> destinations = new ArrayList<JID>();
        
        // Single server. Deliver individual messages to all apps listening.
        // Note that mutiple callIds might belong to the same app so we need 
        // to multiplex the mixer event destinations
        if (gatewayDomain == null) {
            for (String callId : participants) {
                JID jid = jidRegistry.getJID(callId);
                if (!destinations.contains(jid)) {
                    destinations.add(jid);
                }
            }
        }
        // Clustered setup. Everything is forwarded to the gateway. The Gateway
        // will take care of multiplexing
        else {
            destinations.add(getXmppFactory().createJID(gatewayDomain));
        }
        
        for (JID jid : destinations) {
            try {
                // TODO: ouch
                org.w3c.dom.Element documentElement = toDomElement(body);  
                PresenceMessage presence = getXmppFactory().createPresence(from, jid, null, documentElement);
                presence.send();
                xmppMessageListenersGroup.onPresenceSent(presence);
            }
            catch (ServletException se) {
                log.error(se.getMessage(), se);
            }
            catch (Exception e) {
                // In the event of an error, continue dispatching to all remaining JIDs
                log.error("Failed to dispatch event [jid=%s, event=%s]", jid, body.asXML(), e);
            }
        }	    
	}


    // Commands: Client -> Server
    // ================================================================================

    @Override
    protected void processIQRequest(final IQRequest request, DOMElement payload) {

        xmppMessageListenersGroup.onIQReceived(request);
        
        // Handle outbound 'dial' command
        if (payload.getName().equals("dial")) {
            server.handleCommand(null, null, payload, new TransportCallback() {
                public void handle(Element result, Exception err) {
                    if (err != null) {
                        sendIqError(request, err);
                    } else {
                    	String callId = result.attributeValue("id");
                    	jidRegistry.put(callId, request.getFrom().getBareJID());
                        sendIqResult(request, toDomElement(result));
                    }
                }
            });
            return;
            
        } 
        
        //TODO: Refactor this. Right now it is necessary to get mixers output working.
        else if (payload.getName().equals("join")) {
            String mixerName = payload.attributeValue("mixer-name");
            if (mixerName != null) {
                jidRegistry.put(mixerName, request.getFrom().getBareJID());
            }
        }

        // Extract Call ID
        String callId = request.getTo().getNode();
        
        // Extract Component ID
        String componentId = null;
        if(request.getTo().getResource() != null) {
            componentId = request.getTo().getResource();
        }
        else {
            componentId = UUID.randomUUID().toString();
        }
        
        server.handleCommand(callId, componentId, payload, new TransportCallback() {
            public void handle(Element result, Exception err) {
                if(err != null) {
                    sendIqError(request, err);
                }
                else if (result == null) {
                    sendIqResult(request, null);
                } 
                else {
                    sendIqResult(request, toDomElement(result));
                }
            }
        });

    }
    
    public JID getCallDestination(URI offerTo) throws RayoProtocolException{
    	
		String forwardDestination = rayoLookupService.lookup(offerTo);

		if (forwardDestination == null) {
		    throw new RayoProtocolException(
		            com.rayo.server.exception.RayoProtocolException.Condition.ITEM_NOT_FOUND, 
		            "No application mapped to target address [address=" + offerTo.toString() + "]");
		}
		
		getLog().debug("Received Offer. Offer will be delivered to [%s]", forwardDestination);
		
        return getXmppFactory().createJID(forwardDestination);

    }

    // Routing and Presence
    // ================================================================================

    @Override
    public void onPropertyChanged(String property, String newValue) {
        if (property.equals(RayoAdminService.WEIGHT) ||
            property.equals(RayoAdminService.PRIORITY) ||
            property.equals(RayoAdminService.DEFAULT_PLATFORM_ID)) {
            broadcastPresence("chat");
        }
    }
    
    @Override
    public void onQuiesceModeEntered() {
        broadcastPresence("away");
    }
    
    @Override
    public void onQuiesceModeExited() {
        broadcastPresence("chat");
    }
    
    @Override
    public void onShutdown() {
        broadcastPresence("unavailable");
    }
    
    /**
     * Broadcasts presence of this Rayo Node to the configured Rayo Gateway
     * 
     * @param status Presence status to be broadcasted
     */
    private void broadcastPresence(final String status) {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            public void run() {
                doPresenceBroadcast(status);        
            }
        },0);
    }

    private void appendNodeInfoElement(CoreDocumentImpl document, org.w3c.dom.Element nodeInfoElement, String name, String value) {
        org.w3c.dom.Element platform = document.createElement(name);
        platform.setTextContent(value);                 
        nodeInfoElement.appendChild(platform);
    }

    private void doPresenceBroadcast(final String status) {
        
        RayoAdminService adminService = (RayoAdminService)getAdminService();
        String gatewayDomain = adminService.getGatewayDomain();
        
        if (adminService.getGatewayDomain() != null) {
            int retries = 0;
            do {
                PresenceMessage presence = null;            
                try {
                    if (status.equalsIgnoreCase("unavailable")) {
                        presence = getXmppFactory().createPresence(getLocalDomain(), gatewayDomain, "unavailable", (org.w3c.dom.Element)null);              
                    } else {            
                        CoreDocumentImpl document = new CoreDocumentImpl(false);
                        org.w3c.dom.Element showElement = document.createElement("show");
                        showElement.setTextContent(status.toLowerCase());
                        org.w3c.dom.Element nodeInfoElement = document.createElementNS("urn:xmpp:rayo:cluster:1", "node-info");
                        appendNodeInfoElement(document, nodeInfoElement, "platform", adminService.getPlatform());
                        appendNodeInfoElement(document, nodeInfoElement, "weight", adminService.getWeight());
                        appendNodeInfoElement(document, nodeInfoElement, "priority", adminService.getPriority());
                        presence = getXmppFactory().createPresence(getLocalDomain(), gatewayDomain, null, showElement, nodeInfoElement);
                    }           
        
                    presence.send();
                    break;
                } catch (Exception e) {
                    log.error("Could not broadcast presence to gateway [%s]", gatewayDomain, e);
                    try {
                        Thread.sleep(broadcastRetryDelay);
                    } catch (InterruptedException e1) {}
                    retries++;
                }
            } while(retries < broadcastRetries);
        }
    }
    
    @Override
    protected void doPresence(PresenceMessage presence) throws ServletException, IOException {
        
        JID toJid = presence.getTo();
        JID fromJid = presence.getFrom();
        RayoAdminService adminService = (RayoAdminService)getAdminService();
        String gatewayDomain = adminService.getGatewayDomain();
        if (fromJid.getNode() == null) {
            if (gatewayDomain != null && fromJid.getDomain().equals(gatewayDomain)) {
                if (presence.getType().equals("error")) {
                    String callId = toJid.getNode();
                    if (callId != null) {
                        HangupCommand command = new HangupCommand();
                        command.setCallId(callId);
                        // No calback. Nothing we can really do if hangup fails.
                        server.handleCommand(callId, null, command, null);
                    }
                } else {
                    log.warn("Ignoring presence message from Gateay");
                }
            } else {
                log.warn("Ignoring presence message from unknown domain");
            }
        } else {
            log.warn("Ignoring unknown presence message");
        }
    }    
    
    // Util
    // ================================================================================

    @Override
    protected void sendIqError(IQRequest request, IQResponse response) throws IOException {
    	super.sendIqError(request, response);
        xmppMessageListenersGroup.onErrorSent(response);        
    }
    
    @Override
    protected IQResponse sendIqResult(IQRequest request, org.w3c.dom.Element result) {
    	try {
            IQResponse response = super.sendIqResult(request, result);
            xmppMessageListenersGroup.onIQSent(response);
            return response;
        }
        catch (IOException e) {
            throw new IllegalStateException("Cannot dispatch result", e);
        }
    }

    private org.w3c.dom.Element toDomElement(Element resultElement) {
        try {
            return new DOMWriter().write(resultElement.getDocument()).getDocumentElement();
        }
        catch (DocumentException e) {
            throw new IllegalStateException(e);
        }
    }
    
    // Properties
    // ================================================================================

	public void setJidRegistry(JIDRegistry jidRegistry) {
		this.jidRegistry = jidRegistry;
	}

	public void setXmppMessageListenersGroup(XmppMessageListenerGroup xmppMessageListenersGroup) {
		this.xmppMessageListenersGroup = xmppMessageListenersGroup;
	}
	
	@Override
	protected Loggerf getLog() {
		
		return log;
	}

	public void setBroadcastRetries(int broadcastRetries) {
		this.broadcastRetries = broadcastRetries;
	}

	public void setBroadcastRetryDelay(int broadcastRetryDelay) {
		this.broadcastRetryDelay = broadcastRetryDelay;
	}
	
	public void setRayoLookupService(RayoJIDLookupService<OfferEvent> rayoLookupService) {
		this.rayoLookupService = rayoLookupService;
	}

	public MixerManager getMixerManager() {
		return mixerManager;
	}

	public void setMixerManager(MixerManager mixerManager) {
		this.mixerManager = mixerManager;
	}

    public void setServer(Server server) {
        this.server = server;
    }

}
