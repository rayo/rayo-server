package com.tropo.core;

import com.voxeo.exceptions.VException;

public class CallException extends VException {

    public CallException() {
        super();
    }

    public CallException(String s, Object... args) {
        super(s, args);
    }

    public CallException(String message, Throwable cause) {
        super(message, cause);
    }

    public CallException(String message) {
        super(message);
    }

    public CallException(Throwable cause) {
        super(cause);
    }

}
