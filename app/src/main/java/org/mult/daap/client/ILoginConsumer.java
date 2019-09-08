package org.mult.daap.client;

public interface ILoginConsumer {
    boolean isFinishing();
    void onBeforeLogin();
    void onAfterLogin(int result);
}
