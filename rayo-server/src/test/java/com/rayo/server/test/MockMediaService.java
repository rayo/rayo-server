package com.tropo.server.test;

import java.net.URI;

import javax.media.mscontrol.mediagroup.MediaGroup;

import com.voxeo.moho.MediaService;
import com.voxeo.moho.event.EventSource;
import com.voxeo.moho.event.MohoOutputCompleteEvent;
import com.voxeo.moho.event.OutputCompleteEvent.Cause;
import com.voxeo.moho.media.Input;
import com.voxeo.moho.media.Output;
import com.voxeo.moho.media.Prompt;
import com.voxeo.moho.media.Recording;
import com.voxeo.moho.media.input.InputCommand;
import com.voxeo.moho.media.output.OutputCommand;
import com.voxeo.moho.media.record.RecordCommand;

public class MockMediaService implements MediaService<EventSource> {

	private EventSource eventSource;

	public MockMediaService(EventSource eventSource) {
		
		this.eventSource = eventSource;
	}

	@Override
	public Output<EventSource> output(String text) {

		eventSource.dispatch(new MohoOutputCompleteEvent<EventSource>(eventSource, Cause.END));
		return null;
	}

	@Override
	public Output<EventSource> output(URI media) {

		eventSource.dispatch(new MohoOutputCompleteEvent<EventSource>(eventSource, Cause.END));
		return null;
	}

	@Override
	public Output<EventSource> output(OutputCommand output) {

		eventSource.dispatch(new MohoOutputCompleteEvent<EventSource>(eventSource, Cause.END));
		return null;
	}

	@Override
	public Prompt<EventSource> prompt(String text, String grammar, int repeat) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Prompt<EventSource> prompt(URI media, String grammar, int repeat) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Prompt<EventSource> prompt(OutputCommand output, InputCommand input, int repeat) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Input<EventSource> input(String grammar) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Input<EventSource> input(InputCommand input) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Recording<EventSource> record(URI recording) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Recording<EventSource> record(RecordCommand command) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MediaGroup getMediaGroup() {
		// TODO Auto-generated method stub
		return null;
	}
}
