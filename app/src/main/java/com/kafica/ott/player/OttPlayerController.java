package com.kafica.ott.player;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;
import android.view.Surface;

// Maps to MediaPlayer / OTT Control blocks in diagram
// openStream → setSurface → setVolume → play → playbackStarted
// Handles: buffering states, error, release
public class OttPlayerController {

    private static final String TAG = "OttPlayerController";

    public interface PlaybackCallback {
        void onPlaybackStarted();
        void onBufferingStarted();
        void onBufferingEnded();
        void onError(String error);
    }

    private final Context context;
    private MediaPlayer mediaPlayer;
    private Surface surface;
    private float volume = 1.0f;

    public OttPlayerController(Context context) {
        this.context = context;
    }

    // setSurface(surface) — called from onSetSurface before tune
    public void setSurface(Surface surface) {
        this.surface = surface;
        if (mediaPlayer != null) {
            mediaPlayer.setSurface(surface);
        }
    }

    // setVolume(volume) — called from onSetStreamVolume
    public void setVolume(float volume) {
        this.volume = volume;
        if (mediaPlayer != null) {
            mediaPlayer.setVolume(volume, volume);
        }
    }

    // startPlayback(streamUri, surface, volume, callback)
    // maps to the full green phase in diagram:
    // openStream → HTTP GET manifest → streamOpened →
    // setSurface → setVolume → play → mediaSegments → playbackStarted
    public void startPlayback(String streamUri, Surface surface,
                              float volume, PlaybackCallback callback) {
        // always release previous player before starting new one
        release();

        this.surface = surface;
        this.volume = volume;

        Log.d(TAG, "startPlayback: " + streamUri);

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioAttributes(
                new android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MOVIE)
                        .build()
        );

        try {
            // setDataSource — openStream(streamUri) from diagram
            mediaPlayer.setDataSource(streamUri);

            if (surface != null) {
                mediaPlayer.setSurface(surface);
            }
            mediaPlayer.setVolume(volume, volume);

            mediaPlayer.setOnPreparedListener(mp -> {
                Log.d(TAG, "MediaPlayer prepared → start()");
                mp.start();
                callback.onPlaybackStarted();
            });

            // [buffering] alt frame from diagram
            mediaPlayer.setOnInfoListener((mp, what, extra) -> {
                if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
                    Log.d(TAG, "MEDIA_INFO_BUFFERING_START");
                    callback.onBufferingStarted();
                } else if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END) {
                    Log.d(TAG, "MEDIA_INFO_BUFFERING_END");
                    callback.onBufferingEnded();
                }
                return true;
            });

            // [stream error] alt frame from diagram
            // error(responseCode) → notifyVideoUnavailable(UNKNOWN)
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "MediaPlayer error: what=" + what + " extra=" + extra);
                callback.onError("MediaPlayer error: " + what + "/" + extra);
                return true;
            });

            mediaPlayer.setOnCompletionListener(mp -> {
                Log.d(TAG, "MediaPlayer completed");
            });

            // HTTP GET manifest/playlist happens inside prepareAsync
            // maps to openStream → HTTP GET manifest → streamOpened
            mediaPlayer.prepareAsync();

        } catch (Exception e) {
            Log.e(TAG, "startPlayback exception: " + e.getMessage());
            callback.onError(e.getMessage());
        }
    }

    // stop() → release() — [user leaves channel] opt frame
    // called from OttSession.onRelease()
    public void release() {
        if (mediaPlayer != null) {
            Log.d(TAG, "release()");
            final MediaPlayer mp = mediaPlayer;
            mediaPlayer = null;
            // Disconnect surface on the calling thread FIRST
            // so the next player can connect immediately
            try { mp.setSurface(null); } catch (Exception ignored) {}
            new Thread(() -> {
                try {
                    mp.reset();
                    mp.release();
                } catch (Exception e) {
                    Log.e(TAG, "Error releasing player: " + e.getMessage());
                }
            }).start();
        }
    }
    }
