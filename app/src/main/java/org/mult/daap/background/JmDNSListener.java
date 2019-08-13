package org.mult.daap.background;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import java.io.IOException;
import java.net.InetAddress;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

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
                        @Override
                        public void serviceResolved(ServiceEvent arg0) {
                        }

                        @Override
                        public void serviceRemoved(ServiceEvent arg0) {
                        }

                        @Override
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
            try {
                jmdns.close();
            } catch (IOException e) {
                // todo
            }
        }
    }

    private void addServer(ServiceEvent serviceEvent) {
        ServiceInfo si = jmdns.getServiceInfo(serviceEvent.getType(), serviceEvent.getName());
        String[] addresses = si.getHostAddresses();
        if (null != addresses && addresses.length > 0) {
            Bundle bundle = new Bundle();
            bundle.putString("name", si.getName());
            bundle.putString("address",
                    addresses[0] + ":" + si.getPort());
            Message msg = Message.obtain();
            msg.setTarget(handler);
            msg.setData(bundle);
            handler.sendMessage(msg);
        }
    }
}