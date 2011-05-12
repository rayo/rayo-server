package samples;

import java.net.URI;

public class AudioSaySample extends BaseSample {

	public void run() throws Exception {
		
		client.waitFor("offer");
		client.answer();
		client.say(new URI("http://ccmixter.org/content/DoKashiteru/DoKashiteru_-_you_(na-na-na-na).mp3"));
		Thread.sleep(5000);
		client.hangup();
	}
	
	public static void main(String[] args) throws Exception {
		
		AudioSaySample sample = new AudioSaySample();
		sample.connect("localhost", "userc", "1");
		sample.run();
		sample.shutdown();
	}
}
