package com.tropo.server.filter

import static org.junit.Assert.*

import org.junit.Before;
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner

import com.tropo.core.validation.ValidationException;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations=["/tropo-context-activemq-cdr.xml"])
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
	public void testSetAttribute() throws InterruptedException {
		
		assertNull filtersChain.getAttribute("test")
		filtersChain.setAttribute("test","value")
		assertEquals filtersChain.getAttribute("test"),"value"
	}
	
	@Test
	public void testCommandRequestExecutedInSequence() throws InterruptedException {
		
		def queue = new LinkedList()
		def filter1 = [handleCommandRequest:{command, chain -> queue.offer("filter1")}] as MessageFilter
		def filter2 = [handleCommandRequest:{command, chain -> queue.offer("filter2")}] as MessageFilter
		def filter3 = [handleCommandRequest:{command, chain -> queue.offer("filter3")}] as MessageFilter
		filtersChain.addFilter(filter1)
		filtersChain.addFilter(filter2)
		filtersChain.addFilter(filter3)
		
		filtersChain.handleCommandRequest(null)
		assertEquals queue.poll(),"filter1"
		assertEquals queue.poll(),"filter2"
		assertEquals queue.poll(),"filter3"
	}
	
	@Test
	public void testCommandResponseExecutedInSequence() throws InterruptedException {
		
		def queue = new LinkedList()
		def filter1 = [handleCommandResponse:{response, chain -> queue.offer("filter1")}] as MessageFilter
		def filter2 = [handleCommandResponse:{response, chain -> queue.offer("filter2")}] as MessageFilter
		def filter3 = [handleCommandResponse:{response, chain -> queue.offer("filter3")}] as MessageFilter
		filtersChain.addFilter(filter1)
		filtersChain.addFilter(filter2)
		filtersChain.addFilter(filter3)
		
		filtersChain.handleCommandResponse(null)
		assertEquals queue.poll(),"filter1"
		assertEquals queue.poll(),"filter2"
		assertEquals queue.poll(),"filter3"
	}
	
	@Test
	public void testEventExecutedInSequence() throws InterruptedException {
		
		def queue = new LinkedList()
		def filter1 = [handleEvent:{event, chain -> queue.offer("filter1")}] as MessageFilter
		def filter2 = [handleEvent:{event, chain -> queue.offer("filter2")}] as MessageFilter
		def filter3 = [handleEvent:{event, chain -> queue.offer("filter3")}] as MessageFilter
		filtersChain.addFilter(filter1)
		filtersChain.addFilter(filter2)
		filtersChain.addFilter(filter3)
		
		filtersChain.handleEvent(null)
		assertEquals queue.poll(),"filter1"
		assertEquals queue.poll(),"filter2"
		assertEquals queue.poll(),"filter3"
	}
}