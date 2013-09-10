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
import com.rayo.server.CallManager
import com.rayo.server.test.MockAddress
import com.rayo.server.test.MockSIPFactoryImpl
import com.rayo.server.test.MockSipURI
import com.rayo.server.test.MockTelURL
import com.tropo.test.MockResultSet
import com.tropo.test.MockResultSetMetaData
import com.voxeo.moho.ApplicationContext

class JdbcAppInstanceResolverTest {

	def gmc
	def subject
	def sql = 'foo'
	def jdbc

	def sipFactory;
	def applicationContext;
	def callManager;

	@Before
	void init() {
		sipFactory = new MockSIPFactoryImpl()

		applicationContext = [
			getSipFactory : { return sipFactory }
		] as ApplicationContext

		callManager = [
			getApplicationContext : { return applicationContext }
		] as CallManager

		gmc = new GMockController()
		subject = new JdbcAppInstanceResolver()
		subject.setCallManager(callManager)
		jdbc = subject.jdbcTemplate = gmc.mock(JdbcOperations)
		subject.lookupSql = sql
	}

	@Test
	void notNull() {
		assertThat subject,is(notNullValue())
	}

	@Test
	void mapperIn() {
		// create a MockTelURL
		def mockTelUrl = new MockTelURL();
		mockTelUrl.setPhoneNumber("+15613504458");

		// create a MockAddress and give it the MockTelURL
		def mockAddress = new MockAddress();
		mockAddress.setTelUrl(mockTelUrl);

		// give the MockAddress to the sipFactory
		sipFactory.setAddress(mockAddress);

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
		// create a MockTelURL
		def mockTelUrl = new MockTelURL();
		mockTelUrl.setPhoneNumber("+15613504458");

		// create a MockAddress and give it the MockTelURL
		def mockAddress = new MockAddress();
		mockAddress.setTelUrl(mockTelUrl);

		// give the MockAddress to the sipFactory
		sipFactory.setAddress(mockAddress);

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
		// create a MockSipURI
		def mockSipUri = new MockSipURI();
		mockSipUri.setUser("foobar");
		mockSipUri.setHost("104.65.174.101");

		// create a MockAddress and give it the MockSipURI
		def mockAddress = new MockAddress();
		mockAddress.setSipUri(mockSipUri);

		// give the MockAddress to the sipFactory
		sipFactory.setAddress(mockAddress);

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
		// create a MockSipURI
		def mockSipUri = new MockSipURI();
		mockSipUri.setUser("foobar");
		mockSipUri.setHost("104.65.174.100");

		// create a MockAddress and give it the MockSipURI
		def mockAddress = new MockAddress();
		mockAddress.setSipUri(mockSipUri);

		// give the MockAddress to the sipFactory
		sipFactory.setAddress(mockAddress);

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
		// create a MockTelURL
		def mockTelUrl = new MockTelURL();
		mockTelUrl.setPhoneNumber("+12152065077");

		// create a MockAddress and give it the MockTelURL
		def mockAddress = new MockAddress();
		mockAddress.setTelUrl(mockTelUrl);

		// give the MockAddress to the sipFactory
		sipFactory.setAddress(mockAddress);

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
		// create a MockTelURL
		def mockTelUrl = new MockTelURL();
		mockTelUrl.setPhoneNumber("+12152065077");

		// create a MockAddress and give it the MockTelURL
		def mockAddress = new MockAddress();
		mockAddress.setTelUrl(mockTelUrl);

		// give the MockAddress to the sipFactory
		sipFactory.setAddress(mockAddress);

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

	@Test
	void mapperPServedUserUpper() {
		// create a MockTelURL
		def mockTelUrl = new MockTelURL();
		mockTelUrl.setPhoneNumber("+12152065077");

		// create a MockAddress and give it the MockTelURL
		def mockAddress = new MockAddress();
		mockAddress.setTelUrl(mockTelUrl);

		// give the MockAddress to the sipFactory
		sipFactory.setAddress(mockAddress);

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
		// create a MockTelURL
		def mockTelUrl = new MockTelURL();
		mockTelUrl.setPhoneNumber("+12152065077");

		// create a MockAddress and give it the MockTelURL
		def mockAddress = new MockAddress();
		mockAddress.setTelUrl(mockTelUrl);

		// give the MockAddress to the sipFactory
		sipFactory.setAddress(mockAddress);

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
		// create a MockSipURI
		def mockSipUri = new MockSipURI();
		mockSipUri.setUser("jdecastro");
		mockSipUri.setHost("att.net");

		// create a MockAddress and give it the MockSipURI
		def mockAddress = new MockAddress();
		mockAddress.setSipUri(mockSipUri);

		// give the MockAddress to the sipFactory
		sipFactory.setAddress(mockAddress);

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
		// create a MockSipURI
		def mockSipUri = new MockSipURI();
		mockSipUri.setUser("jdecastro");
		mockSipUri.setHost("att.net");

		// create a MockAddress and give it the MockSipURI
		def mockAddress = new MockAddress();
		mockAddress.setSipUri(mockSipUri);

		// give the MockAddress to the sipFactory
		sipFactory.setAddress(mockAddress);

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

	private static toXML(s) {
		DocumentHelper.parseText(s).rootElement
	}
}