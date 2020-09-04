/*
 * Created on Aug 18, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.mult.daap.client;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Greg, for GIT
 */
public abstract class Playlist {
	@SuppressWarnings("rawtypes")
	protected ArrayList status_listeners = new ArrayList();

	public String name;
	public boolean all_songs;
	protected int status;

	public static final int STATUS_NOT_INITIALIZED = 0;
	public static final int STATUS_INITIALIZING = 1;
	public static final int STATUS_INITIALIZED = 2;

	public void initialize() throws Exception {
	}

	@SuppressWarnings("rawtypes")
	public abstract Collection getSongs();

	public int getStatus() {
		return status;
	}

	public void setStatus(int s) {
		status = s;
		fireStatusChanged();
	}

	public boolean contains(Object o) {
		if (o == null)
			return false;
		if (getSongs().contains(o))
			return true;
		else
			return false;
	}

	public String getName() {
		return name;
	}

	public String toString() {
		return name;
	}

	public String getToolTipText() {
		return name;
	}

	public void fireStatusChanged() {
		for (int i = 0; i < status_listeners.size(); i++) {
			StatusListener h = (StatusListener) status_listeners.get(i);
			h.stateUpdated(this);
		}
	}

	@SuppressWarnings("unchecked")
	public void addStatusListener(StatusListener sl) {
		status_listeners.add(sl);
	}

	public boolean removeStatusListener(StatusListener sl) {
		return status_listeners.remove(sl);
	}

}
