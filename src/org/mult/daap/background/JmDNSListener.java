package org.mult.daap.background;

import java.io.IOException;
import java.net.InetAddress;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class JmDNSListener {
	private JmDNS jmdns = null;
	private Handler handler = null;

	private class Lookup extends Thread {
		private final ServiceEvent e;

		public Lookup(final ServiceEvent e) {
			this.e = e;
		}

		public void run() {
			ServiceInfo si = jmdns.getServiceInfo(e.getType(), e.getName());
			Bundle bundle = new Bundle();
			bundle.putString("name", si.getName());
			bundle.putString("address",
					si.getHostAddress() + ":" + si.getPort());
			Message msg = Message.obtain();
			msg.setTarget(handler);
			msg.setData(bundle);
			handler.sendMessage(msg);
		}
	}

	public JmDNSListener(Handler handler, InetAddress wifi) {
		try {
			this.handler = handler;
			jmdns = JmDNS.create(wifi);
			jmdns.addServiceListener("_daap._tcp.local.",
					new ServiceListener() {
						public void serviceResolved(ServiceEvent arg0) {
						}

						public void serviceRemoved(ServiceEvent arg0) {
						}

						public void serviceAdded(ServiceEvent e) {
							Lookup l = new Lookup(e);
							l.start();
						}
					});
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NoSuchMethodError e) {
			e.printStackTrace();
		}
	}

	public void interrupt() {
		if (jmdns != null) {
			jmdns.close();
		}
	}
}