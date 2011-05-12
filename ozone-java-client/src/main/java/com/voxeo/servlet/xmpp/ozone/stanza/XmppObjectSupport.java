package com.voxeo.servlet.xmpp.ozone.stanza;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import org.dom4j.Element;

public class XmppObjectSupport {

	public static <T extends XmppObject> T newChildInstance(Class<T> clazz, XmppObject parent, String childName) {
		
		if (!parent.hasChild(childName)) {
			return null;
		}
		
		try {
			Constructor<T> constructor = clazz.getConstructor();			
			T instance = constructor.newInstance();
			instance.setElement(parent.getChildElement(childName));
			return instance;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public static <T extends XmppObject> List<T> newChildListInstance(Class<T> clazz, XmppObject parent, String childName) {
		
		if (!parent.hasChild(childName)) {
			return null;
		}
		List<T> list = new ArrayList<T>();
		try {
			for (Element element: parent.getChildElements(childName)) {			
				Constructor<T> constructor = clazz.getConstructor();			
				T instance = constructor.newInstance();
				instance.setElement(element);
				list.add(instance);
			}
			return list;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static <T extends XmppObject> T copy(Class<T> clazz, XmppObject object) {
		
		try {
			Constructor<T> constructor = clazz.getConstructor();			
			T instance = constructor.newInstance();
			instance.setElement(object.getElement());
			return instance;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
