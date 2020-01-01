package org.mult.daap.client;

import org.mult.daap.db.entity.SongEntity;

import java.util.List;

public interface IQueueWorker {
    void songsAddedToQueue(List<SongEntity> songEntity);

    void songsRemovedFromQueue();
}
