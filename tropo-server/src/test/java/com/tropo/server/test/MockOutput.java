package com.tropo.server.test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.voxeo.moho.event.OutputCompleteEvent;
import com.voxeo.moho.media.Output;

public class MockOutput implements Output {

    private OutputCompleteEvent result;

    public MockOutput(OutputCompleteEvent result) {
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
    public OutputCompleteEvent get() throws InterruptedException, ExecutionException {
        return null;
    }

    @Override
    public OutputCompleteEvent get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return result;
    }

    @Override
    public void move(boolean direction, int time) {}

    @Override
    public void jump(int index) {}

    @Override
    public void speed(boolean upOrDown) {}

    @Override
    public void volume(boolean upOrDown) {}

    @Override
    public void pause() {}

    @Override
    public void resume() {}

}
