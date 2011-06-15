package com.tropo.server.application;

import java.io.IOException;
import java.net.URL;
import java.util.Iterator;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.tropo.core.application.SimplePlatform;
import com.tropo.core.application.SimpleToken;
import com.voxeo.moho.ApplicationContext;
import com.voxeo.servlet.xmpp.JID;
import com.voxeo.servlet.xmpp.XmppFactory;

/**

<applications>
	<account id="123">
		<application startUrl="http://foo.com/app" id="9876">
			<platform name="staging" id="123">
				<jid bare="devapp1@dev.null">
					<token>1234567890abcdef</token>
					<endpoint>tel:+18885551212</endpoint>
				</jid>
				<jid bare="devapp2@dev.null">
					<token>fedcba0987654321</token>
				</jid>
				<jid bare="devapp3@dev.null">
					<endpoint>sip:alpha@beta.gamma</endpoint>
				</jid>
			</ppid>
			<platform name="production" id="456">
				<jid bare="prodapp1@dev.null">
					<endpoint>revenue@pipe.dream</endpoint>
				</jid>
				<jid bare="prodapp2@dev.null">
					<token>1357924680acebdf</token>
				</jid>
			</ppid>
		</application>
		<application ...>
			...
		</application>
	</account>
	<account ...>
		...
	</account>
</applications>

 * @author steven
 *
 */
public class XmlBackedOzoneApplicationLookupService extends InMemoryOzoneApplicationLookupService
{
	public XmlBackedOzoneApplicationLookupService (XmppFactory xmppFactory, ApplicationContext mohoAppContext, URL url) throws IOException, DocumentException
	{
		read(xmppFactory, mohoAppContext, url);
	}
	
	private void read (XmppFactory xmppFactory, ApplicationContext mohoAppContext, URL url) throws IOException, DocumentException
	{
		Document doc = new SAXReader().read(url);
		Element root = doc.getRootElement();
		if (!"applications".equals(root.getName()))
		{
			throw new DocumentException("Document does not have <applications> as its root tag.");
		}
		
		for (@SuppressWarnings("unchecked")Iterator<Element> accountIter = root.elementIterator("account");
			 accountIter.hasNext();
			 )
		{
			Element accountElement = accountIter.next();
			int accountID = Integer.parseInt(accountElement.attributeValue("id"));

			for (@SuppressWarnings("unchecked")Iterator<Element> appIter = accountElement.elementIterator("application");
				 appIter.hasNext();
				 )
			{
				Element appElement = appIter.next();
				String startUrl = appElement.attributeValue("startUrl");
				int appID = Integer.parseInt(appElement.attributeValue("id"));
				
				ConcreteOzoneApplication ozoneApp = new ConcreteOzoneApplication(startUrl, accountID, appID);
				
				for (@SuppressWarnings("unchecked") Iterator<Element> platformIter = appElement.elementIterator("platform");
					 platformIter.hasNext();
					 )
				{
					Element platformElement = platformIter.next();
					SimplePlatform platform = new SimplePlatform(platformElement.attributeValue("name"),
						Integer.parseInt(platformElement.attributeValue("id")));
					
					for (@SuppressWarnings("unchecked")Iterator<Element> jidIter = platformElement.elementIterator("jid");
						 jidIter.hasNext();
						 )
					{
						Element jidElement = jidIter.next();
						JID bareJid = xmppFactory.createJID(jidElement.attributeValue("bare"));
						
						for (@SuppressWarnings("unchecked")Iterator<Element> tokenIter = jidElement.elementIterator("token");
							 tokenIter.hasNext();
							 )
						{
							Element tokenElement = tokenIter.next();
							ozoneApp.addToken(new SimpleToken(tokenElement.getText(), platform, appID), bareJid, platform);
						}
						
						for (@SuppressWarnings("unchecked")Iterator<Element> endpointIter = jidElement.elementIterator("endpoint");
							 endpointIter.hasNext();
							 )
						{
							Element endpointElement = endpointIter.next();
							ozoneApp.addEndpoint(mohoAppContext.createEndpoint(endpointElement.getText()), bareJid, platform);
						}
					}
					
					addApplication(ozoneApp);
				}
			}
		}
	}
}
