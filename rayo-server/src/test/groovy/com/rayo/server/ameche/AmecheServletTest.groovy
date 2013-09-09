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
import org.junit.Ignore
import org.junit.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

import com.rayo.core.HangupCommand
import com.rayo.server.CommandHandler


@Ignore
class AmecheServletTest {

	private static Element OFFER_EVENT = toXML('<offer to="tel:+13055195825" from="tel:+15613504458"/>')
	private static Element OFFER_EVENT_1 = toXML('<offer to="tel:+13055195825;sescase=term;regstate=reg" from="tel:+15613504458;sescase=term;regstate=reg"/>')
	private static Element OFFER_EVENT_2 = toXML('<offer to="sip:+12152065077@104.65.174.101;user=phone" from="sip:+12152065077@104.65.174.101;user=phone"/>')
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

		servlet.commandHandler = {
			callId, componentId, command, callback ->
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

		servlet.appInstanceResolver = {
			apps.collect {
				new AppInstance(
						it.port.toString(),
						URI.create("http://127.0.0.1:" + it.port + "/")
						)
			}
		} as AppInstanceResolver

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

		// App #2 is going to start a <say> component

		commandResponseQueue.push(toXML('<ref id="say-1"/>'))

		def response = new MockHttpServletResponse()
		servlet.doPost(buildRequest('<say xmlns="urn:xmpp:rayo:1">bling</say>', apps[1]), response)

		assertEquals('<ref id="say-1"/>', response.content.toString())

		// Send a <repeating> event and make sure only apps[1] gets it
		def REPEATING_EVENT = toXML('<repeating/>')

		apps[0].expectNothing()
		apps[1].expect(REPEATING_EVENT)
		apps[2].expectNothing()

		servlet.callEvent('foo', 'say-1', REPEATING_EVENT)
		assertAppRequests()

		// Send a complete event and make sure only apps[1] gets it
		def COMPLETE_EVENT = toXML('<complete/>')

		apps[0].expectNothing()
		apps[1].expect(COMPLETE_EVENT)
		apps[2].expectNothing()

		servlet.callEvent('foo', 'say-1', COMPLETE_EVENT)
		assertAppRequests()

		// Send the complete again and make sure it's ignored
		apps[0].expectNothing()
		apps[1].expectNothing()
		apps[2].expectNothing()

		servlet.callEvent('foo', 'say-1', COMPLETE_EVENT)
		assertAppRequests()
	}

	@Test
	public void failedDialSecondLeg() {

		apps.each {
			it.expect(OFFER_EVENT);
		}

		commandResponseQueue.push(new IllegalStateException())

		servlet.callEvent('foo', null, OFFER_EVENT)

		apps.each{
			def response = new MockHttpServletResponse()
			servlet.doPost(CONTINUE_REQUEST, response)
			assertEquals(203, response.status)
		}

		commandQueue.poll(); // discard DialCommand
	}

	@Test
	public void failedCommand() {

		apps.each {
			it.expect(OFFER_EVENT);
		}

		commandResponseQueue.push(toXML('<ref id="bar"/>'))

		servlet.callEvent('foo', null, OFFER_EVENT)

		apps.each{
			def response = new MockHttpServletResponse()
			servlet.doPost(CONTINUE_REQUEST, response)
			assertEquals(203, response.status)
		}

		commandQueue.poll(); // discard DialCommand

		// Fail the <output> command
		commandResponseQueue.push(new IllegalStateException())

		def response = new MockHttpServletResponse()
		servlet.doPost(buildRequest('<output xmlns="urn:xmpp:rayo:output:1">bling</output>', apps[1]), response)

		assertEquals(500, response.status)
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
	public void timeoutOffer_1() {
		// same test as above, but with OFFER_EVENT_1 in order to
		// test differently formatted numbers

		// <offer>
		apps[0].expect(OFFER_EVENT_1)
		apps[1].expect(OFFER_EVENT_1, null, 2000)
		apps[2].expect(OFFER_EVENT_1)

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
	public void timeoutOffer_2() {
		// same test as above, but with OFFER_EVENT_2 in order to
		// test differently formatted numbers

		// <offer>
		apps[0].expect(OFFER_EVENT_2)
		apps[1].expect(OFFER_EVENT_2, null, 2000)
		apps[2].expect(OFFER_EVENT_2)

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

	// TODO additional test cases?
	// See [AMECHE-326], use of P-Served-User for app instance
	// resolving. Add test case (one or more) that uses
	// P-Served-User.
	
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

	private static buildRequest(payload, fromServer=null) {
		def request = new MockHttpServletRequest([
			content: payload.bytes,
			remoteAddr: "127.0.0.1",

		])
		request.addHeader("call-id", "foo")
		if(fromServer != null) {
			request.addHeader("app-instance-id", fromServer.port.toString())
		}
		else {
			request.addHeader("app-instance-id", "8881")
		}
		return request
	}

	def poll = {
		commandQueue.poll(100, TimeUnit.SECONDS);
	}

	def assertAppRequests = {
		apps.each { it.verify() }
	}

	private static toXML(s) {
		DocumentHelper.parseText(s).rootElement
	}

}
