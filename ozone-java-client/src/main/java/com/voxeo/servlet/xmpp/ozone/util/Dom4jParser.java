package com.voxeo.servlet.xmpp.ozone.util;

import java.io.StringReader;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

public class Dom4jParser {

	public static Element parseXml(String string) {
		
		try {
			SAXReader reader = new SAXReader();
			Document doc = reader.read(new StringReader(string));
			Element root = doc.getRootElement();
			return root;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
