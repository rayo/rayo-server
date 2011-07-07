package com.tropo.ozone.gateway;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.tropo.core.util.CollectionMap;
import com.voxeo.guido.Guido;
import com.voxeo.guido.GuidoException;
import com.voxeo.logging.Loggerf;
import com.voxeo.servlet.xmpp.JID;

public class InMemoryGatewayDatastore implements GatewayDatastore
{
	private static final Loggerf LOG = Loggerf.getLogger(InMemoryGatewayDatastore.class);
	
	private Map<String, TropoNode> hostnameMap = new HashMap<String, TropoNode>();
	private Map<String, TropoNode> addressMap = new HashMap<String, TropoNode>();
	private Map<JID, TropoNode> nodeMap = new HashMap<JID, TropoNode>();
	private ReadWriteLock tropoNodeLock = new ReentrantReadWriteLock();
	private CollectionMap<String, ArrayList<TropoNode>, TropoNode> platformMap = new CollectionMap<String, ArrayList<TropoNode>, TropoNode>();

	private CollectionMap<JID, ArrayList<JID>, JID> clientJIDs = new CollectionMap<JID, ArrayList<JID>, JID>();
	private Map<JID, String> jidToPlatformMap = new HashMap<JID, String>();
	private ReadWriteLock jidLock = new ReentrantReadWriteLock();

	private Map<String, JID> callToClientMap = new HashMap<String, JID>();
	private Map<JID, Collection<String>> clientToCallMap = new HashMap<JID, Collection<String>>();
	private Map<String, TropoNode> callToNodeMap = new HashMap<String, TropoNode>();
	private Map<TropoNode, Collection<String>> nodeToCallMap = new HashMap<TropoNode, Collection<String>>();
	private ReadWriteLock callLock = new ReentrantReadWriteLock();

	public InMemoryGatewayDatastore () {}

	public String lookupPlatformID (JID clientJid)
	{
		Lock readLock = jidLock.readLock();
		readLock.lock();
		try
		{
			String platformId = jidToPlatformMap.get(clientJid);
			LOG.debug("Platform lookup for %s found %s", clientJid, platformId);
			return platformId;
		}
		finally
		{
			readLock.unlock();
		}
	}

	public String getDomainName (String ipAddress)
	{
		String domain = ipAddress;
		Lock readLock = tropoNodeLock.readLock();
		readLock.lock();
		try
		{
			TropoNode node = addressMap.get(ipAddress);
			if (node == null)
			{
				try
				{
					domain = InetAddress.getByName(ipAddress).getHostName();
				}
				catch (UnknownHostException weTried)
				{
					LOG.debug("No domain name could be found for " + ipAddress);
				}
			}
			else
			{
				domain = node.getHostname();
			}
			LOG.debug("%s mapped to domain %s", ipAddress, domain);
			return domain;
		}
		finally
		{
			readLock.unlock();
		}
	}

	public Collection<JID> getClients (JID appJid)
	{
		Lock readLock = jidLock.readLock();
		readLock.lock();
		try
		{
			Collection<JID> clients = clientJIDs.lookupAll(appJid);
			LOG.debug("Clients for %s found: %s", appJid, clients);
			return clients;
		}
		finally
		{
			readLock.unlock();
		}
	}

	public void addClient (JID clientJid, String platformID) throws UnknownApplicationException
	{
		Lock writeLock = jidLock.writeLock();
		writeLock.lock();
		try
		{
			clientJIDs.add(clientJid.getBareJID(), clientJid);
			jidToPlatformMap.put(clientJid, platformID);
			LOG.debug("Client %s added for platform %s", clientJid, platformID);
		}
		finally
		{
			writeLock.unlock();
		}
	}

	public void removeClient (JID clientJid)
	{
		Lock writeLock = jidLock.writeLock();
		writeLock.lock();
		try
		{
			clientJIDs.remove(clientJid.getBareJID(), clientJid);
			jidToPlatformMap.remove(clientJid);
			LOG.debug("Client %s removed", clientJid);
		}
		finally
		{
			writeLock.unlock();
		}
	}

	public void removeApplication (JID appJid)
	{
		Lock writeLock = jidLock.writeLock();
		writeLock.lock();
		try
		{
			Collection<JID> jids = clientJIDs.removeAll(appJid);
			if (jids != null)
			{
				for (JID jid : jids)
				{
					String platformId = jidToPlatformMap.remove(jid);
					LOG.debug("Removed %s from platform %s", jid, platformId);
				}
			}
			LOG.debug("Removed application %s", appJid);
		}
		finally
		{
			writeLock.unlock();
		}
	}

	public Collection<JID> getTropoNodes (String platformID)
	{
		Lock readLock = tropoNodeLock.readLock();
		readLock.lock();
		try
		{
			Set<JID> jids = new HashSet<JID>();
			for (TropoNode node : platformMap.lookupAll(platformID, readLock))
			{
				jids.add(node.getJID());
			}
			LOG.debug("Tropo nodes found for %s: %s", platformID, jids);
			return jids;
		}
		finally
		{
			readLock.unlock();
		}
	}

	public String selectTropoNodeJID (String platformID)
	{
		Lock readLock = tropoNodeLock.readLock();
		readLock.lock();
		try
		{
			TropoNode node = platformMap.lookup(platformID, readLock);
			String nodeJid = node.getJID().toString();
			LOG.debug("Selected Tropo node %s for platform %s", nodeJid, platformID);
			return nodeJid;
		}
		finally
		{
			readLock.unlock();
		}
	}

	public Collection<String> getPlatformIDs (JID nodeJid)
	{
		Lock lock = tropoNodeLock.readLock();
		lock.lock();
		try
		{
			TropoNode node = nodeMap.get(nodeJid);
			Collection<String> platformIDs = Collections.unmodifiableSet(node.getPlatformIds());
			LOG.debug("Platform IDs found for %s: %s", nodeJid, platformIDs);
			return platformIDs;
		}
		finally
		{
			lock.unlock();
		}
	}

	public void setPlatformIDs (JID nodeJid, Collection<String> platformIDs) throws UnknownHostException
	{
		Lock writeLock = tropoNodeLock.writeLock();
		writeLock.lock();
		try
		{
			LOG.debug("Adding %s to platforms %s", nodeJid, platformIDs);
			TropoNode node = nodeMap.get(nodeJid);
			if (node == null)
			{
				String hostname = nodeJid.getDomain();
				String ipAddress = InetAddress.getByName(hostname).getHostAddress();
				node = new TropoNode(hostname, ipAddress, nodeJid, new HashSet<String>(platformIDs));
				hostnameMap.put(hostname, node);
				addressMap.put(ipAddress, node);
				nodeMap.put(nodeJid, node);
				LOG.debug("Created: %s", node);
			}
			else
			{
				// Not checking hostname or IP since JID would not have matched
				for (String platformID : platformIDs)
				{
					platformMap.remove(platformID, node, writeLock);
					LOG.debug("Removed %s from platform %s", node, platformID);
				}
				node.setPlatformIds(new HashSet<String>(platformIDs));
			}

			for (String platformID : platformIDs)
			{
				platformMap.add(platformID, node);
				LOG.debug("Added %s to platform %s", node, platformID);
			}
		}
		finally
		{
			writeLock.unlock();
		}
	}

	public void removeTropoNode (JID nodeJid)
	{
		Lock writeLock = tropoNodeLock.writeLock();
		writeLock.lock();
		try
		{
			TropoNode node = nodeMap.remove(nodeJid);
			if (node != null)
			{
				hostnameMap.remove(node.getHostname());
				addressMap.remove(node.getAddress());
				for (String platformID : node.getPlatformIds())
				{
					platformMap.remove(platformID, node, writeLock);
					LOG.debug("Removed %s from platform %s", node, platformID);
				}
			}
			LOG.debug("Removed node %s", nodeJid);
		}
		finally
		{
			writeLock.unlock();
		}
	}

	public String getClientJID (String callID)
	{
		Lock readLock = callLock.readLock();
		readLock.lock();
		try
		{
			String clientJid = callToClientMap.get(callID).toString();
			LOG.debug("Call ID %s mapped to client JID %s", callID, clientJid);
			return clientJid;
		}
		finally
		{
			readLock.unlock();
		}
	}

	@SuppressWarnings("unchecked")
	public Collection<String> getCalls (JID clientJid)
	{
		Lock readLock = callLock.readLock();
		readLock.lock();
		try
		{
			Collection<String> calls = clientToCallMap.get(clientJid);
			if (calls == null)
			{
				calls = Collections.EMPTY_SET;
			}
			LOG.debug("Found calls for %s: %s", clientJid, calls);
			return Collections.unmodifiableCollection(calls);
		}
		finally
		{
			readLock.unlock();
		}
	}

	public void mapCallToClient (String callID, JID clientJid)
	{
		Lock readLock = tropoNodeLock.readLock();
		Lock writeLock = callLock.writeLock();
		lockAll(readLock, writeLock);
		try
		{
			String host = null;
			try
			{
				Guido guido = new Guido(callID, false, null);
				host = guido.decodeHost();
			}
			catch (GuidoException cannotDecode)
			{
				LOG.warn("Could not decode callID " + callID, cannotDecode);
			}

			if (host == null)
			{
				throw new IllegalArgumentException("callID " + callID + " does not map to a known host");
			}

			TropoNode node = hostnameMap.get(host);
			if (node == null)
			{
				throw new IllegalArgumentException(host + " does not map to a known Tropo node");
			}

			callToClientMap.put(callID, clientJid);
			LOG.debug("Call %s mapped to client %s", callID, clientJid);

			Collection<String> clientCalls = clientToCallMap.get(clientJid);
			if (clientCalls == null)
			{
				clientCalls = new HashSet<String>();
				clientToCallMap.put(clientJid, clientCalls);
			}
			clientCalls.add(callID);
			LOG.debug("Client %s is mapped to calls %s", clientJid, clientCalls);

			callToNodeMap.put(callID, node);

			Collection<String> nodeCalls = nodeToCallMap.get(node);
			if (nodeCalls == null)
			{
				nodeCalls = new HashSet<String>();
				nodeToCallMap.put(node, nodeCalls);
			}
			nodeCalls.add(callID);
			LOG.debug("Node %s is mapped to calls %s", node, nodeCalls);
		}
		finally
		{
			writeLock.unlock();
			readLock.unlock();
		}
	}

	public void removeCall (String callID)
	{
		Lock writeLock = callLock.writeLock();
		writeLock.lock();
		try
		{
			JID client = callToClientMap.remove(callID);
			if (client != null)
			{
				LOG.debug("Call %s removed from client %s", callID, client);
				Collection<String> calls = clientToCallMap.get(client);
				if (calls != null)
				{
					calls.remove(callID);
					LOG.debug("Client %s mapped to calls %s", client, calls);
					if (calls.isEmpty())
					{
						clientToCallMap.remove(client);
					}
				}
			}

			TropoNode node = callToNodeMap.remove(callID);
			if (node != null)
			{
				LOG.debug("Call %s removed from Tropo node %s", callID, node);
				Collection<String> calls = nodeToCallMap.get(client);
				if (calls != null)
				{
					calls.remove(callID);
					LOG.debug("Node %s mapped to call %s", node, calls);
					if (calls.isEmpty())
					{
						nodeToCallMap.remove(node);
					}
				}
			}
		}
		finally
		{
			writeLock.unlock();
		}
	}

	public Collection<String> getCallsForNode (JID nodeJid)
	{
		Lock readLock1 = tropoNodeLock.readLock();
		Lock readLock2 = callLock.readLock();
		lockAll(readLock1, readLock2);
		try
		{
			@SuppressWarnings("unchecked")
			Collection<String> retval = Collections.EMPTY_SET;
			TropoNode node = nodeMap.get(nodeJid);
			if (node != null)
			{
				Collection<String> calls = nodeToCallMap.get(node);
				if (calls != null)
				{
					retval = Collections.unmodifiableCollection(calls);
				}
			}
			LOG.debug("Node %s has calls %s", nodeJid, retval);
			return retval;
		}
		finally
		{
			readLock1.unlock();
			readLock2.unlock();
		}
	}

	private void lockAll (Lock... locks)
	{
		boolean success = true;
		do
		{
			int i = 0;
			try
			{
				while (i < locks.length && success)
				{
					success = locks[i].tryLock(10, TimeUnit.MILLISECONDS);
					++i;
				}
			}
			catch (InterruptedException restart)
			{
				success = false;
			}
			catch (Exception ex)
			{
				success = false;
				LOG.warn("Exception caught while locking locks", ex);
				if (ex instanceof RuntimeException)
				{
					throw (RuntimeException) ex;
				}
				else
				{
					throw new RuntimeException("Unexpected checked exception", ex);
				}
			}
			finally
			{
				if (!success)
				{
					for (int j = 0; j < 1; ++j)
					{
						locks[j].unlock();
					}
				}
			}
		}
		while (!success);
	}

	private static class TropoNode
	{
		private String hostname;
		private String address;
		private JID jid;
		private Set<String> platformIds;
		private String toString;
		private int hashCode;

		TropoNode (String hostname, String address, JID jid, Set<String> platformIds)
		{
			this.hostname = hostname;
			this.address = address;
			this.jid = jid;
			this.platformIds = platformIds;
			this.toString = new StringBuilder(super.toString()).append("[hostname=").append(hostname).append(" address=").append(address).append(" jid=")
					.append(jid).append(" platformIds=").append(platformIds).append("]").toString();
			hashCode = hostname.hashCode();
		}

		String getHostname ()
		{
			return hostname;
		}

		String getAddress ()
		{
			return address;
		}

		JID getJID ()
		{
			return jid;
		}

		Set<String> getPlatformIds ()
		{
			return platformIds;
		}

		void setPlatformIds (Set<String> platformIds)
		{
			this.platformIds = platformIds;
		}

		public String toString ()
		{
			return toString;
		}

		public int hashCode ()
		{
			return hashCode;
		}

		public boolean equals (Object that)
		{
			boolean isEqual = that instanceof TropoNode;
			if (isEqual)
			{
				TropoNode thatTropoNode = (TropoNode) that;
				isEqual = this.hostname.equals(thatTropoNode.hostname) && this.address.equals(thatTropoNode.address);
			}
			return isEqual;
		}
	}
}
