package com.tropo.server.test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.voxeo.moho.Participant;
import com.voxeo.moho.event.InputCompleteEvent;
import com.voxeo.moho.media.Input;

public class MockInput implements Input<Participant> {

    private InputCompleteEvent<Participant> result;

    public MockInput(InputCompleteEvent<Participant> result) {
        this.result = result;
    }

    @Override
    public void stop() {
        result.getSource().dispatch(result);
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return true;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return true;
    }

    @Override
    public InputCompleteEvent<Participant> get() throws InterruptedException, ExecutionException {
        return result;
    }

    @Override
    public InputCompleteEvent<Participant> get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return result;
    }

}
