package com.voxeo.ozone.client.internal;

import org.junit.After;
import org.junit.Before;

import com.voxeo.ozone.client.OzoneClient;
import com.voxeo.ozone.client.internal.NettyServer;
import com.voxeo.ozone.client.listener.StanzaListener;
import com.voxeo.ozone.client.test.config.TestConfig;
import com.voxeo.servlet.xmpp.ozone.stanza.Error;
import com.voxeo.servlet.xmpp.ozone.stanza.IQ;
import com.voxeo.servlet.xmpp.ozone.stanza.Message;
import com.voxeo.servlet.xmpp.ozone.stanza.Presence;

public abstract class XmppFunctionalTest {

	static final String AUDIT_ENDPOINT = "http://localhost:8080/ozone/audit.html";
	String mohoCallId;
	protected OzoneClient ozone;
	private String username = "userc";

//	SipStack sipStack;
//	SipPhone sipPhone;
	
	@Before
	public void setUp() throws Exception {
		
//		sipStack = new SipStack(null, 6667);
//		sipPhone = sipStack.createSipPhone("127.0.0.1", "sip:funcional-test-suite@127.0.0.1"); // create user a
		NettyServer server = new NettyServer(5222);

		mohoCallId = null;
		ozone = new OzoneClient(TestConfig.serverEndpoint);
		ozone.connect(username, "1");

	}
		
	public void assertReceived(String message) {
		
	}
	
	@After
	public void dispose() throws Exception {
		
		ozone.disconnect();
	}
	
	void hookToExistingCall() throws Exception {
		
//		SipCall call = sipPhone.makeCall("sip:ozone@127.0.0.1", "127.0.0.1:6061/UDP");
//		Thread.sleep(1000);

//		String sipCallId = call.getDialog().getCallId().getCallId();
		
//		assertFalse(call.isCallAnswered());	
	}
	
	void login(String username, String resource) throws Exception {
		
		ozone.connect(username, "1", resource);
		
		ozone.addStanzaListener(new StanzaListener() {
			
			@Override
			public void onPresence(Presence presence) {

				System.out.println(String.format("Message from server: [%s]",presence));
			}
			
			@Override
			public void onMessage(Message message) {

				System.out.println(String.format("Message from server: [%s]",message));
			}
			
			@Override
			public void onIQ(IQ iq) {

				System.out.println(String.format("Message from server: [%s]",iq));				
			}
			
			@Override
			public void onError(Error error) {

				System.out.println(String.format("Message from server: [%s]",error));
			}
		});
	}
	
	void disconnect() throws Exception {
		
		ozone.disconnect();
	}
	
	protected void setUsername(String username) {
		
		this.username = username;
	}
}
