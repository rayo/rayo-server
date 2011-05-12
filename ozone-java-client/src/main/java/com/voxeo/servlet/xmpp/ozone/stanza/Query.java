package com.voxeo.servlet.xmpp.ozone.stanza;

public class Query extends AbstractXmppObject {

	public static final String NAME = "query";
	
	public Query() {
		
		super();
	}
	
	public Query(String namespace) {
		
		super(namespace);
	}
	
	public Query addChild(XmppObject object) {
		
		add(object);
		return this;
	}
	
	public String getNamespace() {
		
		return attribute("namespace");
	}
	
	public Query setNamespace(String namespace) {
		
		setAttribute("namespace", namespace);
		return this;
	}
	
	public String getNode() {
		
		return attribute("node");
	}
	
	public Query setNode(String node) {
		
		setAttribute("node", node);
		return this;
	}
	
	@Override
	public String getStanzaName() {

		return NAME;
	}
	
	@Override
	public Query copy() {

		Query query = new Query();
		query.copy(this);
		return query;
	}
}
