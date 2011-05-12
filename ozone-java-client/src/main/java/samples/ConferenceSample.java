package samples;


public class ConferenceSample extends BaseSample {

	public void run() throws Exception {
		
		client.waitFor("offer");
		client.answer();
		client.conference("123456");
		Thread.sleep(60000);
		client.hangup();
	}
	
	public static void main(String[] args) throws Exception {
		
		ConferenceSample sample1 = new ConferenceSample();
		sample1.connect("localhost", "userc", "1");
		// Launch your first soft phone to answer this call
		sample1.run();
		
		ConferenceSample sample2 = new ConferenceSample();
		sample2.connect("localhost", "usera", "1");
		// Launch a second soft phone
		sample2.run();
				
		sample1.shutdown();
		sample2.shutdown();
	}	
}
