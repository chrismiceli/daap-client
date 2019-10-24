package org.mult.daap.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import org.mult.daap.db.entity.ServerEntity;

@Dao
public interface ServerDao {
    @Query("SELECT * FROM servers LIMIT 1")
    ServerEntity loadServer();

    /**
     * Sets the single DAAP server to use for the app.
     * This removes any existing saved DAAP server
     *
     * @param server the server object to store
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void setDaapServer(ServerEntity server);
}
