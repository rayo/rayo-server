package com.rayo.server.ameche

import static org.junit.Assert.*

import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

import org.apache.http.impl.client.DefaultHttpClient
import org.dom4j.DocumentHelper
import org.dom4j.Element
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

import com.rayo.core.DialCommand;
import com.rayo.core.HangupCommand;
import com.rayo.server.CommandHandler


class AmecheServletTest {

    private static Element OFFER_EVENT = toXML('<offer to="tel:+13055195825" from="tel:+15613504458"/>')
    private static Element RINGING_EVENT = toXML('<ringing/>')
    private static Element END_EVENT = toXML('<end/>')
    private static MockHttpServletRequest CONTINUE_REQUEST = buildRequest("<continue/>");

    private static BlockingQueue<Object> commandQueue = new LinkedBlockingQueue<Object>()
    private static LinkedList<Object> commandResponseQueue = new LinkedList<Object>()
    
    private static AmecheServlet servlet
    
    private static apps = []

    @BeforeClass
    public static void beforeClass() {
        
        apps = [
            new MockAppInstanceServer(8881),
            new MockAppInstanceServer(8882),
            new MockAppInstanceServer(8883)
        ]

        servlet = new AmecheServlet()
        
        servlet.commandHandler = { callId, componentId, command, callback ->
            commandQueue.add([
                callId: callId,
                componentId: componentId,
                command: command
            ])
            if(callback != null) {
                def response = commandResponseQueue.poll()
                if(response instanceof Exception) {
                    callback.handle(null, response)
                }
                else {
                    callback.handle(response, null)
                }
            }
        } as CommandHandler
    
        servlet.endpointResolver = {
            apps.collect {
                URI.create("http://127.0.0.1:" + it.port + "/")
            }
        } as EndpointResolver

        servlet.http = new DefaultHttpClient()
    
    }
    
    @Before
    public void setup() {
        commandQueue.clear()
        commandResponseQueue.clear()
    }
    
    // TESTS
    // ---------------------------------------------------------------------------------------------------

    @Test
    public void simple() {
        
        apps.each {
            it.expect(OFFER_EVENT);
        }
        
        servlet.callEvent('foo', null, OFFER_EVENT)
        
        commandResponseQueue.push(toXML('<ref id="bar"/>'))
        
        apps.each{
            def response = new MockHttpServletResponse()
            servlet.doPost(CONTINUE_REQUEST, response)
            assertEquals(203, response.status)
        }

        def dialCommand = commandQueue.poll().command;
        assertEquals("DialCommand", dialCommand.class.simpleName)
        assertEquals("tel:+13055195825", dialCommand.to.toString())
        assertEquals("tel:+15613504458", dialCommand.from.toString())

        assertAppRequests()

        // Now send a <ringing/> event and make sure it
        //  a) Gets to all app instance
        //  b) Contains the parent call id
                
        apps.each {
            it.expect(RINGING_EVENT, [
                "call-id":"bar",
                "parent-call-id":"foo"
            ]);
        }
        
        // Send <ringing> from B-Party
        servlet.callEvent('bar', null, RINGING_EVENT)
        
        assertAppRequests()
        
    }
    
    @Test
    public void failedOffer() {

        // <offer>        
        apps[0].expect(OFFER_EVENT)
        apps[1].expect(OFFER_EVENT).forceFail()
        apps[2].expect(OFFER_EVENT)
        
        servlet.callEvent('foo', null, OFFER_EVENT)
        
        commandResponseQueue.push(toXML('<ref id="bar"/>'))
        
        apps.each {
            def response = new MockHttpServletResponse()
            servlet.doPost(CONTINUE_REQUEST, response)
            assertEquals(203, response.status)
        }
        
        assertAppRequests()
        
        // <end>        
        apps[0].expect(END_EVENT)
        apps[1].expectNothing()
        apps[2].expect(END_EVENT)
        
        servlet.callEvent('foo', null, END_EVENT)
        assertAppRequests()
        
    }
    
    @Test
    public void timeoutOffer() {

        // <offer>
        apps[0].expect(OFFER_EVENT)
        apps[1].expect(OFFER_EVENT, null, 2000)
        apps[2].expect(OFFER_EVENT)
        
        servlet.callEvent('foo', null, OFFER_EVENT)
        
        commandResponseQueue.push(toXML('<ref id="bar"/>'))
        
        // Simulate each app instance sending <continue>
        apps.each {
            def response = new MockHttpServletResponse()
            servlet.doPost(CONTINUE_REQUEST, response)
            assertEquals(203, response.status)
        }

        assertAppRequests()

        // Make sure we got the dial        
        def dialCommand = commandQueue.poll().command;
        assertEquals("DialCommand", dialCommand.class.simpleName)
        assertEquals("tel:+13055195825", dialCommand.to.toString())
        assertEquals("tel:+15613504458", dialCommand.from.toString())

        // Simulate call ending
        apps[0].expect(END_EVENT)
        apps[1].expectNothing()
        apps[2].expect(END_EVENT)
        
        servlet.callEvent('foo', null, END_EVENT)

        // Make sure the B-leg was hung up        
        def hangupCommand = commandQueue.poll()
        assertEquals(HangupCommand, hangupCommand.command.class)
        assertEquals("bar", hangupCommand.callId)

        assertAppRequests()
        
    }
    
    @Test
    public void rejectOffer() {

        // <offer>
        apps[0].expect(OFFER_EVENT)
        apps[1].expectNothing()
        apps[2].expectNothing()
        
        servlet.callEvent('foo', null, OFFER_EVENT)
        
        def response = new MockHttpServletResponse()
        servlet.doPost(buildRequest('<reject xmlns="urn:xmpp:rayo:1" />'), response)
        assertEquals(203, response.status)
        
        assertEquals("reject", poll().command.name)
        assertAppRequests()

    }
    
    

    // UTIL
    // ---------------------------------------------------------------------------------------------------
    
    private static buildRequest(payload) {
        def request = new MockHttpServletRequest([
            content: payload.bytes,
            remoteAddr: "127.0.0.1",
            
        ])
        request.addHeader("call-id", "foo")
        return request
    }
    
    def poll = {
        commandQueue.poll(100, TimeUnit.SECONDS);
    }
    
    def assertAppRequests = {
        apps.each {
            it.verify()
        }
    }

    private static toXML(s) {
        DocumentHelper.parseText(s).rootElement
    }
    
}
