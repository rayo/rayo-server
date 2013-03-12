package com.rayo.server.ameche;

public class AppInstanceException extends Exception {

    private static final long serialVersionUID = -8096746567312806036L;

    public AppInstanceException() {
        super();
    }

    public AppInstanceException(String message, Throwable throwable) {
        super(message, throwable);
    }

    public AppInstanceException(String message) {
        super(message);
    }

    public AppInstanceException(Throwable throwable) {
        super(throwable);
    }

}
