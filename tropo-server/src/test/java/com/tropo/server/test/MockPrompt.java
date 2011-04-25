package com.tropo.server.test;

import com.voxeo.moho.MediaException;
import com.voxeo.moho.media.Input;
import com.voxeo.moho.media.Output;
import com.voxeo.moho.media.Prompt;

public class MockPrompt implements Prompt {

    Input input;
    Output output;

    public MockPrompt(Input input, Output output) {
        this.input = input;
        this.output = output;
    }

    @Override
    public Input getInput() throws MediaException {
        return null;
    }

    @Override
    public Output getOutput() throws MediaException {
        return null;
    }

    @Override
    public String getResult() throws MediaException {
        try {
            return input.get().getValue();
        }
        catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

}
