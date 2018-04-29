package org.mult.daap.db.dao;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;

import org.mult.daap.db.entity.ServerEntity;

@Dao
public interface ServerDao {
    @Query("SELECT * FROM servers")
    ServerEntity[] loadAllServers();

    /**
     * Sets the single DAAP server to use for the app.
     * This removes any existing saved DAAP server
     *
     * @param server the server object to store
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void setDaapServer(ServerEntity server);

    @Delete
    public void deleteServers(ServerEntity... users);
}
