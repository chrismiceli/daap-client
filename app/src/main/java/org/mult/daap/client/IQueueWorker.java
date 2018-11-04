package org.mult.daap.client;

import org.mult.daap.db.entity.SongEntity;

public interface IQueueWorker {
    void songAddedToTopOfQueue(SongEntity songEntity);
}
