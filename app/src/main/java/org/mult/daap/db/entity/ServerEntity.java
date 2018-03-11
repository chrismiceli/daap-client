package org.mult.daap.db.entity;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

import org.mult.daap.model.Server;

@Entity(tableName = "servers")
public class ServerEntity implements Server {
    @PrimaryKey
    @NonNull
    private String address;

    private String password;

    public String getAddress() {
        return address;
    }

    public String getPassword() {
        return password;
    }

    public ServerEntity(String address, String password) {
        this.address = address;
        this.password = password;
    }
}