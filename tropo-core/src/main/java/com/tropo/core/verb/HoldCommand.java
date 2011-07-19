package com.tropo.core.verb;

public class HoldCommand extends AbstractVerbCommand {

	private boolean state;

	public boolean isState() {
		return state;
	}

	public void setState(boolean state) {
		this.state = state;
	}
}
