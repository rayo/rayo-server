package com.voxeo.ozone.client.test;

import java.net.URI;

import org.junit.Test;

import com.voxeo.ozone.client.internal.XmppIntegrationTest;
import com.voxeo.ozone.client.ref.SayRef;

public class SayTest extends XmppIntegrationTest {
	
	@Test
	public void testTextSay() throws Exception {
		
		ozone.answer();
		ozone.say("hello!");
		
		Thread.sleep(400);
		assertServerReceived("<iq id=\"*\" type=\"set\" from=\"userc@localhost/voxeo\" to=\"#callId@localhost\"><say xmlns=\"urn:xmpp:ozone:say:1\"><speak xmlns=\"\">hello!</speak></say></iq>");
	}

	@Test
	public void testAudioSay() throws Exception {
		
		ozone.answer();
		ozone.sayAudio("http://ccmixter.org/content/DoKashiteru/DoKashiteru_-_you_(na-na-na-na).mp3");
		
		Thread.sleep(400);
		assertServerReceived("<iq id=\"*\" type=\"set\" from=\"userc@localhost/voxeo\" to=\"#callId@localhost\"><say xmlns=\"urn:xmpp:ozone:say:1\"><audio url=\"http://ccmixter.org/content/DoKashiteru/DoKashiteru_-_you_(na-na-na-na).mp3\"></audio></say></iq>");
	}

	@Test
	public void testAudioSayUri() throws Exception {
		
		ozone.answer();
		ozone.say(new URI("http://ccmixter.org/content/DoKashiteru/DoKashiteru_-_you_(na-na-na-na).mp3"));
		
		Thread.sleep(400);
		assertServerReceived("<iq id=\"*\" type=\"set\" from=\"userc@localhost/voxeo\" to=\"#callId@localhost\"><say xmlns=\"urn:xmpp:ozone:say:1\"><audio url=\"http://ccmixter.org/content/DoKashiteru/DoKashiteru_-_you_(na-na-na-na).mp3\"></audio></say></iq>");
	}

	@Test
	public void testStop() throws Exception {
		
		ozone.answer();
		SayRef say = ozone.say(new URI("http://ccmixter.org/content/DoKashiteru/DoKashiteru_-_you_(na-na-na-na).mp3"));
		say.stop();
		
		Thread.sleep(400);
		assertServerReceived("<iq id=\"*\" type=\"set\" from=\"userc@localhost/voxeo\" to=\"" + say.getId() +"\"><stop xmlns=\"urn:xmpp:ozone:say:1\"/></iq>");
	}

	@Test
	public void testResume() throws Exception {
		
		ozone.answer();
		SayRef say = ozone.say(new URI("http://ccmixter.org/content/DoKashiteru/DoKashiteru_-_you_(na-na-na-na).mp3"));
		say.resume();
		
		Thread.sleep(400);
		assertServerReceived("<iq id=\"*\" type=\"set\" from=\"userc@localhost/voxeo\" to=\"" + say.getId() +"\"><resume xmlns=\"urn:xmpp:ozone:say:1\"></resume></iq>");
	}

	@Test
	public void testPause() throws Exception {
		
		ozone.answer();
		SayRef say = ozone.say(new URI("http://ccmixter.org/content/DoKashiteru/DoKashiteru_-_you_(na-na-na-na).mp3"));
		say.pause();
		
		Thread.sleep(400);
		assertServerReceived("<iq id=\"*\" type=\"set\" from=\"userc@localhost/voxeo\" to=\"" + say.getId() +"\"><pause xmlns=\"urn:xmpp:ozone:say:1\"></pause></iq>");
	}
	
	
}
