package samples;

import java.net.URI;

public class TransferSample extends BaseSample {

	public void run() throws Exception {
		
		client.waitFor("offer");
		client.answer();
		// Feel free to configure with your prefered sip phone
		client.transfer(new URI("sip:mperez@127.0.0.1:3060"));
		Thread.sleep(60000);
		client.hangup();
	}
	
	public static void main(String[] args) throws Exception {

		TransferSample sample = new TransferSample();
		sample.connect("localhost", "userc", "1");
		sample.run();
		sample.shutdown();
	}	
}
