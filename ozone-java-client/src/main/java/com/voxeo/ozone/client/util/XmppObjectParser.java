package com.voxeo.ozone.client.util;

import java.beans.PropertyDescriptor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.dom4j.DocumentFactory;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.QName;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.voxeo.ozone.client.UnknownXmppObjectException;
import com.voxeo.servlet.xmpp.ozone.Namespaces;
import com.voxeo.servlet.xmpp.ozone.extensions.Extension;
import com.voxeo.servlet.xmpp.ozone.extensions.ExtensionsManager;
import com.voxeo.servlet.xmpp.ozone.stanza.Bind;
import com.voxeo.servlet.xmpp.ozone.stanza.IQ;
import com.voxeo.servlet.xmpp.ozone.stanza.Message;
import com.voxeo.servlet.xmpp.ozone.stanza.Presence;
import com.voxeo.servlet.xmpp.ozone.stanza.Session;
import com.voxeo.servlet.xmpp.ozone.stanza.Error;

/**
 * Utility class that helps to parse packets. Any parsing packets method that must be shared
 * between many clients must be placed in this utility class.
 * 
 * @author Gaston Dombiak is the original author. This class comes originally from the Smack project.
 * @author Martin Perez 
 */
public class XmppObjectParser {

	static DocumentFactory df = DocumentFactory.getInstance();
	
    /**
     * Namespace used to store packet properties.
     */
    private static final String PROPERTIES_NAMESPACE =
            "http://www.jivesoftware.com/xmlns/xmpp/properties";

    /**
     * Parses a message packet.
     *
     * @param parser the XML parser, positioned at the start of a message packet.
     * @return a Message packet.
     * @throws Exception if an exception occurs while parsing the packet.
     */
    public static Message parseMessage(XmlPullParser parser) throws Exception {
    	
        Message message = new Message();
        message.setId(parser.getAttributeValue("", "id"));
        message.setTo(parser.getAttributeValue("", "to"));
        message.setFrom(parser.getAttributeValue("", "from"));
        message.setType(Message.Type.valueOf(parser.getAttributeValue("", "type")));
        message.setXmlLang(getLanguageAttribute(parser));

        // Parse sub-elements. We include extra logic to make sure the values
        // are only read once. This is because it's possible for the names to appear
        // in arbitrary sub-elements.
        boolean done = false;
        String thread = null;
        Map<String, Object> properties = null;
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                String elementName = parser.getName();
                String namespace = parser.getNamespace();
                if (elementName.equals("subject")) {
                    String subject = parseContent(parser);
                    message.setSubject(subject);

                } else if (elementName.equals("body")) {
                    String xmlLang = getLanguageAttribute(parser);
                    String body = parseContent(parser);
                    message.setBody(body);
                } else if (elementName.equals("thread")) {
                    if (thread == null) {
                        thread = parser.nextText();                        
                    }
                } else if (elementName.equals("error")) {
                    message.setError(parseError(parser));
                } else if (elementName.equals("properties") &&
                        	namespace.equals(PROPERTIES_NAMESPACE)) {
                    properties = parseProperties(parser);
                } else {
                    // Otherwise, it must be a packet extension.
                    //message.addExtension(PacketParserUtils.parsePacketExtension(elementName, namespace, parser));
                }
            }
            else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("message")) {
                    done = true;
                }
            }
        }

        message.setThread(thread);
        if (properties != null) {
            for (String name : properties.keySet()) {
            	//TODO: Properties handling
                //message.setProperty(name, properties.get(name));
            }
        }
        return message;
    }

    /**
     * Returns the content of a tag as string regardless of any tags included.
     * 
     * @param parser the XML pull parser
     * @return the content of a tag as string
     * @throws XmlPullParserException if parser encounters invalid XML
     * @throws IOException if an IO error occurs
     */
    private static String parseContent(XmlPullParser parser)
                    throws XmlPullParserException, IOException {
        String content = "";
        int parserDepth = parser.getDepth();
        while (!(parser.next() == XmlPullParser.END_TAG && parser
                        .getDepth() == parserDepth)) {
            content += parser.getText();
        }
        return content;
    }

    /**
     * Parses a presence packet.
     *
     * @param parser the XML parser, positioned at the start of a presence packet.
     * @return a Presence packet.
     * @throws Exception if an exception occurs while parsing the packet.
     */
    public static Presence parsePresence(XmlPullParser parser) throws Exception {
    	
        Presence.Type type = null; // Null is available
        String typeString = parser.getAttributeValue("", "type");
        if (typeString != null && !typeString.equals("")) {
            try {
                type = Presence.Type.valueOf(typeString);
            }
            catch (IllegalArgumentException iae) {
                System.err.println("Found invalid presence type " + typeString);
            }
        }
        Presence presence = new Presence().setType(type);
        presence.setTo(parser.getAttributeValue("", "to"));
        presence.setFrom(parser.getAttributeValue("", "from"));
        presence.setId(parser.getAttributeValue("", "id"));

        String language = getLanguageAttribute(parser);
        if (language != null && !"".equals(language.trim())) {
        	presence.setXmlLang(language);
        }


        // Parse sub-elements
        boolean done = false;
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                String elementName = parser.getName();
                String namespace = parser.getNamespace();
                if (elementName.equals("status")) {
                    presence.setStatus(parser.nextText());
                }
                else if (elementName.equals("priority")) {
                    try {
                        int priority = Integer.parseInt(parser.nextText());
                        presence.setPriority(priority);
                    }
                    catch (NumberFormatException nfe) {
                        // Ignore.
                    }
                    catch (IllegalArgumentException iae) {
                        // Presence priority is out of range so assume priority to be zero
                        presence.setPriority(0);
                    }
                }
                else if (elementName.equals("show")) {
                    String modeText = parser.nextText();
                    try {
                        presence.setShow(Presence.Show.valueOf(modeText));
                    }
                    catch (IllegalArgumentException iae) {
                        System.err.println("Found invalid presence mode " + modeText);
                    }
                }
                else if (elementName.equals("error")) {
                    presence.setError(parseError(parser));
                }
                else if (elementName.equals("properties") &&
                        namespace.equals(PROPERTIES_NAMESPACE))
                {
                	/*
                    Map<String,Object> properties = parseProperties(parser);
                    // Set packet properties.
                    for (String name : properties.keySet()) {
                        presence.setProperty(name, properties.get(name));
                    }
                    */
                }
                // Otherwise, it must be a packet extension.
                else {
                    //presence.addExtension(PacketParserUtils.parsePacketExtension(elementName, namespace, parser));
                }
            }
            else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("presence")) {
                    done = true;
                }
            }
        }
        return presence;
    }

    /**
     * Parses an IQ packet.
     *
     * @param parser the XML parser, positioned at the start of an IQ packet.
     * @return an IQ object.
     * @throws Exception if an exception occurs while parsing the packet.
     */
    public static IQ parseIQ(XmlPullParser parser) throws Exception {
    	
        IQ iqPacket = new IQ();

        iqPacket.setId(parser.getAttributeValue("", "id"));
        iqPacket.setTo(parser.getAttributeValue("", "to"));
        iqPacket.setFrom(parser.getAttributeValue("", "from"));
        iqPacket.setType(IQ.Type.valueOf(parser.getAttributeValue("", "type")));
        Error error = null;

        boolean done = false;
        while (!done) {
            int eventType = parser.next();

            if (eventType == XmlPullParser.START_TAG) {
                String elementName = parser.getName();
                String namespace = parser.getNamespace();
                if (elementName.equals("error")) {
                    error = XmppObjectParser.parseError(parser);
                } else if (elementName.equals("query") && namespace.equals(Namespaces.AUTH)) {
                    //iqPacket = parseAuthentication(parser);
                } else if (elementName.equals("query") && namespace.equals(Namespaces.ROSTER)) {
                    //iqPacket = parseRoster(parser);
                } else if (elementName.equals("query") && namespace.equals(Namespaces.REGISTER)) {
                    //iqPacket = parseRegistration(parser);
                } else if (elementName.equals("bind") && namespace.equals(Namespaces.BIND)) {
                    iqPacket.setChild(parseResourceBinding(parser));
                } else if (elementName.equals("session") && namespace.equals(Namespaces.SESSION)) {
                    iqPacket.setChild(parseSession(parser));
                }                
                // Otherwise, see if there is a registered provider for
                // this element name and namespace.
                else {
                	Element root = buildAndParseElement(parser,null);
                	Element parent = root;
                	boolean extensionDone = false;
                    while (!extensionDone) {
                        int extensionType = parser.next();
                        if (extensionType == XmlPullParser.START_TAG) {
                        	
                        	Element newElement = buildAndParseElement(parser,parent);
                        	parent = newElement;
                        } else if (extensionType == XmlPullParser.TEXT) {
                            parent.addText(parser.getText());
                        } else if (extensionType == XmlPullParser.END_TAG) {
                            if (parent == root) {
                            	extensionDone = true;
                            } else {
                            	parent = parent.getParent();
                            }
                        }
                    }       	
                	Extension extension = new Extension(root);
                	iqPacket.setChild(extension);
                }
            }
            else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("iq")) {
                    done = true;
                }
            }
        }
        // Decide what to do when an IQ packet was not understood
        if (iqPacket == null) {
        	throw new UnknownXmppObjectException();
        	/*
            if (IQ.Type.get == type || IQ.Type.set == type ) {
                // If the IQ stanza is of type "get" or "set" containing a child element
                // qualified by a namespace it does not understand, then answer an IQ of
                // type "error" with code 501 ("feature-not-implemented")
                iqPacket = new IQ() {
                    public String getChildElementXML() {
                        return null;
                    }
                };
                iqPacket.setId(id);
                iqPacket.setTo(from);
                iqPacket.setFrom(to);
                iqPacket.setType(IQ.Type.error);
                iqPacket.setError(new Error(Error.Condition.feature_not_implemented));
                connection.sendPacket(iqPacket);
                return null;
            }
            else {
                // If an IQ packet wasn't created above, create an empty IQ packet.
                iqPacket = new IQ() {
                    public String getChildElementXML() {
                        return null;
                    }
                };
            }
            */
        }

        if (error != null) {
        	iqPacket.setError(error);
        }

        return iqPacket;
    }

	private static Element buildAndParseElement(XmlPullParser parser, Element parent) throws XmlPullParserException {
		
		QName qname = (parser.getPrefix() == null) ? df.createQName(parser.getName(), parser.getNamespace()) : 
										df.createQName(parser.getName(), parser.getPrefix(), parser.getNamespace());                        	
		Element newElement = df.createElement(qname);
		int nsStart = parser.getNamespaceCount(parser.getDepth() - 1);
		int nsEnd = parser.getNamespaceCount(parser.getDepth());
		for (int i = nsStart; i < nsEnd; i++)
		if (parser.getNamespacePrefix(i) != null) {
			newElement.addNamespace(parser.getNamespacePrefix(i), parser.getNamespaceUri(i));
		}
		for (int i = 0; i < parser.getAttributeCount(); i++) {
			QName qa = (parser.getAttributePrefix(i) == null) ? df.createQName(parser.getAttributeName(i)) : 
				df.createQName(parser.getAttributeName(i), parser.getAttributePrefix(i), parser.getAttributeNamespace(i));
			newElement.addAttribute(qa, parser.getAttributeValue(i));
		}
		
		if (parent != null) {
			parent.add(newElement);
		}
		
		return newElement;
	}
/*
    private static Authentication parseAuthentication(XmlPullParser parser) throws Exception {
        Authentication authentication = new Authentication();
        boolean done = false;
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals("username")) {
                    authentication.setUsername(parser.nextText());
                }
                else if (parser.getName().equals("password")) {
                    authentication.setPassword(parser.nextText());
                }
                else if (parser.getName().equals("digest")) {
                    authentication.setDigest(parser.nextText());
                }
                else if (parser.getName().equals("resource")) {
                    authentication.setResource(parser.nextText());
                }
            }
            else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("query")) {
                    done = true;
                }
            }
        }
        return authentication;
    }

    private static RosterPacket parseRoster(XmlPullParser parser) throws Exception {
        RosterPacket roster = new RosterPacket();
        boolean done = false;
        RosterPacket.Item item = null;
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals("item")) {
                    String jid = parser.getAttributeValue("", "jid");
                    String name = parser.getAttributeValue("", "name");
                    // Create packet.
                    item = new RosterPacket.Item(jid, name);
                    // Set status.
                    String ask = parser.getAttributeValue("", "ask");
                    RosterPacket.ItemStatus status = RosterPacket.ItemStatus.fromString(ask);
                    item.setItemStatus(status);
                    // Set type.
                    String subscription = parser.getAttributeValue("", "subscription");
                    RosterPacket.ItemType type = RosterPacket.ItemType.valueOf(subscription != null ? subscription : "none");
                    item.setItemType(type);
                }
                if (parser.getName().equals("group") && item!= null) {
                    final String groupName = parser.nextText();
                    if (groupName != null && groupName.trim().length() > 0) {
                        item.addGroupName(groupName);
                    }
                }
            }
            else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("item")) {
                    roster.addRosterItem(item);
                }
                if (parser.getName().equals("query")) {
                    done = true;
                }
            }
        }
        return roster;
    }

     private static Registration parseRegistration(XmlPullParser parser) throws Exception {
        Registration registration = new Registration();
        Map<String, String> fields = null;
        boolean done = false;
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                // Any element that's in the jabber:iq:register namespace,
                // attempt to parse it if it's in the form <name>value</name>.
                if (parser.getNamespace().equals("jabber:iq:register")) {
                    String name = parser.getName();
                    String value = "";
                    if (fields == null) {
                        fields = new HashMap<String, String>();
                    }

                    if (parser.next() == XmlPullParser.TEXT) {
                        value = parser.getText();
                    }
                    // Ignore instructions, but anything else should be added to the map.
                    if (!name.equals("instructions")) {
                        fields.put(name, value);
                    }
                    else {
                        registration.setInstructions(value);
                    }
                }
                // Otherwise, it must be a packet extension.
                else {
                    registration.addExtension(
                        PacketParserUtils.parsePacketExtension(
                            parser.getName(),
                            parser.getNamespace(),
                            parser));
                }
            }
            else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("query")) {
                    done = true;
                }
            }
        }
        registration.setAttributes(fields);
        return registration;
    }
*/
    private static Bind parseResourceBinding(XmlPullParser parser) throws IOException, XmlPullParserException {
    	
        Bind bind = new Bind();
        boolean done = false;
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals("resource")) {
                    bind.setResource(parser.nextText());
                }
                else if (parser.getName().equals("jid")) {
                    bind.setJID(parser.nextText());
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("bind")) {
                    done = true;
                }
            }
        }

        return bind;
    }

    private static Session parseSession(XmlPullParser parser) throws IOException, XmlPullParserException {
    	
        Session session = new Session();
        boolean done = false;
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
            } else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("session")) {
                    done = true;
                }
            }
        }

        return session;
    }
    /**
     * Parse the available SASL mechanisms reported from the server.
     *
     * @param parser the XML parser, positioned at the start of the mechanisms stanza.
     * @return a collection of Stings with the mechanisms included in the mechanisms stanza.
     * @throws Exception if an exception occurs while parsing the stanza.
     */
    public static Collection<String> parseMechanisms(XmlPullParser parser) throws Exception {
        List<String> mechanisms = new ArrayList<String>();
        boolean done = false;
        while (!done) {
            int eventType = parser.next();

            if (eventType == XmlPullParser.START_TAG) {
                String elementName = parser.getName();
                if (elementName.equals("mechanism")) {
                    mechanisms.add(parser.nextText());
                }
            }
            else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("mechanisms")) {
                    done = true;
                }
            }
        }
        return mechanisms;
    }

    /**
     * Parse the available compression methods reported from the server.
     *
     * @param parser the XML parser, positioned at the start of the compression stanza.
     * @return a collection of Stings with the methods included in the compression stanza.
     * @throws Exception if an exception occurs while parsing the stanza.
     */
    public static Collection<String> parseCompressionMethods(XmlPullParser parser)
            throws IOException, XmlPullParserException {
        List<String> methods = new ArrayList<String>();
        boolean done = false;
        while (!done) {
            int eventType = parser.next();

            if (eventType == XmlPullParser.START_TAG) {
                String elementName = parser.getName();
                if (elementName.equals("method")) {
                    methods.add(parser.nextText());
                }
            }
            else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("compression")) {
                    done = true;
                }
            }
        }
        return methods;
    }

    /**
     * Parse a properties sub-packet. If any errors occur while de-serializing Java object
     * properties, an exception will be printed and not thrown since a thrown
     * exception will shut down the entire connection. ClassCastExceptions will occur
     * when both the sender and receiver of the packet don't have identical versions
     * of the same class.
     *
     * @param parser the XML parser, positioned at the start of a properties sub-packet.
     * @return a map of the properties.
     * @throws Exception if an error occurs while parsing the properties.
     */
    public static Map<String, Object> parseProperties(XmlPullParser parser) throws Exception {
        Map<String, Object> properties = new HashMap<String, Object>();
        while (true) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG && parser.getName().equals("property")) {
                // Parse a property
                boolean done = false;
                String name = null;
                String type = null;
                String valueText = null;
                Object value = null;
                while (!done) {
                    eventType = parser.next();
                    if (eventType == XmlPullParser.START_TAG) {
                        String elementName = parser.getName();
                        if (elementName.equals("name")) {
                            name = parser.nextText();
                        }
                        else if (elementName.equals("value")) {
                            type = parser.getAttributeValue("", "type");
                            valueText = parser.nextText();
                        }
                    }
                    else if (eventType == XmlPullParser.END_TAG) {
                        if (parser.getName().equals("property")) {
                            if ("integer".equals(type)) {
                                value = Integer.valueOf(valueText);
                            }
                            else if ("long".equals(type))  {
                                value = Long.valueOf(valueText);
                            }
                            else if ("float".equals(type)) {
                                value = Float.valueOf(valueText);
                            }
                            else if ("double".equals(type)) {
                                value = Double.valueOf(valueText);
                            }
                            else if ("boolean".equals(type)) {
                                value = Boolean.valueOf(valueText);
                            }
                            else if ("string".equals(type)) {
                                value = valueText;
                            }
                            else if ("java-object".equals(type)) {
                                try {
                                    byte [] bytes = Base64.decodeBase64(valueText);
                                    ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes));
                                    value = in.readObject();
                                }
                                catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            if (name != null && value != null) {
                                properties.put(name, value);
                            }
                            done = true;
                        }
                    }
                }
            }
            else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("properties")) {
                    break;
                }
            }
        }
        return properties;
    }

    /**
     * Parses SASL authentication error packets.
     * 
     * @param parser the XML parser.
     * @return a SASL Failure packet.
     * @throws Exception if an exception occurs while parsing the packet.
     */
    /*
    public static Failure parseSASLFailure(XmlPullParser parser) throws Exception {
        String condition = null;
        boolean done = false;
        while (!done) {
            int eventType = parser.next();

            if (eventType == XmlPullParser.START_TAG) {
                if (!parser.getName().equals("failure")) {
                    condition = parser.getName();
                }
            }
            else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("failure")) {
                    done = true;
                }
            }
        }
        return new Failure(condition);
    }
*/
    /**
     * Parses stream error packets.
     *
     * @param parser the XML parser.
     * @return an stream error packet.
     * @throws Exception if an exception occurs while parsing the packet.
     */
    /*
    public static StreamError parseStreamError(XmlPullParser parser) throws IOException,
            XmlPullParserException {
    StreamError streamError = null;
    boolean done = false;
    while (!done) {
        int eventType = parser.next();

        if (eventType == XmlPullParser.START_TAG) {
            streamError = new StreamError(parser.getName());
        }
        else if (eventType == XmlPullParser.END_TAG) {
            if (parser.getName().equals("error")) {
                done = true;
            }
        }
    }
    return streamError;
}
*/
    /**
     * Parses error sub-packets.
     *
     * @param parser the XML parser.
     * @return an error sub-packet.
     * @throws Exception if an exception occurs while parsing the packet.
     */
    public static Error parseError(XmlPullParser parser) throws Exception {
    	
        final String errorNamespace = "urn:ietf:params:xml:ns:xmpp-stanzas";
        
        // Prism is also sending errors in this namespace
        final String errorStreamNamespace = "urn:ietf:params:xml:ns:xmpp-streams";
        
    	String errorCode = "-1";
        String type = null;
        String message = null;
        Error.Condition condition = null;
        //List<PacketExtension> extensions = new ArrayList<PacketExtension>();

        // Parse the error header
        for (int i=0; i<parser.getAttributeCount(); i++) {
            if (parser.getAttributeName(i).equals("code")) {
                errorCode = parser.getAttributeValue("", "code");
            }
            if (parser.getAttributeName(i).equals("type")) {
            	type = parser.getAttributeValue("", "type");
            }
        }
        boolean done = false;
        // Parse the text and condition tags
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals("text")) {
                    message = parser.nextText();
                }
                else {
                	// Condition tag, it can be xmpp error or an application defined error.
                    String elementName = parser.getName();
                    String namespace = parser.getNamespace();
                    if (errorNamespace.equals(namespace) || errorStreamNamespace.equals(namespace)) {
                    	condition = Error.Condition.fromXMPP(elementName);
                    }
                    else {
                    	//extensions.add(parsePacketExtension(elementName, namespace, parser));
                    }
                }
            }
                else if (eventType == XmlPullParser.END_TAG) {
                    if (parser.getName().equals("error")) {
                        done = true;
                    }
                }
        }

        Error.Type errorType = Error.Type.cancel;
        try {
            if (type != null) {
                errorType = Error.Type.fromXMPP(type.toUpperCase());
            }
        } catch (IllegalArgumentException iae) {
            iae.printStackTrace();
        }
        return new Error(condition,errorType,message);
        //return new XMPPError(Integer.parseInt(errorCode), errorType, condition, message, extensions);
    }


    private static String getLanguageAttribute(XmlPullParser parser) {
    	for (int i = 0; i < parser.getAttributeCount(); i++) {
            String attributeName = parser.getAttributeName(i);
            if ( "xml:lang".equals(attributeName) ||
                    ("lang".equals(attributeName) &&
                            "xml".equals(parser.getAttributePrefix(i)))) {
    			return parser.getAttributeValue(i);
    		}
    	}
    	return null;
    }

    public static Object parseWithIntrospection(String elementName,
            Class objectClass, XmlPullParser parser) throws Exception
    {
        boolean done = false;
        Object object = objectClass.newInstance();
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                String name = parser.getName();
                String stringValue = parser.nextText();
                PropertyDescriptor descriptor = new PropertyDescriptor(name, objectClass);
                // Load the class type of the property.
                Class propertyType = descriptor.getPropertyType();
                // Get the value of the property by converting it from a
                // String to the correct object type.
                Object value = decode(propertyType, stringValue);
                // Set the value of the bean.
                descriptor.getWriteMethod().invoke(object, value);
            }
            else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals(elementName)) {
                    done = true;
                }
            }
        }
        return object;
    }

    /**
     * Decodes a String into an object of the specified type. If the object
     * type is not supported, null will be returned.
     *
     * @param type the type of the property.
     * @param value the encode String value to decode.
     * @return the String value decoded into the specified type.
     * @throws Exception If decoding failed due to an error.
     */
    private static Object decode(Class type, String value) throws Exception {
        if (type.getName().equals("java.lang.String")) {
            return value;
        }
        if (type.getName().equals("boolean")) {
            return Boolean.valueOf(value);
        }
        if (type.getName().equals("int")) {
            return Integer.valueOf(value);
        }
        if (type.getName().equals("long")) {
            return Long.valueOf(value);
        }
        if (type.getName().equals("float")) {
            return Float.valueOf(value);
        }
        if (type.getName().equals("double")) {
            return Double.valueOf(value);
        }
        if (type.getName().equals("java.lang.Class")) {
            return Class.forName(value);
        }
        return null;
    }
}
