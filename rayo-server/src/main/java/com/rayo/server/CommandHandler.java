package com.rayo.server;

import org.dom4j.Element;

import com.rayo.core.CallCommand;

public interface CommandHandler {

    public void handleCommand(String callId, String componentId, Element xml, TransportCallback callback);

    public void handleCommand(String callId, String componentId, CallCommand command, TransportCallback callback);

}