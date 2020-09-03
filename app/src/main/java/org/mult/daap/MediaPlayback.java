package org.mult.daap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Formatter;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.mult.daap.client.Song;
import org.mult.daap.client.widget.DAAPClientAppWidgetOneProvider;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.Html;
import android.text.Layout;
import android.text.TextUtils.TruncateAt;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class MediaPlayback extends Activity implements View.OnTouchListener, View.OnLongClickListener {

	private static final int MENU_STOP = 0;
	private static final int MENU_LIBRARY = 1;
	private static final int MENU_DOWNLOAD = 2;
	private static final int REFRESH = 0;
	private static final int COPYING_DIALOG = 1;
	private static final int SUCCESS_COPYING_DIALOG = 2;
	private static final int ERROR_COPYING_DIALOG = 3;
	private static final String logTag = MediaPlayer.class.getName();

	private static MediaPlayer mediaPlayer;
	private MediaPlaybackService mMediaPlaybackService = null;
	private static Song song;
	private TextView mArtistName;
	private TextView mAlbumName;
	private TextView mTrackName;
	private TextView mCurrentTime;
	private TextView mTotalTime;
	private TextView mSongSummary;
	private ImageButton mShuffleButton;
	private ImageButton mRepeatButton;
	private ImageButton mPrevButton;
	private ImageButton mNextButton;
	private ImageButton mPauseButton;
	private SeekBar mProgress;
	private int mTouchSlop;
	private int mInitialX = -1;
	private int mLastX = -1;
	private int mTextWidth = 0;
	private int mViewWidth = 0;
	private boolean mDraggingLabel = false;
	boolean scrobbler_support = false;

	private DAAPClientAppWidgetOneProvider mAppWidgetProvider = DAAPClientAppWidgetOneProvider.getInstance();

	public MediaPlayback() {
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setResult(Activity.RESULT_OK);
		if (Contents.address == null) {
			// We got kicked out of memory probably
			clearState();
			Contents.clearLists();
			stopNotification();
			setResult(Activity.RESULT_CANCELED);
			finish();
			return;
		}
		setVolumeControlStream(AudioManager.STREAM_MUSIC);
		setContentView(R.layout.audio_player);
		mTouchSlop = ViewConfiguration.get(this).getScaledTouchSlop();
		mProgress = (SeekBar) findViewById(android.R.id.progress);
		mShuffleButton = (ImageButton) findViewById(R.id.shuffleButton);
		mRepeatButton = (ImageButton) findViewById(R.id.repeatButton);
		mPauseButton = (ImageButton) findViewById(R.id.pause);
		mPrevButton = (ImageButton) findViewById(R.id.prev);
		mNextButton = (ImageButton) findViewById(R.id.next);
		mCurrentTime = (TextView) findViewById(R.id.currenttime);
		mTotalTime = (TextView) findViewById(R.id.totaltime);
		mArtistName = (TextView) findViewById(R.id.artistname);
		mAlbumName = (TextView) findViewById(R.id.albumname);
		mTrackName = (TextView) findViewById(R.id.trackname);
		mSongSummary = (TextView) findViewById(R.id.song_summary);
		SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		scrobbler_support = mPrefs.getBoolean("scrobbler_pref", false);
	}

	@Override
	public void onResume() {
		super.onResume();
		View v = (View) mArtistName.getParent();
		v.setOnTouchListener(this);
		v.setOnLongClickListener(this);
		v = (View) mAlbumName.getParent();
		v.setOnTouchListener(this);
		v.setOnLongClickListener(this);
		v = (View) mTrackName.getParent();
		v.setOnTouchListener(this);
		v.setOnLongClickListener(this);
		if (Contents.shuffle) {
			mShuffleButton.setImageResource(R.drawable.ic_menu_shuffle_on);
		} else {
			mShuffleButton.setImageResource(R.drawable.ic_menu_shuffle);
		}
		if (Contents.repeat) {
			mRepeatButton.setImageResource(R.drawable.ic_menu_repeat_on);
		} else {
			mRepeatButton.setImageResource(R.drawable.ic_menu_repeat);
		}
		mShuffleButton.setOnClickListener(mShuffleListener);
		mRepeatButton.setOnClickListener(mRepeatListener);
		mPauseButton.setOnClickListener(mPauseListener);
		mPrevButton.setOnClickListener(mPrevListener);
		mNextButton.setOnClickListener(mNextListener);
		mProgress.setMax(100);
		mProgress.setProgress(0);
		mProgress.setSecondaryProgress(0);
		mProgress.setOnSeekBarChangeListener(mSeekListener);
		if (mediaPlayer == null) {
			try {
				startSong(Contents.getSong());
			} catch (IndexOutOfBoundsException e) {
				Log.e(logTag, "Something went wrong with playlist/queue");
				e.printStackTrace();
				finish();
			}
		}
		setUpActivity();
		queueNextRefresh(refreshNow());
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog = null;
		if (id == COPYING_DIALOG) {
			ProgressDialog progressDialog = new ProgressDialog(this);
			progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			progressDialog.setMessage(getString(R.string.downloading_file));
			progressDialog.setCancelable(false);
			dialog = progressDialog;
		} else if (id == ERROR_COPYING_DIALOG || id == SUCCESS_COPYING_DIALOG) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			if (id == ERROR_COPYING_DIALOG) {
				builder.setMessage(R.string.save_error);
			} else {
				builder.setMessage(R.string.save_complete);
			}
			builder.setPositiveButton(android.R.string.ok, new OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});
			dialog = builder.create();
		}
		return dialog;
	}

	private void startSong(Song song) {
		clearState();
		mProgress.setEnabled(false);
		mediaPlayer = new MediaPlayer();
		MediaPlayback.song = song;
		try {
			mediaPlayer.setDataSource(Contents.daapHost.getSongURL(song));
			mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			mediaPlayer.setOnCompletionListener(normalOnCompletionListener);
			mediaPlayer.setOnErrorListener(mediaPlayerErrorListener);
			mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
				public void onPrepared(MediaPlayer mp) {
					mp.start();
					mProgress.setEnabled(true);
					stopNotification();
					startNotification();
					queueNextRefresh(refreshNow());
					mAppWidgetProvider.notifyChange(mMediaPlaybackService, MediaPlayback.this, MediaPlaybackService.PLAYSTATE_CHANGED);
				}
			});
			mediaPlayer.prepareAsync();
			TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
			tm.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
			setUpActivity();
			if (scrobbler_support) {
				scrobble(0); // START
			}
		} catch (Exception e) {
			e.printStackTrace();
			Toast.makeText(this, R.string.media_playback_error, Toast.LENGTH_LONG).show();
			finish();
		}
	}

	private void setUpActivity() {
		mArtistName.setText(song.artist);
		mAlbumName.setText(song.album);
		mTrackName.setText(song.name);
		mProgress.setProgress(0);
		mProgress.setSecondaryProgress(0);
		// Share this notification directly with our widgets
		mAppWidgetProvider.notifyChange(mMediaPlaybackService, this, MediaPlaybackService.META_CHANGED);
		new LastFMGetSongInfo().execute(song);
	}

	public String getTrackName() {
		return song.name;
	}

	public String getArtistName() {
		return song.artist;
	}

	public boolean isPlaying() {
		if (mediaPlayer != null)
			return mediaPlayer.isPlaying();

		return false;
	}

	private OnSeekBarChangeListener mSeekListener = new OnSeekBarChangeListener() {
		public void onStartTrackingTouch(SeekBar bar) {
			// intentionally left blank
		}

		public void onProgressChanged(SeekBar bar, int progress, boolean fromuser) {
			if (!fromuser) {
				return;
			}
			if (mediaPlayer == null) {
				bar.setProgress(0);
				return;
			} else {
				double doubleProgress = (double) progress;
				double doubleDuration;
				// get correct length of the song we are going to seek in
				doubleDuration = mediaPlayer.getDuration();
				int desiredSeek = (int) ((doubleProgress / 100.0) * doubleDuration);
				mediaPlayer.seekTo(desiredSeek);
				bar.setProgress(progress);
				handler.removeMessages(REFRESH);
				queueNextRefresh(refreshNow());
			}
		}

		public void onStopTrackingTouch(SeekBar seekBar) {
			// intentionally left blank
		}
	};

	private View.OnClickListener mShuffleListener = new View.OnClickListener() {

		public void onClick(View v) {
			if (Contents.shuffle) {
				Contents.shuffle = false;
				mShuffleButton.setImageResource(R.drawable.ic_menu_shuffle);
			} else {
				Contents.shuffle = true;
				mShuffleButton.setImageResource(R.drawable.ic_menu_shuffle_on);
			}
		}
	};

	private View.OnClickListener mRepeatListener = new View.OnClickListener() {

		public void onClick(View v) {
			if (Contents.repeat) {
				Contents.repeat = false;
				mRepeatButton.setImageResource(R.drawable.ic_menu_repeat);
			} else {
				Contents.repeat = true;
				mRepeatButton.setImageResource(R.drawable.ic_menu_repeat_on);
			}
		}
	};

	private View.OnClickListener mPauseListener = new View.OnClickListener() {
		public void onClick(View v) {
			if (mediaPlayer != null) {
				if (mediaPlayer.isPlaying()) {
					if (scrobbler_support) {
						scrobble(2); // PAUSE
					}
					mediaPlayer.pause();
					stopNotification();
				} else {
					if (scrobbler_support) {
						scrobble(1); // RESUME
					}
					mediaPlayer.start();
					startNotification();
				}
				setPauseButton();
				queueNextRefresh(refreshNow());
				mAppWidgetProvider.notifyChange(mMediaPlaybackService, MediaPlayback.this, MediaPlaybackService.PLAYSTATE_CHANGED);
			}
		}
	};
	private View.OnClickListener mPrevListener = new View.OnClickListener() {
		public void onClick(View v) {
			try {
				startSong(Contents.getPreviousSong());
				mAppWidgetProvider.notifyChange(mMediaPlaybackService, MediaPlayback.this, MediaPlaybackService.PLAYSTATE_CHANGED);
			} catch (IndexOutOfBoundsException e) {
				handler.removeMessages(REFRESH);
				stopNotification();
				clearState();
				finish();
				return;
			}
		}
	};
	private View.OnClickListener mNextListener = new View.OnClickListener() {
		public void onClick(View v) {
			normalOnCompletionListener.onCompletion(mediaPlayer);
			mAppWidgetProvider.notifyChange(mMediaPlaybackService, MediaPlayback.this, MediaPlaybackService.PLAYSTATE_CHANGED);
		}
	};

	@Override
	public void onPause() {
		handler.removeMessages(REFRESH);
		super.onPause();
	}

	@Override
	public void onStop() {
		handler.removeMessages(REFRESH);
		super.onStop();
	}

	@Override
	public void onStart() {
		super.onStart();

		IntentFilter f = new IntentFilter();
		f.addAction(MediaPlaybackService.PREVIOUS);
		f.addAction(MediaPlaybackService.NEXT);
		f.addAction(MediaPlaybackService.TOGGLEPAUSE);
		f.addAction(MediaPlaybackService.PAUSE);
		f.addAction(MediaPlaybackService.STOP);
		f.addAction(MediaPlaybackService.ADDED);
		f.addAction(MediaPlaybackService.HEADSET_CHANGE);
		registerReceiver(mStatusListener, new IntentFilter(f));

		if (mMediaPlaybackService == null) {
			bindService(new Intent(this, MediaPlaybackService.class), connection, Context.BIND_AUTO_CREATE);
		}
	}

	@Override
	public void onNewIntent(Intent intent) {
		setIntent(intent);
	}

	@Override
	public void onDestroy() {
		handler.removeMessages(REFRESH);
		if (scrobbler_support) {
			scrobble(3); // COMPLETE
		}
		super.onDestroy();

		if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
			// Share this notification directly with our widgets
			mAppWidgetProvider.notifyChange(mMediaPlaybackService, this, MediaPlaybackService.PLAYER_CLOSED);

			unregisterReceiver(mStatusListener);
			// Detach our existing connection.
			unbindService(connection);
			mMediaPlaybackService = null;
		}
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		if (mediaPlayer != null && Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
			menu.findItem(MENU_DOWNLOAD).setEnabled(true);
		} else {
			menu.findItem(MENU_DOWNLOAD).setEnabled(false);
		}
		return true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, MENU_STOP, 0, R.string.menu_stop).setIcon(android.R.drawable.ic_menu_close_clear_cancel);
		menu.add(0, MENU_LIBRARY, 0, R.string.menu_library).setIcon(R.drawable.ic_menu_list);
		menu.add(0, MENU_DOWNLOAD, 1, R.string.save).setIcon(android.R.drawable.ic_menu_save);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_STOP:
			clearState();
			stopNotification();
			finish();
			break;
		case MENU_LIBRARY:
			Intent intent = new Intent(MediaPlayback.this, PlaylistBrowser.class);
			startActivity(intent);
			break;
		case MENU_DOWNLOAD:
			showDialog(COPYING_DIALOG);
			new Thread(new FileCopier()).start();
			break;
		}
		return true;
	}

	private class FileCopier implements Runnable {
		public void run() {
			if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) && mediaPlayer != null) {
				boolean wasPlaying = mediaPlayer.isPlaying();
				try {
					File directory = new File(Environment.getExternalStorageDirectory(), "DAAP");
					directory.mkdirs();
					File destination = new File(directory, "DAAP-" + song.id + "." + song.format);
					mediaPlayer.pause();
					InputStream songStream = Contents.daapHost.getSongStream(song);
					FileOutputStream destinationStream = new FileOutputStream(destination);
					byte[] buffer = new byte[1024];
					int len;
					while ((len = songStream.read(buffer)) > 0) {
						destinationStream.write(buffer, 0, len);
					}
					if (songStream != null)
						songStream.close();
					if (destinationStream != null)
						destinationStream.close();
					destination.deleteOnExit();
					handler.sendEmptyMessage(COPYING_DIALOG);

					if (wasPlaying)
						mediaPlayer.start();
					handler.sendEmptyMessage(SUCCESS_COPYING_DIALOG);
				} catch (Exception e) {
					if (wasPlaying)
						mediaPlayer.start();
					handler.sendEmptyMessage(COPYING_DIALOG);
					handler.sendEmptyMessage(ERROR_COPYING_DIALOG);
					e.printStackTrace();
				}
			}
		}
	}

	private void setPauseButton() throws IllegalStateException {
		try {
			if (mediaPlayer != null) {
				mPauseButton.setEnabled(true);
				if (mediaPlayer.isPlaying()) {
					mPauseButton.setImageResource(android.R.drawable.ic_media_pause);
				} else {
					mPauseButton.setImageResource(android.R.drawable.ic_media_play);
				}
			} else {
				mPauseButton.setEnabled(false);
			}
		} catch (IllegalStateException e) {
			throw e;
		}
	}

	private final Handler handler = new Handler() {
		@Override
		public void handleMessage(Message message) {
			switch (message.what) {
			case REFRESH:
				queueNextRefresh(refreshNow());
				break;
			case COPYING_DIALOG:
				dismissDialog(COPYING_DIALOG);
				break;
			case SUCCESS_COPYING_DIALOG:
				showDialog(SUCCESS_COPYING_DIALOG);
				break;
			case ERROR_COPYING_DIALOG:
				showDialog(ERROR_COPYING_DIALOG);
				break;
			}
		}
	};

	private void queueNextRefresh(long delay) {
		handler.removeMessages(REFRESH);
		handler.sendEmptyMessageDelayed(REFRESH, delay);
	}

	private long refreshNow() {
		try {
			if (mediaPlayer == null) {
				return 500;
			}
			mTotalTime.setText(makeTimeString(song.time / 1000));
			int mDuration = song.time;
			setPauseButton();
			long pos = mediaPlayer.getCurrentPosition();
			long remaining = 1000 - (pos % 1000);
			if ((pos >= 0) && (mDuration > 0)) {
				mCurrentTime.setText(makeTimeString(pos / 1000));
				if (mediaPlayer.isPlaying()) {
					mCurrentTime.setVisibility(View.VISIBLE);
				} else {
					// blink the counter
					int vis = mCurrentTime.getVisibility();
					mCurrentTime.setVisibility(vis == View.INVISIBLE ? View.VISIBLE : View.INVISIBLE);
					remaining = 500;
				}
				mProgress.setProgress((int) (100 * pos / mDuration));
			}
			// return the number of milliseconds until the next full second, so
			// the counter can be updated at just the right time
			return remaining;
		} catch (IllegalStateException e) {
			return 500;
		}
	}

	private static String makeTimeString(long secs) {
		String durationformat = (secs < 3600 ? "%2$d:%5$02d" : "%1$d:%3$02d:%5$02d");
		StringBuilder sFormatBuilder = new StringBuilder();
		Formatter sFormatter = new Formatter(sFormatBuilder, Locale.getDefault());
		sFormatBuilder.setLength(0);
		final Object[] timeArgs = new Object[5];
		timeArgs[0] = secs / 3600;
		timeArgs[1] = secs / 60;
		timeArgs[2] = (secs / 60) % 60;
		timeArgs[3] = secs;
		timeArgs[4] = secs % 60;
		return sFormatter.format(durationformat, timeArgs).toString();
	}

	public boolean onLongClick(View arg0) {
		return false;
	}

	public void startNotification() {
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		Notification notification = new Notification(R.drawable.stat_notify_musicplayer, song.name, System.currentTimeMillis());
		PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, getIntent(), 0);
		// notification.setLatestEventInfo(getApplicationContext(), song.name, song.artist, contentIntent);
		notification.flags |= Notification.FLAG_ONGOING_EVENT;
		notification.flags |= Notification.FLAG_NO_CLEAR;
		notificationManager.notify(0, notification);
	}

	public void stopNotification() {
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancelAll();
	}

	private PhoneStateListener phoneStateListener = new PhoneStateListener() {
		public void onCallStateChanged(int state, String incomingNumsber) {
			switch (state) {
			// change to idle
			case TelephonyManager.CALL_STATE_IDLE:
				break;
			case TelephonyManager.CALL_STATE_OFFHOOK:
				if (mediaPlayer != null) {
					mediaPlayer.pause();
				}
				break;
			case TelephonyManager.CALL_STATE_RINGING:
				if (mediaPlayer != null) {
					mediaPlayer.pause();
				}
				break;
			}
		}
	};

	TextView textViewForContainer(View v) {
		View vv = v.findViewById(R.id.artistname);
		if (vv != null)
			return (TextView) vv;
		vv = v.findViewById(R.id.albumname);
		if (vv != null)
			return (TextView) vv;
		vv = v.findViewById(R.id.trackname);
		if (vv != null)
			return (TextView) vv;
		return null;
	}

	public boolean onTouch(View v, MotionEvent event) {
		int action = event.getAction();
		TextView tv = textViewForContainer(v);
		if (tv == null) {
			return false;
		}
		if (action == MotionEvent.ACTION_DOWN) {
			v.setBackgroundColor(0xff606060);
			mInitialX = mLastX = (int) event.getX();
			mDraggingLabel = false;
		} else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
			v.setBackgroundColor(0);
			if (mDraggingLabel) {
				Message msg = mLabelScroller.obtainMessage(0, tv);
				mLabelScroller.sendMessageDelayed(msg, 1000);
			}
		} else if (action == MotionEvent.ACTION_MOVE) {
			if (mDraggingLabel) {
				int scrollx = tv.getScrollX();
				int x = (int) event.getX();
				int delta = mLastX - x;
				if (delta != 0) {
					mLastX = x;
					scrollx += delta;
					if (scrollx > mTextWidth) {
						// scrolled the text completely off the view to the left
						scrollx -= mTextWidth;
						scrollx -= mViewWidth;
					}
					if (scrollx < -mViewWidth) {
						// scrolled the text completely off the view to the
						// right
						scrollx += mViewWidth;
						scrollx += mTextWidth;
					}
					tv.scrollTo(scrollx, 0);
				}
				return true;
			}
			int delta = mInitialX - (int) event.getX();
			if (Math.abs(delta) > mTouchSlop) {
				mLabelScroller.removeMessages(0, tv);
				if (tv.getEllipsize() != null) {
					tv.setEllipsize(null);
				}
				Layout ll = tv.getLayout();
				if (ll == null) {
					return false;
				}
				mTextWidth = (int) tv.getLayout().getLineWidth(0);
				mViewWidth = tv.getWidth();
				if (mViewWidth > mTextWidth) {
					tv.setEllipsize(TruncateAt.END);
					v.cancelLongPress();
					return false;
				}
				mDraggingLabel = true;
				tv.setHorizontalFadingEdgeEnabled(true);
				v.cancelLongPress();
				return true;
			}
		}
		return false;
	}

	Handler mLabelScroller = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			TextView tv = (TextView) msg.obj;
			int x = tv.getScrollX();
			x = x * 3 / 4;
			tv.scrollTo(x, 0);
			if (x == 0) {
				tv.setEllipsize(TruncateAt.END);
			} else {
				Message newmsg = obtainMessage(0, tv);
				mLabelScroller.sendMessageDelayed(newmsg, 15);
			}
		}
	};

	public static void clearState() {
		if (mediaPlayer != null) {
			try {
				mediaPlayer.stop();
				mediaPlayer.release();
			} catch (IllegalStateException e) {
				e.printStackTrace();
				Log.i(logTag, "Usually this is not a problem.");
			}
		}
		mediaPlayer = null;
	}

	private OnErrorListener mediaPlayerErrorListener = new MediaPlayer.OnErrorListener() {
		public boolean onError(MediaPlayer mp, int what, int extra) {
			Log.e(logTag, "Error in MediaPlayer: (" + what + ") with extra (" + extra + ")");
			clearState();
			return false;
		}
	};

	private OnCompletionListener normalOnCompletionListener = new OnCompletionListener() {
		public void onCompletion(MediaPlayer mp) {
			try {
				if (scrobbler_support) {
					scrobble(3); // COMPLETE
				}
				if (Contents.shuffle == true) {
					startSong(Contents.getRandomSong());
				} else if (Contents.repeat == true) {
					mp.seekTo(0);
					mp.start();
					queueNextRefresh(refreshNow());
				} else {
					startSong(Contents.getNextSong());
				}
			} catch (IndexOutOfBoundsException e) {
				handler.removeMessages(REFRESH);
				stopNotification();
				clearState();
				finish();
				return;
			}
		}
	};

	private class LastFMGetSongInfo extends AsyncTask<Song, Void, String> {
		protected String doInBackground(Song... song) {
			String key = "47c0f71763c30293aa52f0ac166e410f";
			String retval = "";
			try {
				URL lastFM = new URL("http://ws.audioscrobbler.com/2.0/?method=artist.getinfo&artist=" + URLEncoder.encode(song[0].artist, "UTF-8") + "&api_key=" + key);
				Log.d("MediaPlayback", "Songurl = " + lastFM);
				URLConnection lfmConnection = lastFM.openConnection();
				DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
				Document doc = dBuilder.parse(lfmConnection.getInputStream());
				NodeList nList = doc.getElementsByTagName("summary");
				for (int temp = 0; temp < nList.getLength(); temp++) {
					Node nNode = nList.item(temp);
					if (nNode.getNodeType() == Node.ELEMENT_NODE) {
						retval = nNode.getFirstChild().getNodeValue();
						retval = retval.replace("&quot;", "\"").replace("&apos;", "\'").replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&");
						break;
					}
				}
			} catch (Exception e) {
				// Do nothing
			}
			return retval;
		}

		protected void onPostExecute(String result) {
			mSongSummary.setText(Html.fromHtml(result));
			mSongSummary.setMovementMethod(LinkMovementMethod.getInstance());
		}
	}

	private void scrobble(int code) {
		boolean playing = false;
		if (code == 0 || code == 1) {
			playing = true;
		} else { // 2, 3
			playing = false;
		}
		Intent bCast = new Intent("com.adam.aslfms.notify.playstatechanged");
		bCast.putExtra("state", code);
		bCast.putExtra("app-name", "Daap-client");
		bCast.putExtra("app-package", "org.mult.daap");
		bCast.putExtra("artist", song.artist);
		bCast.putExtra("album", song.album);
		bCast.putExtra("track", song.name);
		bCast.putExtra("duration", song.time / 1000);
		sendBroadcast(bCast);
		Intent i = new Intent("net.jjc1138.android.scrobbler.action.MUSIC_STATUS");
		i.putExtra("playing", playing);
		i.putExtra("artist", song.artist);
		i.putExtra("album", song.album);
		i.putExtra("track", song.name);
		i.putExtra("secs", song.time / 1000);
		sendBroadcast(i);
	}

	private ServiceConnection connection = new ServiceConnection() {
		public void onServiceConnected(ComponentName classname, IBinder service) {
			// This is called when the connection with the service has been
			// established, giving us the service object we can use to
			// interact with the service. Because we have bound to a explicit
			// service that we know is running in our own process, we can
			// cast its IBinder to a concrete class and directly access it.
			mMediaPlaybackService = ((MediaPlaybackService.LocalBinder) service).getService();
		}

		public void onServiceDisconnected(ComponentName classname) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			// Because it is running in our same process, we should never
			// see this happen.
			mMediaPlaybackService = null;
		}
	};

	private BroadcastReceiver mStatusListener = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (MediaPlaybackService.PREVIOUS.equals(action)) {
				startSong(Contents.getPreviousSong());
				mAppWidgetProvider.notifyChange(mMediaPlaybackService, MediaPlayback.this, MediaPlaybackService.PLAYSTATE_CHANGED);
			} else if (MediaPlaybackService.NEXT.equals(action)) {
				normalOnCompletionListener.onCompletion(mediaPlayer);
				mAppWidgetProvider.notifyChange(mMediaPlaybackService, MediaPlayback.this, MediaPlaybackService.PLAYSTATE_CHANGED);
			} else if (MediaPlaybackService.TOGGLEPAUSE.equals(action)) {
				if (mediaPlayer != null) {
					if (mediaPlayer.isPlaying()) {
						if (scrobbler_support) {
							scrobble(2); // PAUSE
						}
						mediaPlayer.pause();
						stopNotification();
					} else {
						if (scrobbler_support) {
							scrobble(1); // RESUME
						}
						mediaPlayer.start();
						startNotification();
					}
					setPauseButton();
					queueNextRefresh(refreshNow());
					mAppWidgetProvider.notifyChange(mMediaPlaybackService, MediaPlayback.this, MediaPlaybackService.PLAYSTATE_CHANGED);
				}
			} else if (MediaPlaybackService.PAUSE.equals(action)) {
				if (mediaPlayer != null) {
					if (mediaPlayer.isPlaying()) {
						if (scrobbler_support) {
							scrobble(2); // PAUSE
						}
						mediaPlayer.pause();
						stopNotification();
					} else {
						if (scrobbler_support) {
							scrobble(1); // RESUME
						}
						mediaPlayer.start();
						startNotification();
					}
					setPauseButton();
					queueNextRefresh(refreshNow());
					mAppWidgetProvider.notifyChange(mMediaPlaybackService, MediaPlayback.this, MediaPlaybackService.PLAYSTATE_CHANGED);
				}
			} else if (MediaPlaybackService.STOP.equals(action)) {
				if (mediaPlayer != null) {
					if (mediaPlayer.isPlaying()) {
						if (scrobbler_support) {
							scrobble(2); // PAUSE
						}
						mediaPlayer.pause();
						mediaPlayer.seekTo(0);
						stopNotification();
					}
					setPauseButton();
					queueNextRefresh(refreshNow());
					mAppWidgetProvider.notifyChange(mMediaPlaybackService, MediaPlayback.this, MediaPlaybackService.PLAYSTATE_CHANGED);
				}
			} else if (MediaPlaybackService.ADDED.equals(action)) {
				int[] appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
				mAppWidgetProvider.performUpdate(mMediaPlaybackService, MediaPlayback.this, appWidgetIds, "");
			} else if (MediaPlaybackService.HEADSET_CHANGE.equals(action)) {
				if (mediaPlayer != null) {
					if (mediaPlayer.isPlaying()) {
						if (scrobbler_support) {
							scrobble(2); // PAUSE
						}
						mediaPlayer.pause();
						stopNotification();
					}
					setPauseButton();
					queueNextRefresh(refreshNow());
					mAppWidgetProvider.notifyChange(mMediaPlaybackService, MediaPlayback.this, MediaPlaybackService.PLAYSTATE_CHANGED);
				}
			}
		}
	};
};