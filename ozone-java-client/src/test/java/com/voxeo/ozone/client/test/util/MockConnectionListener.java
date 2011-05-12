package com.voxeo.ozone.client.test.util;

import com.voxeo.ozone.client.XmppConnectionAdapter;

public class MockConnectionListener extends XmppConnectionAdapter {

	int errorsCount = 0;
	int establishedCount = 0;
	int finishedCount = 0;
	int resettedCount = 0;
	
	@Override
	public void connectionError(String connectionId, Exception e) {

		errorsCount++;
	}
	
	@Override
	public void connectionEstablished(String connectionId) {

		establishedCount++;
	}
	
	@Override
	public void connectionFinished(String connectionId) {

		finishedCount++;
	}
	
	@Override
	public void connectionReset(String connectionId) {

		resettedCount++;
	}

	public int getErrorsCount() {
		return errorsCount;
	}

	public void setErrorsCount(int errorsCount) {
		this.errorsCount = errorsCount;
	}

	public int getEstablishedCount() {
		return establishedCount;
	}

	public void setEstablishedCount(int establishedCount) {
		this.establishedCount = establishedCount;
	}

	public int getFinishedCount() {
		return finishedCount;
	}

	public void setFinishedCount(int finishedCount) {
		this.finishedCount = finishedCount;
	}

	public int getResettedCount() {
		return resettedCount;
	}

	public void setResettedCount(int resettedCount) {
		this.resettedCount = resettedCount;
	}
	
	
}
