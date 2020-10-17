package org.mult.daap;

import android.os.Handler;
import android.os.Message;

import java.lang.ref.WeakReference;

class CopyHandler extends Handler {
    private final WeakReference<MediaPlayback> mediaPlaybackActivityWeakReference;

    CopyHandler(MediaPlayback mediaPlayback) {
        this.mediaPlaybackActivityWeakReference = new WeakReference<>(mediaPlayback);
    }

    @Override
    public void handleMessage(Message message) {
        MediaPlayback mediaPlayback = this.mediaPlaybackActivityWeakReference.get();
        if (mediaPlayback != null) {
            switch (message.what) {
                case MediaPlayback.REFRESH:
                    mediaPlayback.queueNextRefresh(mediaPlayback.refreshNow());
                    break;
                case MediaPlayback.COPYING_DIALOG:
                    mediaPlayback.dismissDialog(MediaPlayback.COPYING_DIALOG);
                    break;
                case MediaPlayback.SUCCESS_COPYING_DIALOG:
                    mediaPlayback.showDialog(MediaPlayback.SUCCESS_COPYING_DIALOG);
                    break;
                case MediaPlayback.ERROR_COPYING_DIALOG:
                    mediaPlayback.showDialog(MediaPlayback.ERROR_COPYING_DIALOG);
                    break;
            }
        }
    }
}