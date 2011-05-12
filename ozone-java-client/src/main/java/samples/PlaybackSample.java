package samples;

import java.net.URI;

import com.voxeo.ozone.client.ref.SayRef;

public class PlaybackSample extends BaseSample {

	public void run() throws Exception {
		
		client.waitFor("offer");
		client.answer();

		SayRef say = client.say(new URI("http://ccmixter.org/content/DoKashiteru/DoKashiteru_-_you_(na-na-na-na).mp3"));
		say.pause();
		Thread.sleep(10000);
		say.resume();
		Thread.sleep(10000);
		say.stop();
		Thread.sleep(10000);		

		client.hangup();
	}
		
	public static void main(String[] args) throws Exception {
		
		PlaybackSample sample = new PlaybackSample();
		sample.connect("localhost", "userc", "1");
		sample.run();
		sample.shutdown();
	}	
}
