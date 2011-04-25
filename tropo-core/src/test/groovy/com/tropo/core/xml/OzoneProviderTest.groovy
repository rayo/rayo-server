package com.tropo.core.xml

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertNotNull

import java.io.StringReader
import java.net.URI
import java.util.HashMap
import java.util.Map

import org.dom4j.Document
import org.dom4j.io.SAXReader
import org.junit.Before
import org.junit.Test

import com.tropo.core.AcceptCommand
import com.tropo.core.AnswerCommand
import com.tropo.core.CallRejectReason
import com.tropo.core.HangupCommand
import com.tropo.core.Offer
import com.tropo.core.RedirectCommand
import com.tropo.core.RejectCommand

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