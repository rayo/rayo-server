package com.tropo.server;

import com.voxeo.logging.Loggerf;


public class Request {

    private static final Loggerf log = Loggerf.getLogger(Request.class);
    
    private Object command;
    private ResponseHandler callback;

    public Request(Object command, ResponseHandler callback) {
        this.command = command;
        this.callback = callback;
    }
    
    public void reply(Object message) {
        if(callback != null) {
            // toString needed to keep Loggerf from printing the stacktrace
            log.info("Reply [%s]", message != null ? message.toString() : null);
            try {
                callback.handle(new Response(message));
            } catch (Exception e) {
                log.error("Uncaught exception calling response handler [handler=%s]", callback, e);
            }
        }
        else {
            // toString needed to keep Loggerf from printing the stacktrace
            log.info("Cannot reply. No callback. [%s]", message != null ? message.toString() : null);
        }
    }

    public Object getCommand() {
        return command;
    }

    @Override
    public String toString() {
        return command.toString();
    }
    
}
