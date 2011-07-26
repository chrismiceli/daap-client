/*
 * Created on Jan 21, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.mult.daap.client;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Greg
 * 
 *         TODO To change the template for this generated type comment go to
 *         Window - Preferences - Java - Code Style - Code Templates
 */
public abstract class BasicStatusObject {

	@SuppressWarnings("rawtypes")
	private List status_listeners = new ArrayList();
	private int status;

	@SuppressWarnings("unchecked")
	public void addStatusListener(StatusListener sl) {
		status_listeners.add(sl);
	}

	public void removeStatusListener(StatusListener sl) {
		status_listeners.remove(sl);
	}

	private void fireStatusChanged() {
		for (int i = 0; i < status_listeners.size(); i++) {
			((StatusListener) status_listeners.get(i)).stateUpdated(this);
		}
	}

	public int getStatus() {
		return status;
	}

	protected void setStatus(int i) {
		status = i;
		fireStatusChanged();
	}
}
