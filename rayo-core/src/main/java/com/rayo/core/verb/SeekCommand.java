package com.rayo.core.verb;

import javax.validation.constraints.NotNull;

import com.rayo.core.validation.Messages;

public class SeekCommand extends AbstractVerbCommand {

	public static enum Direction { FORWARD, BACK }
	
	@NotNull(message=Messages.MISSING_DIRECTION)
	private Direction direction;
	
	@NotNull(message=Messages.MISSING_AMOUNT)
	private Integer amount;
	
	public Direction getDirection() {
		return direction;
	}
	public void setDirection(Direction direction) {
		this.direction = direction;
	}
	public Integer getAmount() {
		return amount;
	}
	public void setAmount(Integer amount) {
		this.amount = amount;
	}
}
