package org.mult.daap.mediaplayback;

import android.os.Handler;
import android.os.Message;

import java.lang.ref.WeakReference;

class CopyHandler extends Handler {
    private final WeakReference<MediaPlaybackActivity> mediaPlaybackActivityWeakReference;

    CopyHandler(MediaPlaybackActivity mediaPlayback) {
        this.mediaPlaybackActivityWeakReference = new WeakReference<>(mediaPlayback);
    }

    @Override
    public void handleMessage(Message message) {
        MediaPlaybackActivity mediaPlaybackActivity = this.mediaPlaybackActivityWeakReference.get();
        if (mediaPlaybackActivity != null) {
            switch (message.what) {
                case MediaPlaybackActivity.REFRESH:
                    mediaPlaybackActivity.queueNextRefresh(mediaPlaybackActivity.refreshNow());
                    break;
                case MediaPlaybackActivity.COPYING_DIALOG:
                    mediaPlaybackActivity.dismissDialog(MediaPlaybackActivity.COPYING_DIALOG);
                    break;
                case MediaPlaybackActivity.SUCCESS_COPYING_DIALOG:
                    mediaPlaybackActivity.showDialog(MediaPlaybackActivity.SUCCESS_COPYING_DIALOG);
                    break;
                case MediaPlaybackActivity.ERROR_COPYING_DIALOG:
                    mediaPlaybackActivity.showDialog(MediaPlaybackActivity.ERROR_COPYING_DIALOG);
                    break;
            }
        }
    }
}
