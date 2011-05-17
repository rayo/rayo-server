# Tropo2 Java Client Library

This client library lets you build Ozone applications in Java. It is a very lightweight library that lets you create Ozone applications with very few lines of code. 

## Samples

Feel free to check out the samples available [here](https://github.com/tropo/tropo2/tree/master/ozone-java-client/src/main/java/samples). You can run any of these samples from the command line with Maven. Here is an example:

	mvn exec:java -Dexec.mainClass="samples.TextSaySample" -Dexec.classpathScope=compile -Ddetail=true

All the samples try to connect with and username "userc" and a password "1" to a Prism instance in localhost:5222. You can change the source code if you need it. All the samples will wait for an incoming offer, so after running the command line from above you should open your favorite sip phone and call your Prism instance. After doing this the rest of the sample will process the incoming offer and execute the code.   
 
## Using the Ozone client library 

The easiest way to use the Ozone's Java Client library is to create an instance of the [OzoneClient](https://github.com/tropo/tropo2/blob/master/ozone-java-client/src/main/java/com/voxeo/ozone/client/OzoneClient.java) class. This class lets you interact with the Ozone Server through a set of very useful and simple methods. Here is an example:

	OzoneClient ozone = new OzoneClient("localhost"); // Prepares the client instance
	ozone.connect("userc","1","voxeo"); // connects and logs in
	ozone.waitFor("offer"); // Waits for an incoming call
	ozone.answer(); // Answers the call
	ozone.say("Hello"); // Says hello

	// You can also play audio
	SayRef say = ozone.say("http://somemp3.mp3");
	say.pause(); // and pause it
	say.resume(); // and resume it

	// And finally disconnect
	ozone.disconnect();

All the samples available in the [samples source folder](https://github.com/tropo/tropo2/tree/master/ozone-java-client/src/main/java/samples) use the [OzoneClient](https://github.com/tropo/tropo2/blob/master/ozone-java-client/src/main/java/com/voxeo/ozone/client/OzoneClient.java) class. This is for example part of the source code of the [Conference Sample](https://github.com/tropo/tropo2/tree/master/ozone-java-client/src/main/java/samples):

	public void run() throws Exception {
		
		client.waitFor("offer");
		client.answer();
		client.conference("123456");
		Thread.sleep(60000);
		client.hangup();
	}


Feel free to look at the samples source code and the OzoneClient class as there is tons of available methods. Right now the following verbs are implemented:

	* Say
	* Ask
	* Conference
	* Transfer
	* Dial

## Using the low level access libraries

OzoneClient is an abstraction class that lets you to quickly create Ozone applications. However, when you need more control over your interaction with an Ozone server it's much better to use some implementation of [XmppConnection](https://github.com/tropo/tropo2/blob/master/ozone-java-client/src/main/java/com/voxeo/ozone/client/SimpleXmppConnection.java). 

XmppConnection instances let you can manage the connection with the Ozone server. You can listen and react to all type of events and stanzas. You can manage the authentication process. You can create your own IQ stanzas and send it to the server. You can send stanzas synchronously or asynchronously or register callbacks for your messages. 

There is lots of stuff. This documentation will be improved in the future but right now we recommend you to start looking at the [tons of different examples available as unit tests](https://github.com/tropo/tropo2/tree/master/ozone-java-client/src/test/java/com/voxeo/ozone/client/test) that show how the API can be used. 

