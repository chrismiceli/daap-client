package org.mult.daap.db.dao;

import org.mult.daap.db.entity.HistoryEntity;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface HistoryDao {
    @Query("SELECT * FROM history LIMIT 1")
    HistoryEntity getPreviousEntry();

    @Delete
    void deleteEntry(HistoryEntity historyEntity);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void addEntry(HistoryEntity song);

    @Query("DELETE FROM history WHERE history.entryId NOT IN (SELECT entryId FROM history LIMIT 100)")
    void removeOldEntries();

    @Query("DELETE FROM history")
    void clearHistory();
}
