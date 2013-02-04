package com.rayo.server.filter

import static org.junit.Assert.*

import org.junit.Before;
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner

import com.rayo.server.exception.RayoProtocolException;
import com.rayo.server.filter.FilterChain;
import com.rayo.server.filter.MessageFilter;
import com.rayo.core.CallCommand;
import com.rayo.core.CallEvent;
import com.rayo.core.validation.ValidationException;
import com.voxeo.servlet.xmpp.StanzaError.Condition;
import com.voxeo.servlet.xmpp.StanzaError.Type;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations=["/rayo-context-activemq-cdr.xml"])
public class FilterChainTest {

    @Autowired
    private FilterChain filtersChain
	
	@Before
	public void init() {
		
		filtersChain.clear()
	}
	
    @Test
    public void testAddFilter() throws InterruptedException {
		
		assertEquals filtersChain.filters.size(), 0
		filtersChain.addFilter([:] as MessageFilter)
		assertEquals filtersChain.filters.size(), 1
	}	
	
	@Test
	public void testClear() throws InterruptedException {
		
		assertEquals filtersChain.filters.size(), 0
		filtersChain.addFilter([:] as MessageFilter)
		filtersChain.clear()
		assertEquals filtersChain.filters.size(), 0
	}
	
	@Test
	public void testRemoveFilter() throws InterruptedException {
		
		assertEquals filtersChain.filters.size(), 0
		def filter = [:] as MessageFilter
		filtersChain.addFilter(filter)
		assertEquals filtersChain.filters.size(), 1
		filtersChain.removeFilter(filter)
		assertEquals filtersChain.filters.size(), 0
	}
	
	@Test
	public void testAddFilterIndex() throws InterruptedException {
		
		def filter1 = [:] as MessageFilter
		def filter2 = [:] as MessageFilter
		def filter3 = [:] as MessageFilter
		filtersChain.addFilter(filter1)
		filtersChain.addFilter(filter2)
		filtersChain.addFilter(1,filter3)
		assertEquals filtersChain.filters[1],filter3
	}
		
	@Test
	public void testCommandRequestExecutedInSequence() throws InterruptedException {
		
		def queue = new LinkedList()
		def filter1 = [handleCommandRequest:{command, chain -> queue.offer("filter1"); return command}] as MessageFilter
		def filter2 = [handleCommandRequest:{command, chain -> queue.offer("filter2"); return command}] as MessageFilter
		def filter3 = [handleCommandRequest:{command, chain -> queue.offer("filter3"); return command}] as MessageFilter
		filtersChain.addFilter(filter1)
		filtersChain.addFilter(filter2)
		filtersChain.addFilter(filter3)
		
		def callCommand = [:] as CallCommand
		filtersChain.handleCommandRequest(callCommand)
		assertEquals queue.poll(),"filter1"
		assertEquals queue.poll(),"filter2"
		assertEquals queue.poll(),"filter3"
	}
	
	@Test
	public void testNullCommandInterruptsFilterChain() throws InterruptedException {
		
		def queue = new LinkedList()
		def filter1 = [handleCommandRequest:{command, chain -> queue.offer("filter1"); return command}] as MessageFilter
		def filter2 = [handleCommandRequest:{command, chain -> null}] as MessageFilter
		def filter3 = [handleCommandRequest:{command, chain -> queue.offer("filter3"); return command}] as MessageFilter
		filtersChain.addFilter(filter1)
		filtersChain.addFilter(filter2)
		filtersChain.addFilter(filter3)
		
		def callCommand = [:] as CallCommand
		filtersChain.handleCommandRequest(callCommand)
		assertEquals queue.poll(),"filter1"
		assertNull queue.poll()
	}
	
	@Test
	public void testProtocolExceptionInCommandInterruptsFilterChain() throws InterruptedException {
		
		def queue = new LinkedList()
		def filter1 = [handleCommandRequest:{command, chain -> queue.offer("filter1"); return command}] as MessageFilter
		def filter2 = [handleCommandRequest:{command, chain -> throw new RayoProtocolException(RayoProtocolException.Condition.BAD_REQUEST,"error")}] as MessageFilter
		def filter3 = [handleCommandRequest:{command, chain -> queue.offer("filter3"); return command}] as MessageFilter
		filtersChain.addFilter(filter1)
		filtersChain.addFilter(filter2)
		filtersChain.addFilter(filter3)
		
		try {
			def callCommand = [:] as CallCommand
			filtersChain.handleCommandRequest(callCommand)
			fail("Expected Exception")
		} catch (RayoProtocolException e) {}
		assertEquals queue.poll(),"filter1"
		assertNull queue.poll()
	}
	
	@Test
	public void testCommandResponseExecutedInSequence() throws InterruptedException {
		
		def queue = new LinkedList()
		def filter1 = [handleCommandResponse:{response, chain -> queue.offer("filter1"); return response}] as MessageFilter
		def filter2 = [handleCommandResponse:{response, chain -> queue.offer("filter2"); return response}] as MessageFilter
		def filter3 = [handleCommandResponse:{response, chain -> queue.offer("filter3"); return response}] as MessageFilter
		filtersChain.addFilter(filter1)
		filtersChain.addFilter(filter2)
		filtersChain.addFilter(filter3)
		
		def commandResponse = new Object()
		filtersChain.handleCommandResponse(commandResponse)
		assertEquals queue.poll(),"filter1"
		assertEquals queue.poll(),"filter2"
		assertEquals queue.poll(),"filter3"
	}
	
	@Test
	public void testNullResponseInterruptsFilterChain() throws InterruptedException {
		
		def queue = new LinkedList()
		def filter1 = [handleCommandResponse:{response, chain -> queue.offer("filter1"); return response}] as MessageFilter
		def filter2 = [handleCommandResponse:{response, chain -> null}] as MessageFilter
		def filter3 = [handleCommandResponse:{response, chain -> queue.offer("filter3"); return response}] as MessageFilter
		filtersChain.addFilter(filter1)
		filtersChain.addFilter(filter2)
		filtersChain.addFilter(filter3)
		
		def commandResponse = new Object()
		filtersChain.handleCommandResponse(commandResponse)
		assertEquals queue.poll(),"filter1"
		assertNull queue.poll()
	}
	
	@Test
	public void testProtocolExceptionInResponseInterruptsFilterChain() throws InterruptedException {
		
		def queue = new LinkedList()
		def filter1 = [handleCommandResponse:{response, chain -> queue.offer("filter1"); return response}] as MessageFilter
		def filter2 = [handleCommandResponse:{response, chain -> throw new RayoProtocolException(RayoProtocolException.Condition.BAD_REQUEST,"error")}] as MessageFilter
		def filter3 = [handleCommandResponse:{response, chain -> queue.offer("filter3"); return response}] as MessageFilter
		filtersChain.addFilter(filter1)
		filtersChain.addFilter(filter2)
		filtersChain.addFilter(filter3)
		
		def commandResponse = new Object()
		try {
			filtersChain.handleCommandResponse(commandResponse)
			fail("Expected Exception")
		} catch (RayoProtocolException e) {}
		assertEquals queue.poll(),"filter1"
		assertNull queue.poll()
	}
	
	@Test
	public void testEventExecutedInSequence() throws InterruptedException {
		
		def queue = new LinkedList()
		def filter1 = [handleEvent:{event, chain -> queue.offer("filter1");return event}] as MessageFilter
		def filter2 = [handleEvent:{event, chain -> queue.offer("filter2");return event}] as MessageFilter
		def filter3 = [handleEvent:{event, chain -> queue.offer("filter3");return event}] as MessageFilter
		filtersChain.addFilter(filter1)
		filtersChain.addFilter(filter2)
		filtersChain.addFilter(filter3)
		
		def CallEvent event = [:] as CallEvent
		filtersChain.handleEvent(event)
		assertEquals queue.poll(),"filter1"
		assertEquals queue.poll(),"filter2"
		assertEquals queue.poll(),"filter3"
	}
	
	@Test
	public void testNullEventInterruptsFilterChain() throws InterruptedException {
		
		def queue = new LinkedList()
		def filter1 = [handleEvent:{event, chain -> queue.offer("filter1");return event}] as MessageFilter
		def filter2 = [handleEvent:{event, chain -> null}] as MessageFilter
		def filter3 = [handleEvent:{event, chain -> queue.offer("filter3");return event}] as MessageFilter
		filtersChain.addFilter(filter1)
		filtersChain.addFilter(filter2)
		filtersChain.addFilter(filter3)
		
		def CallEvent event = [:] as CallEvent
		filtersChain.handleEvent(event)
		assertEquals queue.poll(),"filter1"
		assertNull queue.poll()
	}
	
	@Test
	public void testProtocolExceptionInEventInterruptsFilterChain() throws InterruptedException {
		
		def queue = new LinkedList()
		def filter1 = [handleEvent:{event, chain -> queue.offer("filter1");return event}] as MessageFilter
		def filter2 = [handleEvent:{event, chain -> throw new RayoProtocolException(RayoProtocolException.Condition.BAD_REQUEST,"error")}] as MessageFilter
		def filter3 = [handleEvent:{event, chain -> queue.offer("filter3");return event}] as MessageFilter
		filtersChain.addFilter(filter1)
		filtersChain.addFilter(filter2)
		filtersChain.addFilter(filter3)
		
		def CallEvent event = [:] as CallEvent
		try {
			filtersChain.handleEvent(event)
			fail("Expected Exception")
		} catch (RayoProtocolException e) {}
		assertEquals queue.poll(),"filter1"
		assertNull queue.poll()
	}
	
	@Test
	public void testSetAttribute() throws InterruptedException {

		def queue = new LinkedList()
		def filter1 = [handleCommandRequest:{command, context ->
			context.setAttribute("test","value")
			queue.offer("filter1")
			return command}] as MessageFilter
		def filter2 = [handleCommandRequest:{command, context ->
			queue.offer(context.getAttribute("test")); return command}] as MessageFilter

		filtersChain.addFilter(filter1)
		filtersChain.addFilter(filter2)

		def callCommand = [:] as CallCommand
		filtersChain.handleCommandRequest(callCommand)
		assertEquals queue.poll(),"filter1"
		assertEquals queue.poll(),"value"
	}
	
	@Test
	public void testFilterContextIsResetAcrossChainExecutions() throws InterruptedException {

		def queue = new LinkedList()
		def filter1 = [handleCommandRequest:{command, context ->
			context.setAttribute("test","value");return command}] as MessageFilter

		filtersChain.addFilter(filter1)
		filtersChain.handleCommandRequest(null)
		
		def filter2 = [handleCommandRequest:{command, context ->
			queue.offer(context.getAttribute("test")?context.getAttribute("test"):"empty"); return command}] as MessageFilter
		filtersChain.clear()
		filtersChain.addFilter(filter2)
		
		def callCommand = [:] as CallCommand
		filtersChain.handleCommandRequest(callCommand)

		assertEquals queue.poll(),"empty"
	}
}