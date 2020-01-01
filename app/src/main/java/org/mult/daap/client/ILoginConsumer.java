package org.mult.daap.client;

public interface ILoginConsumer {
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    boolean isFinishing();
    void onBeforeLogin();
    void onAfterLogin(int result);
}
