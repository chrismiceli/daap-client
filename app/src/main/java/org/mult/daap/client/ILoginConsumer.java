package org.mult.daap.client;

import org.mult.daap.db.entity.ServerEntity;

public interface ILoginConsumer {
    boolean isFinishing();
    void onBeforeLogin();
    void onAfterLogin(int result);
}
