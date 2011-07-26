// /Copyright 2003-2005 Arthur van Hoff, Rick Blair
// Licensed under Apache License version 2.0
// Original license LGPL
package javax.jmdns.impl;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import javax.jmdns.ServiceTypeListener;
import javax.jmdns.impl.tasks.Announcer;
import javax.jmdns.impl.tasks.Canceler;
import javax.jmdns.impl.tasks.Prober;
import javax.jmdns.impl.tasks.RecordReaper;
import javax.jmdns.impl.tasks.Renewer;
import javax.jmdns.impl.tasks.Responder;
import javax.jmdns.impl.tasks.ServiceInfoResolver;
import javax.jmdns.impl.tasks.ServiceResolver;
import javax.jmdns.impl.tasks.TypeResolver;

import android.util.Log;

// REMIND: multiple IP addresses
/** mDNS implementation in Java.
 * @version %I%, %G%
 * @author Arthur van Hoff, Rick Blair, Jeff Sonstein, Werner Randelshofer,
 * Pierre Frisch, Scott Lewis */
public class JmDNSImpl extends JmDNS {
	public final static String TAG = JmDNSImpl.class.toString();
	/** This is the multicast group, we are listening to for multicast DNS
	 * messages. */
	private InetAddress group;
	/** This is our multicast socket. */
	private MulticastSocket socket;
	/** Used to fix live lock problem on unregester. */
	private boolean closed = false;
	/** Holds instances of JmDNS.DNSListener. Must by a synchronized collection,
	 * because it is updated from concurrent threads. */
	@SuppressWarnings("rawtypes")
	private List listeners;
	/** Holds instances of ServiceListener's. Keys are Strings holding a fully
	 * qualified service type. Values are LinkedList's of ServiceListener's. */
	@SuppressWarnings("rawtypes")
	private Map serviceListeners;
	/** Holds instances of ServiceTypeListener's. */
	@SuppressWarnings("rawtypes")
	private List typeListeners;
	/** Cache for DNSEntry's. */
	private DNSCache cache;
	/** This hashtable holds the services that have been registered. Keys are
	 * instances of String which hold an all lower-case version of the fully
	 * qualified service name. Values are instances of ServiceInfo. */
	@SuppressWarnings("rawtypes")
	Map services;
	/** This hashtable holds the service types that have been registered or that
	 * have been received in an incoming datagram. Keys are instances of String
	 * which hold an all lower-case version of the fully qualified service type.
	 * Values hold the fully qualified service type. */
	@SuppressWarnings("rawtypes")
	Map serviceTypes;
	/** This is the shutdown hook, we registered with the java runtime. */
	private Thread shutdown;
	/** Handle on the local host */
	private HostInfo localHost;
	private Thread incomingListener = null;
	/** Throttle count. This is used to count the overall number of probes sent
	 * by JmDNS. When the last throttle increment happened . */
	private int throttle;
	/** Last throttle increment. */
	private long lastThrottleIncrement;
	/** The timer is used to dispatch all outgoing messages of JmDNS. It is also
	 * used to dispatch maintenance tasks for the DNS cache. */
	Timer timer;
	/** The source for random values. This is used to introduce random delays in
	 * responses. This reduces the potential for collisions on the network. */
	private final static Random random = new Random();
	/** This lock is used to coordinate processing of incoming and outgoing
	 * messages. This is needed, because the Rendezvous Conformance Test does
	 * not forgive race conditions. */
	private Object ioLock = new Object();
	/** If an incoming package which needs an answer is truncated, we store it
	 * here. We add more incoming DNSRecords to it, until the JmDNS.Responder
	 * timer picks it up. Remind: This does not work well with multiple planned
	 * answers for packages that came in from different clients. */
	private DNSIncoming plannedAnswer;
	// State machine
	/** The state of JmDNS.
	 * <p/>
	 * For proper handling of concurrency, this variable must be changed only
	 * using methods advanceState(), revertState() and cancel(). */
	private DNSState state = DNSState.PROBING_1;
	/** Timer task associated to the host name. This is used to prevent from
	 * having multiple tasks associated to the host name at the same time. */
	private TimerTask task;
	/** This hashtable is used to maintain a list of service types being
	 * collected by this JmDNS instance. The key of the hashtable is a service
	 * type name, the value is an instance of JmDNS.ServiceCollector.
	 * @see #list */
	@SuppressWarnings("rawtypes")
	private HashMap serviceCollectors = new HashMap();

	/** Create an instance of JmDNS. */
	public JmDNSImpl(InetAddress addr, String hostname) throws IOException {
		Log.d(TAG, "JmDNS instance created");
		try {
			// InetAddress addr = InetAddress.getLocalHost();
			// init(addr.isLoopbackAddress() ? null : addr, addr.getHostName());
			// //
			// [PJYF Oct 14 2004] Why do we disallow the loopback address?
			// init(addr.isLoopbackAddress() ? null : addr, hostname); // [PJYF
			// Oct
			// 14 2004] Why do we disallow the loopback address?
			init(addr, hostname);
		} catch (IOException e) {
			init(null, "computer");
		}
	}

	/** Create an instance of JmDNS and bind it to a specific network interface
	 * given its IP-address. */
	public JmDNSImpl(InetAddress addr) throws IOException {
		try {
			init(addr, addr.getHostName());
		} catch (IOException e) {
			init(null, "computer");
		}
	}

	/** Initialize everything.
	 * @param address
	 * The interface to which JmDNS binds to.
	 * @param name
	 * The host name of the interface. */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void init(InetAddress address, String name) throws IOException {
		// A host name with "." is illegal. so strip off everything and append
		// .local.
		int idx = name.indexOf(".");
		if (idx > 0) {
			name = name.substring(0, idx);
		}
		name += ".local.";
		// localHost to IP address binding
		localHost = new HostInfo(address, name);
		// Log.w(TAG, "one");
		cache = new DNSCache(100);
		listeners = Collections.synchronizedList(new ArrayList());
		serviceListeners = new HashMap();
		typeListeners = new ArrayList();
		// Log.w(TAG, "two");
		services = new Hashtable(20);
		serviceTypes = new Hashtable(20);
		// REMIND: If I could pass in a name for the Timer thread,
		// I would pass 'JmDNS.Timer'.
		timer = new Timer();
		new RecordReaper(this).start(timer);
		shutdown = new Thread(new Shutdown(), "JmDNS.Shutdown");
		Runtime.getRuntime().addShutdownHook(shutdown);
		incomingListener = new Thread(new SocketListener(this),
				"JmDNS.SocketListener");
		// Bind to multicast socket
		Log.d(TAG, "about to openMulticastSocket()");
		openMulticastSocket(getLocalHost());
		start(getServices().values());
		Log.d(TAG, "finished multicast socket");
	}

	@SuppressWarnings("rawtypes")
	private void start(Collection serviceInfos) {
		// Log.d(TAG, "start1");
		setState(DNSState.PROBING_1);
		incomingListener.start();
		new Prober(this).start(timer);
		// Log.d(TAG, "start2");
		for (Iterator iterator = serviceInfos.iterator(); iterator.hasNext();) {
			try {
				// Log.d(TAG, "start3");
				registerService(new ServiceInfoImpl(
						(ServiceInfoImpl) iterator.next()));
				// Log.d(TAG, "start4");
			} catch (Exception exception) {
				Log.d(TAG,
						"start() Registration exception: "
								+ exception.getMessage());
			}
		}
		// Log.d(TAG, "start5");
	}

	private void openMulticastSocket(HostInfo hostInfo) throws IOException {
		// Log.d(TAG, "1");
		if (group == null) {
			group = InetAddress.getByName(DNSConstants.MDNS_GROUP);
		}
		if (socket != null) {
			this.closeMulticastSocket();
		}
		// Log.d(TAG, "2");
		socket = new MulticastSocket(DNSConstants.MDNS_PORT);
		if ((hostInfo != null) && (localHost.getInterface() != null)) {
			// Log.d(TAG, "3");
			// socket.setNetworkInterface(hostInfo.getInterface());
		}
		// Log.d(TAG, "4");
		socket.setTimeToLive(255);
		socket.joinGroup(group);
		// Log.d(TAG, "5");
	}

	private void closeMulticastSocket() {
		Log.d(TAG, "closeMulticastSocket()");
		if (socket != null) {
			// close socket
			try {
				socket.leaveGroup(group);
				socket.close();
				if (incomingListener != null) {
					incomingListener.join();
				}
			} catch (Exception exception) {
				Log.d(TAG, "closeMulticastSocket() Close socket exception: "
						+ exception.getMessage());
			}
			socket = null;
		}
	}

	// State machine
	/** Sets the state and notifies all objects that wait on JmDNS. */
	public synchronized void advanceState() {
		setState(getState().advance());
		notifyAll();
	}

	/** Sets the state and notifies all objects that wait on JmDNS. */
	synchronized void revertState() {
		setState(getState().revert());
		notifyAll();
	}

	/** Sets the state and notifies all objects that wait on JmDNS. */
	synchronized void cancel() {
		setState(DNSState.CANCELED);
		notifyAll();
	}

	/** Returns the current state of this info. */
	public DNSState getState() {
		return state;
	}

	/** Return the DNSCache associated with the cache variable */
	public DNSCache getCache() {
		return cache;
	}

	/** @see javax.jmdns.JmDNS#getHostName() */
	public String getHostName() {
		return localHost.getName();
	}

	public HostInfo getLocalHost() {
		return localHost;
	}

	/** @see javax.jmdns.JmDNS#getInterface() */
	public InetAddress getInterface() throws IOException {
		return socket.getInterface();
	}

	/** @see javax.jmdns.JmDNS#getServiceInfo(java.lang.String, java.lang.String) */
	public ServiceInfo getServiceInfo(String type, String name) {
		return getServiceInfo(type, name, 3 * 1000);
	}

	/** @see javax.jmdns.JmDNS#getServiceInfo(java.lang.String, java.lang.String,
	 * int) */
	public ServiceInfo getServiceInfo(String type, String name, int timeout) {
		ServiceInfoImpl info = new ServiceInfoImpl(type, name);
		new ServiceInfoResolver(this, info).start(timer);
		try {
			long end = System.currentTimeMillis() + timeout;
			long delay;
			synchronized (info) {
				while (!info.hasData()
						&& (delay = end - System.currentTimeMillis()) > 0) {
					info.wait(delay);
				}
			}
		} catch (InterruptedException e) {
			// empty
		}
		return info;
		// return (info.hasData()) ? info : null;
	}

	/** @see javax.jmdns.JmDNS#requestServiceInfo(java.lang.String,
	 * java.lang.String) */
	public void requestServiceInfo(String type, String name) {
		requestServiceInfo(type, name, 3 * 1000);
	}

	/** @see javax.jmdns.JmDNS#requestServiceInfo(java.lang.String,
	 * java.lang.String, int) */
	public void requestServiceInfo(String type, String name, int timeout) {
		registerServiceType(type);
		ServiceInfoImpl info = new ServiceInfoImpl(type, name);
		new ServiceInfoResolver(this, info).start(timer);
		try {
			long end = System.currentTimeMillis() + timeout;
			long delay;
			synchronized (info) {
				while (!info.hasData()
						&& (delay = end - System.currentTimeMillis()) > 0) {
					info.wait(delay);
				}
			}
		} catch (InterruptedException e) {
			Log.d(TAG, "requestServiceInfo() ran out of time");
			// empty
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	void handleServiceResolved(ServiceInfoImpl info) {
		List list = (List) serviceListeners.get(info.type.toLowerCase());
		if (list != null) {
			ServiceEvent event = new ServiceEventImpl(this, info.type,
					info.getName(), info);
			// Iterate on a copy in case listeners will modify it
			final ArrayList listCopy = new ArrayList(list);
			for (Iterator iterator = listCopy.iterator(); iterator.hasNext();) {
				((ServiceListener) iterator.next()).serviceResolved(event);
			}
		}
	}

	/** @see javax.jmdns.JmDNS#addServiceTypeListener(javax.jmdns.ServiceTypeListener) */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void addServiceTypeListener(ServiceTypeListener listener)
			throws IOException {
		synchronized (this) {
			typeListeners.remove(listener);
			typeListeners.add(listener);
		}
		// report cached service types
		for (Iterator iterator = serviceTypes.values().iterator(); iterator
				.hasNext();) {
			listener.serviceTypeAdded(new ServiceEventImpl(this,
					(String) iterator.next(), null, null));
		}
		new TypeResolver(this).start(timer);
	}

	/** @see javax.jmdns.JmDNS#removeServiceTypeListener(javax.jmdns.ServiceTypeListener) */
	public void removeServiceTypeListener(ServiceTypeListener listener) {
		synchronized (this) {
			typeListeners.remove(listener);
		}
	}

	/** @see javax.jmdns.JmDNS#addServiceListener(java.lang.String,
	 * javax.jmdns.ServiceListener) */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void addServiceListener(String type, ServiceListener listener) {
		String lotype = type.toLowerCase();
		removeServiceListener(lotype, listener);
		List list = null;
		synchronized (this) {
			list = (List) serviceListeners.get(lotype);
			if (list == null) {
				list = Collections.synchronizedList(new LinkedList());
				serviceListeners.put(lotype, list);
			}
			list.add(listener);
		}
		// report cached service types
		for (Iterator i = cache.iterator(); i.hasNext();) {
			for (DNSCache.CacheNode n = (DNSCache.CacheNode) i.next(); n != null; n = n
					.next()) {
				DNSRecord rec = (DNSRecord) n.getValue();
				if (rec.type == DNSConstants.TYPE_SRV) {
					if (rec.name.endsWith(type)) {
						listener.serviceAdded(new ServiceEventImpl(this, type,
								toUnqualifiedName(type, rec.name), null));
					}
				}
			}
		}
		new ServiceResolver(this, type).start(timer);
	}

	/** @see javax.jmdns.JmDNS#removeServiceListener(java.lang.String,
	 * javax.jmdns.ServiceListener) */
	@SuppressWarnings("rawtypes")
	public void removeServiceListener(String type, ServiceListener listener) {
		type = type.toLowerCase();
		List list = (List) serviceListeners.get(type);
		if (list != null) {
			synchronized (this) {
				list.remove(listener);
				if (list.size() == 0) {
					serviceListeners.remove(type);
				}
			}
		}
	}

	/** @see javax.jmdns.JmDNS#registerService(javax.jmdns.ServiceInfo) */
	@SuppressWarnings("unchecked")
	public void registerService(ServiceInfo infoAbstract) throws IOException {
		ServiceInfoImpl info = (ServiceInfoImpl) infoAbstract;
		registerServiceType(info.type);
		// bind the service to this address
		info.server = localHost.getName();
		info.addr = localHost.getAddress();
		synchronized (this) {
			makeServiceNameUnique(info);
			services.put(info.getQualifiedName().toLowerCase(), info);
		}
		new /* Service */Prober(this).start(timer);
		try {
			synchronized (info) {
				while (info.getState().compareTo(DNSState.ANNOUNCED) < 0) {
					info.wait();
				}
			}
		} catch (InterruptedException e) {
			// empty
		}
		Log.d(TAG, "registerService() JmDNS registered service as " + info);
	}

	/** @see javax.jmdns.JmDNS#unregisterService(javax.jmdns.ServiceInfo) */
	public void unregisterService(ServiceInfo infoAbstract) {
		ServiceInfoImpl info = (ServiceInfoImpl) infoAbstract;
		synchronized (this) {
			services.remove(info.getQualifiedName().toLowerCase());
		}
		info.cancel();
		// Note: We use this lock object to synchronize on it.
		// Synchronizing on another object (e.g. the ServiceInfo) does
		// not make sense, because the sole purpose of the lock is to
		// wait until the canceler has finished. If we synchronized on
		// the ServiceInfo or on the Canceler, we would block all
		// accesses to synchronized methods on that object. This is not
		// what we want!
		Object lock = new Object();
		new Canceler(this, info, lock).start(timer);
		// Remind: We get a deadlock here, if the Canceler does not run!
		try {
			synchronized (lock) {
				lock.wait();
			}
		} catch (InterruptedException e) {
			// empty
		}
	}

	/** @see javax.jmdns.JmDNS#unregisterAllServices() */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void unregisterAllServices() {
		Log.d(TAG, "unregisterAllServices()");
		if (services.size() == 0) {
			return;
		}
		Collection list;
		synchronized (this) {
			list = new LinkedList(services.values());
			services.clear();
		}
		for (Iterator iterator = list.iterator(); iterator.hasNext();) {
			((ServiceInfoImpl) iterator.next()).cancel();
		}
		Object lock = new Object();
		new Canceler(this, list, lock).start(timer);
		// Remind: We get a livelock here, if the Canceler does not run!
		try {
			synchronized (lock) {
				if (!closed) {
					lock.wait();
				}
			}
		} catch (InterruptedException e) {
			// empty
		}
	}

	/** @see javax.jmdns.JmDNS#registerServiceType(java.lang.String) */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void registerServiceType(String type) {
		String name = type.toLowerCase();
		if (serviceTypes.get(name) == null) {
			if ((type.indexOf("._mdns._udp.") < 0)
					&& !type.endsWith(".in-addr.arpa.")) {
				Collection list;
				synchronized (this) {
					serviceTypes.put(name, type);
					list = new LinkedList(typeListeners);
				}
				for (Iterator iterator = list.iterator(); iterator.hasNext();) {
					((ServiceTypeListener) iterator.next())
							.serviceTypeAdded(new ServiceEventImpl(this, type,
									null, null));
				}
			}
		}
	}

	/** Generate a possibly unique name for a service using the information we
	 * have in the cache.
	 * @return returns true, if the name of the service info had to be changed. */
	private boolean makeServiceNameUnique(ServiceInfoImpl info) {
		String originalQualifiedName = info.getQualifiedName();
		long now = System.currentTimeMillis();
		boolean collision;
		do {
			collision = false;
			// Check for collision in cache
			for (DNSCache.CacheNode j = cache.find(info.getQualifiedName()
					.toLowerCase()); j != null; j = j.next()) {
				DNSRecord a = (DNSRecord) j.getValue();
				if ((a.type == DNSConstants.TYPE_SRV) && !a.isExpired(now)) {
					DNSRecord.Service s = (DNSRecord.Service) a;
					if (s.port != info.port
							|| !s.server.equals(localHost.getName())) {
						Log.d(TAG,
								"makeServiceNameUnique() JmDNS.makeServiceNameUnique srv collision:"
										+ a
										+ " s.server="
										+ s.server
										+ " "
										+ localHost.getName()
										+ " equals:"
										+ (s.server.equals(localHost.getName())));
						info.setName(incrementName(info.getName()));
						collision = true;
						break;
					}
				}
			}
			// Check for collision with other service infos published by JmDNS
			Object selfService = services.get(info.getQualifiedName()
					.toLowerCase());
			if (selfService != null && selfService != info) {
				info.setName(incrementName(info.getName()));
				collision = true;
			}
		} while (collision);
		return !(originalQualifiedName.equals(info.getQualifiedName()));
	}

	String incrementName(String name) {
		try {
			int l = name.lastIndexOf('(');
			int r = name.lastIndexOf(')');
			if ((l >= 0) && (l < r)) {
				name = name.substring(0, l) + "("
						+ (Integer.parseInt(name.substring(l + 1, r)) + 1)
						+ ")";
			} else {
				name += " (2)";
			}
		} catch (NumberFormatException e) {
			name += " (2)";
		}
		return name;
	}

	/** Add a listener for a question. The listener will receive updates of
	 * answers to the question as they arrive, or from the cache if they are
	 * already available. */
	@SuppressWarnings("unchecked")
	public void addListener(DNSListener listener, DNSQuestion question) {
		long now = System.currentTimeMillis();
		// add the new listener
		synchronized (this) {
			listeners.add(listener);
		}
		// report existing matched records
		if (question != null) {
			for (DNSCache.CacheNode i = cache.find(question.name); i != null; i = i
					.next()) {
				DNSRecord c = (DNSRecord) i.getValue();
				if (question.answeredBy(c) && !c.isExpired(now)) {
					listener.updateRecord(this, now, c);
				}
			}
		}
	}

	/** Remove a listener from all outstanding questions. The listener will no
	 * longer receive any updates. */
	public void removeListener(DNSListener listener) {
		synchronized (this) {
			listeners.remove(listener);
		}
	}

	// Remind: Method updateRecord should receive a better name.
	/** Notify all listeners that a record was updated. */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void updateRecord(long now, DNSRecord rec) {
		// We do not want to block the entire DNS while we are updating the
		// record
		// for each listener (service info)
		List listenerList = null;
		synchronized (this) {
			listenerList = new ArrayList(listeners);
		}
		for (Iterator iterator = listenerList.iterator(); iterator.hasNext();) {
			DNSListener listener = (DNSListener) iterator.next();
			listener.updateRecord(this, now, rec);
		}
		if (rec.type == DNSConstants.TYPE_PTR
				|| rec.type == DNSConstants.TYPE_SRV) {
			List serviceListenerList = null;
			synchronized (this) {
				serviceListenerList = (List) serviceListeners.get(rec.name
						.toLowerCase());
				// Iterate on a copy in case listeners will modify it
				if (serviceListenerList != null) {
					serviceListenerList = new ArrayList(serviceListenerList);
				}
			}
			if (serviceListenerList != null) {
				boolean expired = rec.isExpired(now);
				String type = rec.getName();
				String name = ((DNSRecord.Pointer) rec).getAlias();
				// DNSRecord old = (DNSRecord)services.get(name.toLowerCase());
				if (!expired) {
					// new record
					ServiceEvent event = new ServiceEventImpl(this, type,
							toUnqualifiedName(type, name), null);
					for (Iterator iterator = serviceListenerList.iterator(); iterator
							.hasNext();) {
						((ServiceListener) iterator.next()).serviceAdded(event);
					}
				} else {
					// expire record
					ServiceEvent event = new ServiceEventImpl(this, type,
							toUnqualifiedName(type, name), null);
					for (Iterator iterator = serviceListenerList.iterator(); iterator
							.hasNext();) {
						((ServiceListener) iterator.next())
								.serviceRemoved(event);
					}
				}
			}
		}
	}

	/** Handle an incoming response. Cache answers, and pass them on to the
	 * appropriate questions. */
	@SuppressWarnings("rawtypes")
	void handleResponse(DNSIncoming msg) throws IOException {
		long now = System.currentTimeMillis();
		boolean hostConflictDetected = false;
		boolean serviceConflictDetected = false;
		for (Iterator i = msg.answers.iterator(); i.hasNext();) {
			boolean isInformative = false;
			DNSRecord rec = (DNSRecord) i.next();
			// Log.d(TAG, String.format("handleResponse rec=%s",
			// rec.toString()));
			boolean expired = rec.isExpired(now);
			// update the cache
			DNSRecord c = (DNSRecord) cache.get(rec);
			if (c != null) {
				if (expired) {
					isInformative = true;
					cache.remove(c);
				} else {
					c.resetTTL(rec);
					rec = c;
				}
			} else {
				if (!expired) {
					isInformative = true;
					cache.add(rec);
				}
			}
			switch (rec.type) {
			case DNSConstants.TYPE_PTR:
				// handle _mdns._udp records
				if (rec.getName().indexOf("._mdns._udp.") >= 0) {
					if (!expired
							&& rec.name.startsWith("_services._mdns._udp.")) {
						isInformative = true;
						registerServiceType(((DNSRecord.Pointer) rec).alias);
					}
					continue;
				}
				registerServiceType(rec.name);
				break;
			}
			if ((rec.getType() == DNSConstants.TYPE_A)
					|| (rec.getType() == DNSConstants.TYPE_AAAA)) {
				hostConflictDetected |= rec.handleResponse(this);
			} else {
				serviceConflictDetected |= rec.handleResponse(this);
			}
			// notify the listeners
			if (isInformative) {
				updateRecord(now, rec);
			}
		}
		if (hostConflictDetected || serviceConflictDetected) {
			new Prober(this).start(timer);
		}
	}

	/** Handle an incoming query. See if we can answer any part of it given our
	 * service infos. */
	@SuppressWarnings("rawtypes")
	void handleQuery(DNSIncoming in, InetAddress addr, int port)
			throws IOException {
		// Track known answers
		boolean hostConflictDetected = false;
		boolean serviceConflictDetected = false;
		long expirationTime = System.currentTimeMillis()
				+ DNSConstants.KNOWN_ANSWER_TTL;
		for (Iterator i = in.answers.iterator(); i.hasNext();) {
			DNSRecord answer = (DNSRecord) i.next();
			if ((answer.getType() == DNSConstants.TYPE_A)
					|| (answer.getType() == DNSConstants.TYPE_AAAA)) {
				Log.d(TAG, String.format("someone asked for A or AAAA host=%s",
						answer.getName()));
				hostConflictDetected |= answer
						.handleQuery(this, expirationTime);
			} else {
				serviceConflictDetected |= answer.handleQuery(this,
						expirationTime);
			}
		}
		if (plannedAnswer != null) {
			plannedAnswer.append(in);
		} else {
			if (in.isTruncated()) {
				plannedAnswer = in;
			}
			new Responder(this, in, addr, port).start();
		}
		if (hostConflictDetected || serviceConflictDetected) {
			new Prober(this).start(timer);
		}
	}

	/** Add an answer to a question. Deal with the case when the outgoing packet
	 * overflows */
	public DNSOutgoing addAnswer(DNSIncoming in, InetAddress addr, int port,
			DNSOutgoing out, DNSRecord rec) throws IOException {
		if (out == null) {
			out = new DNSOutgoing(DNSConstants.FLAGS_QR_RESPONSE
					| DNSConstants.FLAGS_AA);
		}
		try {
			out.addAnswer(in, rec);
		} catch (IOException e) {
			out.flags |= DNSConstants.FLAGS_TC;
			out.id = in.id;
			out.finish();
			send(out);
			out = new DNSOutgoing(DNSConstants.FLAGS_QR_RESPONSE
					| DNSConstants.FLAGS_AA);
			out.addAnswer(in, rec);
		}
		return out;
	}

	/** Send an outgoing multicast DNS message. */
	public void send(DNSOutgoing out) throws IOException {
		out.finish();
		if (!out.isEmpty()) {
			DatagramPacket packet = new DatagramPacket(out.data, out.off,
					group, DNSConstants.MDNS_PORT);
			try {
				new DNSIncoming(packet);
			} catch (IOException e) {
				Log.w(TAG,
						"send(DNSOutgoing) - JmDNS can not parse what it sends!!!",
						e);
			}
			socket.send(packet);
		}
	}

	public void startAnnouncer() {
		new Announcer(this).start(timer);
	}

	public void startRenewer() {
		new Renewer(this).start(timer);
	}

	public void schedule(TimerTask task, int delay) {
		timer.schedule(task, delay);
	}

	// REMIND: Why is this not an anonymous inner class?
	/** Shutdown operations. */
	private class Shutdown implements Runnable {
		public void run() {
			shutdown = null;
			close();
		}
	}

	/** Recover jmdns when there is an error. */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void recover() {
		Log.d(TAG, "recover()");
		// We have an IO error so lets try to recover if anything happens lets
		// close it.
		// This should cover the case of the IP address changing under our feet
		if (DNSState.CANCELED != getState()) {
			synchronized (this) { // Synchronize only if we are not already in
									// process to prevent dead locks
				//
				Log.d(TAG, "recover() Cleanning up");
				// Stop JmDNS
				setState(DNSState.CANCELED); // This protects against recursive
												// calls
				// We need to keep a copy for reregistration
				Collection oldServiceInfos = new ArrayList(getServices()
						.values());
				// Cancel all services
				unregisterAllServices();
				disposeServiceCollectors();
				//
				// close multicast socket
				closeMulticastSocket();
				//
				cache.clear();
				Log.d(TAG, "recover() All is clean");
				//
				// All is clear now start the services
				//
				try {
					openMulticastSocket(getLocalHost());
					start(oldServiceInfos);
				} catch (Exception exception) {
					Log.w(TAG, "recover() Start services exception ", exception);
				}
				Log.i(TAG, "recover() We are back!");
			}
		}
	}

	/** @see javax.jmdns.JmDNS#close() */
	public void close() {
		if (getState() != DNSState.CANCELED) {
			synchronized (this) { // Synchronize only if we are not already in
									// process to prevent dead locks
				// Stop JmDNS
				setState(DNSState.CANCELED); // This protects against recursive
												// calls
				unregisterAllServices();
				disposeServiceCollectors();
				// close socket
				closeMulticastSocket();
				// Stop the timer
				timer.cancel();
				// remove the shutdown hook
				if (shutdown != null) {
					Runtime.getRuntime().removeShutdownHook(shutdown);
				}
			}
		}
	}

	/** List cache entries, for debugging only. */
	void print() {
		System.out.println("---- cache ----");
		cache.print();
		System.out.println();
	}

	/** @see javax.jmdns.JmDNS#printServices() */
	public void printServices() {
		Log.e(TAG, toString());
	}

	@SuppressWarnings("rawtypes")
	public String toString() {
		StringBuffer aLog = new StringBuffer();
		aLog.append("\t---- Services -----");
		if (services != null) {
			for (Iterator k = services.keySet().iterator(); k.hasNext();) {
				Object key = k.next();
				aLog.append("\n\t\tService: " + key + ": " + services.get(key));
			}
		}
		aLog.append("\n");
		aLog.append("\t---- Types ----");
		if (serviceTypes != null) {
			for (Iterator k = serviceTypes.keySet().iterator(); k.hasNext();) {
				Object key = k.next();
				aLog.append("\n\t\tType: " + key + ": " + serviceTypes.get(key));
			}
		}
		aLog.append("\n");
		aLog.append(cache.toString());
		aLog.append("\n");
		aLog.append("\t---- Service Collectors ----");
		if (serviceCollectors != null) {
			synchronized (serviceCollectors) {
				for (Iterator k = serviceCollectors.keySet().iterator(); k
						.hasNext();) {
					Object key = k.next();
					aLog.append("\n\t\tService Collector: " + key + ": "
							+ serviceCollectors.get(key));
				}
				serviceCollectors.clear();
			}
		}
		return aLog.toString();
	}

	/** @see javax.jmdns.JmDNS#list(java.lang.String) */
	@SuppressWarnings("unchecked")
	public ServiceInfo[] list(String type) {
		// Implementation note: The first time a list for a given type is
		// requested, a ServiceCollector is created which collects service
		// infos. This greatly speeds up the performance of subsequent calls
		// to this method. The caveats are, that 1) the first call to this
		// method
		// for a given type is slow, and 2) we spawn a ServiceCollector
		// instance for each service type which increases network traffic a
		// little.
		ServiceCollector collector;
		boolean newCollectorCreated;
		synchronized (serviceCollectors) {
			collector = (ServiceCollector) serviceCollectors.get(type);
			if (collector == null) {
				collector = new ServiceCollector(type);
				serviceCollectors.put(type, collector);
				addServiceListener(type, collector);
				newCollectorCreated = true;
			} else {
				newCollectorCreated = false;
			}
		}
		// After creating a new ServiceCollector, we collect service infos for
		// 200 milliseconds. This should be enough time, to get some service
		// infos from the network.
		if (newCollectorCreated) {
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
			}
		}
		return collector.list();
	}

	/** This method disposes all ServiceCollector instances which have been
	 * created by calls to method <code>list(type)</code>.
	 * @see #list */
	@SuppressWarnings("rawtypes")
	private void disposeServiceCollectors() {
		Log.d(TAG, "disposeServiceCollectors()");
		synchronized (serviceCollectors) {
			for (Iterator i = serviceCollectors.values().iterator(); i
					.hasNext();) {
				ServiceCollector collector = (ServiceCollector) i.next();
				removeServiceListener(collector.type, collector);
			}
			serviceCollectors.clear();
		}
	}

	/** Instances of ServiceCollector are used internally to speed up the
	 * performance of method <code>list(type)</code>.
	 * @see #list */
	private static class ServiceCollector implements ServiceListener {
		/** A set of collected service instance names. */
		@SuppressWarnings({ "unchecked", "rawtypes" })
		private Map infos = Collections.synchronizedMap(new HashMap());
		public String type;

		public ServiceCollector(String type) {
			this.type = type;
		}

		/** A service has been added. */
		public void serviceAdded(ServiceEvent event) {
			synchronized (infos) {
				event.getDNS().requestServiceInfo(event.getType(),
						event.getName(), 0);
			}
		}

		/** A service has been removed. */
		public void serviceRemoved(ServiceEvent event) {
			synchronized (infos) {
				infos.remove(event.getName());
			}
		}

		/** A service hase been resolved. Its details are now available in the
		 * ServiceInfo record. */
		@SuppressWarnings("unchecked")
		public void serviceResolved(ServiceEvent event) {
			synchronized (infos) {
				infos.put(event.getName(), event.getInfo());
			}
		}

		/** Returns an array of all service infos which have been collected by
		 * this ServiceCollector. */
		@SuppressWarnings("unchecked")
		public ServiceInfoImpl[] list() {
			synchronized (infos) {
				return (ServiceInfoImpl[]) infos.values().toArray(
						new ServiceInfoImpl[infos.size()]);
			}
		}

		@SuppressWarnings("rawtypes")
		public String toString() {
			StringBuffer aLog = new StringBuffer();
			synchronized (infos) {
				for (Iterator k = infos.keySet().iterator(); k.hasNext();) {
					Object key = k.next();
					aLog.append("\n\t\tService: " + key + ": " + infos.get(key));
				}
			}
			return aLog.toString();
		}
	};

	private static String toUnqualifiedName(String type, String qualifiedName) {
		if (qualifiedName.endsWith(type)) {
			return qualifiedName.substring(0,
					qualifiedName.length() - type.length() - 1);
		} else {
			return qualifiedName;
		}
	}

	public void setState(DNSState state) {
		this.state = state;
	}

	public void setTask(TimerTask task) {
		this.task = task;
	}

	public TimerTask getTask() {
		return task;
	}

	@SuppressWarnings("rawtypes")
	public Map getServices() {
		return services;
	}

	public void setLastThrottleIncrement(long lastThrottleIncrement) {
		this.lastThrottleIncrement = lastThrottleIncrement;
	}

	public long getLastThrottleIncrement() {
		return lastThrottleIncrement;
	}

	public void setThrottle(int throttle) {
		this.throttle = throttle;
	}

	public int getThrottle() {
		return throttle;
	}

	public static Random getRandom() {
		return random;
	}

	public void setIoLock(Object ioLock) {
		this.ioLock = ioLock;
	}

	public Object getIoLock() {
		return ioLock;
	}

	public void setPlannedAnswer(DNSIncoming plannedAnswer) {
		this.plannedAnswer = plannedAnswer;
	}

	public DNSIncoming getPlannedAnswer() {
		return plannedAnswer;
	}

	void setLocalHost(HostInfo localHost) {
		this.localHost = localHost;
	}

	@SuppressWarnings("rawtypes")
	public Map getServiceTypes() {
		return serviceTypes;
	}

	public void setClosed(boolean closed) {
		this.closed = closed;
	}

	public boolean isClosed() {
		return closed;
	}

	public MulticastSocket getSocket() {
		return socket;
	}

	public InetAddress getGroup() {
		return group;
	}
}