package samples;


public class TextSaySample extends BaseSample {

	public void run() throws Exception {
		
		client.waitFor("offer");
		client.answer();
		client.say("Hello World. This is a test on Ozone. I hope you heard this message. Bye bye.");
		Thread.sleep(5000);
		client.hangup();
	}
	
	public static void main(String[] args) throws Exception {
		
		TextSaySample sample = new TextSaySample();
		sample.connect("localhost", "userc", "1");
		sample.run();
		sample.shutdown();
	}
}
