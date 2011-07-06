package com.tropo.ozone.gateway;

import java.io.IOException;
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

import org.apache.log4j.Logger;
import org.springframework.core.io.Resource;

import com.tropo.core.util.CollectionMap;
import com.voxeo.guido.Guido;
import com.voxeo.guido.GuidoException;
import com.voxeo.servlet.xmpp.JID;

public class InMemoryGatewayDatastore implements GatewayDatastore
{
	private static final Logger LOG = Logger.getLogger(InMemoryGatewayDatastore.class);

	private Router router;
	
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

	public InMemoryGatewayDatastore (Router router)
	{
		this.router = router;
	}
	
	public String lookupJID (String from, String to, Map<String, String> headers)
	{
		return router.lookupJID(from, to, headers);
	}

	public String lookupPlatformID (JID clientJid)
	{
		Lock readLock = jidLock.readLock();
		readLock.lock();
		try
		{
			return jidToPlatformMap.get(clientJid);
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
			return clientJIDs.lookupAll(appJid);
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
					jidToPlatformMap.remove(jid);
				}
			}
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
			return node.getJID().toString();
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
			return Collections.unmodifiableSet(node.getPlatformIds());
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
			TropoNode node = nodeMap.get(nodeJid);
			if (node == null)
			{
				String hostname = nodeJid.getDomain();
				String ipAddress = InetAddress.getByName(hostname).getHostAddress();
				node = new TropoNode(hostname, ipAddress, nodeJid, new HashSet<String>(platformIDs));
				hostnameMap.put(hostname, node);
				addressMap.put(ipAddress, node);
				nodeMap.put(nodeJid, node);
			}
			else
			{
				// Not checking hostname or IP since JID would not have matched
				for (String platformID : platformIDs)
				{
					platformMap.remove(platformID, node, writeLock);
				}
				node.setPlatformIds(new HashSet<String>(platformIDs));
			}

			for (String platformID : platformIDs)
			{
				platformMap.add(platformID, node);
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
				}
			}
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
			return callToClientMap.get(callID).toString();
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

			Collection<String> clientCalls = clientToCallMap.get(clientJid);
			if (clientCalls == null)
			{
				clientCalls = new HashSet<String>();
				clientToCallMap.put(clientJid, clientCalls);
			}
			clientCalls.add(callID);

			callToNodeMap.put(callID, node);

			Collection<String> nodeCalls = nodeToCallMap.get(node);
			if (nodeCalls == null)
			{
				nodeCalls = new HashSet<String>();
				nodeToCallMap.put(node, nodeCalls);
			}
			nodeCalls.add(callID);
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
				Collection<String> calls = clientToCallMap.get(client);
				if (calls != null)
				{
					calls.remove(callID);
					if (calls.isEmpty())
					{
						clientToCallMap.remove(client);
					}
				}
			}

			TropoNode node = callToNodeMap.remove(callID);
			if (node != null)
			{
				Collection<String> calls = nodeToCallMap.get(client);
				if (calls != null)
				{
					calls.remove(callID);
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
	
	public interface Router
	{
		String lookupJID (String from, String to, Map<String, String> headers);
		void setResource (Resource resource) throws IOException;
	}
}
