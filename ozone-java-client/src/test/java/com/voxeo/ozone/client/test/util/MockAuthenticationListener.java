package com.voxeo.ozone.client.test.util;

import java.util.Collection;

import com.voxeo.ozone.client.auth.AuthenticationListener;
import com.voxeo.servlet.xmpp.ozone.stanza.sasl.Challenge;
import com.voxeo.servlet.xmpp.ozone.stanza.sasl.Failure;
import com.voxeo.servlet.xmpp.ozone.stanza.sasl.Success;

public class MockAuthenticationListener implements AuthenticationListener {

	int bindRequiredCount;
	int challengeCount;
	int failureCount;
	int successCount;
	int sessionsCount;
	int settingsCount;
	
	@Override
	public void authBindingRequired() {

		bindRequiredCount++;
	}
	
	@Override
	public void authChallenge(Challenge challenge) {

		challengeCount++;
	}
	
	@Override
	public void authFailure(Failure failure) {

		failureCount++;
	}
	
	@Override
	public void authSessionsSupported() {

		sessionsCount++;
	}
	
	@Override
	public void authSettingsReceived(Collection<String> mechanisms) {

		settingsCount++;
	}
	
	@Override
	public void authSuccessful(Success success) {

		successCount++;
	}

	public int getBindRequiredCount() {
		return bindRequiredCount;
	}

	public void setBindRequiredCount(int bindRequiredCount) {
		this.bindRequiredCount = bindRequiredCount;
	}

	public int getChallengeCount() {
		return challengeCount;
	}

	public void setChallengeCount(int challengeCount) {
		this.challengeCount = challengeCount;
	}

	public int getFailureCount() {
		return failureCount;
	}

	public void setFailureCount(int failureCount) {
		this.failureCount = failureCount;
	}

	public int getSuccessCount() {
		return successCount;
	}

	public void setSuccessCount(int successCount) {
		this.successCount = successCount;
	}

	public int getSessionsCount() {
		return sessionsCount;
	}

	public void setSessionsCount(int sessionsCount) {
		this.sessionsCount = sessionsCount;
	}

	public int getSettingsCount() {
		return settingsCount;
	}

	public void setSettingsCount(int settingsCount) {
		this.settingsCount = settingsCount;
	}
}
