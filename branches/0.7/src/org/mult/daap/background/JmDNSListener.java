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

public class JmDNSListener implements ServiceListener {

   private JmDNS jmdns = null;
   private Handler handler = null;

   public JmDNSListener(Handler handler) {
      try {
         InetAddress addr = InetAddress.getLocalHost();
         this.handler = handler;
         jmdns = new JmDNS(addr);
         jmdns.addServiceListener("_daap._tcp.local.", this);

      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   private class resolveThread extends Thread {
      private ServiceEvent event;

      public resolveThread(ServiceEvent e) {
         event = e;
      }

      @Override
      public void run() {
         jmdns.requestServiceInfo(event.getType(), event.getName());
      }
   }

   public void serviceAdded(ServiceEvent event) {
      (new resolveThread(event)).start();
   }

   public void serviceRemoved(ServiceEvent event) {
   }

   public void serviceResolved(ServiceEvent event) {
      if (event.getType().equals("_daap._tcp.local.") == false) {
         return;
      }
      ServiceInfo info = event.getInfo();
      if (info == null) {
         return;
      } else {
         Bundle bundle = new Bundle();
         bundle.putString("name", info.getName());
         bundle.putString("address", info.getHostAddress() + ":"
               + info.getPort());
         Message msg = Message.obtain();
         msg.setTarget(handler);
         msg.setData(bundle);
         handler.sendMessage(msg);
      }
   }
}
