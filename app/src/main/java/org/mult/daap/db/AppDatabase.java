package org.mult.daap.db;

import android.arch.lifecycle.MutableLiveData;
import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;
import android.support.annotation.VisibleForTesting;

import org.mult.daap.db.dao.PlaylistDao;
import org.mult.daap.db.dao.ServerDao;
import org.mult.daap.db.dao.SongDao;
import org.mult.daap.db.entity.PlaylistEntity;
import org.mult.daap.db.entity.PlaylistSongEntity;
import org.mult.daap.db.entity.ServerEntity;
import org.mult.daap.db.entity.SongEntity;

@Database(
        entities = {
            ServerEntity.class,
            SongEntity.class,
            PlaylistEntity.class,
            PlaylistSongEntity.class
        },
        version = 1,
        exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static AppDatabase sInstance;

    @VisibleForTesting
    private static final String DATABASE_NAME = "daap-db";

    public abstract ServerDao serverDao();

    public abstract SongDao songDao();

    public abstract PlaylistDao playlistDao();

    private final MutableLiveData<Boolean> mIsDatabaseCreated = new MutableLiveData<>();

    public static AppDatabase getInstance(final Context context) {
        if (sInstance == null) {
            synchronized (AppDatabase.class) {
                if (sInstance == null) {
                    sInstance = Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, DATABASE_NAME).build();
                    sInstance.updateDatabaseCreated(context.getApplicationContext());
                }
            }
        }
        return sInstance;
    }

    private void updateDatabaseCreated(final Context context) {
        if (context.getDatabasePath(DATABASE_NAME).exists()) {
            mIsDatabaseCreated.postValue(true);
        }
    }
}