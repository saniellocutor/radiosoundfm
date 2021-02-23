package com.streaminghost.radiosoundfm.services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.Surface;

import androidx.annotation.Nullable;

import com.streaminghost.radiosoundfm.R;
import com.streaminghost.radiosoundfm.activities.MainActivity;
import com.streaminghost.radiosoundfm.services.metadata.Metadata;
import com.streaminghost.radiosoundfm.services.metadata.ShoutcastDataSourceFactory;
import com.streaminghost.radiosoundfm.services.metadata.ShoutcastMetadataListener;
import com.streaminghost.radiosoundfm.services.parser.AlbumArtGetter;
import com.streaminghost.radiosoundfm.utilities.Tools;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.analytics.AnalyticsListener;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.decoder.DecoderCounters;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSourceEventListener;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.util.Util;

import java.io.IOException;

import okhttp3.OkHttpClient;

public class RadioService extends Service implements Player.EventListener, AudioManager.OnAudioFocusChangeListener, ShoutcastMetadataListener {

    public static final String ACTION_PLAY = "com.app.yoursingleradio.ACTION_PLAY";
    public static final String ACTION_PAUSE = "com.app.yoursingleradio.ACTION_PAUSE";
    public static final String ACTION_RESUME = "com.app.yoursingleradio.ACTION_RESUME";
    public static final String ACTION_STOP = "com.app.yoursingleradio.ACTION_STOP";
    private final IBinder iBinder = new LocalBinder();
    private Handler handler;
    private SimpleExoPlayer exoPlayer;
    private MediaSessionCompat mediaSession;
    private MediaControllerCompat.TransportControls transportControls;
    private boolean onGoingCall = false;
    private TelephonyManager telephonyManager;
    private WifiManager.WifiLock wifiLock;
    private AudioManager audioManager;
    private MediaNotificationManager notificationManager;
    private boolean serviceInUse = false;
    private String status;
    private String strAppName;
    private String strLiveBroadcast;
    private String streamUrl;

    public class LocalBinder extends Binder {
        public RadioService getService() {
            return RadioService.this;
        }
    }

    private BroadcastReceiver becomingNoisyReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            pause();
        }

    };

    private PhoneStateListener phoneStateListener = new PhoneStateListener() {

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {

            if (state == TelephonyManager.CALL_STATE_OFFHOOK || state == TelephonyManager.CALL_STATE_RINGING) {
                if (!isPlaying()) return;
                onGoingCall = true;
                stop();

            } else if (state == TelephonyManager.CALL_STATE_IDLE) {
                if (!onGoingCall) return;
                onGoingCall = false;
                resume();
            }
        }

    };

    private MediaSessionCompat.Callback mediasSessionCallback = new MediaSessionCompat.Callback() {
        @Override
        public void onPause() {
            super.onPause();
            pause();
        }

        @Override
        public void onStop() {
            super.onStop();
            stop();
            notificationManager.cancelNotify();
        }

        @Override
        public void onPlay() {
            super.onPlay();
            resume();
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        serviceInUse = true;
        return iBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        strAppName = getResources().getString(R.string.app_name);
        strLiveBroadcast = getResources().getString(R.string.notification_playing);

        onGoingCall = false;

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        notificationManager = new MediaNotificationManager(this);

        wifiLock = ((WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL, "mcScPAmpLock");

        mediaSession = new MediaSessionCompat(this, getClass().getSimpleName());
        transportControls = mediaSession.getController().getTransportControls();
        mediaSession.setActive(true);
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mediaSession.setMetadata(new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "...")
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, strAppName)
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, strLiveBroadcast)
                .build());
        mediaSession.setCallback(mediasSessionCallback);

        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);

        handler = new Handler();
        DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        @SuppressWarnings("deprecation") AdaptiveTrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory(bandwidthMeter);
        DefaultTrackSelector trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);
        exoPlayer = ExoPlayerFactory.newSimpleInstance(getApplicationContext(), trackSelector);
        exoPlayer.addListener(this);
        exoPlayer.addAnalyticsListener(new AnalyticsListener() {
                                           @Override
                                           public void onPlayerStateChanged(EventTime eventTime, boolean playWhenReady, int playbackState) {

                                           }

                                           @Override
                                           public void onTimelineChanged(EventTime eventTime, int reason) {

                                           }

                                           @Override
                                           public void onPositionDiscontinuity(EventTime eventTime, int reason) {

                                           }

                                           @Override
                                           public void onSeekStarted(EventTime eventTime) {

                                           }

                                           @Override
                                           public void onSeekProcessed(EventTime eventTime) {

                                           }

                                           @Override
                                           public void onPlaybackParametersChanged(EventTime eventTime, PlaybackParameters playbackParameters) {

                                           }

                                           @Override
                                           public void onRepeatModeChanged(EventTime eventTime, int repeatMode) {

                                           }

                                           @Override
                                           public void onShuffleModeChanged(EventTime eventTime, boolean shuffleModeEnabled) {

                                           }

                                           @Override
                                           public void onLoadingChanged(EventTime eventTime, boolean isLoading) {

                                           }

                                           @Override
                                           public void onPlayerError(EventTime eventTime, ExoPlaybackException error) {

                                           }

                                           @Override
                                           public void onTracksChanged(EventTime eventTime, TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

                                           }

                                           @Override
                                           public void onLoadStarted(EventTime eventTime, MediaSourceEventListener.LoadEventInfo loadEventInfo, MediaSourceEventListener.MediaLoadData mediaLoadData) {

                                           }

                                           @Override
                                           public void onLoadCompleted(EventTime eventTime, MediaSourceEventListener.LoadEventInfo loadEventInfo, MediaSourceEventListener.MediaLoadData mediaLoadData) {

                                           }

                                           @Override
                                           public void onLoadCanceled(EventTime eventTime, MediaSourceEventListener.LoadEventInfo loadEventInfo, MediaSourceEventListener.MediaLoadData mediaLoadData) {

                                           }

                                           @Override
                                           public void onLoadError(EventTime eventTime, MediaSourceEventListener.LoadEventInfo loadEventInfo, MediaSourceEventListener.MediaLoadData mediaLoadData, IOException error, boolean wasCanceled) {

                                           }

                                           @Override
                                           public void onDownstreamFormatChanged(EventTime eventTime, MediaSourceEventListener.MediaLoadData mediaLoadData) {

                                           }

                                           @Override
                                           public void onUpstreamDiscarded(EventTime eventTime, MediaSourceEventListener.MediaLoadData mediaLoadData) {

                                           }

                                           @Override
                                           public void onMediaPeriodCreated(EventTime eventTime) {

                                           }

                                           @Override
                                           public void onMediaPeriodReleased(EventTime eventTime) {

                                           }

                                           @Override
                                           public void onReadingStarted(EventTime eventTime) {

                                           }

                                           @Override
                                           public void onBandwidthEstimate(EventTime eventTime, int totalLoadTimeMs, long totalBytesLoaded, long bitrateEstimate) {

                                           }

                                           @Override
                                           public void onSurfaceSizeChanged(EventTime eventTime, int width, int height) {

                                           }

                                           @Override
                                           public void onMetadata(EventTime eventTime, com.google.android.exoplayer2.metadata.Metadata metadata) {

                                           }

                                           @Override
                                           public void onDecoderEnabled(EventTime eventTime, int trackType, DecoderCounters decoderCounters) {

                                           }

                                           @Override
                                           public void onDecoderInitialized(EventTime eventTime, int trackType, String decoderName, long initializationDurationMs) {

                                           }

                                           @Override
                                           public void onDecoderInputFormatChanged(EventTime eventTime, int trackType, Format format) {

                                           }

                                           @Override
                                           public void onDecoderDisabled(EventTime eventTime, int trackType, DecoderCounters decoderCounters) {

                                           }

                                           @Override
                                           public void onAudioSessionId(EventTime eventTime, int audioSessionId) {
                                               Tools.onAudioSessionId(getAudioSessionId());
                                           }

                                           @Override
                                           public void onAudioAttributesChanged(EventTime eventTime, AudioAttributes audioAttributes) {

                                           }

                                           @Override
                                           public void onVolumeChanged(EventTime eventTime, float volume) {

                                           }

                                           @Override
                                           public void onAudioUnderrun(EventTime eventTime, int bufferSize, long bufferSizeMs, long elapsedSinceLastFeedMs) {

                                           }

                                           @Override
                                           public void onDroppedVideoFrames(EventTime eventTime, int droppedFrames, long elapsedMs) {

                                           }

                                           @Override
                                           public void onVideoSizeChanged(EventTime eventTime, int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {

                                           }

                                           @Override
                                           public void onRenderedFirstFrame(EventTime eventTime, @Nullable Surface surface) {

                                           }

                                           @Override
                                           public void onDrmSessionAcquired(EventTime eventTime) {

                                           }

                                           @Override
                                           public void onDrmKeysLoaded(EventTime eventTime) {

                                           }

                                           @Override
                                           public void onDrmSessionManagerError(EventTime eventTime, Exception error) {

                                           }

                                           @Override
                                           public void onDrmKeysRestored(EventTime eventTime) {

                                           }

                                           @Override
                                           public void onDrmKeysRemoved(EventTime eventTime) {

                                           }

                                           @Override
                                           public void onDrmSessionReleased(EventTime eventTime) {

                                           }
                                       }
        );

        exoPlayer.setPlayWhenReady(true);
        registerReceiver(becomingNoisyReceiver, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
        status = PlaybackStatus.IDLE;

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        String action = intent.getAction();

        if (TextUtils.isEmpty(action))
            return START_NOT_STICKY;

        int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            stop();
            return START_NOT_STICKY;
        }

        if (action.equalsIgnoreCase(ACTION_PLAY)) {
            transportControls.play();
        } else if (action.equalsIgnoreCase(ACTION_PAUSE)) {
            transportControls.pause();
        } else if (action.equalsIgnoreCase(ACTION_RESUME)) {
            Intent meaw = new Intent(getApplicationContext(), MainActivity.class);
            meaw.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(meaw);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    transportControls.pause();
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            transportControls.play();
                        }
                    }, 10);
                }
            }, 250);

        } else if (action.equalsIgnoreCase(ACTION_STOP)) {
            transportControls.stop();
        }

        return START_NOT_STICKY;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        serviceInUse = false;
        if (status.equals(PlaybackStatus.IDLE))
            stopSelf();
        return super.onUnbind(intent);
    }

    @Override
    public void onRebind(final Intent intent) {
        serviceInUse = true;
    }

    @Override
    public void onDestroy() {

        pause();
        exoPlayer.release();
        exoPlayer.removeListener(this);

        if (telephonyManager != null)
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);

        notificationManager.cancelNotify();
        mediaSession.release();
        unregisterReceiver(becomingNoisyReceiver);

        super.onDestroy();

    }

    @Override
    public void onAudioFocusChange(int focusChange) {

        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                exoPlayer.setVolume(0.8f);
                resume();
                break;

            case AudioManager.AUDIOFOCUS_LOSS:
                stop();
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                if (isPlaying()) pause();
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                if (isPlaying())
                    exoPlayer.setVolume(0.1f);
                break;
        }

    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {

        switch (playbackState) {
            case Player.STATE_BUFFERING:
                status = PlaybackStatus.LOADING;
                break;
            case Player.STATE_ENDED:
                status = PlaybackStatus.STOPPED;
                break;
            case Player.STATE_IDLE:
                status = PlaybackStatus.IDLE;
                break;
            case Player.STATE_READY:
                status = playWhenReady ? PlaybackStatus.PLAYING : PlaybackStatus.PAUSED;
                break;
            default:
                status = PlaybackStatus.IDLE;
                break;
        }

        if (!status.equals(PlaybackStatus.IDLE))
            notificationManager.startNotify(status);

        Tools.onEvent(status);
    }

    public void onRepeatModeChanged(int repeatMode) {

    }

    @Override
    public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {

    }

    @Override
    public void onTimelineChanged(Timeline timeline, @Nullable Object manifest, int reason) {

    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

    }

    @Override
    public void onLoadingChanged(boolean isLoading) {

    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
        Tools.onEvent(PlaybackStatus.ERROR);
    }

    @Override
    public void onPositionDiscontinuity(int reason) {

    }

    @Override
    public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {

    }

    @Override
    public void onSeekProcessed() {

    }


    public String getStreamUrl() {
        return streamUrl;
    }

    private String getUserAgent() {
        StringBuilder result = new StringBuilder(64);
        result.append("Dalvik/");
        result.append(System.getProperty("java.vm.version"));
        result.append(" (Linux; U; Android ");

        String version = Build.VERSION.RELEASE;
        result.append(version.length() > 0 ? version : "1.0");

        if ("REL".equals(Build.VERSION.CODENAME)) {
            String model = Build.MODEL;
            if (model.length() > 0) {
                result.append("; ");
                result.append(model);
            }
        }

        String id = Build.ID;
        if (id.length() > 0) {
            result.append(" Build/");
            result.append(id);
        }
        result.append(")");
        return result.toString();
    }

    public void play(String streamUrl) {

        this.streamUrl = streamUrl;
        if (wifiLock != null && !wifiLock.isHeld()) {
            wifiLock.acquire();
        }

        DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        ShoutcastDataSourceFactory dataSourceFactory = new ShoutcastDataSourceFactory(new OkHttpClient.Builder().build(), Util.getUserAgent(this, getClass().getSimpleName()), bandwidthMeter, this);
        ExtractorMediaSource mediaSource = new ExtractorMediaSource.Factory(dataSourceFactory)
                .setExtractorsFactory(new DefaultExtractorsFactory())
                .createMediaSource(Uri.parse(streamUrl));

        exoPlayer.prepare(mediaSource);
        exoPlayer.setPlayWhenReady(true);
    }

    public int getAudioSessionId() {
        return exoPlayer.getAudioSessionId();
    }

    public void resume() {
        if (streamUrl != null)
            play(streamUrl);
    }

    public void pause() {
        exoPlayer.setPlayWhenReady(false);
        audioManager.abandonAudioFocus(this);
        wifiLockRelease();
    }

    public void stop() {
        exoPlayer.stop();
        audioManager.abandonAudioFocus(this);
        wifiLockRelease();
    }

    public void playOrPause(String url) {

        if (streamUrl != null && streamUrl.equals(url)) {
            if (!isPlaying()) {
                play(streamUrl);
            } else {
                pause();
            }
        } else {
            if (isPlaying()) {
                pause();
            }
            play(url);
        }

    }

    public String getStatus() {

        return status;
    }

    @Override
    public void onMetadataReceived(final Metadata data) {
        final String artistAndSong = data.getArtist() + " " + data.getSong();
        AlbumArtGetter.getImageForQuery(artistAndSong, new AlbumArtGetter.AlbumCallback() {
            @Override
            public void finished(Bitmap art) {
                notificationManager.startNotify(art, data);
                //Post meta to Fragments
                Tools.onMetaDataReceived(data, art);
            }
        }, this);
    }

    public Metadata getMetaData() {
        return notificationManager.getMetaData();
    }

    public Bitmap getAlbumArt() {
        return notificationManager.getAlbumArt();
    }

    public MediaSessionCompat getMediaSession() {
        return mediaSession;
    }

    public boolean isPlaying() {
        return this.status.equals(PlaybackStatus.PLAYING);
    }

    private void wifiLockRelease() {
        if (wifiLock != null && wifiLock.isHeld()) {
            wifiLock.release();
        }
    }

}
