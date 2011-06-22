package com.tropo.ozone.gateway;

import java.util.Collection;

public interface TropoNodeService {
	TropoNode lookup (int ppid);
	TropoNode lookup (String hostnameOrAddress);
	void add (String name, String address, int ppid);
	void remove (String hostnameOrAddress);
	Collection<TropoNode> lookupAll (int ppid);
	
	class TropoNode {
		private String hostname;
		private String address;
		private int ppid;
		private String toString;
		private int hashCode;
		
		TropoNode (String hostname, String address, int ppid) {
			this.hostname = hostname;
			this.address = address;
			this.ppid = ppid;
			this.toString = new StringBuilder(super.toString())
				.append("[hostname=")
				.append(hostname)
				.append(" address=")
				.append(address)
				.append(" ppid=")
				.append(ppid)
				.append("]")
				.toString();
			hashCode = hostname.hashCode();
		}

		public String getHostname() {
			return hostname;
		}

		public String getAddress() {
			return address;
		}

		public int getPpid() {
			return ppid;
		}
		
		public String toString () {
			return toString;
		}
		
		public int hashCode () {
			return hashCode;
		}
		
		public boolean equals (Object that) {
			boolean isEqual = that instanceof TropoNode;
			if (isEqual) {
				TropoNode thatTropoNode = (TropoNode)that;
				isEqual = this.hostname.equals(thatTropoNode.hostname) &&
					this.address.equals(thatTropoNode.address) &&
					this.ppid == thatTropoNode.ppid;
			}
			return isEqual;
		}
	}
}
