package com.tropo.core;

import com.voxeo.logging.LoggingContext;

public class Logging {

    public static void context(ExecutionContext executionContext, Offer offer) {
        LoggingContext loggingContext = LoggingContext.get();
        loggingContext.clear();
        loggingContext.setAccountID(executionContext.getAccountId());
        loggingContext.setSessionGUID(offer.getCallId());
    }
    
    public static void context(ExecutionContext executionContext) {
        LoggingContext loggingContext = LoggingContext.get();
        loggingContext.clear();
        loggingContext.setAccountID(executionContext.getAccountId());
    }


}
