package org.mult.daap.db.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

import org.mult.daap.model.Server;

@Entity(tableName = "servers")
public class ServerEntity implements Server {
    @PrimaryKey
    @NonNull
    private final String address;

    private final String password;

    public ServerEntity(@NonNull String address, String password) {
        this.address = address;
        this.password = password;
    }

    @NonNull
    public String getAddress() {
        return address;
    }

    public String getPassword() {
        return password;
    }
}