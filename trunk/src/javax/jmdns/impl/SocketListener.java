// Copyright 2003-2005 Arthur van Hoff, Rick Blair
// Licensed under Apache License version 2.0
// Original license LGPL

package javax.jmdns.impl;

import java.io.IOException;
import java.net.DatagramPacket;

/**
 * Listen for multicast packets.
 */
class SocketListener implements Runnable {
   public final static String TAG = SocketListener.class.toString();

   /**
     * 
     */
   private final JmDNSImpl jmDNSImpl;

   /**
    * @param jmDNSImpl
    */
   SocketListener(JmDNSImpl jmDNSImpl) {
      this.jmDNSImpl = jmDNSImpl;
   }

   public void run() {
      try {
         byte buf[] = new byte[DNSConstants.MAX_MSG_ABSOLUTE];
         DatagramPacket packet = new DatagramPacket(buf, buf.length);
         while (this.jmDNSImpl.getState() != DNSState.CANCELED) {
            packet.setLength(buf.length);
            this.jmDNSImpl.getSocket().receive(packet);
            if (this.jmDNSImpl.getState() == DNSState.CANCELED) {
               break;
            }
            try {
               if (this.jmDNSImpl.getLocalHost().shouldIgnorePacket(packet)) {
                  // Log.d(TAG, String.format("someone told me to ignore=%s",
                  // packet));
                  continue;
               }

               DNSIncoming msg = new DNSIncoming(packet);

               synchronized (this.jmDNSImpl.getIoLock()) {
                  if (msg.isQuery()) {
                     // Log.d(TAG, String.format("sending off request=%s",
                     // msg.toString()));
                     if (packet.getPort() != DNSConstants.MDNS_PORT) {
                        this.jmDNSImpl.handleQuery(msg, packet.getAddress(), packet.getPort());
                     }
                     this.jmDNSImpl.handleQuery(msg, this.jmDNSImpl.getGroup(), DNSConstants.MDNS_PORT);
                  } else {
                     // Log.d(TAG, String.format("sending off response=%s",
                     // msg.toString()));
                     this.jmDNSImpl.handleResponse(msg);
                  }
               }
            } catch (IOException e) {
               e.printStackTrace();
            }
         }
      } catch (IOException e) {
         if (this.jmDNSImpl.getState() != DNSState.CANCELED) {
            this.jmDNSImpl.recover();
         }
      }
   }
}