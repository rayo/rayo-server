package com.tropo.server.test;

import java.net.URI;

import javax.media.mscontrol.mediagroup.MediaGroup;

import com.voxeo.moho.MediaService;
import com.voxeo.moho.event.EventSource;
import com.voxeo.moho.event.OutputCompleteEvent;
import com.voxeo.moho.event.OutputCompleteEvent.Cause;
import com.voxeo.moho.media.Input;
import com.voxeo.moho.media.Output;
import com.voxeo.moho.media.Prompt;
import com.voxeo.moho.media.Recording;
import com.voxeo.moho.media.input.InputCommand;
import com.voxeo.moho.media.output.OutputCommand;
import com.voxeo.moho.media.record.RecordCommand;

public class MockMediaService implements MediaService {

	private EventSource eventSource;

	public MockMediaService(EventSource eventSource) {
		
		this.eventSource = eventSource;
	}

	@Override
	public Output output(String text) {

		eventSource.dispatch(new OutputCompleteEvent(eventSource, Cause.END));
		return null;
	}

	@Override
	public Output output(URI media) {

		eventSource.dispatch(new OutputCompleteEvent(eventSource, Cause.END));
		return null;
	}

	@Override
	public Output output(OutputCommand output) {

		eventSource.dispatch(new OutputCompleteEvent(eventSource, Cause.END));
		return null;
	}

	@Override
	public Prompt prompt(String text, String grammar, int repeat) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Prompt prompt(URI media, String grammar, int repeat) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Prompt prompt(OutputCommand output, InputCommand input, int repeat) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Input input(String grammar) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Input input(InputCommand input) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Recording record(URI recording) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Recording record(RecordCommand command) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MediaGroup getMediaGroup() {
		// TODO Auto-generated method stub
		return null;
	}
}
