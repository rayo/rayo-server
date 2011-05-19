package samples;

import java.net.URI;

import com.tropo.core.verb.AudioItem;
import com.tropo.core.verb.Conference;
import com.tropo.core.verb.PromptItems;
import com.tropo.core.verb.SsmlItem;


public class ConferenceSample extends BaseSample {

	public void run(Conference conference) throws Exception {
		
		client.waitFor("offer");
		client.answer();
		client.command(conference);
		
	}
	
	public static void main(String[] args) throws Exception {
		
		ConferenceSample sample1 = new ConferenceSample();
		sample1.connect("localhost", "userc", "1");
		
        Conference conference = new Conference();
        conference.setRoomName("12345");
        conference.setModerator(true);
        PromptItems announcement = new PromptItems();
        announcement.add(new AudioItem(URI.create("http://localhost:8080/www/josedecastro.mp3")));
        announcement.add(new SsmlItem("<speak>has joined the conference</speak>"));
        conference.setAnnouncement(announcement);
        PromptItems music = new PromptItems();
        music.add(new AudioItem(URI.create("http://com.twilio.music.ambient.s3.amazonaws.com/gurdonark_-_Exurb.mp3")));
        conference.setHoldMusic(music);

		// Launch your first soft phone to answer this call
		sample1.run(conference);

		System.in.read();
		
        // Second call with moderation enabled
		ConferenceSample sample2 = new ConferenceSample();
		sample2.connect("localhost", "usera", "1");
		
		conference.setModerator(true);
		sample2.run(conference);

		Thread.sleep(Integer.MAX_VALUE);

	}	
}
