package com.tropo.server.application;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.springframework.core.io.Resource;

/**

<routes>
	<route id="1234">
		<name1>value1</name>
		<name2>value2</name>
	</route>
</routes>

 * @author steven
 *
 */
public class XmlBackedMetaDataLookupService extends InMemoryMetaDataLookupService
{
	public XmlBackedMetaDataLookupService (Resource xmlResource) throws IOException, DocumentException
	{
		read(xmlResource);
	}
	
	private void read ( Resource xmlResource) throws IOException, DocumentException
	{
		Document doc = new SAXReader().read(xmlResource.getInputStream());
		Element root = doc.getRootElement();
		if (!"routes".equals(root.getName()))
		{
			throw new DocumentException("Document does not have <routes> as its root tag.");
		}
		
		for (@SuppressWarnings("unchecked")Iterator<Element> routeIter = root.elementIterator("route");
			 routeIter.hasNext();
			 )
		{
			Element routeElement = routeIter.next();
			String routeID = routeElement.attributeValue("id");
			Map<String, String> metadata = new HashMap<String, String>();
			
			for (@SuppressWarnings("unchecked")Iterator<Element> keyValueIter = routeElement.elementIterator();
				 keyValueIter.hasNext();
				 )
			{
				Element keyValueElement = keyValueIter.next();
				metadata.put(keyValueElement.getName(), keyValueElement.getText());
			}
			
			addMapping(routeID, metadata);
		}
	}
}
