package samples;

import com.voxeo.ozone.client.OzoneClient;
import com.voxeo.ozone.client.listener.StanzaListener;
import com.voxeo.servlet.xmpp.ozone.stanza.Error;
import com.voxeo.servlet.xmpp.ozone.stanza.IQ;
import com.voxeo.servlet.xmpp.ozone.stanza.Message;
import com.voxeo.servlet.xmpp.ozone.stanza.Presence;

public abstract class BaseSample {

	protected OzoneClient client;
	
	public void connect(String server, String username, String password) throws Exception {
		
		client = new OzoneClient(server);
		login(username,password,"voxeo");
	}
		
	public void shutdown() throws Exception {
		
		client.disconnect();
	}
		
	void login(String username, String password, String resource) throws Exception {
		
		client.connect(username, password, resource);
		
		client.addStanzaListener(new StanzaListener() {
			
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
}
