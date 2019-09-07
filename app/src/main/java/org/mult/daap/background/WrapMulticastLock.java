package org.mult.daap.background;

import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;

public class WrapMulticastLock {
    private final WrapMulticastLock instance;

    private WrapMulticastLock() {
        instance = null;
    }

    public void acquire() {
    }

    public void release() {
    }

    public WrapMulticastLock(WifiManager wifiManager) {
        instance = new New(wifiManager);
    }

    public WrapMulticastLock getInstance() {
        return instance;
    }

    private class New extends WrapMulticastLock {
        private MulticastLock mInstance;

        New(WifiManager wifiManager) {
            try {
                mInstance = wifiManager.createMulticastLock("myLock");
            } catch (Exception ignored) {
            }
        }

        @Override
        public void acquire() {
            try {
                mInstance.acquire();
            } catch (Exception ignored) {
            }
        }

        @Override
        public void release() {
            try {
                mInstance.release();
            } catch (Exception ignored) {
            }
        }
    }
}