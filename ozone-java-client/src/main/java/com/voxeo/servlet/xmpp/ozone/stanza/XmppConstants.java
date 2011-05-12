package com.voxeo.servlet.xmpp.ozone.stanza;

public class XmppConstants {

  public static final String XMPP_VERSION_STRING = "XMPP/1.0";

  public static final String XMPP_Content_Encoding = "UTF-8";

  public static final String Client_Namespace = "jabber:client";

  public static final String Server_Namespace = "jabber:server";

  public static final String Stream_Namespace = "http://etherx.jabber.org/streams";

  public static final String Stanza_Namespace = "urn:ietf:params:xml:ns:xmpp-stanzas";

  public static final String SASL_Namespace = "urn:ietf:params:xml:ns:xmpp-sasl";

  public static final String TLS_Namespace = "urn:ietf:params:xml:ns:xmpp-tls";

  public static final String Dialback_Namespace = "jabber:server:dialback";

  public static final String ResourceBinding_Namespace = "urn:ietf:params:xml:ns:xmpp-bind";
  
  public static final String Dialback_Feature_Namespace = "urn:xmpp:features:dialback";

  public static final String SESSION_AuthInfo_Attribute_Name = "SESSION_AuthInfo_Attribute_Name";

  public static final String SESSION_SASL_Attribute_Name = "SESSION_SASL_Attribute_Name";

  public static final String XMPP_VERSION = "1.0";

  public static final String XMPP_OLDVERSION = "0.0";

  public static final String XMPP_Default_Language = "en";

  public static final int XMPP_Default_ServerPort = 5269;

  public static final int XMPP_Default_ClientPort = 5222;

  public static final String CIPHER_SUITE_ATTR = "javax.servlet.xmpp.cipher_suite";

  public static final String CERTIFICATES_ATTR = "javax.servlet.xmpp.X509Certificate";

  public static final String Session_needAuthkey_ATTR = "Session_needAuthkey_ATTR";

  public static final String Session_needAuthSessionID_ATTR = "Session_needAuthSessionID_ATTR";

  public static final String SendMessage_PARAMS_PeerDomain = "SendMessage_PARAMS_PeerDomain";

  public static final String SendMessage_PARAMS_LocalDomain = "SendMessage_PARAMS_LocalDomain";

  public static final String SendMessage_PARAMS_IsClientSide = "SendMessage_PARAMS_IsClientSide";

  public static final String Service_ClosedConnection = "Service_ClosedConnection";

  public static final String XMPP_ERROR_MESSAGE = "XMPP_ERROR_MESSAGE";

  public static final String XMPP_AUTH_MECHANISM_PLAIN = "PLAIN";

  public static final String XMPP_AUTH_MECHANISM_DIGESTMD5 = "DIGEST-MD5";

  public static final String PREFIX_BIND_RESOURCE = "resource";
}
