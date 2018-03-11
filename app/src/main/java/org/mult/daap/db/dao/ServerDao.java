package org.mult.daap.db.dao;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.OnConflictStrategy;

import java.util.List;

import org.mult.daap.db.entity.ServerEntity;

@Dao
public interface ServerDao {
    @Query("SELECT * FROM servers")
    LiveData<List<ServerEntity>> loadAllServers();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ServerEntity> servers);
}
