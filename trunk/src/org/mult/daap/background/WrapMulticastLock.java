package org.mult.daap.background;

import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;

public class WrapMulticastLock {
	private MulticastLock mInstance;
	private WifiManager wifiManager = null;

	public WrapMulticastLock(WifiManager wifim) {
		wifiManager = wifim;
		mInstance = wifiManager.createMulticastLock("mylock");
	}

	public void acquire() {
		mInstance.acquire();
	}

	public void release() {
		mInstance.release();
	}
}