package com.tropo.functional

import static org.junit.Assert.*
import org.junit.Test;
import org.junit.Before;

import com.voxeo.prism.tf.TestFramework;

class DeployTest {

	def prismLocation
	def prismPort
	def warLocation
	def serverName
	def serverPort
	def appName
	
	@Before
	public void loadProperties() {

		prismLocation = System.getProperty('prism.location')
		prismPort = System.getProperty('prism.port')
		warLocation = System.getProperty('war.location')
		serverName = System.getProperty('server.name')
		serverPort = System.getProperty('server.port')
		appName = System.getProperty('app.name')
	
		def is = getClass().getClassLoader().getResourceAsStream("test.properties")
		def props = new Properties()
		props.load(is)
		if (!prismLocation) {
			prismLocation = props.get('prism.location')
		}
		if (!prismPort) {
			prismPort = props.get('prism.port')
		}		
		if (!warLocation) {
			warLocation = props.get('war.location')
		}		
		if (!serverName) {
			serverName = props.get('server.name')
		}		
		if (!serverPort) {
			serverPort = props.get('server.port')
		}
		if (!appName) {
			appName = props.get('app.name')
		}		
	} 
	
	@Test
	public void testDeploy() {
		
		def tf
		def server
				
		try {
			tf = TestFramework.create(Integer.valueOf(prismPort))
			server = tf.createPrismServer(prismLocation)
			server.undeploy appName
			def app = tf.createFileApplication(warLocation)
			server.deploy app			
			server.start 'Tropo Functional Test Server'
			
			assertEquals new URL("http://${serverName}:${serverPort}/" + appName).openConnection().responseCode, 200
		} catch (Exception e) {
			e.printStackTrace()
			throw e;
		} finally {
			if (server.isRunning()) {
				server.stop()
			}		
			TestFramework.release(tf.report(server))
		}
		
	}
}
