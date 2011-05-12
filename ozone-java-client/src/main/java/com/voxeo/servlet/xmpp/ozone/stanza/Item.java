package com.voxeo.servlet.xmpp.ozone.stanza;


public class Item extends AbstractXmppObject {

	public Item() {
		
		super();
	}
	
	public String getJID() {
		
		return attribute("jid");
	}
	
	public Item setJID(String jid) {
		
		setAttribute("jid", jid);
		return this;

	}
	
	public String getAffiliation() {
		
		return attribute("affilition");
	}
	
	public Item setAffiliation(String affiliation) {
		
		setAttribute("affiliation", affiliation);
		return this;
	}
	
	public String getNick() {
		
		return attribute("nick");
	}
	
	public Item setNick(String nick) {
		
		setAttribute("nick", nick);
		return this;
	}
	
	public String getName() {
		
		return attribute("name");
	}
	
	public Item setName(String name) {
		
		setAttribute("name", name);
		return this;
	}
	
	public String getSubscription() {
		
		return attribute("subscription");
	}
	
	public Item setSubscription(String subscription) {
		
		setAttribute("subscription", subscription);
		return this;
	}
	
	public String getGroup() {
		
		return value("group");
	}
	
	public Item setGroup(String group) {
		
		set("group", group);
		return this;
	}
	
	public String getRole() {
		
		return attribute("role");
	}
	
	public Item setRole(String role) {
		
		setAttribute("role", role);
		return this;
	}
	
	@Override
	public String getStanzaName() {

		return "item";
	}
	
	@Override
	public XmppObject copy() {

		Item item = new Item();
		item.copy(this);
		return item;
	}
}
