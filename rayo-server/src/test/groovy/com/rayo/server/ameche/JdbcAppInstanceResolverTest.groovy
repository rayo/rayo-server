package com.rayo.server.ameche

import org.junit.Before;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.RowMapper;

import com.rayo.core.CallDirection;
import com.tropo.test.AwesomeTestRunner;
import com.tropo.test.Database;
import com.tropo.test.MockResultSet;
import com.tropo.test.MockResultSetMetaData;

import org.dom4j.DocumentHelper;
import org.gmock.*

import static com.tropo.test.Assert.*
import static com.tropo.test.Matchers.*
import static org.gmock.GMock.*

class JdbcAppInstanceResolverTest {

	def gmc
	def subject
	def sql = 'foo'
	def jdbc
	
	@Before
	void init() {
		gmc = new GMockController()
		subject = new JdbcAppInstanceResolver()
		jdbc = subject.jdbcTemplate = gmc.mock(JdbcOperations)
		subject.lookupSql = sql
	}
	
	@Test
	void notNull() {
		assertThat subject,is(notNullValue())
	}
	
	@Test
	void mapper() {
		def addy = 'foobar'
		def args = [addy] as Object[]
		def columns = ['appInstanceId', 'url', 'priority', 'permissions', 'required'] as String[]
		def rows = [
			[42, 'http://foo.bar:9999', 10, 4, true] as Object[],
			[45, 'http://foo.bar:9998', 10, 4, true] as Object[],
		] as Object[][]		
		def rs = MockResultSet.create(MockResultSetMetaData.create(columns), rows)
		jdbc.query(sql, args, match {RowMapper mapper->
			(1..rows.length).each {rowIdx->
				rs.next()
				AppInstance instance = mapper.mapRow(rs, rowIdx)
				println instance
				assertThat instance,is(notNullValue())
				assertThat instance.id,is(rows[rowIdx - 1][0].toString())				
				assertThat instance.endpoint,is(URI.create(rows[rowIdx - 1][1]))
				assertThat instance.priority, is(10)
				assertThat instance.permissions, is(4)				
				assertThat instance.required, is(true)				
			} 
			assertThat rs.next(),is(false)
			return true
		})
		gmc.play {
			def offer = toXML("""<offer to="$addy" from="tel:+15613504458"/>""")
			subject.lookup(offer, CallDirection.IN)
		}
	}
	
	private static toXML(s) {
		DocumentHelper.parseText(s).rootElement
	}
}