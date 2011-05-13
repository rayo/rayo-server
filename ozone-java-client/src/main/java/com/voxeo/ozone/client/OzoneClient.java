package com.voxeo.ozone.client;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.joda.time.Duration;

import com.tropo.core.AnswerCommand;
import com.tropo.core.DialCommand;
import com.tropo.core.HangupCommand;
import com.tropo.core.verb.Ask;
import com.tropo.core.verb.AudioItem;
import com.tropo.core.verb.Choices;
import com.tropo.core.verb.ChoicesList;
import com.tropo.core.verb.Conference;
import com.tropo.core.verb.PauseCommand;
import com.tropo.core.verb.PromptItem;
import com.tropo.core.verb.PromptItems;
import com.tropo.core.verb.RefEvent;
import com.tropo.core.verb.ResumeCommand;
import com.tropo.core.verb.Say;
import com.tropo.core.verb.SsmlItem;
import com.tropo.core.verb.StopCommand;
import com.tropo.core.verb.Transfer;
import com.voxeo.ozone.client.auth.AuthenticationListener;
import com.voxeo.ozone.client.listener.OzoneMessageListener;
import com.voxeo.ozone.client.listener.StanzaListener;
import com.voxeo.ozone.client.ref.SayRef;
import com.voxeo.servlet.xmpp.ozone.extensions.Extension;
import com.voxeo.servlet.xmpp.ozone.stanza.IQ;

/**
 * This class servers as a client to the Ozone XMPP platform.
 * 
 * @author martin
 *
 */
public class OzoneClient {

	private final XmppConnection connection;
	private static final String DEFAULT_RESOURCE = "voxeo";

	private String lastCallId;

	/**
	 * Creates a new client object. This object will be used to interact with an Ozone server.
	 * 
	 * @param server Server that this client will be connecting to
	 */
	public OzoneClient(String server) {

		this(server, null);
	}

	/**
	 * Creates a new client object that will use the specified port number. 
	 * This object will be used to interact with an Ozone server.
	 * 
	 * @param server Server that this client will be connecting to
	 * @param port Port number that the server is listening at
	 */
	public OzoneClient(String server, Integer port) {

		connection = new SimpleXmppConnection(server, port);
	}
	
	/**
	 * Connects and authenticates into the Ozone Server. By default it will use the resource 'voxeo'.
	 * 
	 * @param username Ozone username
	 * @param password Ozone password
	 * 
	 * @throws XmppException If the client is not able to connect or authenticate into the Ozone Server
	 */
	public void connect(String username, String password) throws XmppException {
		
		connect(username, password, DEFAULT_RESOURCE);
	}

	/**
	 * Connects and authenticates into the Ozone Server. By default it will use the resource 'voxeo'.
	 * 
	 * @param username Ozone username
	 * @param password Ozone password
	 * @param resource Resource that will be used in this communication
	 * 
	 * @throws XmppException If the client is not able to connect or authenticate into the Ozone Server
	 */
	public void connect(String username, String password, String resource) throws XmppException {
		
		connection.connect();
		connection.login(username, password, resource);
		connection.addStanzaListener(new OzoneMessageListener("offer") {
			
			@Override
			public void messageReceived(Object object) {
				
				IQ iq = (IQ)object;
				lastCallId = iq.getFrom().substring(0, iq.getFrom().indexOf('@'));
			}
		});		
	}

	/**
	 * Adds a callback class to listen for events on all the incoming stanzas.
	 * 
	 * @param listener Stanza Callback.
	 */
	public void addStanzaListener(StanzaListener listener) {
		
		connection.addStanzaListener(listener);
	}
	
	/**
	 * Adds a callback class to listen for authentication events.
	 * 
	 * @param listener Callback.
	 */
	public void addAuthenticationListener(AuthenticationListener listener) {
		
		connection.addAuthenticationListener(listener);
	}
	
	/**
	 * Disconnects this client connection from the Ozone server
	 * 
	 */
	public void disconnect() throws XmppException {
		
		connection.disconnect();
	}
	
	/**
	 * <p>Waits for an Ozone message. This is a blocking call and therefore should be used carefully. 
	 * When invoked, the invoking thread will block until it receives the specified Ozone 
	 * message.</p>
	 * 
	 * @param ozoneMessage Ozone message that the invoking thread will be waiting for
	 *  
	 * @return Object The first Ozone messaging received that matches the specified message name
	 * 
	 * @throws XmppException If there is any problem waiting for the message
	 */
	public Object waitFor(String ozoneMessage) throws XmppException {
		
		Extension extension = (Extension)connection.waitForExtension(ozoneMessage);
		return extension.getObject();
	}
	
	/**
	 * <p>Waits for an Ozone message. This is a blocking call and therefore should be used carefully. 
	 * When invoked, the invoking thread will block until it receives the specified Ozone 
	 * message.</p>
	 * 
	 * @param ozoneMessage Ozone message that the invoking thread will be waiting for
	 * @param clazz Class to cast the returning object to
	 *  
	 * @return T The first Ozone messaging received that matches the specified message name
	 * 
	 * @throws XmppException If there is any problem waiting for the message
	 */
	@SuppressWarnings("unchecked")
	public <T> T waitFor(String ozoneMessage, Class<T> clazz) throws XmppException {
		
		Extension extension = (Extension)connection.waitForExtension(ozoneMessage);
		return (T)extension.getObject();
	}
	
	/**
	 * <p>Waits for an Ozone message. This is a blocking call but uses a timeout to specify 
	 * the amount of time that the connection will wait until the specified message is received.
	 * If no message is received during the specified timeout then a <code>null</code> object 
	 * will be returned.</p>
	 * 
	 * @param ozoneMessage Ozone message that the invoking thread will be waiting for
	 * @param timeout Timeout that will be used when waiting for an incoming Ozone message
	 *  
	 * @return Object The first Ozone messaging received that matches the specified message name 
	 * or <code>null</code> if no message is received during the specified timeout
	 * 
	 * @throws XmppException If there is any problem waiting for the message
	 */
	public Object waitFor(String extensionName, int timeout) throws XmppException {
		
		Extension extension = (Extension)connection.waitForExtension(extensionName, timeout);
		return extension.getObject();
	}
	
	/**
	 * <p>Waits for an Ozone message. This is a blocking call but uses a timeout to specify 
	 * the amount of time that the connection will wait until the specified message is received.
	 * If no message is received during the specified timeout then a <code>null</code> object 
	 * will be returned.</p>
	 * 
	 * @param ozoneMessage Ozone message that the invoking thread will be waiting for
	 * @param clazz Class to cast the returning object to
	 * @param timeout Timeout that will be used when waiting for an incoming Ozone message
	 *  
	 * @return Object The first Ozone messaging received that matches the specified message name 
	 * or <code>null</code> if no message is received during the specified timeout
	 * 
	 * @throws XmppException If there is any problem waiting for the message
	 */
	@SuppressWarnings("unchecked")
	public <T> T waitFor(String extensionName, Class<T> clazz, int timeout) throws XmppException {
		
		Extension extension = (Extension)connection.waitForExtension(extensionName, timeout);
		return (T)extension.getObject();
	}

	/**
	 * Answers the latest call that this connection has received from the Ozone server
	 * 
	 * @throws XmppException If there is any issue while answering the call
	 */
	public void answer() throws XmppException {
		
		if (lastCallId != null) {
			answer(lastCallId);
		}
	}
	
	/**
	 * Answers the call with the id specified as a parameter. 
	 * 
	 * @param callId Id of the call that will be answered
	 * 
	 * @throws XmppException If there is any issue while answering the call
	 */
	public void answer(String callId) throws XmppException {
		
		AnswerCommand answer = new AnswerCommand();
		IQ iq = new IQ(IQ.Type.set)
			.setFrom(buildFrom())
			.setTo(buildTo(callId))
			.setChild(Extension.create(answer));
		connection.send(iq);		
	}

	/**
	 * Accepts the latest call that this connection has received from the Ozone server
	 * 
	 * @throws XmppException If there is any issue while accepting the call
	 */
	public void accept() throws XmppException {
		
		if (lastCallId != null) {
			answer(lastCallId);
		}
	}
	
	/**
	 * Accepts the call with the id specified as a parameter. 
	 * 
	 * @param callId Id of the call that will be accepted
	 * 
	 * @throws XmppException If there is any issue while accepting the call
	 */
	public void accept(String callId) throws XmppException {
		
		AnswerCommand answer = new AnswerCommand();
		IQ iq = new IQ(IQ.Type.set)
			.setFrom(buildFrom())
			.setTo(buildTo(callId))
			.setChild(Extension.create(answer));
		connection.send(iq);		
	}	
	
	/**
	 * Sends a 'Say' command to Ozone that will play the specified audio file
	 * 
	 * @param audio URI to the audio file
	 * @return SayRef SayRef instance that allows to handle the say stream
	 * 
	 * @throws XmppException If there is any issues while sending the say command
	 * @throws URISyntaxException If the specified audio file is not a valid URI
	 */
	public SayRef sayAudio(String audio) throws XmppException, URISyntaxException {
	
		return say(new URI(audio));
	}
	
	/**
	 * Sends a 'Say' command to Ozone that will play the specified audio file
	 * 
	 * @param uri URI to an audio resource that will be played
	 * @return SayRef SayRef instance that allows to handle the say stream
	 * 
	 * @throws XmppException If there is any issues while sending the say command
	 */
	public SayRef say(URI uri) throws XmppException {
	
		if (lastCallId != null) {
			return say(uri, lastCallId);
		}
		return null;
	}	
	
	/**
	 * Sends a 'Say' command to Ozone that will play the specified audio file
	 * 
	 * @param uri URI to an audio resource that will be played
	 * @param callId Id of the call to which the say command will be sent 
	 * @return SayRef SayRef instance that allows to handle the say stream
	 * 
	 * @throws XmppException If there is any issues while sending the say command
	 */
	public SayRef say(URI uri, String callId) throws XmppException {

		return internalSay(new AudioItem(uri), callId);
	}

	
	/**
	 * Instructs Ozone to say the specified text
	 * 
	 * @param text Text that we want to say
	 * @return SayRef SayRef instance that allows to handle the say stream
	 * 
	 * @throws XmppException If there is any issues while sending the say command
	 */
	public SayRef say(String text) throws XmppException {
	
		if (lastCallId != null) {
			return say(text, lastCallId);
		}
		return null;
	}	
	
	/**
	 * Instructs Ozone to say the specified text on the call with the specified id
	 * 
	 * @param text Text that we want to say
	 * @param callId Id of the call to which the say command will be sent 
	 * @return SayRef SayRef instance that allows to handle the say stream
	 * 
	 * @throws XmppException If there is any issues while sending the say command
	 */
	public SayRef say(String text, String callId) throws XmppException {

		return internalSay(new SsmlItem("<speak>" + text + "</speak>"), callId);
	}

	/**
	 * Transfer the last received call to another phone speaking some text before doing the transfer.
	 * 
	 * @param text Text that will be prompted to the user
	 * @param to URI of the call destination
	 * 
	 * @throws XmppException If there is any issue while transfering the call
	 */
	public void transfer(String text, URI to) throws XmppException {

		if (lastCallId != null) {
			transfer(text, to, lastCallId);
		}
	}

	/**
	 * Transfers the last received call to another destination
	 * 
	 * @param to URI of the call destination
	 * 
	 * @throws XmppException If there is any issue while transfering the call
	 */
	public void transfer(URI to) throws XmppException {

		transfer(null, to);
	}

	/**
	 * Transfers a specific call to another destination
	 * 
	 * @param text Text that will be prompted to the user
	 * @param callId Id of the call we want to transfer
	 * 
	 * @throws XmppException If there is any issue while transfering the call
	 */
	public void transfer(URI to, String callId) throws XmppException {

		transfer(null, to, callId);
	}
	
	/**
	 * Transfers a call to another phone speaking some text before doing the transfer.
	 * 
	 * @param text Text that will be prompted to the user
	 * @param to URI of the call destination
	 * @param callId Id of the call that we want to transfer
	 * 
	 * @throws XmppException If there is any issue while transfering the call
	 */
	public void transfer(String text, URI to, String callId) throws XmppException {

		Transfer transfer = new Transfer();
		transfer.setTimeout(new Duration(20000));
		transfer.setTerminator('#');
		PromptItems items = new PromptItems();
		if (text != null) {
			SsmlItem ssml = new SsmlItem("<speak>" + text + "</speak>");
			items.add(ssml);
		}
		transfer.setPromptItems(items);
		List<URI> uriList = new ArrayList<URI>();
		uriList.add(to);
		transfer.setTo(uriList);
		
		IQ iq = new IQ(IQ.Type.set)
			.setFrom(buildFrom())
			.setTo(buildTo(callId))
			.setChild(Extension.create(transfer));
		connection.send(iq);
	}

	/**
	 * Calls a specific destination
	 * 
	 * @param text URI to call in text format
	 * 
	 * @throws XmppException If there is any issue while dialing
	 * @throws URISyntaxException If the specified text is not a valid URI
	 */
	public void dial(String text) throws XmppException, URISyntaxException {

		dial(new URI(text));
	}
	
	/**
	 * Calls a specific destination
	 * 
	 * @param to URI to dial
	 * 
	 * @throws XmppException If there is any issue while dialing
	 */
	public void dial(URI to) throws XmppException {

		if (lastCallId != null) {
			dial(to, lastCallId);
		}
	}

	
	/**
	 * Calls a specific destination from the specified call id
	 * 
	 * @param callId Id of the call from we want to dial
	 * 
	 * @throws XmppException If there is any issue while transfering the call
	 */
	public void dial(URI to, String callId) throws XmppException {

		DialCommand dial = new DialCommand();
		dial.setTo(to);
		try {
			dial.setFrom(new URI("sip:userc@127.0.0.1:5060"));
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		IQ iq = new IQ(IQ.Type.set)
			.setFrom(buildFrom())
			.setTo(buildTo(callId))
			.setChild(Extension.create(dial));
		connection.send(iq);
	}

	/**
	 * Instructs Ozone to ask a question with the specified choices on the latest active call  
	 * 
	 * @param text Text that will be prompted
	 * @param choicesText Choices
	 * 
	 * @throws XmppException If there is any issue while asking the question 
	 */
	public void ask(String text, String choicesText) throws XmppException {

		if (lastCallId != null) {
			ask(text,choicesText,lastCallId);
		}
	}	
	
	/**
	 * Instructs Ozone to ask a question with the specified choices on the call with the given id  
	 * 
	 * @param text Text that will be prompted
	 * @param choicesText Choices
	 * @param callId Id of the call in which the question will be asked
	 * 
	 * @throws XmppException If there is any issue while asking the question 
	 */
	public void ask(String text, String choicesText, String callId) throws XmppException {
		
		Ask ask = new Ask();
		PromptItems items = new PromptItems();
		SsmlItem ssml = new SsmlItem("<speak>" + text + "</speak>");
		items.add(ssml);
		ask.setPromptItems(items);
		ChoicesList choicesList = new ChoicesList();
		Choices choices = new Choices();
		choices.setContent(choicesText);
		choices.setContentType("application/grammar+voxeo");
		choicesList.add(choices);
		ask.setChoices(choicesList);
		ask.setTerminator('#');
		ask.setTimeout(new Duration(15000));
		
		IQ iq = new IQ(IQ.Type.set)
			.setFrom(buildFrom())
			.setTo(buildTo(callId))
			.setChild(Extension.create(ask));
		connection.send(iq);
	}
	
	/**
	 * Creates a conference and joins the last active call 
	 * 
	 * @param conferenceId Id of the conference
	 * 
	 * @throws XmppException If there is any problem while creating the conference
	 */
	public void conference(String conferenceId) throws XmppException {
	
		if (lastCallId != null) {
			conference(conferenceId, lastCallId);
		}
	}
	
	/**
	 * Creates a conference and joins the last active call 
	 * 
	 * @param conferenceId Id of the conference
	 * @param callId Id of the call that will be starting the conference
	 * 
	 * @throws XmppException If there is any problem while creating the conference
	 */
	public void conference(String conferenceId, String callId) throws XmppException {
		
		Conference conference = new Conference();
		conference.setVerbId(conferenceId);
		conference.setTerminator('#');
		
		IQ iq = new IQ(IQ.Type.set)
			.setFrom(buildFrom())
			.setTo(buildTo(callId))
			.setChild(Extension.create(conference));
		connection.send(iq);
		
	}
	
	private SayRef internalSay(PromptItem item, String callId) throws XmppException {
		
		Say say = new Say();
		PromptItems items = new PromptItems();
		items.add(item);
		say.setPromptItems(items);
		
		final SayRef ref = new SayRef(this, say);
		
		IQ iq = new IQ(IQ.Type.set)
			.setFrom(buildFrom())
			.setTo(buildTo(callId))
			.setChild(Extension.create(say));
		IQ result = ((IQ)connection.sendAndWait(iq));
		if (result != null) {
			RefEvent reference = (RefEvent)result.getExtension().getObject();
			ref.setId(reference.getJid());
			return ref;
		} else {
			return null;
		}
	}
	
	/**
	 * Pauses a say command 
	 * 
	 * @param say Say command that we want to pause
	 */
	public void pause(SayRef say) throws XmppException {
		
		PauseCommand pause = new PauseCommand();
		IQ iq = new IQ(IQ.Type.set)
			.setFrom(buildFrom())
			.setTo(say.getId())
			.setChild(Extension.create(pause));
		connection.send(iq);
	}
	
	/**
	 * Resumes a say command 
	 * 
	 * @param say Say command that we want to resume
	 */
	public void resume(SayRef say) throws XmppException {
		
		ResumeCommand resume = new ResumeCommand();
		IQ iq = new IQ(IQ.Type.set)
			.setFrom(buildFrom())
			.setTo(say.getId())
			.setChild(Extension.create(resume));
		connection.send(iq);
	}
	
	
	/**
	 * Stops a say command
	 * 
	 * @param say Say command that we want to stop
	 */
	public void stop(SayRef say) throws XmppException {
		
		StopCommand stop = new StopCommand();
		IQ iq = new IQ(IQ.Type.set)
			.setFrom(buildFrom())
			.setTo(say.getId())
			.setChild(Extension.create(stop));
		connection.send(iq);
	}
	
	/**
	 * Hangs up the latest active call
	 */
	public void hangup() throws XmppException {
		
		if (lastCallId != null) {
			hangup(lastCallId);
		}
	}
	
	/**
	 * Hangs up the specified call id
	 * 
	 * @param callId Id of the call to be hung up
	 */
	public void hangup(String callId) throws XmppException {
		
		HangupCommand hangup = new HangupCommand(null);
		
		IQ iq = new IQ(IQ.Type.set)
			.setFrom(buildFrom())
			.setTo(buildTo(callId))
			.setChild(Extension.create(hangup));
		connection.send(iq);
	}
	
	public String getLastCallId() {
		
		return lastCallId;
	}
	
	private String buildFrom() {
		
		return connection.getUsername() + "@" + connection.getServiceName() + "/" + connection.getResource();
	}
	
	private String buildTo(String callid) {
		
		return callid + "@" + connection.getServiceName();
	}
}
