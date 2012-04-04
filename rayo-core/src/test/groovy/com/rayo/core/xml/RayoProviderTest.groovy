package com.rayo.core.xml

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertNull
import static org.junit.Assert.assertTrue

import java.io.StringReader
import java.net.URI
import java.util.HashMap
import java.util.Map

import javax.media.mscontrol.join.Joinable

import org.dom4j.Document
import org.dom4j.io.SAXReader
import org.joda.time.Duration
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner

import com.rayo.core.AcceptCommand
import com.rayo.core.AnswerCommand
import com.rayo.core.AnsweredEvent
import com.rayo.core.CallRejectReason
import com.rayo.core.DestroyMixerCommand;
import com.rayo.core.DialCommand
import com.rayo.core.DtmfCommand
import com.rayo.core.DtmfEvent
import com.rayo.core.EndEvent
import com.rayo.core.StoppedSpeakingEvent
import com.rayo.core.HangupCommand
import com.rayo.core.JoinCommand
import com.rayo.core.JoinDestinationType
import com.rayo.core.JoinedEvent
import com.rayo.core.OfferEvent
import com.rayo.core.RedirectCommand
import com.rayo.core.RejectCommand
import com.rayo.core.RingingEvent
import com.rayo.core.StartedSpeakingEvent
import com.rayo.core.UnjoinCommand
import com.rayo.core.UnjoinedEvent
import com.rayo.core.validation.Validator
import com.rayo.core.verb.Ask
import com.rayo.core.verb.AskCompleteEvent
import com.rayo.core.verb.Choices
import com.rayo.core.verb.HoldCommand
import com.rayo.core.verb.Input
import com.rayo.core.verb.InputCompleteEvent
import com.rayo.core.verb.InputMode
import com.rayo.core.verb.KickCommand
import com.rayo.core.verb.MediaType
import com.rayo.core.verb.MuteCommand
import com.rayo.core.verb.Output
import com.rayo.core.verb.OutputCompleteEvent
import com.rayo.core.verb.PauseCommand
import com.rayo.core.verb.Record
import com.rayo.core.verb.RecordCompleteEvent
import com.rayo.core.verb.RecordPauseCommand
import com.rayo.core.verb.RecordResumeCommand
import com.rayo.core.verb.ResumeCommand
import com.rayo.core.verb.Say
import com.rayo.core.verb.SayCompleteEvent
import com.rayo.core.verb.SeekCommand
import com.rayo.core.verb.SpeedDownCommand
import com.rayo.core.verb.SpeedUpCommand
import com.rayo.core.verb.Ssml
import com.rayo.core.verb.StopCommand
import com.rayo.core.verb.Transfer
import com.rayo.core.verb.TransferCompleteEvent
import com.rayo.core.verb.UnholdCommand
import com.rayo.core.verb.UnmuteCommand
import com.rayo.core.verb.VerbCompleteEvent
import com.rayo.core.verb.VolumeDownCommand
import com.rayo.core.verb.VolumeUpCommand
import com.rayo.core.verb.AskCompleteEvent.Reason
import com.rayo.core.xml.providers.AskProvider
import com.rayo.core.xml.providers.InputProvider
import com.rayo.core.xml.providers.OutputProvider
import com.rayo.core.xml.providers.RayoProvider
import com.rayo.core.xml.providers.RecordProvider
import com.rayo.core.xml.providers.SayProvider
import com.rayo.core.xml.providers.TransferProvider
import com.voxeo.moho.Mixer;
import com.voxeo.moho.Participant;
import com.voxeo.moho.Participant.JoinType
import com.voxeo.moho.media.output.OutputCommand.BargeinType


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations=["/rayo-providers.xml"])
public class RayoProviderTest {

	@Autowired
    XmlProviderManager provider

    SAXReader reader = new SAXReader()
    	
    @Before
    public void setup() {
        
    }

    // OfferEvent
    // ====================================================================================

    @Test
    public void offerToXml() {
        Map<String, String> headers = new HashMap<String, String>();
        OfferEvent offer = new OfferEvent();
        offer.setTo(new URI("tel:44477773333333"));
        offer.setFrom(new URI("tel:34637710708"));
        headers.put("test","atest");
        offer.setHeaders(headers);

        assertEquals("""<offer xmlns="urn:xmpp:rayo:1" to="tel:44477773333333" from="tel:34637710708"><header name="test" value="atest"/></offer>""", toXml(offer));
    }
    
    @Test
    public void offerFromXml() {

        assertProperties(fromXml("""<offer xmlns="urn:xmpp:rayo:1" to="tel:44477773333333" from="tel:34637710708"><header name="test" value="atest"/></offer>"""), [
            to: new URI("tel:44477773333333"),
            from: new URI("tel:34637710708"),
            headers: [
                test: "atest"
            ]
        ])

    }
    
    // Join
    // ====================================================================================

	@Test
	public void joinToXmlWithCallId() {
		
		def join = new JoinCommand(direction:Joinable.Direction.DUPLEX, media:JoinType.BRIDGE, force:"true", to:"1234", type: JoinDestinationType.CALL);
		
		assertEquals("""<join xmlns="urn:xmpp:rayo:1" direction="duplex" media="bridge" force="true" call-id="1234"/>""", toXml(join));
	}
	
	@Test
	public void joinToXmlWithMixerName() {
		
		def join = new JoinCommand(direction:Joinable.Direction.DUPLEX, media:JoinType.BRIDGE, to:"1234", type: JoinDestinationType.MIXER);
		
		assertEquals("""<join xmlns="urn:xmpp:rayo:1" direction="duplex" media="bridge" mixer-name="1234"/>""", toXml(join));
	}
		
	@Test
	public void joinCallIdFromXml() {

		def join = fromXml("""<join xmlns="urn:xmpp:rayo:1" direction="duplex" media="bridge" force="true" call-id="1234"/>""")
		assertProperties(join, [
			direction: Joinable.Direction.DUPLEX,
			media: JoinType.BRIDGE,
			to:"1234",
			force: Boolean.TRUE,
			type: JoinDestinationType.CALL
		])
	}
	
	@Test
	public void joinMixerNameFromXml() {

		def join = fromXml("""<join xmlns="urn:xmpp:rayo:1" direction="duplex" media="bridge" mixer-name="1234"/>""")
		assertProperties(join, [
			direction: Joinable.Direction.DUPLEX,
			media: JoinType.BRIDGE,
			to:"1234",
			type: JoinDestinationType.MIXER
		])
	}
	
	// Unjoin
	// ====================================================================================

	@Test
	public void unjoinCallIdToXml() {
		
		def unjoin = new UnjoinCommand(from:"1234", type:JoinDestinationType.CALL);
		
		assertEquals("""<unjoin xmlns="urn:xmpp:rayo:1" call-id="1234"/>""", toXml(unjoin));
	}
	
	@Test
	public void unjoinCallIdFromXml() {
		
		def unjoin = fromXml("""<unjoin xmlns="urn:xmpp:rayo:1" call-id="1234"/>""")
		assertProperties(unjoin, [
			from:"1234",
			type: JoinDestinationType.CALL
		])
	}
	
	@Test
	public void unjoinMixerNameToXml() {
		
		def unjoin = new UnjoinCommand(from:"1234", type:JoinDestinationType.MIXER);
		
		assertEquals("""<unjoin xmlns="urn:xmpp:rayo:1" mixer-name="1234"/>""", toXml(unjoin));
	}
	
	
	@Test
	public void unjoinMixerNameFromXml() {
		
		def unjoin = fromXml("""<unjoin xmlns="urn:xmpp:rayo:1" mixer-name="1234"/>""")
		assertProperties(unjoin, [
			from:"1234",
			type: JoinDestinationType.MIXER
		])
	}
	
	// Unjoined Event
	// ====================================================================================

	@Test
	public void unjoinedCallToXml() {

		def unjoined = new UnjoinedEvent(null, 'abcd', JoinDestinationType.CALL)
		assertEquals("""<unjoined xmlns="urn:xmpp:rayo:1" call-id="abcd"/>""", toXml(unjoined));
	}
	
	@Test
	public void unjoinedMixerToXml() {

		def unjoined = new UnjoinedEvent(null, 'abcd', JoinDestinationType.MIXER)
		assertEquals("""<unjoined xmlns="urn:xmpp:rayo:1" mixer-name="abcd"/>""", toXml(unjoined));
	}
	
	@Test
	public void unjoinedCallFromXml() {

		def unjoined = fromXml("""<unjoined xmlns="urn:xmpp:rayo:1" call-id="abcd"/>""")
		assertProperties(unjoined, [
			from: "abcd",
			type: JoinDestinationType.CALL
		])
	}
	
	@Test
	public void unjoinedMixerFromXml() {

		def unjoined = fromXml("""<unjoined xmlns="urn:xmpp:rayo:1" mixer-name="abcd"/>""")
		assertProperties(unjoined, [
			from: "abcd",
			type: JoinDestinationType.MIXER
		])
	}
	
	// Joined Event
	// ====================================================================================

	@Test
	public void joinedCallToXml() {

		def joined = new JoinedEvent(null, 'abcd', JoinDestinationType.CALL)
		assertEquals("""<joined xmlns="urn:xmpp:rayo:1" call-id="abcd"/>""", toXml(joined));
	}
	
	@Test
	public void joinedMixerToXml() {

		def joined = new JoinedEvent(null, 'abcd', JoinDestinationType.MIXER)
		assertEquals("""<joined xmlns="urn:xmpp:rayo:1" mixer-name="abcd"/>""", toXml(joined));
	}
	
	@Test
	public void joinedCallFromXml() {

		def joined = fromXml("""<joined xmlns="urn:xmpp:rayo:1" call-id="abcd"/>""")
		assertProperties(joined, [
			to: "abcd",
			type: JoinDestinationType.CALL
		])
	}
	
	@Test
	public void joinedMixerFromXml() {

		def joined = fromXml("""<joined xmlns="urn:xmpp:rayo:1" mixer-name="abcd"/>""")
		assertProperties(joined, [
			to: "abcd",
			type: JoinDestinationType.MIXER
		])
	}
	
	// Dial
	// ====================================================================================

	@Test
	public void dialToXml() {
		Map<String, String> headers = new HashMap<String, String>();
		DialCommand command = new DialCommand();
		command.setTo(new URI("tel:44477773333333"));
		command.setFrom(new URI("tel:34637710708"));
		headers.put("test","atest");
		command.setHeaders(headers);

		assertEquals("""<dial xmlns="urn:xmpp:rayo:1" to="tel:44477773333333" from="tel:34637710708"><header name="test" value="atest"/></dial>""", toXml(command));
	}

	@Test
	public void dialWithNestedJoinToXml() {
		
		DialCommand command = new DialCommand();
		command.setTo(new URI("tel:44477773333333"));
		command.setFrom(new URI("tel:34637710708"));
		def join = new JoinCommand();
		command.join = join
		
		assertEquals("""<dial xmlns="urn:xmpp:rayo:1" to="tel:44477773333333" from="tel:34637710708"><join direction="duplex" media="bridge"/></dial>""", toXml(command));
	}
	
	
	@Test
	public void dialWithFullNestedCallJoinToXml() {
		
		DialCommand command = new DialCommand();
		command.setTo(new URI("tel:44477773333333"));
		command.setFrom(new URI("tel:34637710708"));
		def join = new JoinCommand(direction:Joinable.Direction.DUPLEX, media:JoinType.BRIDGE, to:"1234", type:JoinDestinationType.CALL);
		command.join = join
		
		assertEquals("""<dial xmlns="urn:xmpp:rayo:1" to="tel:44477773333333" from="tel:34637710708"><join direction="duplex" media="bridge" call-id="1234"/></dial>""", toXml(command));
	}
	
	@Test
	public void dialWithFullNestedMixerJoinToXml() {
		
		DialCommand command = new DialCommand();
		command.setTo(new URI("tel:44477773333333"));
		command.setFrom(new URI("tel:34637710708"));
		def join = new JoinCommand(direction:Joinable.Direction.DUPLEX, media:JoinType.BRIDGE, to:"1234", type:JoinDestinationType.MIXER);
		command.join = join
		
		assertEquals("""<dial xmlns="urn:xmpp:rayo:1" to="tel:44477773333333" from="tel:34637710708"><join direction="duplex" media="bridge" mixer-name="1234"/></dial>""", toXml(command));
	}
	
	@Test
	public void dialFromXml() {

		assertProperties(fromXml("""<dial xmlns="urn:xmpp:rayo:1" to="tel:44477773333333" from="tel:34637710708"><header name="test" value="atest"/></dial>"""), [
			to: new URI("tel:44477773333333"),
			from: new URI("tel:34637710708"),
			headers: [
				test: "atest"
			]
		])

	}
	
	@Test
	public void dialWithNestedCallFromXml() {

		def dial = fromXml("""<dial xmlns="urn:xmpp:rayo:1" to="tel:44477773333333" from="tel:34637710708"><join xmlns="urn:xmpp:rayo:join:1" call-id="abcd"/></dial>""")
		assertProperties(dial, [
			to: new URI("tel:44477773333333"),
			from: new URI("tel:34637710708"),
		])
		assertProperties(dial.join, [
			direction: Joinable.Direction.DUPLEX,
			media: JoinType.BRIDGE,
			to:"abcd",
			type: JoinDestinationType.CALL
		])
	}
	
	@Test
	public void dialWithNestedMixerFromXml() {

		def dial = fromXml("""<dial xmlns="urn:xmpp:rayo:1" to="tel:44477773333333" from="tel:34637710708"><join xmlns="urn:xmpp:rayo:join:1" mixer-name="abcd"/></dial>""")
		assertProperties(dial, [
			to: new URI("tel:44477773333333"),
			from: new URI("tel:34637710708"),
		])
		assertProperties(dial.join, [
			direction: Joinable.Direction.DUPLEX,
			media: JoinType.BRIDGE,
			to:"abcd",
			type: JoinDestinationType.MIXER
		])
	}
	
	@Test
	public void dialWithNestedJoinFullFromXml() {

		def dial = fromXml("""<dial xmlns="urn:xmpp:rayo:1" to="tel:44477773333333" from="tel:34637710708"><join direction="duplex" media="bridge" call-id="1234"/></dial>""")
		assertProperties(dial, [
			to: new URI("tel:44477773333333"),
			from: new URI("tel:34637710708"),
		])
		assertProperties(dial.join, [
			direction: Joinable.Direction.DUPLEX,
			media: JoinType.BRIDGE,
			to:"1234",
			type:JoinDestinationType.CALL
		])
	}
	
	@Test
	public void dialWithNestedJoinMixerFullFromXml() {

		def dial = fromXml("""<dial xmlns="urn:xmpp:rayo:1" to="tel:44477773333333" from="tel:34637710708"><join direction="duplex" media="bridge" mixer-name="1234"/></dial>""")
		assertProperties(dial, [
			to: new URI("tel:44477773333333"),
			from: new URI("tel:34637710708"),
		])
		assertProperties(dial.join, [
			direction: Joinable.Direction.DUPLEX,
			media: JoinType.BRIDGE,
			to:"1234",
			type:JoinDestinationType.MIXER
		])
	}
		
    // Accept
    // ====================================================================================

    @Test
    public void acceptToXml() {
        AcceptCommand accept = new AcceptCommand();
        assertEquals("""<accept xmlns="urn:xmpp:rayo:1"/>""", toXml(accept));
    }

    @Test
    public void acceptWithHeadersToXml() {
        AcceptCommand accept = new AcceptCommand([
            headers: ["test":"atest"]
        ]);
        assertEquals("""<accept xmlns="urn:xmpp:rayo:1"><header name="test" value="atest"/></accept>""", toXml(accept));
    }
    
    @Test
    public void acceptFromXml() {
        assertNotNull fromXml("""<accept xmlns="urn:xmpp:rayo:1"></accept>""")
    }

    @Test
    public void acceptWithHeadersFromXml() {
        assertProperties(fromXml("""<accept xmlns="urn:xmpp:rayo:1"><header name="test" value="atest"/></accept>"""), [
            headers: [test:"atest"]
        ])
    }
    
    // Answer
    // ====================================================================================
    
    @Test
    public void answerToXml() {
        AnswerCommand answer = new AnswerCommand();
        assertEquals("""<answer xmlns="urn:xmpp:rayo:1"/>""", toXml(answer));
    }
    
    @Test
    public void answerWithHeadersToXml() {
        AnswerCommand answer = new AnswerCommand([
            headers: ["test":"atest"]
        ]);
        assertEquals("""<answer xmlns="urn:xmpp:rayo:1"><header name="test" value="atest"/></answer>""", toXml(answer));
    }
    
    @Test
    public void answerFromXml() {
        assertNotNull fromXml("""<answer xmlns="urn:xmpp:rayo:1"></answer>""")
    }
    
    @Test
    public void answerWithHeadersFromXml() {
        assertProperties(fromXml("""<answer xmlns="urn:xmpp:rayo:1"><header name="test" value="atest"/></answer>"""), [
            headers: [test:"atest"]
        ])
    }

    // Dtmf Event
    // ====================================================================================

    @Test
    public void dtmfToXml() {
        DtmfEvent event = new DtmfEvent(null, "5");
        assertEquals("""<dtmf xmlns="urn:xmpp:rayo:1" signal="5"/>""", toXml(event));
    }
    
	// Record
	// ====================================================================================

	@Test
	public void recordToXml() {
		
		def record = new Record();
		assertEquals("""<record xmlns="urn:xmpp:rayo:record:1"/>""", toXml(record));
	}
	
	@Test
	public void recordFullToXml() {
		
		def record = new Record(to:new URI("file:/tmp/myrecording.mp3"), 
								format:"INFERRED", initialTimeout:new Duration(10000), finalTimeout:new Duration(10000), 
								maxDuration:new Duration(500000), startBeep:true, stopBeep:true,
								startPaused:false);
							
		assertEquals("""<record xmlns="urn:xmpp:rayo:record:1" to="file:/tmp/myrecording.mp3" start-beep="true" stop-beep="true" start-paused="false" final-timeout="10000" format="INFERRED" initial-timeout="10000" max-duration="500000"/>""", toXml(record));
	}

	@Test
	public void recordFromXml() {
		
		assertNotNull fromXml("""<record xmlns="urn:xmpp:rayo:record:1"></record>""")
	}
	
	@Test
	public void recordFullFromXml() {
		
		def record = fromXml("""<record xmlns="urn:xmpp:rayo:record:1" to="file:/tmp/myrecording.mp3" start-beep="true" stop-beep="true" start-paused="true" 
		final-timeout="10000" format="INFERRED" initial-timeout="10000" max-duration="500000"/>""")
		assertProperties(record, [
			to: new URI("file:/tmp/myrecording.mp3"),
			startBeep: true,
			stopBeep: true,
			startPaused: true,
			finalTimeout: new Duration(10000), 
			format: "INFERRED",
			initialTimeout: new Duration(10000),
			maxDuration: new Duration(500000)	
		])
	}
	
	// Record Pause
	// ====================================================================================
	@Test
	public void pauseRecordFromXml() {
		assertNotNull fromXml("""<pause xmlns="urn:xmpp:rayo:record:1" />""")
	}
	
	@Test
	public void pauseRecordToXml() {
		
		RecordPauseCommand pause = new RecordPauseCommand();
		assertEquals("""<pause xmlns="urn:xmpp:rayo:record:1"/>""", toXml(pause));
	}
	
	// Record Resume
	// ====================================================================================
	@Test
	public void resumeRecordFromXml() {
		assertNotNull fromXml("""<resume xmlns="urn:xmpp:rayo:record:1" />""")
	}
	
	@Test
	public void resumeRecordToXml() {
		
		RecordResumeCommand resume = new RecordResumeCommand();
		assertEquals("""<resume xmlns="urn:xmpp:rayo:record:1"/>""", toXml(resume));
	}
	    
    // Hangup
    // ====================================================================================
    
    @Test
    public void hangupToXml() {
        HangupCommand hangup = new HangupCommand();
        assertEquals("""<hangup xmlns="urn:xmpp:rayo:1"/>""", toXml(hangup));
    }
    
    @Test
    public void hangupWithHeadersToXml() {
        HangupCommand hangup = new HangupCommand([
            headers: ["test":"atest"]
        ]);
        assertEquals("""<hangup xmlns="urn:xmpp:rayo:1"><header name="test" value="atest"/></hangup>""", toXml(hangup));
    }
    
    @Test
    public void hangupFromXml() {
        assertNotNull fromXml("""<hangup xmlns="urn:xmpp:rayo:1"></hangup>""")
    }
    
    @Test
    public void hangupWithHeadersFromXml() {
        assertProperties(fromXml("""<hangup xmlns="urn:xmpp:rayo:1"><header name="test" value="atest"/></hangup>"""), [
            headers: [test:"atest"]
        ])
    }
    
    // Reject
    // ====================================================================================
    
    @Test
    public void rejectToXml() {
        RejectCommand reject = new RejectCommand([reason: CallRejectReason.BUSY]);
        assertEquals("""<reject xmlns="urn:xmpp:rayo:1"><busy/></reject>""", toXml(reject));
    }
    
    @Test
    public void rejectWithHeadersToXml() {
        RejectCommand reject = new RejectCommand([
            reason: CallRejectReason.BUSY,
            headers: ["test":"atest"]
        ]);
        assertEquals("""<reject xmlns="urn:xmpp:rayo:1"><busy/><header name="test" value="atest"/></reject>""", toXml(reject));
    }
    
    @Test
    public void rejectFromXml() {
        assertNotNull fromXml("""<reject xmlns="urn:xmpp:rayo:1"><busy/></reject>""")
    }

    @Test
    public void rejectWithHeadersFromXml() {
        assertProperties(fromXml("""<reject xmlns="urn:xmpp:rayo:1"><busy/><header name="test" value="atest"/></reject>"""), [
            headers: [test:"atest"]
        ])
    }
    
    // Redirect
    // ====================================================================================
    
    @Test
    public void redirectToXml() {
        RedirectCommand redirect = new RedirectCommand([
            to: new URI("tel:+14075551212")
        ]);
        assertEquals("""<redirect xmlns="urn:xmpp:rayo:1" to="tel:+14075551212"/>""", toXml(redirect));
    }
    
    @Test
    public void redirectWithHeadersToXml() {
        RedirectCommand redirect = new RedirectCommand([
            to: new URI("tel:+14075551212"),
            headers: ["test":"atest"]
        ]);
        assertEquals("""<redirect xmlns="urn:xmpp:rayo:1" to="tel:+14075551212"><header name="test" value="atest"/></redirect>""", toXml(redirect));
    }
    
    @Test
    public void redirectFromXml() {
        assertNotNull fromXml("""<redirect xmlns="urn:xmpp:rayo:1" to="tel:+14075551212" />""")
    }
    
    @Test
    public void redirectWithHeadersFromXml() {
        assertProperties(fromXml("""<redirect xmlns="urn:xmpp:rayo:1" to="tel:+14075551212"><header name="test" value="atest"/></redirect>"""), [
            to: new URI("tel:+14075551212"),
            headers: [test:"atest"]
        ])
    }
    
    // Pause
    // ====================================================================================
    @Test
    public void pauseFromXml() {
        assertNotNull fromXml("""<pause xmlns="urn:xmpp:tropo:say:1" />""")
    }
    
    @Test
    public void pauseToXml() {
        
        PauseCommand pause = new PauseCommand();
        assertEquals("""<pause xmlns="urn:xmpp:tropo:say:1"/>""", toXml(pause));
    }
    
    // Resume
    // ====================================================================================
    @Test
    public void resumeFromXml() {
        assertNotNull fromXml("""<resume xmlns="urn:xmpp:tropo:say:1" />""")
    }
    
    @Test
    public void resumeToXml() {
        
        ResumeCommand resume = new ResumeCommand();
        assertEquals("""<resume xmlns="urn:xmpp:tropo:say:1"/>""", toXml(resume));
    }
    
    // Stop
    // ====================================================================================
    @Test
    public void stopFromXml() {
        assertNotNull fromXml("""<stop xmlns="urn:xmpp:rayo:ext:1" />""")
    }
    
    @Test
    public void stopToXml() {
        
        StopCommand stop = new StopCommand();
        assertEquals("""<stop xmlns="urn:xmpp:rayo:ext:1"/>""", toXml(stop));
    }

    // Say
    // ====================================================================================

    @Test
    public void audioSayFromXml() {
        
        def say = fromXml("""<say xmlns="urn:xmpp:tropo:say:1" voice="allison"><audio src="http://ccmixter.org/content/DoKashiteru/DoKashiteru_-_you_(na-na-na-na).mp3"/></say>""")
        assertNotNull say
        assertNotNull say.prompt
        assertEquals say.prompt.text, """<audio src="http://ccmixter.org/content/DoKashiteru/DoKashiteru_-_you_(na-na-na-na).mp3"/>"""
    }

    @Test
    public void ssmlSayFromXml() {
        
        def say = fromXml("""<say xmlns="urn:xmpp:tropo:say:1" voice="allison">Hello World</say>""")
        assertNotNull say
        assertNotNull say.prompt
        assertEquals say.prompt.text,"Hello World"

    }

    @Test
    public void ssmlSayWithMultipleElementsFromXml() {
        
        def say = fromXml("""<say xmlns="urn:xmpp:tropo:say:1" voice="allison"><audio src="a.mp3"/><audio src="a.mp3"/></say>""")
        assertNotNull say
        assertNotNull say.prompt
        assertEquals say.prompt.text,"""<audio src="a.mp3"/><audio src="a.mp3"/>"""

    }

    @Test
    public void emptySayToXml() {
        
        Say say = new Say();
        assertEquals("""<say xmlns="urn:xmpp:tropo:say:1"/>""", toXml(say));
    }
    
    @Test
    public void audioSayToXml() {
        
        Say say = new Say();
        say.prompt = new Ssml("""<audio src="http://ccmixter.org/content/DoKashiteru/DoKashiteru_-_you_(na-na-na-na).mp3"/>""")
        say.prompt.voice = "allison"

        assertEquals("""<say xmlns="urn:xmpp:tropo:say:1" voice="allison"><audio xmlns="" src="http://ccmixter.org/content/DoKashiteru/DoKashiteru_-_you_(na-na-na-na).mp3"/></say>""", toXml(say));
    }
    
    @Test
    public void ssmlSayToXml() {
        
        Say say = new Say();
        say.prompt = new Ssml("Hello World.")
        say.prompt.voice = "allison"

        assertEquals("""<say xmlns="urn:xmpp:tropo:say:1" voice="allison">Hello World.</say>""", toXml(say));
    }

    @Test
    public void ssmlSayWithMultipleElementsToXml() {
        
        Say say = new Say();
        say.prompt = new Ssml("""<audio src="a.mp3"/><audio src="b.mp3"/>""")
        say.prompt.voice = "allison"

        assertEquals("""<say xmlns="urn:xmpp:tropo:say:1" voice="allison"><audio xmlns="" src="a.mp3"/><audio xmlns="" src="b.mp3"/></say>""", toXml(say));
    }

    // Ask
    // ====================================================================================
    @Test
    public void askFromXml() {
        
        def ask = fromXml("""<ask xmlns="urn:xmpp:tropo:ask:1" min-confidence="0.8" mode="dtmf" recognizer="en-us" terminator="#" timeout="3000" bargein="true"><choices content-type="application/grammar+voxeo">a,b</choices><prompt voice="allison">hello world</prompt></ask>""")
        assertNotNull ask
        assertEquals ask.prompt.voice, "allison"
        assertTrue ask.minConfidence == 0.8f
        assertEquals ask.mode, InputMode.DTMF
        assertEquals ask.recognizer,"en-us"
        assertEquals ask.terminator,'#' as char
        assertEquals ask.timeout, new Duration(3000)
    }

    @Test
    public void audioAskFromXml() {
        
        Ask ask = fromXml("""<ask xmlns="urn:xmpp:tropo:ask:1" voice="allison" min-confidence="0.8" mode="dtmf" recognizer="en-us" terminator="#" timeout="3000" bargein="true"><prompt><audio src="http://ccmixter.org/content/DoKashiteru/DoKashiteru_-_you_(na-na-na-na).mp3"/></prompt><choices content-type="application/grammar+voxeo">a,b</choices></ask>""")
        assertNotNull ask
        assertNotNull ask.prompt
        assertEquals ask.prompt.text,"""<audio src="http://ccmixter.org/content/DoKashiteru/DoKashiteru_-_you_(na-na-na-na).mp3"/>"""
    }
    
    @Test
    public void ssmlAskFromXml() {
        
        Ask ask = fromXml("""<ask xmlns="urn:xmpp:tropo:ask:1" voice="allison" min-confidence="0.8" mode="dtmf" recognizer="en-us" terminator="#" timeout="3000" bargein="true"><prompt>Hello World.</prompt><choices content-type="application/grammar+voxeo">a,b</choices></ask>""")
        assertNotNull ask
        assertNotNull ask.prompt
        assertEquals ask.prompt.text,"Hello World."
    }
    
    @Test
    public void choicesAskFromXml() {
        
        def ask = fromXml("""<ask xmlns="urn:xmpp:tropo:ask:1" voice="allison" min-confidence="0.8" mode="dtmf" recognizer="en-us" terminator="#" timeout="3000" bargein="true"><prompt>Hello World.</prompt><choices url="http://test" content-type="grxml" /></ask>""")
        assertNotNull ask
        assertNotNull ask.choices
        assertEquals ask.choices.size(),1
        assertEquals ask.choices[0].uri,new URI("http://test")
        assertEquals ask.choices[0].contentType, "grxml"
        assertNotNull ask.choices
    }
    
    @Test
    public void emptyAskToXml() {
        
        def ask = new Ask()
        assertEquals("""<ask xmlns="urn:xmpp:tropo:ask:1" min-confidence="0.3" mode="any" timeout="30000" bargein="true"/>""", toXml(ask));
    }
    
    @Test
    public void askToXml() {
        
        def ask = new Ask()
        ask.voice = "allison"
        ask.minConfidence = 0.8f
        ask.mode = InputMode.DTMF
        ask.recognizer = 'test'
        ask.terminator = '#' as char
        ask.timeout = new Duration(3000)

        assertEquals("""<ask xmlns="urn:xmpp:tropo:ask:1" voice="allison" min-confidence="0.8" mode="dtmf" recognizer="test" terminator="#" timeout="3000" bargein="true"/>""", toXml(ask));
    }
    
    @Test
    public void audioAskToXml() {
        
        def ask = new Ask()
        ask.voice = "allison"
        ask.minConfidence = 0.8f
        ask.mode = InputMode.DTMF
        ask.recognizer = 'test'
        ask.terminator = '#' as char
        ask.timeout = new Duration(3000)
        ask.prompt = new Ssml("""<audio src='http://ccmixter.org/content/DoKashiteru/DoKashiteru_-_you_(na-na-na-na).mp3' />""")

        assertEquals("""<ask xmlns="urn:xmpp:tropo:ask:1" voice="allison" min-confidence="0.8" mode="dtmf" recognizer="test" terminator="#" timeout="3000" bargein="true"><prompt><audio xmlns="" src="http://ccmixter.org/content/DoKashiteru/DoKashiteru_-_you_(na-na-na-na).mp3"/></prompt></ask>""", toXml(ask));
    }
    
    @Test
    public void ssmlAskToXml() {
        
        def ask = new Ask()
        ask.voice = "allison"
        ask.minConfidence = 0.8f
        ask.mode = InputMode.DTMF
        ask.recognizer = 'test'
        ask.terminator = '#' as char
        ask.timeout = new Duration(3000)
        ask.prompt = new Ssml("Hello World.")

        assertEquals("""<ask xmlns="urn:xmpp:tropo:ask:1" voice="allison" min-confidence="0.8" mode="dtmf" recognizer="test" terminator="#" timeout="3000" bargein="true"><prompt>Hello World.</prompt></ask>""", toXml(ask));
    }
    
    @Test
    public void choicesAskToXml() {
        
        def ask = new Ask()
        ask.voice = "allison"
        ask.minConfidence = 0.8f
        ask.mode = InputMode.DTMF
        ask.recognizer = 'en-us'
        ask.terminator = '#' as char
        ask.timeout = new Duration(3000)
        ask.prompt = new Ssml("Hello World.")
        ask.choices = []
        ask.choices.add new Choices(uri:new URI("http://test"), contentType:"vxml", content:"sales,support")

        assertEquals("""<ask xmlns="urn:xmpp:tropo:ask:1" voice="allison" min-confidence="0.8" mode="dtmf" recognizer="en-us" terminator="#" timeout="3000" bargein="true"><prompt>Hello World.</prompt><choices content-type="vxml" url="http://test"><![CDATA[sales,support]]></choices></ask>""", toXml(ask));
    }
    
    // Transfer
    // ====================================================================================
    @Test
    public void transferFromXml() {
        
        def transfer = fromXml("""<transfer xmlns="urn:xmpp:tropo:transfer:1" terminator="#" timeout="20000"><to>sip:martin@127.0.0.1:6089</to></transfer>""")
        assertNotNull transfer
        assertEquals transfer.terminator, '#' as char
        assertEquals transfer.timeout, new Duration(20000)
    }
    
    @Test
    public void transferToItemsFromXml() {
        
        def transfer = fromXml("""<transfer xmlns="urn:xmpp:tropo:transfer:1" terminator="#" timeout="20000"><to>sip:martin@127.0.0.1:6089</to><to>sip:jose@127.0.0.1:6088</to></transfer>""")
        assertNotNull transfer
        assertEquals transfer.to.size(),2
        assertEquals transfer.to[0].toString(),"sip:martin@127.0.0.1:6089"
        assertEquals transfer.to[1].toString(),"sip:jose@127.0.0.1:6088"
        assertNull transfer.ringbackTone
    }
    
    @Test
    public void transferToAttributeFromXml() {
        
        def transfer = fromXml("""<transfer xmlns="urn:xmpp:tropo:transfer:1" terminator="#" timeout="20000" to="sip:martin@127.0.0.1:6089"></transfer>""")
        assertNotNull transfer
        assertEquals transfer.to.size(),1
        assertEquals transfer.to[0].toString(),"sip:martin@127.0.0.1:6089"
    }

	@Test	
	public void transferOneToElement() {
		
		def transfer = fromXml("""<transfer xmlns="urn:xmpp:tropo:transfer:1" from="sip:name@connfu.com"><to>sip:8517c60c-39a6-4bce-8d21-9df2b6b1ad8c@gw113.phono.com</to></transfer>""")
		assertNotNull transfer
		assertEquals transfer.to.size(),1
		assertEquals transfer.to[0].toString(),"sip:8517c60c-39a6-4bce-8d21-9df2b6b1ad8c@gw113.phono.com"
		assertNotNull transfer.from
	}
    
    @Test
    public void transferMixedToItemsFromXml() {
        
        def transfer = fromXml("""<transfer xmlns="urn:xmpp:tropo:transfer:1" terminator="#" timeout="20000" to="sip:martin@127.0.0.1:6089"><to>sip:jose@127.0.0.1:6088</to></transfer>""")
        assertNotNull transfer
        assertEquals transfer.to.size(),2
        assertEquals transfer.to[0].toString(),"sip:martin@127.0.0.1:6089"
        assertEquals transfer.to[1].toString(),"sip:jose@127.0.0.1:6088"
    }
	
	@Test
	public void directMediaTransferFromXml() {
		
		def transfer = fromXml("""<transfer xmlns="urn:xmpp:tropo:transfer:1" terminator="#" timeout="20000" media="direct" to="sip:martin@127.0.0.1:6089"><to>sip:jose@127.0.0.1:6088</to></transfer>""")
		assertNotNull transfer
		assertEquals transfer.media, MediaType.DIRECT
	}
	
    @Test
    public void audioTransferFromXml() {
        
        Transfer transfer = fromXml("""<transfer xmlns="urn:xmpp:tropo:transfer:1" terminator="#" timeout="20000"><ringback><audio url="http://ccmixter.org/content/DoKashiteru/DoKashiteru_-_you_(na-na-na-na).mp3"/></ringback><to>sip:martin@127.0.0.1:6089</to></transfer>""")
        assertNotNull transfer
        assertNotNull transfer.ringbackTone
        assertEquals transfer.ringbackTone.text,"""<audio url="http://ccmixter.org/content/DoKashiteru/DoKashiteru_-_you_(na-na-na-na).mp3"/>"""
    }
    
    @Test
    public void ssmlTransferFromXml() {
        
        Transfer transfer = fromXml("""<transfer xmlns="urn:xmpp:tropo:transfer:1" voice="allison" terminator="#" timeout="20000"><ringback>We are going to transfer your call. Wait a couple of seconds.</ringback><to>sip:martin@127.0.0.1:6089</to></transfer>""")
        assertNotNull transfer
        assertNotNull transfer.ringbackTone
        assertEquals transfer.ringbackTone.text,"We are going to transfer your call. Wait a couple of seconds."
    }
    
    @Test
    public void emptyTransferToXml() {
        
        def transfer = new Transfer()
        assertEquals("""<transfer xmlns="urn:xmpp:tropo:transfer:1" terminator="#" timeout="30000" media="bridge" answer-on-media="false"/>""", toXml(transfer));
    }
    
    @Test
    public void transferToXml() {
        
        def transfer = new Transfer()
        transfer.timeout = new Duration(20000)
        transfer.terminator = '#' as char
        transfer.to = [new URI("sip:martin@127.0.0.1:6089")]

        assertEquals("""<transfer xmlns="urn:xmpp:tropo:transfer:1" terminator="#" timeout="20000" media="bridge" to="sip:martin@127.0.0.1:6089" answer-on-media="false"/>""", toXml(transfer));
    }

	        
    @Test
    public void audioTransferToXml() {
        
        def transfer = new Transfer()
        transfer.timeout = new Duration(20000)
        transfer.terminator = '#' as char
        transfer.to = [new URI("sip:martin@127.0.0.1:6089")]
        transfer.ringbackTone = new Ssml("""<audio src="http://ccmixter.org/content/DoKashiteru/DoKashiteru_-_you_(na-na-na-na).mp3">Please wait while your call is being transfered.</audio>""")

        assertEquals("""<transfer xmlns="urn:xmpp:tropo:transfer:1" terminator="#" timeout="20000" media="bridge" to="sip:martin@127.0.0.1:6089" answer-on-media="false"><ringback><audio xmlns="" src="http://ccmixter.org/content/DoKashiteru/DoKashiteru_-_you_(na-na-na-na).mp3">Please wait while your call is being transfered.</audio></ringback></transfer>""", toXml(transfer));
    }
    
    @Test
    public void ssmlTransferToXml() {
        
        def transfer = new Transfer()
        transfer.timeout = new Duration(20000)
        transfer.terminator = '#' as char
        transfer.to = [new URI("sip:martin@127.0.0.1:6089")]
        transfer.ringbackTone = new Ssml("We are going to transfer your call. Wait a couple of seconds.")

        assertEquals("""<transfer xmlns="urn:xmpp:tropo:transfer:1" terminator="#" timeout="20000" media="bridge" to="sip:martin@127.0.0.1:6089" answer-on-media="false"><ringback>We are going to transfer your call. Wait a couple of seconds.</ringback></transfer>""", toXml(transfer));
    }
    
    @Test
    public void transferWithMultipleUrisToXml() {
        
        def transfer = new Transfer()
        transfer.timeout = new Duration(20000)
        transfer.terminator = '#' as char
        transfer.to = [new URI("sip:martin@127.0.0.1:6089"),new URI("sip:jose@127.0.0.1:6088")]
        transfer.ringbackTone = new Ssml("We are going to transfer your call. Wait a couple of seconds.")

        assertEquals("""<transfer xmlns="urn:xmpp:tropo:transfer:1" terminator="#" timeout="20000" media="bridge" answer-on-media="false"><ringback>We are going to transfer your call. Wait a couple of seconds.</ringback><to>sip:martin@127.0.0.1:6089</to><to>sip:jose@127.0.0.1:6088</to></transfer>""", toXml(transfer));
    }
    
    // Ask Complete
    // ====================================================================================
    
    @Test
    public void askCompleteFromXml() {
        
        def complete = fromXml("""
            <complete xmlns="urn:xmpp:rayo:ext:1">
                <success confidence="0.7" mode="voice" xmlns="urn:xmpp:tropo:ask:complete:1">
                    <interpretation>aninterpretation</interpretation>
                    <utterance>anutterance</utterance>
                </success>
            </complete>""")
        assertNotNull complete
        assertEquals complete.reason, AskCompleteEvent.Reason.SUCCESS
        assertTrue complete.confidence == 0.7f
        assertTrue complete.mode == InputMode.VOICE
        assertEquals complete.interpretation, "aninterpretation"
        assertEquals complete.utterance, "anutterance"
    }
    
    @Test
    public void askCompleteWithErrorsFromXml() {
        
        def complete = fromXml("""<complete xmlns="urn:xmpp:rayo:ext:1"><error xmlns="urn:xmpp:rayo:ext:complete:1">this is an error</error></complete>""")
        assertNotNull complete
        assertEquals complete.reason, VerbCompleteEvent.Reason.ERROR
        assertEquals complete.errorText, "this is an error"
    }
    
    @Test
    public void emptyAskCompleteToXml() {
        
        def complete = new AskCompleteEvent(new Ask(), VerbCompleteEvent.Reason.HANGUP)
        assertEquals("""<complete xmlns="urn:xmpp:rayo:ext:1"><hangup xmlns="urn:xmpp:rayo:ext:complete:1"/></complete>""", toXml(complete));
    }

    @Test
    public void askCompleteToXml() {
        
        def complete = new AskCompleteEvent(new Ask(), AskCompleteEvent.Reason.SUCCESS)
        complete.mode = InputMode.VOICE
        complete.confidence = 0.7f
        complete.interpretation = "aninterpretation"
        complete.utterance = "anutterance"
        
        assertEquals("""<complete xmlns="urn:xmpp:rayo:ext:1"><success xmlns="urn:xmpp:tropo:ask:complete:1" confidence="0.7" mode="voice"><interpretation>aninterpretation</interpretation><utterance>anutterance</utterance></success></complete>""", toXml(complete));
    }
    
    @Test
    public void askCompleteWithErrorsToXml() {
        
        def complete = new AskCompleteEvent(new Ask(), VerbCompleteEvent.Reason.ERROR)
        complete.errorText = "this is an error"
        
        assertEquals("""<complete xmlns="urn:xmpp:rayo:ext:1"><error xmlns="urn:xmpp:rayo:ext:complete:1">this is an error</error></complete>""", toXml(complete));
    }
    
    // Say Complete
    // ====================================================================================
    @Test
    public void sayCompleteFromXml() {
        
        def complete = fromXml("""<complete xmlns="urn:xmpp:rayo:ext:complete:1"><hangup/></complete>""")
        assertNotNull complete
        assertEquals complete.reason, VerbCompleteEvent.Reason.HANGUP
    }

	@Test
	public void sayCompleteSuccessFromXml() {
		
		def complete = fromXml("""<complete xmlns="urn:xmpp:rayo:ext:1"><success xmlns="urn:xmpp:tropo:say:complete:1"/></complete>""")
		assertNotNull complete
		assertEquals complete.reason, SayCompleteEvent.Reason.SUCCESS
	}

    @Test
    public void sayCompleteWithErrorsFromXml() {
        
        def complete = fromXml("""<complete xmlns="urn:xmpp:rayo:ext:1"><error xmlns="urn:xmpp:rayo:ext:complete:1">this is an error</error></complete>""")
        assertNotNull complete
        assertEquals complete.reason, VerbCompleteEvent.Reason.ERROR
        assertEquals complete.errorText, "this is an error"
    }
    
    @Test
    public void sayCompleteToXml() {
        
        def complete = new SayCompleteEvent(new Say(), VerbCompleteEvent.Reason.HANGUP)
        
        assertEquals("""<complete xmlns="urn:xmpp:rayo:ext:1"><hangup xmlns="urn:xmpp:rayo:ext:complete:1"/></complete>""", toXml(complete));
    }
	
	
	@Test
	public void sayCompleteSuccessToXml() {
		
		def complete = new SayCompleteEvent(new Say(), SayCompleteEvent.Reason.SUCCESS)
		
		assertEquals("""<complete xmlns="urn:xmpp:rayo:ext:1"><success xmlns="urn:xmpp:tropo:say:complete:1"/></complete>""", toXml(complete));
	}
    
    @Test
    public void sayCompleteWithErrorsToXml() {
        
        def complete = new SayCompleteEvent(new Say(), VerbCompleteEvent.Reason.ERROR)
        complete.errorText = "this is an error"
        
        assertEquals("""<complete xmlns="urn:xmpp:rayo:ext:1"><error xmlns="urn:xmpp:rayo:ext:complete:1">this is an error</error></complete>""", toXml(complete));
    }
    
    // Transfer Complete
    // ====================================================================================
    @Test
    public void transferCompleteFromXml() {
        
        def complete = fromXml("""<complete xmlns="urn:xmpp:rayo:ext:1"><hangup xmlns="urn:xmpp:rayo:ext:complete:1"/></complete>""")
        assertNotNull complete
        assertEquals complete.reason, VerbCompleteEvent.Reason.HANGUP
    }
    
    @Test
    public void transferCompleteWithErrorsFromXml() {
        
        def complete = fromXml("""<complete xmlns="urn:xmpp:rayo:ext:1"><error xmlns="urn:xmpp:rayo:ext:complete:1">this is an error</error></complete>""")
        assertNotNull complete
        assertEquals complete.reason, VerbCompleteEvent.Reason.ERROR
        assertEquals complete.errorText, "this is an error"
    }
    
    @Test
    public void transferCompleteToXml() {
        
        def complete = new TransferCompleteEvent(new Transfer(), VerbCompleteEvent.Reason.HANGUP)
        
        assertEquals("""<complete xmlns="urn:xmpp:rayo:ext:1"><hangup xmlns="urn:xmpp:rayo:ext:complete:1"/></complete>""", toXml(complete));
    }
    
    @Test
    public void transferCompleteWithErrorsToXml() {
        
        def complete = new TransferCompleteEvent(new Transfer(), VerbCompleteEvent.Reason.ERROR)
        complete.errorText = "this is an error"
        
        assertEquals("""<complete xmlns="urn:xmpp:rayo:ext:1"><error xmlns="urn:xmpp:rayo:ext:complete:1">this is an error</error></complete>""", toXml(complete));
    }
    
	// Record Complete
	// ====================================================================================
	
	@Test
	public void recordCompleteFromXml() {
		
		def complete = fromXml("""
			<complete xmlns="urn:xmpp:rayo:ext:1">
				<stop xmlns="urn:xmpp:rayo:ext:complete:1"/>
				<recording xmlns="urn:xmpp:rayo:record:complete:1" uri="file:///tmp/abc.mp3" size="12000" duration="35000"/>
			</complete>""")
		assertNotNull complete
		
		assertProperties (complete, [
			reason: VerbCompleteEvent.Reason.STOP,
			uri: new URI("file:///tmp/abc.mp3"),
			size: 12000,
			duration: new Duration(35000)
		])
	}
	
	@Test
	public void recordCompleteWithErrorsFromXml() {
		
		def complete = fromXml("""<complete xmlns="urn:xmpp:rayo:ext:1"><error xmlns="urn:xmpp:rayo:ext:complete:1">this is an error</error></complete>""")
		assertNotNull complete
		assertEquals complete.reason, VerbCompleteEvent.Reason.ERROR
		assertEquals complete.errorText, "this is an error"
	}

	@Test
	public void recordCompleteToXml() {
		
		def complete = new RecordCompleteEvent(new Record(), RecordCompleteEvent.Reason.SUCCESS)
		complete.uri = new URI("file:///tmp/abc.mp3")
		complete.duration = new Duration(35000)
		complete.size = 12000
		
		assertEquals("""<complete xmlns="urn:xmpp:rayo:ext:1"><success xmlns="urn:xmpp:rayo:record:complete:1"/><recording xmlns="urn:xmpp:rayo:record:complete:1" uri="file:///tmp/abc.mp3" size="12000" duration="35000"/></complete>""", toXml(complete));
	}
	
	@Test
	public void recordCompleteWithErrorsToXml() {
		
		def complete = new RecordCompleteEvent(new Record(), VerbCompleteEvent.Reason.ERROR)
		complete.errorText = "this is an error"
		
		assertEquals("""<complete xmlns="urn:xmpp:rayo:ext:1"><error xmlns="urn:xmpp:rayo:ext:complete:1">this is an error</error></complete>""", toXml(complete));
	}
	
    // Output
    // ====================================================================================
    
    @Test
    public void output() {
        
        def output = new Output([
            bargeinType: BargeinType.ANY,
            startOffset: new Duration(2000),
            startPaused: true,
            repeatInterval : new Duration(2000),
            repeatTimes: 10,
            maxTime: new Duration(2000),
            voice: "bling",
            prompt: new Ssml("hello world")
        ])

        assertEquals("""<output xmlns="urn:xmpp:rayo:output:1" interrupt-on="any" start-offset="2000" start-paused="true" repeat-interval="2000" repeat-times="10" max-time="2000" voice="bling">hello world</output>""", toXml(output));
    }

	@Test
	public void outputFromXml() {
		
		def output = fromXml("""<output xmlns="urn:xmpp:rayo:output:1" bargein="ANY" interrupt-on="any" start-offset="2000" start-paused="true" repeat-interval="2000" repeat-times="10" max-time="2000" voice="bling">hello world</output>""")

		def ssml = new Ssml("hello world")
		ssml.voice = "bling"
		
		assertProperties(output, [
            bargeinType: BargeinType.ANY,
            startOffset: new Duration(2000),
            startPaused: true,
            repeatInterval : new Duration(2000),
            repeatTimes: 10,
            maxTime: new Duration(2000),
            voice: "bling",
		])
		assertEquals output.prompt.toString(),ssml.toString()
	}

	@Test
	public void ssmlOutputFromXml() {
		
		def output = fromXml("""<output xmlns="urn:xmpp:rayo:output:1" voice="allison">Hello World</output>""")
		assertNotNull output
		assertNotNull output.prompt
		assertEquals output.prompt.text,"Hello World"

	}

	@Test
	public void ssmlOutputWithMultipleElementsFromXml() {
		
		def output = fromXml("""<output xmlns="urn:xmpp:rayo:output:1" voice="allison"><audio src="a.mp3"/><audio src="a.mp3"/></output>""")
		assertNotNull output
		assertNotNull output.prompt
		assertEquals output.prompt.text,"""<audio src="a.mp3"/><audio src="a.mp3"/>"""

	}

	@Test
	public void emptyOutputToXml() {
		
		Output output = new Output();
		assertEquals("""<output xmlns="urn:xmpp:rayo:output:1"/>""", toXml(output));
	}
	
	@Test
	public void audioOutputToXml() {
		
		Output output = new Output();
		output.prompt = new Ssml("""<audio src="http://ccmixter.org/content/DoKashiteru/DoKashiteru_-_you_(na-na-na-na).mp3"/>""")
		output.prompt.voice = "allison"

		assertEquals("""<output xmlns="urn:xmpp:rayo:output:1" voice="allison"><audio xmlns="" src="http://ccmixter.org/content/DoKashiteru/DoKashiteru_-_you_(na-na-na-na).mp3"/></output>""", toXml(output));
	}
	
	@Test
	public void ssmlOutputToXml() {
		
		Output output = new Output();
		output.prompt = new Ssml("Hello World.")
		output.prompt.voice = "allison"

		assertEquals("""<output xmlns="urn:xmpp:rayo:output:1" voice="allison">Hello World.</output>""", toXml(output));
	}

	@Test
	public void ssmlOutputWithMultipleElementsToXml() {
		
		Output output = new Output();
		output.prompt = new Ssml("""<audio src="a.mp3"/><audio src="b.mp3"/>""")
		output.prompt.voice = "allison"

		assertEquals("""<output xmlns="urn:xmpp:rayo:output:1" voice="allison"><audio xmlns="" src="a.mp3"/><audio xmlns="" src="b.mp3"/></output>""", toXml(output));
	}

	// Output Complete
	// ====================================================================================
	
	@Test
	public void outputCompleteFromXml() {
		
		def complete = fromXml("""<complete xmlns="urn:xmpp:rayo:ext:1"><success xmlns="urn:xmpp:rayo:output:complete:1"/></complete>""")
        assertNotNull complete
        assertEquals complete.reason, OutputCompleteEvent.Reason.SUCCESS
	}
	
	@Test
	public void outputCompleteToXml() {
		
		def complete = new OutputCompleteEvent(new Output(), OutputCompleteEvent.Reason.SUCCESS)
		
		assertEquals("""<complete xmlns="urn:xmpp:rayo:ext:1"><success xmlns="urn:xmpp:rayo:output:complete:1"/></complete>""", toXml(complete));
	}
	
	// Input
	// ====================================================================================
	@Test
	public void inputFromXml() {
		
		def input = fromXml("""<input xmlns="urn:xmpp:rayo:input:1" min-confidence="0.8" sensitivity="0.3" mode="dtmf" recognizer="en-us" max-silence="2000" terminator="#" initial-timeout="3000" inter-digit-timeout="1000"><grammar content-type="application/grammar+voxeo">a,b</grammar></input>""")
		assertNotNull input
		assertProperties(input, [
			mode:InputMode.DTMF,
			recognizer:"en-us",
			terminator:Character.valueOf('#' as char), 
			initialTimeout: new Duration(3000),
			interDigitTimeout: new Duration(1000),
			maxSilence: new Duration(2000)
		])
		assertEquals input.minConfidence, 0.8f, 0
		assertEquals input.sensitivity, 0.3f, 0
		
		assertNotNull input.grammars
		assertEquals input.grammars.size(),1
		assertEquals input.grammars[0].content,"a,b"
		assertEquals input.grammars[0].contentType, "application/grammar+voxeo"
	}

	
	@Test
	public void grammarInputFromXml() {

		def input = fromXml("""<input xmlns="urn:xmpp:rayo:input:1" min-confidence="0.8" mode="dtmf" recognizer="en-us" terminator="#" initial-timeout="3000" inter-digit-timeout="1000"><grammar url="http://test" content-type="grxml"/></input>""")
		assertNotNull input
		assertNotNull input.grammars
		assertEquals input.grammars.size(),1
		assertEquals input.grammars[0].uri,new URI("http://test")
		assertEquals input.grammars[0].contentType, "grxml"
	}
	
	@Test
	public void multipleGrammarsInputFromXml() {

		def input = fromXml("""<input xmlns="urn:xmpp:rayo:input:1" min-confidence="0.8" mode="dtmf" recognizer="en-us" terminator="#" initial-timeout="3000" inter-digit-timeout="1000"><grammar url="http://test" content-type="grxml"/><grammar content-type="application/grammar+voxeo"><![CDATA[a,b]]></grammar></input>""")
		assertNotNull input
		assertNotNull input.grammars
		assertEquals input.grammars.size(),2
		assertEquals input.grammars[0].uri,new URI("http://test")
		assertEquals input.grammars[0].contentType, "grxml"
		assertEquals input.grammars[1].content,"a,b"
		assertEquals input.grammars[1].contentType, "application/grammar+voxeo"

	}
	
	@Test
	public void emptyInputToXml() {
		
		def input = new Input()
		assertEquals("""<input xmlns="urn:xmpp:rayo:input:1" min-confidence="0.3" mode="ANY"/>""", toXml(input));
	}
	
	@Test
	public void inputToXml() {
		
		def input = new Input()
		input.minConfidence = 0.8f
		input.mode = InputMode.DTMF
		input.recognizer = 'en-us'
		input.sensitivity = 0.3f
		input.terminator = '#' as char
		input.initialTimeout = new Duration(3000)
		input.interDigitTimeout = new Duration(1000)
		input.maxSilence = new Duration(2000)
		
		assertEquals("""<input xmlns="urn:xmpp:rayo:input:1" min-confidence="0.8" initial-timeout="3000" mode="DTMF" inter-digit-timeout="1000" recognizer="en-us" sensitivity="0.3" max-silence="2000" terminator="#"/>""", toXml(input));
	}
	
	@Test
	public void grammarInputToXml() {
		
		def input = new Input()
		input.minConfidence = 0.8f
		input.sensitivity = 0.3f
		input.mode = InputMode.DTMF
		input.recognizer = 'en-us'
		input.terminator = '#' as char
		input.initialTimeout = new Duration(3000)
		input.interDigitTimeout = new Duration(1000)
		input.grammars = []
		input.grammars.add new Choices(uri:new URI("http://test"), contentType:"vxml", content:"sales,support")

		assertEquals("""<input xmlns="urn:xmpp:rayo:input:1" min-confidence="0.8" initial-timeout="3000" mode="DTMF" inter-digit-timeout="1000" recognizer="en-us" sensitivity="0.3" terminator="#"><grammar content-type="vxml" url="http://test"><![CDATA[sales,support]]></grammar></input>""", toXml(input));
	}

	@Test
	public void multipleGrammarsInputToXml() {
		
		def input = new Input()
		input.minConfidence = 0.8f
		input.sensitivity = 0.3f
		input.mode = InputMode.DTMF
		input.recognizer = 'en-us'
		input.terminator = '#' as char
		input.initialTimeout = new Duration(3000)
		input.interDigitTimeout = new Duration(1000)
		input.grammars = []
		input.grammars.add new Choices(uri:new URI("http://test"), contentType:"vxml", content:"sales,support")
		input.grammars.add new Choices(content:"a,b", contentType:"application/grammar+voxeo")
		
		assertEquals("""<input xmlns="urn:xmpp:rayo:input:1" min-confidence="0.8" initial-timeout="3000" mode="DTMF" inter-digit-timeout="1000" recognizer="en-us" sensitivity="0.3" terminator="#"><grammar content-type="vxml" url="http://test"><![CDATA[sales,support]]></grammar><grammar content-type="application/grammar+voxeo"><![CDATA[a,b]]></grammar></input>""", toXml(input));
	}
	
	// Input Complete
	// ====================================================================================
	
	@Test
	public void inputCompleteFromXml() {
		
		def complete = fromXml("""<complete xmlns="urn:xmpp:rayo:ext:1"><success xmlns="urn:xmpp:rayo:input:complete:1" confidence="0.65" mode="voice"><interpretation>yes</interpretation><utterance>yes</utterance><tag>yes</tag><concept>yes</concept>
			<nlsml>&lt;?xml version="1.0"?&gt;
				&lt;result grammar="0@c898304f.vxmlgrammar"&gt;
				&lt;interpretation grammar="0@c898304f.vxmlgrammar" confidence="65"&gt;
				&lt;input mode="speech"&gt;yes&lt;/input&gt;
				&lt;/interpretation&gt;
				&lt;/result&gt;
			</nlsml></success></complete>""")
		assertNotNull complete
		
		assertProperties(complete, [
			reason: InputCompleteEvent.Reason.SUCCESS,
			mode: InputMode.VOICE,
			interpretation: "yes",
			concept: "yes",
			utterance: "yes", 
			tag: "yes"
		])
	}

	@Test
	public void inputCompleteToXml() {
		
		def event = new InputCompleteEvent(reason: InputCompleteEvent.Reason.SUCCESS, confidence:0.65, mode:InputMode.VOICE, interpretation:"yes", utterance:"yes", tag:"yes", concept:"yes")
		
		assertEquals toXml(event), """<complete xmlns="urn:xmpp:rayo:ext:1"><success xmlns="urn:xmpp:rayo:input:complete:1" confidence="0.65" mode="voice"><interpretation>yes</interpretation><utterance>yes</utterance><tag>yes</tag><concept>yes</concept></success></complete>"""
	}
	
	@Test
	public void speedUpToXml() {
		
		def speedUp = new SpeedUpCommand()

		assertEquals("""<speed-up xmlns="urn:xmpp:rayo:output:1"/>""", toXml(speedUp));
	}

	@Test
	public void speedUpFromXml() {
		
		def speedUp = fromXml("""<speed-up xmlns='urn:xmpp:rayo:output:1' />""")

		assertNotNull speedUp
		assertTrue speedUp instanceof SpeedUpCommand
	}
	
	
	@Test
	public void speedDownToXml() {
		
		def speedDown = new SpeedDownCommand()

		assertEquals("""<speed-down xmlns="urn:xmpp:rayo:output:1"/>""", toXml(speedDown));
	}

	@Test
	public void speedDownFromXml() {
		
		def speedDown = fromXml("""<speed-down xmlns='urn:xmpp:rayo:output:1' />""")

		assertNotNull speedDown
		assertTrue speedDown instanceof SpeedDownCommand
	}
	
	@Test
	public void volumeUpToXml() {
		
		def volumeUp = new VolumeUpCommand()

		assertEquals("""<volume-up xmlns="urn:xmpp:rayo:output:1"/>""", toXml(volumeUp));
	}

	@Test
	public void volumeUpFromXml() {
		
		def volumeUp = fromXml("""<volume-up xmlns='urn:xmpp:rayo:output:1' />""")

		assertNotNull volumeUp
		assertTrue volumeUp instanceof VolumeUpCommand
	}
	
	
	@Test
	public void volumeDownToXml() {
		
		def volumeDown = new VolumeDownCommand()

		assertEquals("""<volume-down xmlns="urn:xmpp:rayo:output:1"/>""", toXml(volumeDown));
	}

	@Test
	public void volumeDownFromXml() {
		
		def volumeDown = fromXml("""<volume-down xmlns='urn:xmpp:rayo:output:1' />""")

		assertNotNull volumeDown
		assertTrue volumeDown instanceof VolumeDownCommand
	}
	
	@Test
	public void seekToXml() {
		
		def seek = new SeekCommand(direction:SeekCommand.Direction.FORWARD, amount:10000)

		assertEquals("""<seek xmlns="urn:xmpp:rayo:output:1" amount="10000" direction="FORWARD"/>""", toXml(seek));
	}

	@Test
	public void seekFromXml() {
		
		def seek = fromXml("""<seek xmlns="urn:xmpp:rayo:output:1" direction="forward" amount="10000"/>""")

		assertProperties(seek, [
            direction: SeekCommand.Direction.FORWARD,
            amount: 10000
		])	
	}
	
	// Pause
	// ====================================================================================
	@Test
	public void outputPauseFromXml() {
		
		def pause = fromXml("""<pause xmlns="urn:xmpp:rayo:output:1" />""")
		assertNotNull pause
		assertTrue pause instanceof PauseCommand
	}
	
	// Resume
	// ====================================================================================
	@Test
	public void outputResumeFromXml() {
		
		def resume = fromXml("""<resume xmlns="urn:xmpp:rayo:output:1" />""")
		assertNotNull resume
		assertTrue resume instanceof ResumeCommand
	}
	
	// Hold 
	// ====================================================================================

    @Test
    public void holdToXml() {
        HoldCommand hold = new HoldCommand();
        assertEquals("""<hold xmlns="urn:xmpp:rayo:1"/>""", toXml(hold));
    }
    
    @Test
    public void holdFromXml() {
        assertNotNull fromXml("""<hold xmlns="urn:xmpp:rayo:1"></hold>""")
    }

	// Unhold
	// ====================================================================================

	@Test
	public void unholdToXml() {
		UnholdCommand unhold = new UnholdCommand();
		assertEquals("""<unhold xmlns="urn:xmpp:rayo:1"/>""", toXml(unhold));
	}
	
	@Test
	public void unholdFromXml() {
		assertNotNull fromXml("""<unhold xmlns="urn:xmpp:rayo:1"></unhold>""")
	}

	// Mute
	// ====================================================================================

	@Test
	public void muteToXml() {
		MuteCommand mute = new MuteCommand();
		assertEquals("""<mute xmlns="urn:xmpp:rayo:1"/>""", toXml(mute));
	}
	
	@Test
	public void muteFromXml() {
		assertNotNull fromXml("""<mute xmlns="urn:xmpp:rayo:1"></mute>""")
	}

	// Unhold
	// ====================================================================================

	@Test
	public void unmuteToXml() {
		UnmuteCommand unmute = new UnmuteCommand();
		assertEquals("""<unmute xmlns="urn:xmpp:rayo:1"/>""", toXml(unmute));
	}
	
	@Test
	public void unmuteFromXml() {
		assertNotNull fromXml("""<unmute xmlns="urn:xmpp:rayo:1"></unmute>""")
	}

	// Ringing
	// ====================================================================================

	@Test
	public void ringingToXml() {
		
		RingingEvent ringing = new RingingEvent(null);
		assertEquals("""<ringing xmlns="urn:xmpp:rayo:1"/>""", toXml(ringing));
	}

	@Test
	public void ringingToXmlWithHeaders() {
		
		Map<String, String> headers = new HashMap<String, String>();
		headers.put("test", "atest")
		headers.put("test2", "atest2")
		RingingEvent ringing = new RingingEvent(null, headers);
		assertEquals("""<ringing xmlns="urn:xmpp:rayo:1"><header name="test" value="atest"/><header name="test2" value="atest2"/></ringing>""", toXml(ringing));
	}
	
	@Test
	public void ringingFromXml() {
		
		def ringing = fromXml("""<ringing xmlns="urn:xmpp:rayo:1"></ringing>""")
		assertNotNull ringing
		assertTrue ringing instanceof RingingEvent
	}

	@Test
	public void ringingFromXmlWithHeaders() {
		
		def ringing = fromXml("""<ringing xmlns="urn:xmpp:rayo:1"><header name="test" value="atest"/><header name="test2" value="atest2"/></ringing>""")
		assertNotNull ringing
		assertTrue ringing instanceof RingingEvent
		assertNotNull ringing.headers
		assertEquals ringing.headers.size(),2
		assertEquals ringing.headers["test"], "atest"
		assertEquals ringing.headers["test2"], "atest2"
	}

	// Answered
	// ====================================================================================

	@Test
	public void answeredToXml() {
		
		AnsweredEvent ringing = new AnsweredEvent(null);
		assertEquals("""<answered xmlns="urn:xmpp:rayo:1"/>""", toXml(ringing));
	}
	
	@Test
	public void answeredToXmlWithHeaders() {
		
		Map<String, String> headers = new HashMap<String, String>();
		headers.put("test", "atest")
		headers.put("test2", "atest2")

		AnsweredEvent answered = new AnsweredEvent(null, headers);
		assertEquals("""<answered xmlns="urn:xmpp:rayo:1"><header name="test" value="atest"/><header name="test2" value="atest2"/></answered>""", toXml(answered));
	}	
	
	@Test
	public void answeredFromXml() {
		def answered = fromXml("""<answered xmlns="urn:xmpp:rayo:1"></answered>""")
		assertNotNull answered
		assertTrue answered instanceof AnsweredEvent
	}
	
	@Test
	public void answeredFromXmlWithHeaders() {
		def answered = fromXml("""<answered xmlns="urn:xmpp:rayo:1"><header name="test" value="atest"/><header name="test2" value="atest2"/></answered>""")
		assertNotNull answered
		assertTrue answered instanceof AnsweredEvent
		assertNotNull answered.headers
		assertEquals answered.headers.size(),2
		assertEquals answered.headers["test"], "atest"
		assertEquals answered.headers["test2"], "atest2"
	}
	
	// Active Speaker
	// ====================================================================================

	@Test
	public void speakingToXml() {
		
		def mixer = [getName:{ "1234" }, getParticipants:{[] as Participant[]}] as Mixer
		StartedSpeakingEvent speaking = new StartedSpeakingEvent(mixer, "12");

		assertEquals("""<started-speaking xmlns="urn:xmpp:rayo:1" call-id="12"/>""", toXml(speaking));
	}
	
	@Test
	public void speakingFromXml() {

		def speaking = fromXml("""<started-speaking xmlns="urn:xmpp:rayo:1" call-id="1234"></started-speaking>""")
		assertNotNull speaking
		assertTrue speaking instanceof StartedSpeakingEvent
		assertEquals speaking.speakerId,"1234"
	}
	
	@Test
	public void stoppedSpeakingToXml() {
		
		def mixer = [getName:{ "1234" }, getParticipants:{[] as Participant[]}] as Mixer
		StoppedSpeakingEvent speaking = new StoppedSpeakingEvent(mixer, "12");
		assertEquals("""<stopped-speaking xmlns="urn:xmpp:rayo:1" call-id="12"/>""", toXml(speaking));
	}
	
	@Test
	public void stoppedSpeakingFromXml() {
		
		def speaking = fromXml("""<stopped-speaking xmlns="urn:xmpp:rayo:1" call-id="1234"></stopped-speaking>""")
		assertNotNull speaking			
		assertTrue speaking instanceof StoppedSpeakingEvent
		assertEquals speaking.speakerId,"1234"
	}

	// Dtmf Command
	// ====================================================================================
	
	@Test
	public void dtmfFromXml() {
		assertEquals "5", fromXml("""<dtmf xmlns="urn:xmpp:rayo:1" tones="5"/>""").tones
	}

	@Test
	public void dtmfCommandToXml() {
		DtmfCommand dtmf = new DtmfCommand("12")
		assertEquals """<dtmf xmlns="urn:xmpp:rayo:1" tones="12"/>""", toXml(dtmf)
	}
	
	// End Event
	// ====================================================================================
	
	@Test
	public void endEventEmptyToXml() {
		
		EndEvent event = new EndEvent("abcd", null)
		assertEquals """<end xmlns="urn:xmpp:rayo:1"/>""", toXml(event)
	}
	
	@Test
	public void endEventReasonToXml() {
		
		EndEvent event = new EndEvent("abcd", EndEvent.Reason.TIMEOUT, null)
		assertEquals """<end xmlns="urn:xmpp:rayo:1"><timeout/></end>""", toXml(event)
	}
	
	@Test
	public void endEventReasonAndErrorToXml() {
		
		EndEvent event = new EndEvent("abcd", EndEvent.Reason.BUSY, null)
		event.setErrorText("This is an error")
		assertEquals """<end xmlns="urn:xmpp:rayo:1"><busy>This is an error</busy></end>""", toXml(event)
	}
	
	
	@Test
	public void endEventReasonAndErrorWithHeadersToXml() {
		
		Map<String, String> headers = new HashMap<String, String>();
		headers.put("test1", "value1")
		headers.put("test2", "value2")
		EndEvent event = new EndEvent("abcd", EndEvent.Reason.BUSY, headers)
		event.setErrorText("This is an error")
		assertEquals """<end xmlns="urn:xmpp:rayo:1"><busy>This is an error</busy><header name="test1" value="value1"/><header name="test2" value="value2"/></end>""", toXml(event)
	}
		
	@Test
	public void endEventFromXml() {
		
		def event = fromXml("""<end xmlns="urn:xmpp:rayo:1"/>""")
		assertNotNull event
	}
	
	@Test
	public void endEventFromXmlWithReason() {
		
		def event = fromXml("""<end xmlns="urn:xmpp:rayo:1"><timeout/></end>""")
		assertNotNull event
		assertEquals event.reason, EndEvent.Reason.TIMEOUT
	}
	
	@Test
	public void endEventFromXmlWithReasonAndErrorText() {
		
		def event = fromXml("""<end xmlns="urn:xmpp:rayo:1"><busy>This is an error</busy></end>""")
		assertNotNull event
		assertEquals event.reason, EndEvent.Reason.BUSY
		assertEquals event.errorText, "This is an error"
	}
	
	@Test
	public void endEventFromXmlWithReasonAndErrorTextAndHeaders() {
		
		def event = fromXml("""<end xmlns="urn:xmpp:rayo:1"><busy>This is an error</busy><header name="test1" value="value1"/><header name="test2" value="value2"/></end>""")
		assertNotNull event
		assertEquals event.reason, EndEvent.Reason.BUSY
		assertEquals event.errorText, "This is an error"
		assertNotNull event.headers
		assertEquals event.headers["test1"], "value1"
		assertEquals event.headers["test2"], "value2"
	}
	
	// Destroy Mixer If Empty command
	// ====================================================================================
	
	@Test
	public void destroyMixerToXml() {
		
		DestroyMixerCommand dmc = new DestroyMixerCommand();
		assertEquals("""<destroy-if-empty xmlns="urn:xmpp:rayo:1"/>""", toXml(dmc));
	}
	
	@Test
	public void destroyMixerFromXml() {
		
		assertNotNull fromXml("""<destroy-if-empty xmlns="urn:xmpp:rayo:1"/>""")
	}
	
    // Utility
    // ====================================================================================
    
    def fromXml = {xml->
        Document document = reader.read(new StringReader(xml))
        provider.fromXML(document.rootElement)
    }
    
    def toXml = {
        provider.toXML(it).asXML()
    }
    
    def assertProperties = {obj, map->
        map.each {k, v->
            if(v instanceof Map) {
                compareMap(v, obj[k])
            }
            assertEquals v, obj[k]
        }
    }
    
    def compareMap = {m1, m2->
        assertEquals m1.size(), m2.size()
        m1.each {k, v->
            assertEquals v, m2[k]
        }
    }
}