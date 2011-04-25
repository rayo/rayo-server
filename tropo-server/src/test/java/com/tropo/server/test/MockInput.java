package com.tropo.server.test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.voxeo.moho.event.InputCompleteEvent;
import com.voxeo.moho.media.Input;

public class MockInput implements Input {

    private InputCompleteEvent result;

    public MockInput(InputCompleteEvent result) {
        this.result = result;
    }

    @Override
    public void stop() {
        result.source.dispatch(result);
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
    public InputCompleteEvent get() throws InterruptedException, ExecutionException {
        return result;
    }

    @Override
    public InputCompleteEvent get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return result;
    }

}
