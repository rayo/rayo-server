package com.voxeo.servlet.xmpp.ozone.extensions;

import java.util.ArrayList;

import com.tropo.core.validation.Validator;
import com.tropo.core.xml.DefaultXmlProviderManager;
import com.tropo.core.xml.XmlProviderManager;
import com.tropo.core.xml.providers.AskProvider;
import com.tropo.core.xml.providers.ConferenceProvider;
import com.tropo.core.xml.providers.OzoneProvider;
import com.tropo.core.xml.providers.SayProvider;
import com.tropo.core.xml.providers.TransferProvider;
import com.voxeo.ozone.client.xml.providers.OzoneClientProvider;

public class XmlProviderManagerFactory {

	public static XmlProviderManager buildXmlProvider() {
		
		XmlProviderManager manager = new DefaultXmlProviderManager();
		Validator validator = new Validator();
		OzoneProvider ozoneProvider = new OzoneProvider();
		ozoneProvider.setNamespaces(new ArrayList<String>());
		ozoneProvider.getNamespaces().add("urn:xmpp:ozone:1");
		ozoneProvider.setValidator(validator);
		manager.register(ozoneProvider);
		SayProvider sayProvider = new SayProvider();
		sayProvider.setNamespaces(new ArrayList<String>());
		sayProvider.getNamespaces().add("urn:xmpp:ozone:say:1");
		sayProvider.setValidator(validator);
		manager.register(sayProvider);
		AskProvider askProvider = new AskProvider();
		askProvider.setNamespaces(new ArrayList<String>());
		askProvider.getNamespaces().add("urn:xmpp:ozone:ask:1");
		askProvider.setValidator(validator);
		manager.register(askProvider);
		TransferProvider transferProvider = new TransferProvider();
		transferProvider.setNamespaces(new ArrayList<String>());
		transferProvider.getNamespaces().add("urn:xmpp:ozone:transfer:1");
		transferProvider.setValidator(validator);
		manager.register(transferProvider);
		ConferenceProvider conferenceProvider = new ConferenceProvider();
		conferenceProvider.setNamespaces(new ArrayList<String>());
		conferenceProvider.getNamespaces().add("urn:xmpp:ozone:conference:1");
		conferenceProvider.setValidator(validator);
		manager.register(conferenceProvider);
		OzoneClientProvider ozoneClientProvider = new OzoneClientProvider();
		ozoneClientProvider.setNamespaces(new ArrayList<String>());
		ozoneClientProvider.getNamespaces().add("urn:xmpp:ozone:1");
		ozoneClientProvider.setValidator(validator);
		manager.register(ozoneClientProvider);		
		return manager;		
	}
}
