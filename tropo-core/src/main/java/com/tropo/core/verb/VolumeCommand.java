package com.tropo.core.verb;

public class VolumeCommand extends AbstractVerbCommand {

	private boolean up;

	public boolean isUp() {
		return up;
	}

	public void setUp(boolean up) {
		this.up = up;
	}
}
