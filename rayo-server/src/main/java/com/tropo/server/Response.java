package com.tropo.server;

public class Response {

    private Object result;

    public Response(Object result) {
        this.result = result;
    }

    public Object getValue() {
        return result;
    }
    
    public boolean isSuccess() {
        return !(result instanceof Exception);
    }

}
