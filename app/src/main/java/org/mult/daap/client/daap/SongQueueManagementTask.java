package org.mult.daap.client.daap;

import android.content.Context;
import android.os.AsyncTask;

import org.mult.daap.client.IQueueWorker;
import org.mult.daap.db.AppDatabase;
import org.mult.daap.db.dao.PlaylistDao;
import org.mult.daap.db.dao.QueueDao;
import org.mult.daap.db.dao.SongDao;
import org.mult.daap.db.entity.QueueEntity;
import org.mult.daap.db.entity.SongEntity;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class SongQueueManagementTask extends AsyncTask<Void, Void, SongQueueManagementTask.SongQueueResult> {
    private final WeakReference<Context> contextWeakReference;
    private final WeakReference<IQueueWorker> queueWorkerWeakReference;
    private final SongEntity songEntity;
    private final String queueObject;
    private final ObjectTypeToQueue objectType;
    private final SongQueueCommand command;
    private final int playlistId;
    private List<SongEntity> songsAddedOrRemovedFromQueue;

    public enum SongQueueResult {
        ADDED, REMOVED
    }

    public enum SongQueueCommand {
        PUSH, UNSHIFT, TOGGLE
    }

    public enum ObjectTypeToQueue {
        SONG, ALBUM, ARTIST
    }

    public SongQueueManagementTask(Context applicationContext, IQueueWorker queueWorker, SongEntity songEntity, SongQueueCommand command) {
        this.contextWeakReference = new WeakReference<>(applicationContext);
        this.queueWorkerWeakReference = new WeakReference<>(queueWorker);
        this.songEntity = songEntity;
        this.command = command;
        this.queueObject = null;
        this.playlistId = -1;
        this.objectType = ObjectTypeToQueue.SONG;

    }

    public SongQueueManagementTask(Context applicationContext, IQueueWorker queueWorker, int playlistId, String queueObject, ObjectTypeToQueue objectType) {
        this.contextWeakReference = new WeakReference<>(applicationContext);
        this.queueWorkerWeakReference = new WeakReference<>(queueWorker);
        this.songEntity = null;
        this.command = SongQueueCommand.PUSH;
        this.objectType = objectType;
        this.queueObject = queueObject;
        this.playlistId = playlistId;
    }

    @Override
    protected SongQueueResult doInBackground(Void... voids) {
        Context applicationContext = this.contextWeakReference.get();
        if (null == applicationContext) {
            return null;
        }

        this.songsAddedOrRemovedFromQueue = new ArrayList<>();
        if (this.songEntity != null) {
            songsAddedOrRemovedFromQueue.add(this.songEntity);
        }

        SongDao songDao = AppDatabase.getInstance(applicationContext).songDao();

        if (queueObject != null && !queueObject.isEmpty()) {
            if (this.objectType == ObjectTypeToQueue.ALBUM) {
                List<SongEntity> albumSongs;
                if (this.playlistId != -1) {
                    PlaylistDao playlistDao = AppDatabase.getInstance(applicationContext).playlistDao();
                    albumSongs = playlistDao.loadAlbumSongsForPlaylist(this.playlistId, this.queueObject);
                } else {
                    albumSongs = songDao.loadAlbumSongs(this.queueObject);
                }

                for (SongEntity s : albumSongs) {
                    this.songsAddedOrRemovedFromQueue.add(s);
                }
            } else if (this.objectType == ObjectTypeToQueue.ARTIST) {
                List<SongEntity> artistSongs;
                if (this.playlistId != -1) {
                    PlaylistDao playlistDao = AppDatabase.getInstance(applicationContext).playlistDao();
                    artistSongs = playlistDao.loadArtistSongsForPlaylist(this.playlistId, this.queueObject);
                } else {
                    artistSongs = songDao.loadAlbumSongs(this.queueObject);
                }

                for (SongEntity s : artistSongs) {
                    this.songsAddedOrRemovedFromQueue.add(s);
                }
            }
        }

        QueueDao queueDao = AppDatabase.getInstance(applicationContext).queueDao();
        SongQueueResult songQueueResult = SongQueueResult.ADDED;
        for (SongEntity song : this.songsAddedOrRemovedFromQueue) {
            // if toggling and the song is in the queue, then remove the song and continue
            // otherwise, add the song to the end of the queue
            if (this.command == SongQueueCommand.TOGGLE) {
                // should only be able to toggle queuing a specific song and not
                // an entire album/artist
                QueueEntity songInQueue = queueDao.getSongQueueId(song.id);
                if (songInQueue != null) {
                    songQueueResult = SongQueueResult.REMOVED;
                    queueDao.remove(songInQueue);
                    continue;
                }
            }

            int order;
            if (this.command == SongQueueCommand.UNSHIFT) {
                // push the song to the front of the queue, get smallest order
                order = queueDao.getOrderForFrontOfQueue() - 1;
            } else {
                // push the song to the back of the queue, get largest order
                order = queueDao.getOrderForBackOfQueue() + 1;
            }

            queueDao.add(new QueueEntity(song.id, order));
        }

        return songQueueResult;
    }

    @Override
    protected void onPostExecute(SongQueueResult songQueueResult) {
        IQueueWorker queueWorker = this.queueWorkerWeakReference.get();
        if (null != queueWorker && null != songQueueResult) {
            if (songQueueResult == SongQueueResult.ADDED) {
                queueWorker.songsAddedToQueue(this.songsAddedOrRemovedFromQueue);
            } else if (songQueueResult == SongQueueResult.REMOVED) {
                queueWorker.songsRemovedFromQueue(this.songsAddedOrRemovedFromQueue);
            }
        }
    }
}
