package com.rayo.server.ameche

import static com.tropo.test.Assert.*
import static com.tropo.test.Matchers.*
import static org.gmock.GMock.*

import org.dom4j.DocumentHelper
import org.gmock.*
import org.junit.Before
import org.junit.Test
import org.springframework.jdbc.core.JdbcOperations
import org.springframework.jdbc.core.RowMapper

import com.rayo.core.CallDirection
import com.tropo.test.MockResultSet
import com.tropo.test.MockResultSetMetaData

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
	void mapperIn() {
		def addy = '+15613504458'
		def args = [addy] as Object[]
		def columns = [
			'appInstanceId',
			'url',
			'priority',
			'permissions',
			'required'] as String[]
		def rows = [
			[
				42,
				'http://foo.bar:9999',
				10,
				4,
				true] as Object[],
			[
				45,
				'http://foo.bar:9998',
				10,
				4,
				true] as Object[],
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
			def offer = toXML("""<offer to="tel:$addy" from="tel:+15613504458"/>""")
			subject.lookup(offer, CallDirection.IN)
		}
	}

	@Test
	void mapperOut() {
		def addy = '+15613504458'
		def args = [addy] as Object[]
		def columns = [
			'appInstanceId',
			'url',
			'priority',
			'permissions',
			'required'] as String[]
		def rows = [
			[
				42,
				'http://foo.bar:9999',
				10,
				4,
				true] as Object[],
			[
				45,
				'http://foo.bar:9998',
				10,
				4,
				true] as Object[],
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
			def offer = toXML("""<offer to="tel:+15613504458" from="tel:$addy"/>""")
			subject.lookup(offer, CallDirection.OUT)
		}
	}

	@Test
	void mapperInAt() {
		def addy = 'foobar@104.65.174.101'
		def args = [addy] as Object[]
		def columns = [
			'appInstanceId',
			'url',
			'priority',
			'permissions',
			'required'] as String[]
		def rows = [
			[
				42,
				'http://foo.bar:9999',
				10,
				4,
				true] as Object[],
			[
				45,
				'http://foo.bar:9998',
				10,
				4,
				true] as Object[],
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
			def offer = toXML("""<offer to="sip:$addy" from="tel:+15613504458;user=phone"/>""")
			subject.lookup(offer, CallDirection.IN)
		}
	}

	@Test
	void mapperOutAt() {
		def addy = 'foobar@104.65.174.100'
		def args = [addy] as Object[]
		def columns = [
			'appInstanceId',
			'url',
			'priority',
			'permissions',
			'required'] as String[]
		def rows = [
			[
				42,
				'http://foo.bar:9999',
				10,
				4,
				true] as Object[],
			[
				45,
				'http://foo.bar:9998',
				10,
				4,
				true] as Object[],
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
			def offer = toXML("""<offer to="tel:+15613504458;user=phone" from="sip:$addy"/>""")
			subject.lookup(offer, CallDirection.OUT)
		}
	}

	@Test
	void mapperInParam() {
		def addy = '+12152065077'
		def args = [addy] as Object[]
		def columns = [
			'appInstanceId',
			'url',
			'priority',
			'permissions',
			'required'] as String[]
		def rows = [
			[
				42,
				'http://foo.bar:9999',
				10,
				4,
				true] as Object[],
			[
				45,
				'http://foo.bar:9998',
				10,
				4,
				true] as Object[],
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
			def offer = toXML("""<offer to="tel:$addy" from="tel:+12152065077;sescase=term;regstate=reg"/>""")
			subject.lookup(offer, CallDirection.IN)
		}
	}

	@Test
	void mapperOutParam() {
		def addy = '+12152065077'
		def args = [addy] as Object[]
		def columns = [
			'appInstanceId',
			'url',
			'priority',
			'permissions',
			'required'] as String[]
		def rows = [
			[
				42,
				'http://foo.bar:9999',
				10,
				4,
				true] as Object[],
			[
				45,
				'http://foo.bar:9998',
				10,
				4,
				true] as Object[],
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
			def offer = toXML("""<offer to="tel:+12152065077;sescase=term;regstate=reg" from="tel:$addy"/>""")
			subject.lookup(offer, CallDirection.OUT)
		}
	}

	/*
	 @Test
	 void mapperPServedUserUpper() {
	 def addy = '+12152065077'
	 def args = [addy] as Object[]
	 def columns = [
	 'appInstanceId',
	 'url',
	 'priority',
	 'permissions',
	 'required'] as String[]
	 def rows = [
	 [
	 42,
	 'http://foo.bar:9999',
	 10,
	 4,
	 true] as Object[],
	 [
	 45,
	 'http://foo.bar:9998',
	 10,
	 4,
	 true] as Object[],
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
	 def offer = toXML("""<offer to="sip:abc@abc" from="sip:def@def"> <header name="P-Served-User" value="tel:+12152065077;sescase=term;regstate=reg"/><header name="P-Asserted-Identity" value="&lt;sip:bob@foo.bar&gt;"/></offer>""")
	 subject.lookup(offer, CallDirection.IN)
	 }
	 }
	 @Test
	 void mapperPServedUserLower() {
	 def addy = '+12152065077'
	 def args = [addy] as Object[]
	 def columns = [
	 'appInstanceId',
	 'url',
	 'priority',
	 'permissions',
	 'required'] as String[]
	 def rows = [
	 [
	 42,
	 'http://foo.bar:9999',
	 10,
	 4,
	 true] as Object[],
	 [
	 45,
	 'http://foo.bar:9998',
	 10,
	 4,
	 true] as Object[],
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
	 def offer = toXML("""<offer to="sip:abc@abc" from="sip:def@abc"> <header name="p-served-user" value="tel:+12152065077;sescase=term;regstate=reg"/><header name="P-Asserted-Identity" value="&lt;sip:bob@foo.bar&gt;"/></offer>""")
	 subject.lookup(offer, CallDirection.IN)
	 }
	 }
	 @Test
	 void mapperPServedUserLowerSip1() {
	 def addy = 'jdecastro@att.net'
	 def args = [addy] as Object[]
	 def columns = [
	 'appInstanceId',
	 'url',
	 'priority',
	 'permissions',
	 'required'] as String[]
	 def rows = [
	 [
	 42,
	 'http://foo.bar:9999',
	 10,
	 4,
	 true] as Object[],
	 [
	 45,
	 'http://foo.bar:9998',
	 10,
	 4,
	 true] as Object[],
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
	 def offer = toXML("""<offer to="sip:abc@abc" from="sip:def@def"> <header name="p-served-user" value="sip:jdecastro@att.net;foo=bar;bling=baz"/><header name="P-Asserted-Identity" value="&lt;sip:bob@foo.bar&gt;"/></offer>""")
	 subject.lookup(offer, CallDirection.IN)
	 }
	 }
	 @Test
	 void mapperPServedUserLowerSip2() {
	 def addy = 'jdecastro@att.net'
	 def args = [addy] as Object[]
	 def columns = [
	 'appInstanceId',
	 'url',
	 'priority',
	 'permissions',
	 'required'] as String[]
	 def rows = [
	 [
	 42,
	 'http://foo.bar:9999',
	 10,
	 4,
	 true] as Object[],
	 [
	 45,
	 'http://foo.bar:9998',
	 10,
	 4,
	 true] as Object[],
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
	 def offer = toXML("""<offer to="sip:abc@abc" from="sip:def@def"> <header name="p-served-user" value="sip:jdecastro@att.net;foo=bar;bling=baz"/><header name="P-Asserted-Identity" value="&lt;sip:bob@foo.bar&gt;"/></offer>""")
	 subject.lookup(offer, CallDirection.IN)
	 }
	 }
	 */
	private static toXML(s) {
		DocumentHelper.parseText(s).rootElement
	}
}