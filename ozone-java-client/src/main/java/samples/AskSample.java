package samples;

import static org.junit.Assert.assertTrue;

import com.tropo.core.verb.AskCompleteEvent;

public class AskSample extends BaseSample {

	public void run() throws Exception {
		
		client.waitFor("offer");
		client.answer();
		client.ask("Welcome to Orlando Bank. Please enter your five digits number.","[4-5 DIGITS]");
		
		AskCompleteEvent complete = (AskCompleteEvent)client.waitFor("complete");
		assertTrue(complete.isSuccess());

		client.hangup();
	}
	
	public static void main(String[] args) throws Exception {
		
		AskSample sample = new AskSample();
		sample.connect("localhost", "userc", "1");
		sample.run();
		sample.shutdown();
	}
}
