package com.tropo.core.xml

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

import com.tropo.core.AcceptCommand
import com.tropo.core.AnswerCommand
import com.tropo.core.AnsweredEvent;
import com.tropo.core.CallRejectReason
import com.tropo.core.DialCommand
import com.tropo.core.DtmfEvent
import com.tropo.core.FinishedSpeakingEvent;
import com.tropo.core.HangupCommand
import com.tropo.core.JoinCommand
import com.tropo.core.JoinDestinationType
import com.tropo.core.JoinedEvent
import com.tropo.core.OfferEvent
import com.tropo.core.RedirectCommand
import com.tropo.core.RejectCommand
import com.tropo.core.RingingEvent;
import com.tropo.core.SpeakingEvent;
import com.tropo.core.UnjoinCommand
import com.tropo.core.UnjoinedEvent
import com.tropo.core.validation.Validator
import com.tropo.core.verb.Ask
import com.tropo.core.verb.AskCompleteEvent
import com.tropo.core.verb.Choices
import com.tropo.core.verb.Conference
import com.tropo.core.verb.ConferenceCompleteEvent
import com.tropo.core.verb.HoldCommand
import com.tropo.core.verb.InputMode
import com.tropo.core.verb.KickCommand
import com.tropo.core.verb.MediaType
import com.tropo.core.verb.MuteCommand
import com.tropo.core.verb.Output
import com.tropo.core.verb.PauseCommand
import com.tropo.core.verb.Record
import com.tropo.core.verb.RecordCompleteEvent
import com.tropo.core.verb.RecordPauseCommand
import com.tropo.core.verb.RecordResumeCommand
import com.tropo.core.verb.ResumeCommand
import com.tropo.core.verb.Say
import com.tropo.core.verb.SayCompleteEvent
import com.tropo.core.verb.SeekCommand;
import com.tropo.core.verb.SpeedDownCommand;
import com.tropo.core.verb.SpeedUpCommand;
import com.tropo.core.verb.Ssml
import com.tropo.core.verb.StopCommand
import com.tropo.core.verb.Transfer
import com.tropo.core.verb.TransferCompleteEvent
import com.tropo.core.verb.UnholdCommand
import com.tropo.core.verb.UnmuteCommand
import com.tropo.core.verb.VerbCompleteEvent
import com.tropo.core.verb.AskCompleteEvent.Reason
import com.tropo.core.verb.VolumeDownCommand;
import com.tropo.core.verb.VolumeUpCommand;
import com.tropo.core.xml.providers.AskProvider
import com.tropo.core.xml.providers.ConferenceProvider
import com.tropo.core.xml.providers.OutputProvider
import com.tropo.core.xml.providers.RayoProvider
import com.tropo.core.xml.providers.RecordProvider
import com.tropo.core.xml.providers.SayProvider
import com.tropo.core.xml.providers.TransferProvider
import com.voxeo.moho.Participant.JoinType
import com.voxeo.moho.media.output.OutputCommand.BargeinType

public class RayoProviderTest {

    XmlProviderManager provider

    SAXReader reader = new SAXReader()
    
    @Before
    public void setup() {
        
        def validator = new Validator()
        
        provider = new DefaultXmlProviderManager()
        
        [new RayoProvider(validator:validator,namespaces:['urn:xmpp:rayo:1', 'urn:xmpp:rayo:ext:1', 'urn:xmpp:rayo:ext:complete:1']),
         new SayProvider(validator:validator,namespaces:['urn:xmpp:tropo:say:1', 'urn:xmpp:tropo:say:complete:1']),
         new AskProvider(validator:validator,namespaces:['urn:xmpp:tropo:ask:1', 'urn:xmpp:tropo:ask:complete:1']),
         new TransferProvider(validator:validator,namespaces:['urn:xmpp:tropo:transfer:1', 'urn:xmpp:tropo:transfer:complete:1']),
         new ConferenceProvider(validator:validator,namespaces:['urn:xmpp:tropo:conference:1', 'urn:xmpp:tropo:conference:complete:1']),
         new RecordProvider(validator:validator,namespaces:['urn:xmpp:rayo:record:1', 'urn:xmpp:rayo:record:complete:1']),
         new OutputProvider(validator:validator,namespaces:['urn:xmpp:rayo:output:1', 'urn:xmpp:rayo:output:complete:1'])
		].each {
             provider.register it
         }
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
		
		def join = new JoinCommand(direction:Joinable.Direction.DUPLEX, media:JoinType.BRIDGE, to:"1234", type: JoinDestinationType.CALL);
		
		assertEquals("""<join xmlns="urn:xmpp:rayo:1" direction="duplex" media="bridge" call-id="1234"/>""", toXml(join));
	}
	
	@Test
	public void joinToXmlWithMixerId() {
		
		def join = new JoinCommand(direction:Joinable.Direction.DUPLEX, media:JoinType.BRIDGE, to:"1234", type: JoinDestinationType.MIXER);
		
		assertEquals("""<join xmlns="urn:xmpp:rayo:1" direction="duplex" media="bridge" mixer-id="1234"/>""", toXml(join));
	}
		
	@Test
	public void joinCallIdFromXml() {

		def join = fromXml("""<join xmlns="urn:xmpp:rayo:1" direction="duplex" media="bridge" call-id="1234"/>""")
		assertProperties(join, [
			direction: Joinable.Direction.DUPLEX,
			media: JoinType.BRIDGE,
			to:"1234",
			type: JoinDestinationType.CALL
		])
	}
	
	@Test
	public void joinMixerIdFromXml() {

		def join = fromXml("""<join xmlns="urn:xmpp:rayo:1" direction="duplex" media="bridge" mixer-id="1234"/>""")
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
	public void unjoinMixerIdToXml() {
		
		def unjoin = new UnjoinCommand(from:"1234", type:JoinDestinationType.MIXER);
		
		assertEquals("""<unjoin xmlns="urn:xmpp:rayo:1" mixer-id="1234"/>""", toXml(unjoin));
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
		assertEquals("""<unjoined xmlns="urn:xmpp:rayo:1" mixer-id="abcd"/>""", toXml(unjoined));
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

		def unjoined = fromXml("""<unjoined xmlns="urn:xmpp:rayo:1" mixer-id="abcd"/>""")
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
		assertEquals("""<joined xmlns="urn:xmpp:rayo:1" mixer-id="abcd"/>""", toXml(joined));
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

		def joined = fromXml("""<joined xmlns="urn:xmpp:rayo:1" mixer-id="abcd"/>""")
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
	//TODO Is this valid?
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
		
		assertEquals("""<dial xmlns="urn:xmpp:rayo:1" to="tel:44477773333333" from="tel:34637710708"><join direction="duplex" media="bridge" mixer-id="1234"/></dial>""", toXml(command));
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

		def dial = fromXml("""<dial xmlns="urn:xmpp:rayo:1" to="tel:44477773333333" from="tel:34637710708"><join xmlns="urn:xmpp:rayo:join:1" mixer-id="abcd"/></dial>""")
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

		def dial = fromXml("""<dial xmlns="urn:xmpp:rayo:1" to="tel:44477773333333" from="tel:34637710708"><join direction="duplex" media="bridge" mixer-id="1234"/></dial>""")
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
    public void rejectFromXmlDefaultReason() {
        RejectCommand command = fromXml("""<reject xmlns="urn:xmpp:rayo:1"></reject>""")
        assertEquals CallRejectReason.DECLINE, command.reason
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

    // Kick
    // ====================================================================================
    @Test
    public void kickFromXml() {
        assertNotNull fromXml("""<kick xmlns="urn:xmpp:tropo:conference:1" />""")
    }
    
    @Test
    public void kickToXml() {
        KickCommand kick = new KickCommand();
        assertEquals("""<kick xmlns="urn:xmpp:tropo:conference:1"/>""", toXml(kick));
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

        assertEquals("""<ask xmlns="urn:xmpp:tropo:ask:1" voice="allison" min-confidence="0.8" mode="dtmf" recognizer="en-us" terminator="#" timeout="3000" bargein="true"><prompt>Hello World.</prompt><choices content-type="vxml" url="http://test">sales,support</choices></ask>""", toXml(ask));
    }
    
    // Conference
    // ====================================================================================

    @Test
    public void conferenceFromXml() {
        
        def conference = fromXml("""<conference xmlns="urn:xmpp:tropo:conference:1" terminator="#" name="123456" beep="true" mute="true" tone-passthrough="true" moderator="false"><announcement>hello</announcement><music>music</music></conference>""")
        assertNotNull conference
        assertEquals conference.terminator, '#' as char
        assertTrue conference.beep
        assertTrue conference.tonePassthrough
        assertTrue conference.mute
        assertEquals conference.roomName,"123456"
        assertFalse conference.moderator
        assertEquals conference.holdMusic.text , "music"
        assertEquals conference.announcement.text , "hello"
    }
    
    @Test
    public void emptyConferenceToXml() {
        
        def conference = new Conference([
            roomName: "1234"
        ])
        assertEquals("""<conference xmlns="urn:xmpp:tropo:conference:1" name="1234" mute="false" terminator="#" tone-passthrough="true" beep="true" moderator="true"/>""", toXml(conference));
    }
    
    @Test
    public void conferenceToXml() {
        
        def conference = new Conference()
        conference.roomName = "1234"
        conference.terminator = '#' as char
        conference.beep = true
        conference.mute = true
        conference.tonePassthrough = true
        conference.moderator = false
        conference.announcement = new Ssml("hello")
        conference.holdMusic = new Ssml("music")
        
        assertEquals("""<conference xmlns="urn:xmpp:tropo:conference:1" name="1234" mute="true" terminator="#" tone-passthrough="true" beep="true" moderator="false"><announcement>hello</announcement><music>music</music></conference>""", toXml(conference));
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
        
        Transfer transfer = fromXml("""<transfer xmlns="urn:xmpp:tropo:transfer:1" terminator="#" timeout="20000"><ring><audio url="http://ccmixter.org/content/DoKashiteru/DoKashiteru_-_you_(na-na-na-na).mp3"/></ring><to>sip:martin@127.0.0.1:6089</to></transfer>""")
        assertNotNull transfer
        assertNotNull transfer.ringbackTone
        assertEquals transfer.ringbackTone.text,"""<audio url="http://ccmixter.org/content/DoKashiteru/DoKashiteru_-_you_(na-na-na-na).mp3"/>"""
    }
    
    @Test
    public void ssmlTransferFromXml() {
        
        Transfer transfer = fromXml("""<transfer xmlns="urn:xmpp:tropo:transfer:1" voice="allison" terminator="#" timeout="20000"><ring>We are going to transfer your call. Wait a couple of seconds.</ring><to>sip:martin@127.0.0.1:6089</to></transfer>""")
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
        transfer.ringbackTone = new Ssml("""<audio src="http://ccmixter.org/content/DoKashiteru/DoKashiteru_-_you_(na-na-na-na).mp3" />""")

        assertEquals("""<transfer xmlns="urn:xmpp:tropo:transfer:1" terminator="#" timeout="20000" media="bridge" to="sip:martin@127.0.0.1:6089" answer-on-media="false"><audio xmlns="" src="http://ccmixter.org/content/DoKashiteru/DoKashiteru_-_you_(na-na-na-na).mp3"/></transfer>""", toXml(transfer));
    }
    
    @Test
    public void ssmlTransferToXml() {
        
        def transfer = new Transfer()
        transfer.timeout = new Duration(20000)
        transfer.terminator = '#' as char
        transfer.to = [new URI("sip:martin@127.0.0.1:6089")]
        transfer.ringbackTone = new Ssml("We are going to transfer your call. Wait a couple of seconds.")

        assertEquals("""<transfer xmlns="urn:xmpp:tropo:transfer:1" terminator="#" timeout="20000" media="bridge" to="sip:martin@127.0.0.1:6089" answer-on-media="false">We are going to transfer your call. Wait a couple of seconds.</transfer>""", toXml(transfer));
    }
    
    @Test
    public void transferWithMultipleUrisToXml() {
        
        def transfer = new Transfer()
        transfer.timeout = new Duration(20000)
        transfer.terminator = '#' as char
        transfer.to = [new URI("sip:martin@127.0.0.1:6089"),new URI("sip:jose@127.0.0.1:6088")]
        transfer.ringbackTone = new Ssml("We are going to transfer your call. Wait a couple of seconds.")

        assertEquals("""<transfer xmlns="urn:xmpp:tropo:transfer:1" terminator="#" timeout="20000" media="bridge" answer-on-media="false">We are going to transfer your call. Wait a couple of seconds.<to>sip:martin@127.0.0.1:6089</to><to>sip:jose@127.0.0.1:6088</to></transfer>""", toXml(transfer));
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
    
    // Conference Complete
    // ====================================================================================
    @Test
    public void conferenceCompleteFromXml() {
        
        def complete = fromXml("""<complete xmlns="urn:xmpp:rayo:ext:1"><hangup xmlns="urn:xmpp:rayo:ext:complete:1"/></complete>""")
        assertNotNull complete
        assertEquals complete.reason, VerbCompleteEvent.Reason.HANGUP
    }
    
    @Test
    public void conferenceCompleteWithErrorsFromXml() {
        
        def complete = fromXml("""<complete xmlns="urn:xmpp:rayo:ext:1"><error xmlns="urn:xmpp:rayo:ext:complete:1">this is an error</error></complete>""")
        assertNotNull complete
        assertEquals complete.reason, VerbCompleteEvent.Reason.ERROR
        assertEquals complete.errorText, "this is an error"
    }
    
    @Test
    public void conferenceCompleteToXml() {
        
        def complete = new ConferenceCompleteEvent(new Conference(), VerbCompleteEvent.Reason.HANGUP)
        
        assertEquals("""<complete xmlns="urn:xmpp:rayo:ext:1"><hangup xmlns="urn:xmpp:rayo:ext:complete:1"/></complete>""", toXml(complete));
    }
    
    @Test
    public void conferenceCompleteWithErrorsToXml() {
        
        def complete = new ConferenceCompleteEvent(new Conference(), VerbCompleteEvent.Reason.ERROR)
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
				<recording xmlns="urn:xmpp:rayo:record:complete:1" uri="file:///tmp/abc.mp3"/>
			</complete>""")
		assertNotNull complete
		assertEquals complete.reason, VerbCompleteEvent.Reason.STOP
		assertEquals complete.uri, new URI("file:///tmp/abc.mp3")
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
		
		assertEquals("""<complete xmlns="urn:xmpp:rayo:ext:1"><success xmlns="urn:xmpp:rayo:record:complete:1"/><recording xmlns="urn:xmpp:rayo:record:complete:1" uri="file:///tmp/abc.mp3"/></complete>""", toXml(complete));
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
		RingingEvent ringing = new RingingEvent();
		assertEquals("""<ringing xmlns="urn:xmpp:rayo:1"/>""", toXml(ringing));
	}
	
	@Test
	public void ringingFromXml() {
		def ringing = fromXml("""<ringing xmlns="urn:xmpp:rayo:1"></ringing>""")
		assertNotNull ringing
		assertTrue ringing instanceof RingingEvent
	}
	
	// Answered
	// ====================================================================================

	@Test
	public void answeredToXml() {
		AnsweredEvent ringing = new AnsweredEvent();
		assertEquals("""<answered xmlns="urn:xmpp:rayo:1"/>""", toXml(ringing));
	}
	
	@Test
	public void answeredFromXml() {
		def answered = fromXml("""<answered xmlns="urn:xmpp:rayo:1"></answered>""")
		assertNotNull answered
		assertTrue answered instanceof AnsweredEvent
	}
	
	// Active Speaker
	// ====================================================================================

	@Test
	public void speakingToXml() {
		SpeakingEvent speaking = new SpeakingEvent();
		assertEquals("""<speaking xmlns="urn:xmpp:rayo:1"/>""", toXml(speaking));
	}
	
	@Test
	public void speakingFromXml() {
		def speaking = fromXml("""<speaking xmlns="urn:xmpp:rayo:1"></speaking>""")
		assertNotNull speaking
		assertTrue speaking instanceof SpeakingEvent
	}
	
	@Test
	public void finishedSpeakingToXml() {
		FinishedSpeakingEvent speaking = new FinishedSpeakingEvent();
		assertEquals("""<finished-speaking xmlns="urn:xmpp:rayo:1"/>""", toXml(speaking));
	}
	
	@Test
	public void finishedSpeakingFromXml() {
		def speaking = fromXml("""<finished-speaking xmlns="urn:xmpp:rayo:1"></finished-speaking>""")
		assertNotNull speaking
		assertTrue speaking instanceof FinishedSpeakingEvent
	}

	// Dtmf Command
	// ====================================================================================
	
	@Test
	public void dtmfFromXml() {
		assertNotNull "5", fromXml("""<dtmf xmlns="urn:xmpp:rayo:1" key="5"></dtmf>""").key
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