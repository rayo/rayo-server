package com.rayo.server.test;

import com.voxeo.moho.Joint;
import com.voxeo.moho.common.util.SettableResultFuture;
import com.voxeo.moho.event.JoinCompleteEvent;

public class SimpleJoint extends SettableResultFuture<JoinCompleteEvent> implements Joint {

    public SimpleJoint() {}

    public SimpleJoint(JoinCompleteEvent joinCompleteEvent) {
        setResult(joinCompleteEvent);
    }

}
