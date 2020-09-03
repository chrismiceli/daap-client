/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mult.daap.client.widget;

import org.mult.daap.MediaPlayback;
import org.mult.daap.MediaPlaybackService;
import org.mult.daap.R;
import org.mult.daap.Servers;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.view.View;
import android.widget.RemoteViews;

/**
 * Simple widget to show currently playing album art along
 * with play/pause and next track buttons.  
 */
public class DAAPClientAppWidgetOneProvider extends AppWidgetProvider {
    static final String TAG = DAAPClientAppWidgetOneProvider.class.getName();
    
    public static final String CMDAPPWIDGETUPDATE = "appwidgetupdate";

    private static DAAPClientAppWidgetOneProvider sInstance;
    
    public static synchronized DAAPClientAppWidgetOneProvider getInstance() {
        if (sInstance == null) {
            sInstance = new DAAPClientAppWidgetOneProvider();
        }
        return sInstance;
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        defaultAppWidget(context, appWidgetIds);
        
        // Send broadcast intent to any running MediaPlaybackService so it can
        // wrap around with an immediate update.
        Intent updateIntent = new Intent(MediaPlaybackService.SERVICECMD);
        updateIntent.putExtra(MediaPlaybackService.CMDNAME,
                DAAPClientAppWidgetOneProvider.CMDAPPWIDGETUPDATE);
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        updateIntent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
        context.sendBroadcast(updateIntent);
    }
    
    /**
     * Initialize given widgets to default state, where we launch ServeStream on default click
     * and hide actions if service not running.
     */
    private void defaultAppWidget(Context context, int[] appWidgetIds) {
        final Resources res = context.getResources();
        final RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.appwidget_one);
        
        views.setViewVisibility(R.id.title, View.GONE);
        views.setTextViewText(R.id.artist, res.getText(R.string.widget_one_initial_text));

        linkButtons(context, views, false /* not playing */);
        pushUpdate(context, appWidgetIds, views);
    }
    
    private void pushUpdate(Context context, int[] appWidgetIds, RemoteViews views) {
        // Update specific list of appWidgetIds if given, otherwise default to all
        final AppWidgetManager gm = AppWidgetManager.getInstance(context);
        if (appWidgetIds != null) {
            gm.updateAppWidget(appWidgetIds, views);
        } else {
            gm.updateAppWidget(new ComponentName(context, this.getClass()), views);
        }
    }
    
    /**
     * Check against {@link AppWidgetManager} if there are any instances of this widget.
     */
    private boolean hasInstances(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(
                new ComponentName(context, this.getClass()));
        return (appWidgetIds.length > 0);
    }

    /**
     * Handle a change notification coming over from {@link MediaService}
     */
    public void notifyChange(MediaPlaybackService service, MediaPlayback activity, String what) {
        if (service == null)
        	return;
        
    	if (hasInstances(service)) {
            if (MediaPlaybackService.META_CHANGED.equals(what) ||
            		MediaPlaybackService.PLAYSTATE_CHANGED.equals(what) ||
            		MediaPlaybackService.PLAYER_CLOSED.equals(what)) {
                performUpdate(service, activity, null, what);
            }
        }
    }
    
    /**
     * Update all active widget instances by pushing changes 
     */
    public void performUpdate(MediaPlaybackService service, MediaPlayback activity, int[] appWidgetIds, String what) {
    	final Resources res = service.getResources();
        final RemoteViews views = new RemoteViews(service.getPackageName(), R.layout.appwidget_one);
        
        if (what.equals(MediaPlaybackService.PLAYER_CLOSED)) {
        	views.setViewVisibility(R.id.title, View.GONE);
            views.setTextViewText(R.id.artist, res.getText(R.string.widget_one_initial_text));
            
            linkButtons(service, views, false /* not playing */);
        } else {
        	CharSequence trackName = activity.getTrackName();
        	CharSequence artistName = activity.getArtistName();
        	//CharSequence errorState = null;
        
        	if (trackName == null) {
        		if (trackName == null)
        			trackName = res.getText(R.string.widget_one_track_info_unavailable);
        	}
        		
        	if (artistName == null)
        		artistName = res.getText(R.string.widget_one_track_info_unavailable);
        	
        	// Show media info
        	views.setViewVisibility(R.id.title, View.VISIBLE);
        	views.setTextViewText(R.id.title, trackName);
        	views.setTextViewText(R.id.artist, artistName);
        	
            // Set correct drawable for pause state
            final boolean playing = activity.isPlaying();
            if (playing) {
                views.setImageViewResource(R.id.control_play, R.drawable.ic_appwidget_music_pause);
            } else {
                views.setImageViewResource(R.id.control_play, R.drawable.ic_appwidget_music_play);
            }
            
            // Link actions buttons to intents
            linkButtons(service, views, true);
        }
        
        pushUpdate(service, appWidgetIds, views);
    }
    
    /**
     * Link up various button actions using {@link PendingIntents}.
     * 
     * @param playerActive True if player is active in background, which means
     *            widget click will launch {@link StreamMediaActivity},
     *            otherwise we launch {@link StreamListActivity}.
     */
    private void linkButtons(Context context, RemoteViews views, boolean playerActive) {
        // Connect up various buttons and touch events
        Intent intent;
        PendingIntent pendingIntent;
        
        final ComponentName serviceName = new ComponentName(context, MediaPlaybackService.class);
        
        if (playerActive) {
            intent = new Intent(context, MediaPlayback.class);
            pendingIntent = PendingIntent.getActivity(context,
                    0 /* no requestCode */, intent, 0 /* no flags */);
            views.setOnClickPendingIntent(R.id.appwidget_one, pendingIntent);
        } else {
            intent = new Intent(context, Servers.class);
            pendingIntent = PendingIntent.getActivity(context,
                    0 /* no requestCode */, intent, 0 /* no flags */);
            views.setOnClickPendingIntent(R.id.appwidget_one, pendingIntent);
        }
        
		intent = new Intent(MediaPlaybackService.TOGGLEPAUSE_ACTION);
        intent.setComponent(serviceName);
        pendingIntent = PendingIntent.getService(context,
                0 /* no requestCode */, intent, 0 /* no flags */);
        views.setOnClickPendingIntent(R.id.control_play, pendingIntent);
        
        intent = new Intent(MediaPlaybackService.NEXT_ACTION);
        intent.setComponent(serviceName);
        pendingIntent = PendingIntent.getService(context,
                0 /* no requestCode */, intent, 0 /* no flags */);
        views.setOnClickPendingIntent(R.id.control_next, pendingIntent);
    }
}
