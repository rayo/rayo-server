package samples;

import java.net.URI;

public class DialSample extends BaseSample {

	public void run() throws Exception {
		
		client.waitFor("offer");
		client.answer();
		client.dial(new URI("sip:mperez@127.0.0.1:3060"));
		Thread.sleep(60000);
		client.hangup();
	}
		
	public static void main(String[] args) throws Exception {
		
		DialSample sample = new DialSample();
		sample.connect("localhost", "userc", "1");
		sample.run();
		sample.shutdown();
	}	
}
