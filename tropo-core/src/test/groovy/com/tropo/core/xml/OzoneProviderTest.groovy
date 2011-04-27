package com.tropo.core.xml

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertTrue;

import java.io.StringReader
import java.net.URI
import java.util.HashMap
import java.util.Map

import org.dom4j.Document
import org.dom4j.io.SAXReader
import org.joda.time.Duration;
import org.junit.Before
import org.junit.Test

import com.tropo.core.AcceptCommand
import com.tropo.core.AnswerCommand
import com.tropo.core.CallRejectReason
import com.tropo.core.HangupCommand
import com.tropo.core.Offer
import com.tropo.core.RedirectCommand
import com.tropo.core.RejectCommand
import com.tropo.core.verb.Ask;
import com.tropo.core.verb.AudioItem;
import com.tropo.core.verb.InputMode;
import com.tropo.core.verb.KickCommand;
import com.tropo.core.verb.PauseCommand;
import com.tropo.core.verb.PromptItems;
import com.tropo.core.verb.ResumeCommand;
import com.tropo.core.verb.Say;
import com.tropo.core.verb.SsmlItem;
import com.tropo.core.verb.StopCommand;

public class OzoneProviderTest {

    OzoneProvider provider

    SAXReader reader = new SAXReader()
    
    @Before
    public void setup() {
        provider = new OzoneProvider()
    }

    // Offer
    // ====================================================================================

    @Test
    public void offerToXml() {
        Map<String, String> headers = new HashMap<String, String>();
        Offer offer = new Offer();
        offer.setTo(new URI("tel:44477773333333"));
        offer.setFrom(new URI("tel:34637710708"));
        headers.put("test","atest");
        offer.setHeaders(headers);

        assertEquals("<offer xmlns=\"urn:xmpp:ozone:1\" to=\"tel:44477773333333\" from=\"tel:34637710708\"><header name=\"test\" value=\"atest\"/></offer>", provider.toXML(offer).asXML());
    }
    
    @Test
    public void offerFromXml() {

        assertProperties(fromXml("<offer xmlns=\"urn:xmpp:ozone:1\" to=\"tel:44477773333333\" from=\"tel:34637710708\"><header name=\"test\" value=\"atest\"/></offer>"), [
            to: new URI("tel:44477773333333"),
            from: new URI("tel:34637710708"),
            headers: [
                test: "atest"
            ]
        ])

    }
    
    // Accept
    // ====================================================================================

    @Test
    public void acceptToXml() {
        AcceptCommand accept = new AcceptCommand();
        assertEquals("<accept xmlns=\"urn:xmpp:ozone:1\"/>", provider.toXML(accept).asXML());
    }

    @Test
    public void acceptWithHeadersToXml() {
        AcceptCommand accept = new AcceptCommand([
            headers: ["test":"atest"]
        ]);
        assertEquals("<accept xmlns=\"urn:xmpp:ozone:1\"><header name=\"test\" value=\"atest\"/></accept>", provider.toXML(accept).asXML());
    }
    
    @Test
    public void acceptFromXml() {
        assertNotNull fromXml("<accept xmlns=\"urn:xmpp:ozone:1\"></accept>")
    }

    @Test
    public void acceptWithHeadersFromXml() {
        assertProperties(fromXml("<accept xmlns=\"urn:xmpp:ozone:1\"><header name=\"test\" value=\"atest\"/></accept>"), [
            headers: [test:"atest"]
        ])
    }
    
    // Answer
    // ====================================================================================
    
    @Test
    public void answerToXml() {
        AnswerCommand answer = new AnswerCommand();
        assertEquals("<answer xmlns=\"urn:xmpp:ozone:1\"/>", provider.toXML(answer).asXML());
    }
    
    @Test
    public void answerWithHeadersToXml() {
        AnswerCommand answer = new AnswerCommand([
            headers: ["test":"atest"]
        ]);
        assertEquals("<answer xmlns=\"urn:xmpp:ozone:1\"><header name=\"test\" value=\"atest\"/></answer>", provider.toXML(answer).asXML());
    }
    
    @Test
    public void answerFromXml() {
        assertNotNull fromXml("<answer xmlns=\"urn:xmpp:ozone:1\"></answer>")
    }
    
    @Test
    public void answerWithHeadersFromXml() {
        assertProperties(fromXml("<answer xmlns=\"urn:xmpp:ozone:1\"><header name=\"test\" value=\"atest\"/></answer>"), [
            headers: [test:"atest"]
        ])
    }

    // Hangup
    // ====================================================================================
    
    @Test
    public void hangupToXml() {
        HangupCommand hangup = new HangupCommand();
        assertEquals("<hangup xmlns=\"urn:xmpp:ozone:1\"/>", provider.toXML(hangup).asXML());
    }
    
    @Test
    public void hangupWithHeadersToXml() {
        HangupCommand hangup = new HangupCommand([
            headers: ["test":"atest"]
        ]);
        assertEquals("<hangup xmlns=\"urn:xmpp:ozone:1\"><header name=\"test\" value=\"atest\"/></hangup>", provider.toXML(hangup).asXML());
    }
    
    @Test
    public void hangupFromXml() {
        assertNotNull fromXml("<hangup xmlns=\"urn:xmpp:ozone:1\"></hangup>")
    }
    
    @Test
    public void hangupWithHeadersFromXml() {
        assertProperties(fromXml("<hangup xmlns=\"urn:xmpp:ozone:1\"><header name=\"test\" value=\"atest\"/></hangup>"), [
            headers: [test:"atest"]
        ])
    }
    
    // Reject
    // ====================================================================================
    
    @Test
    public void rejectToXml() {
        RejectCommand reject = new RejectCommand([reason: CallRejectReason.BUSY]);
        assertEquals("<reject xmlns=\"urn:xmpp:ozone:1\"><busy/></reject>", provider.toXML(reject).asXML());
    }
    
    @Test
    public void rejectWithHeadersToXml() {
        RejectCommand reject = new RejectCommand([
            reason: CallRejectReason.BUSY,
            headers: ["test":"atest"]
        ]);
        assertEquals("<reject xmlns=\"urn:xmpp:ozone:1\"><busy/><header name=\"test\" value=\"atest\"/></reject>", provider.toXML(reject).asXML());
    }
    
    @Test
    public void rejectFromXml() {
        assertNotNull fromXml("<reject xmlns=\"urn:xmpp:ozone:1\"><busy/></reject>")
    }
    
    @Test
    public void rejectWithHeadersFromXml() {
        assertProperties(fromXml("<reject xmlns=\"urn:xmpp:ozone:1\"><busy/><header name=\"test\" value=\"atest\"/></reject>"), [
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
        assertEquals("""<redirect xmlns="urn:xmpp:ozone:1" to="tel:+14075551212"/>""", provider.toXML(redirect).asXML());
    }
    
    @Test
    public void redirectWithHeadersToXml() {
        RedirectCommand redirect = new RedirectCommand([
            to: new URI("tel:+14075551212"),
            headers: ["test":"atest"]
        ]);
        assertEquals("""<redirect xmlns="urn:xmpp:ozone:1" to="tel:+14075551212"><header name="test" value="atest"/></redirect>""", provider.toXML(redirect).asXML());
    }
    
    @Test
    public void redirectFromXml() {
        assertNotNull fromXml("""<redirect xmlns="urn:xmpp:ozone:1" to="tel:+14075551212" />""")
    }
    
    @Test
    public void redirectWithHeadersFromXml() {
        assertProperties(fromXml("""<redirect xmlns="urn:xmpp:ozone:1" to="tel:+14075551212"><header name="test" value="atest"/></redirect>"""), [
            to: new URI("tel:+14075551212"),
            headers: [test:"atest"]
        ])
    }

	// Kick
	// ====================================================================================
    @Test
    public void kickFromXml() {
        assertNotNull fromXml("""<kick xmlns=\"urn:xmpp:ozone:conference:1\" />""")
    }
	
	@Test
	public void kickToXml() {
		KickCommand kick = new KickCommand();
		assertEquals("""<kick xmlns=\"urn:xmpp:ozone:conference:1\"/>""", provider.toXML(kick).asXML());
	}
	
	// Pause
	// ====================================================================================
	@Test
	public void pauseFromXml() {
		assertNotNull fromXml("""<pause xmlns=\"urn:xmpp:ozone:say:1\" />""")
	}
	
	@Test
	public void pauseToXml() {
		
		PauseCommand pause = new PauseCommand();
		assertEquals("""<pause xmlns=\"urn:xmpp:ozone:say:1\"/>""", provider.toXML(pause).asXML());
	}
	
	// Resume
	// ====================================================================================
	@Test
	public void resumeFromXml() {
		assertNotNull fromXml("""<resume xmlns=\"urn:xmpp:ozone:say:1\" />""")
	}
	
	@Test
	public void resumeToXml() {
		
		ResumeCommand resume = new ResumeCommand();
		assertEquals("""<resume xmlns=\"urn:xmpp:ozone:say:1\"/>""", provider.toXML(resume).asXML());
	}
	
	// Stop
	// ====================================================================================
	@Test
	public void stopFromXml() {
		assertNotNull fromXml("""<stop xmlns=\"urn:xmpp:ozone:1\" />""")
	}
	
	@Test
	public void stopToXml() {
		
		StopCommand stop = new StopCommand();
		assertEquals("""<stop xmlns=\"urn:xmpp:ozone:1\"/>""", provider.toXML(stop).asXML());
	}

	// Say
	// ====================================================================================
	@Test
	public void emptySayFromXml() {
		
		def say = fromXml("""<say xmlns=\"urn:xmpp:ozone:say:1\"/>""")
		assertNotNull say
	}

	@Test
	public void sayFromXml() {
		
		def say = fromXml("""<say xmlns=\"urn:xmpp:ozone:say:1\" voice=\"allison\"/>""")
		assertNotNull say
		assertEquals say.voice, "allison"
	}

	@Test
	public void audioSayFromXml() {
		
		def say = fromXml("""<say xmlns=\"urn:xmpp:ozone:say:1\" voice=\"allison\"><audio url=\"http://ccmixter.org/content/DoKashiteru/DoKashiteru_-_you_(na-na-na-na).mp3\"/></say>""")
		assertNotNull say
		assertNotNull say.promptItems
		assertEquals say.promptItems.size(),1
		assertEquals say.promptItems[0].toUri().toString(),"http://ccmixter.org/content/DoKashiteru/DoKashiteru_-_you_(na-na-na-na).mp3"
	}

	@Test
	public void ssmlSayFromXml() {
		
		def say = fromXml("""<say xmlns=\"urn:xmpp:ozone:say:1\" voice=\"allison\"><speak>Hello World</speak></say>""")
		assertNotNull say
		assertNotNull say.promptItems
		assertEquals say.promptItems.size(),1
		assertEquals say.promptItems[0].text,"<speak>Hello World</speak>"

	}
	
	@Test
	public void emptySayToXml() {
		
		Say say = new Say();
		assertEquals("""<say xmlns=\"urn:xmpp:ozone:say:1\"/>""", provider.toXML(say).asXML());
	}
	
	@Test
	public void sayToXml() {
		
		Say say = new Say();
		say.voice = "allison"
		assertEquals("""<say xmlns=\"urn:xmpp:ozone:say:1\" voice=\"allison\"/>""", provider.toXML(say).asXML());
	}
	
	@Test
	public void audioSayToXml() {
		
		Say say = new Say();
		say.voice = "allison"
		say.promptItems = []
		say.promptItems.add new AudioItem(new URI("http://ccmixter.org/content/DoKashiteru/DoKashiteru_-_you_(na-na-na-na).mp3"))

		assertEquals("""<say xmlns=\"urn:xmpp:ozone:say:1\" voice=\"allison\"><audio url=\"http://ccmixter.org/content/DoKashiteru/DoKashiteru_-_you_(na-na-na-na).mp3\"/></say>""", provider.toXML(say).asXML());
	}

	@Test
	public void ssmlSayToXml() {
		
		Say say = new Say();
		say.voice = "allison"
		say.promptItems = []
		say.promptItems.add new SsmlItem("<speak>Hello World.</speak>")

		assertEquals("""<say xmlns=\"urn:xmpp:ozone:say:1\" voice=\"allison\"><speak xmlns=\"\">Hello World.</speak></say>""", provider.toXML(say).asXML());
	}

	// Ask
	// ====================================================================================
	@Test
	public void emptyAskFromXml() {
		
		def ask = fromXml("""<ask xmlns=\"urn:xmpp:ozone:ask:1\"/>""")
		assertNotNull ask
	}

	@Test
	public void askFromXml() {
		
		def ask = fromXml("""<ask xmlns=\"urn:xmpp:ozone:ask:1\" voice=\"allison\" min-confidence=\"0.8\" mode=\"dtmf\" recognizer=\"test\" terminator=\"#\" timeout=\"PT3S\" bargein=\"true\"></ask>""")
		assertNotNull ask
		assertEquals ask.voice, "allison"
		assertTrue ask.minConfidence == 0.8f
		assertEquals ask.mode, InputMode.dtmf
		assertEquals ask.recognizer,"test"
		assertEquals ask.terminator,'#' as char
		assertEquals ask.timeout, new Duration(3000)		
	}
	
	@Test
	public void audioAskFromXml() {
		
		def ask = fromXml("""<ask xmlns=\"urn:xmpp:ozone:ask:1\" voice=\"allison\" min-confidence=\"0.8\" mode=\"dtmf\" recognizer=\"test\" terminator=\"#\" timeout=\"PT3S\" bargein=\"true\"><prompt><audio url=\"http://ccmixter.org/content/DoKashiteru/DoKashiteru_-_you_(na-na-na-na).mp3\"/></prompt></ask>""")
		assertNotNull ask
		assertNotNull ask.promptItems
		assertEquals ask.promptItems.size(),1
		assertEquals ask.promptItems[0].toUri().toString(),"http://ccmixter.org/content/DoKashiteru/DoKashiteru_-_you_(na-na-na-na).mp3"
	}
	
	@Test
	public void ssmlAskFromXml() {
		
		def ask = fromXml("""<ask xmlns=\"urn:xmpp:ozone:ask:1\" voice=\"allison\" min-confidence=\"0.8\" mode=\"dtmf\" recognizer=\"test\" terminator=\"#\" timeout=\"PT3S\" bargein=\"true\"><prompt><speak xmlns=\"\">Hello World.</speak></prompt></ask>""")
		assertNotNull ask
		assertNotNull ask.promptItems
		assertEquals ask.promptItems.size(),1
		assertEquals ask.promptItems[0].text,"<speak>Hello World.</speak>"
	}
	
	@Test
	public void emptyAskToXml() {
		
		def ask = new Ask()
		assertEquals("""<ask xmlns=\"urn:xmpp:ozone:ask:1\" min-confidence=\"0.3\" mode=\"both\" timeout=\"PT30S\" bargein=\"true\"/>""", provider.toXML(ask).asXML());
	}
	
	@Test
	public void askToXml() {
		
		def ask = new Ask()
		ask.voice = "allison"
		ask.minConfidence = 0.8f
		ask.mode = InputMode.dtmf
		ask.recognizer = 'test'
		ask.terminator = '#' as char
		ask.timeout = new Duration(3000)

		assertEquals("""<ask xmlns=\"urn:xmpp:ozone:ask:1\" voice=\"allison\" min-confidence=\"0.8\" mode=\"dtmf\" recognizer=\"test\" terminator=\"#\" timeout=\"PT3S\" bargein=\"true\"/>""", provider.toXML(ask).asXML());
	}
	
	@Test
	public void audioAskToXml() {
		
		def ask = new Ask()
		ask.voice = "allison"
		ask.minConfidence = 0.8f
		ask.mode = InputMode.dtmf
		ask.recognizer = 'test'
		ask.terminator = '#' as char
		ask.timeout = new Duration(3000)
		ask.promptItems = []
		ask.promptItems.add new AudioItem(new URI("http://ccmixter.org/content/DoKashiteru/DoKashiteru_-_you_(na-na-na-na).mp3"))

		assertEquals("""<ask xmlns=\"urn:xmpp:ozone:ask:1\" voice=\"allison\" min-confidence=\"0.8\" mode=\"dtmf\" recognizer=\"test\" terminator=\"#\" timeout=\"PT3S\" bargein=\"true\"><prompt><audio url=\"http://ccmixter.org/content/DoKashiteru/DoKashiteru_-_you_(na-na-na-na).mp3\"/></prompt></ask>""", provider.toXML(ask).asXML());
	}
	
	@Test
	public void ssmlAskToXml() {
		
		def ask = new Ask()
		ask.voice = "allison"
		ask.minConfidence = 0.8f
		ask.mode = InputMode.dtmf
		ask.recognizer = 'test'
		ask.terminator = '#' as char
		ask.timeout = new Duration(3000)
		ask.promptItems = []
		ask.promptItems.add new SsmlItem("<speak>Hello World.</speak>")

		assertEquals("""<ask xmlns=\"urn:xmpp:ozone:ask:1\" voice=\"allison\" min-confidence=\"0.8\" mode=\"dtmf\" recognizer=\"test\" terminator=\"#\" timeout=\"PT3S\" bargein=\"true\"><prompt><speak xmlns=\"\">Hello World.</speak></prompt></ask>""", provider.toXML(ask).asXML());
	}
	
    // Utility
    // ====================================================================================
    
    def fromXml = {xml->
        Document document = reader.read(new StringReader(xml))
        return provider.fromXML(document.rootElement)
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