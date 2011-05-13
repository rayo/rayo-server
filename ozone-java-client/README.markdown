# Tropo2 Java Client Library

This client library lets you build Ozone applications in Java. It is a very lightweight library that lets you create Ozone application with very few lines of code. 

## Samples

Feel free to check out the samples available [here](https://github.com/tropo/tropo2/tree/master/ozone-java-client/src/main/java/samples). You can run any of these samples from the command line with Maven. Here is an example:

mvn exec:java -Dexec.mainClass="com.vineetmanohar.module.Main" 
 
All the samples connect with username "userc" and password "1" to a Prism instance in localhost:5222. You can change the source code if you need it. All the samples will wait for an incoming offer, so after running the command line from above you should open your favorite sip phone and call your Prism instance.   
 
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

Feel free to look at the class source code as there is tons of available methods. Right now the following verbs are implemented:

	* Say
	* Ask
	* Conference
	* Transfer
	* Dial

## Using the low level access libraries

OzoneClient is an abstraction that lets you quickly create Ozone applications. However, when you need more control over your interaction with an Ozone server it's much better to use some implementation of [XmppConnection](https://github.com/tropo/tropo2/blob/master/ozone-java-client/src/main/java/com/voxeo/ozone/client/SimpleXmppConnection.java). 

XmppConnection instances let you manage your connection with the Ozone server. You can listen and react to all type of events. You manage connection and the authentication process. You can create your own IQ stanzas and send it to the server. You can send stanzas synchronously or asynchronously or register callbacks for your messages. 

There is lots of stuff. This documentation will be improved in the future but right now we recommend you to start looking at the [tons of different tests](https://github.com/tropo/tropo2/tree/master/ozone-java-client/src/test/java/com/voxeo/ozone/client/test) which contain many examples on how to use the API. 

