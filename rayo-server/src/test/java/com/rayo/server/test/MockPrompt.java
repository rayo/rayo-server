package com.rayo.server.test;

import com.voxeo.moho.MediaException;
import com.voxeo.moho.Participant;
import com.voxeo.moho.media.Input;
import com.voxeo.moho.media.Output;
import com.voxeo.moho.media.Prompt;

public class MockPrompt implements Prompt<Participant> {

    Input<Participant> input;
    Output<Participant> output;

    public MockPrompt(Input<Participant> input, Output<Participant> output) {
        this.input = input;
        this.output = output;
    }

    @Override
    public Input<Participant> getInput() throws MediaException {
        return null;
    }

    @Override
    public Output<Participant> getOutput() throws MediaException {
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
