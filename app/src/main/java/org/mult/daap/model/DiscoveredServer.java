package org.mult.daap.model;

import org.mult.daap.db.entity.ServerEntity;

/**
 * Represents a DAAP server found on a LAN using multi-cast DNS
 */
public class DiscoveredServer extends ServerEntity {
    public DiscoveredServer(String name, String address) {
        super(address, null);
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    private String name;
}
