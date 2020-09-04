package org.mult.daap.background;

import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.os.Build;

public class WrapMulticastLock {
    public final WrapMulticastLock instance;

    public WrapMulticastLock() {
        instance = null;
    }

    public void acquire() {
    }

    public void release() {
    }

    public WrapMulticastLock(WifiManager wifim) {
        instance = Integer.parseInt(Build.VERSION.SDK) < 4 ? new Old(wifim)
                : new New(wifim);
    }

    public WrapMulticastLock getInstance() {
        return instance;
    }

    private class Old extends WrapMulticastLock {
        public Old(WifiManager wifim) {
        }

        @Override
        public void acquire() {
        }

        @Override
        public void release() {
        }
    }

    private class New extends WrapMulticastLock {
        private MulticastLock mInstance;

        public New(WifiManager wifim) {
            try {
                mInstance = wifim.createMulticastLock("mylock");
            } catch (Exception e) {
            }
        }

        @Override
        public void acquire() {
            try {
                mInstance.acquire();
            } catch (Exception e) {
            }
        }

        @Override
        public void release() {
            try {
                mInstance.release();
            } catch (Exception e) {
            }
        }
    }
}