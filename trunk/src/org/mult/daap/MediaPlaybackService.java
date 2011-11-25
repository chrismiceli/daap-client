package org.mult.daap;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.mult.daap.client.widget.DAAPClientAppWidgetOneProvider;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.media.AudioManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class MediaPlaybackService extends Service {
	private static final String TAG = MediaPlaybackService.class.getName();
	
    public static final String PLAYSTATE_CHANGED = "org.mult.daap.playstatechanged";
    public static final String META_CHANGED = "org.mult.daap.metachanged";
    public static final String PLAYER_CLOSED = "org.mult.daap.playerclosed";
	
    public static final String SERVICECMD = "org.mult.daap.mediaservicecommand";
    public static final String CMDNAME = "command";
    public static final String CMDTOGGLEPAUSE = "togglepause";
    public static final String CMDSTOP = "stop";
    public static final String CMDPAUSE = "pause";
    public static final String CMDPREVIOUS = "previous";
    public static final String CMDNEXT = "next";
	
    public static final String TOGGLEPAUSE_ACTION = "org.mult.daap.mediaservicecommand.togglepause";
    public static final String PAUSE_ACTION = "org.mult.daap.mediaservicecommand.pause";
    public static final String NEXT_ACTION = "org.mult.daap.mediaservicecommand.next";

    public static final String TOGGLEPAUSE = "togglepause";
    public static final String STOP = "stop";
    public static final String PAUSE = "pause";
    public static final String PREVIOUS = "previous";
    public static final String NEXT = "next";
    public static final String ADDED = "added";
    
    private int mServiceStartId = -1;
    private AudioManager mAudioManager;
    
    private static Method mRegisterMediaButtonEventReceiver;
    
    static {
        initializeRemoteControlRegistrationMethods();
    }
    
    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String cmd = intent.getStringExtra("command");
            Log.v(TAG, "mIntentReceiver.onReceive " + action + " / " + cmd);
            if (NEXT_ACTION.equals(action)) {
                notifyChange(NEXT);
            } else if (TOGGLEPAUSE_ACTION.equals(action)) {
                notifyChange(TOGGLEPAUSE);
            } else if (PAUSE_ACTION.equals(action)) {
                notifyChange(PAUSE);
            } else if (DAAPClientAppWidgetOneProvider.CMDAPPWIDGETUPDATE.equals(cmd)) {
                // Someone asked us to refresh a set of specific widgets, probably
                // because they were just added.
                notifyChange(ADDED);
            }
        }
    };
    
    /**
     * Default constructor
     */
    public MediaPlaybackService() {
    }

    public class LocalBinder extends Binder {
    	MediaPlaybackService getService() {
            return MediaPlaybackService.this;
        }
    }
    
    @Override
    public void onCreate() {
        super.onCreate();

        Log.v(TAG, "onCreate called");
        
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        registerRemoteControl();
        
        IntentFilter commandFilter = new IntentFilter();
        commandFilter.addAction(SERVICECMD);
        commandFilter.addAction(TOGGLEPAUSE_ACTION);
        commandFilter.addAction(PAUSE_ACTION);
        commandFilter.addAction(NEXT_ACTION);
        registerReceiver(mIntentReceiver, commandFilter);
    }

    @Override
    public void onDestroy() {
    	
    	Log.v(TAG, "onDestroy called");
        
        unregisterReceiver(mIntentReceiver);
		
        super.onDestroy();
    }
    
    private final IBinder mBinder = new LocalBinder();
    
    @Override
    public IBinder onBind(Intent intent) {
    	
		Log.v(TAG, "onBind called");
    	
		// Make sure we stay running
		startService(new Intent(this, MediaPlaybackService.class));
    	
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
    
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mServiceStartId = startId;

        if (intent != null) {
            String action = intent.getAction();
            String cmd = intent.getStringExtra("command");
            Log.v(TAG, "onStartCommand " + action + " / " + cmd);
            
            if (CMDNEXT.equals(cmd) || NEXT_ACTION.equals(action)) {
                notifyChange(NEXT);
            } else if (CMDPREVIOUS.equals(cmd)) {
                notifyChange(PREVIOUS);
            } else if (CMDTOGGLEPAUSE.equals(cmd) || TOGGLEPAUSE_ACTION.equals(action)) {
                notifyChange(TOGGLEPAUSE);
            } else if (CMDPAUSE.equals(cmd) || PAUSE_ACTION.equals(action)) {
                notifyChange(PAUSE);
            } else if (CMDSTOP.equals(cmd)) {
            	notifyChange(STOP);
            }
        }
        
        return START_STICKY;
    }
            
    @Override
    public boolean onUnbind(Intent intent) {        
		stopSelf(mServiceStartId);
    	
		Log.v(TAG, "onUnbind succedded");
        return true;
    }
    
    private void notifyChange(String what) {
    	// send notification to MediaPlayback activity
        Intent i = new Intent(what);
        sendBroadcast(i);
    }
    
    private static void initializeRemoteControlRegistrationMethods() {
    	Log.v(TAG, "Attempting to load mRegisterMediaButtonEventReceiver method");
    	try {
    		if (mRegisterMediaButtonEventReceiver == null) {
    	         mRegisterMediaButtonEventReceiver = AudioManager.class.getMethod(
    	               "registerMediaButtonEventReceiver",
    	               new Class[] { ComponentName.class } );
    	    }
    		/* success, this device will take advantage of better remote */
    		/* control event handling                                    */
    	} catch (NoSuchMethodException nsme) {
    		/* failure, still using the legacy behavior, but this app    */
    		/* is future-proof!                                          */
    		nsme.printStackTrace();
    	}
    }
    
    private void registerRemoteControl() {
        try {
            if (mRegisterMediaButtonEventReceiver == null) {
                return;
            }
            mRegisterMediaButtonEventReceiver.invoke(mAudioManager,
            		new ComponentName(this.getPackageName(), MediaButtonIntentReceiver.class.getName()));
        } catch (InvocationTargetException ite) {
            /* unpack original exception when possible */
            Throwable cause = ite.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else if (cause instanceof Error) {
                throw (Error) cause;
            } else {
                /* unexpected checked exception; wrap and re-throw */
                throw new RuntimeException(ite);
            }
        } catch (IllegalAccessException ie) {
            Log.e(TAG, "unexpected " + ie);
        }
    }

}