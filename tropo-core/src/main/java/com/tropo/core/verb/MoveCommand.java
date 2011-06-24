package com.tropo.core.verb;

public class MoveCommand extends AbstractVerbCommand {

	private boolean direction;
	private int time;
	
	public boolean isDirection() {
		return direction;
	}
	public void setDirection(boolean direction) {
		this.direction = direction;
	}
	public int getTime() {
		return time;
	}
	public void setTime(int time) {
		this.time = time;
	}
	
	
}
