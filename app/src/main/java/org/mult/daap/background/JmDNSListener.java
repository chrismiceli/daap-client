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

public class JmDNSListener extends Thread {
	private final Handler handler;
	private final InetAddress wifi;
	private JmDNS jmdns;

	public JmDNSListener(Handler handler, InetAddress wifi) {
	    this.handler = handler;
	    this.wifi = wifi;
    }

    public void Run() {
		try {
			jmdns = JmDNS.create(wifi);
			jmdns.addServiceListener("_daap._tcp.local.",
					new ServiceListener() {
						public void serviceResolved(ServiceEvent arg0) {
						}

						public void serviceRemoved(ServiceEvent arg0) {
						}

						public void serviceAdded(ServiceEvent serviceEvent) {
							addServer(serviceEvent);
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

	private void addServer(ServiceEvent serviceEvent) {
		ServiceInfo si = jmdns.getServiceInfo(serviceEvent.getType(), serviceEvent.getName());
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