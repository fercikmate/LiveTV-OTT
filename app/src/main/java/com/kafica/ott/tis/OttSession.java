package com.kafica.ott.tis;

import android.content.Context;
import com.kafica.ott.client.ProxyUrlBuilder;
import android.media.tv.TvInputManager;
import android.media.tv.TvInputService;
import android.net.Uri;
import android.util.Log;
import android.view.Surface;
import com.kafica.ott.model.ChannelInfo;
import com.kafica.ott.model.ProgramInfo;
import com.kafica.ott.player.OttPlayerController;
import com.kafica.ott.repository.OttChannelRepository;


// TvInputService.Session participant from diagram
// Full lifecycle: onSetSurface → onSetStreamVolume → onTune → onRelease
public class OttSession extends TvInputService.Session {

    private static final String TAG = "OttSession";

    private final Context context;
    private final String inputId;
    private final OttPlayerController playerController;
    private final OttChannelRepository repository;

    private Surface currentSurface;
    private float currentVolume = 1.0f;

    public OttSession(Context context, String inputId) {
        super(context);
        this.context = context;
        this.inputId = inputId;
        this.repository = new OttChannelRepository(context, inputId);
        this.playerController = new OttPlayerController(context);
        Log.i(TAG, "+++ New Session Created for Input: " + inputId + " +++");
    }

    @Override
    public boolean onSetSurface(Surface surface) {
        // More descriptive logging for the "null" issue
        if (surface == null) {
            Log.w(TAG, "onSetSurface(null): Surface is being destroyed or detached.");
        } else {
            Log.i(TAG, "onSetSurface: Valid surface received. mNativeObject present.");
        }

        this.currentSurface = surface;
        playerController.setSurface(surface);
        return true;
    }

    @Override
    public void onSetStreamVolume(float volume) {
        Log.v(TAG, "onSetStreamVolume: " + volume);
        this.currentVolume = volume;
        playerController.setVolume(volume);
    }

    @Override
    public boolean onTune(Uri channelUri) {
        Log.i(TAG, ">>> onTune Request: " + channelUri);

        notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_TUNING);

        ChannelInfo channel = repository.queryChannel(channelUri);

        if (channel == null) {
            Log.e(TAG, "!!! onTune FATAL: Channel URI could not be resolved in Database.");
            notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_UNKNOWN);
            return false;
        }

        if (channel.streamUri == null || channel.streamUri.isEmpty()) {
            Log.e(TAG, "!!! onTune FATAL: Stream URL is empty for " + channel.displayName);
            notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_UNKNOWN);
            return false;
        }

        String proxiedUrl = ProxyUrlBuilder.wrap(channel.streamUri);
        Log.i(TAG, "TUNING START -> " + channel.displayName + " (ID: " + channel.displayNumber + ")");
        Log.d(TAG, "Requesting URL via Proxy: " + proxiedUrl);

        ProgramInfo program = repository.queryCurrentProgram(channel.channelId);
        Log.i(TAG, "Current EPG: " + (program != null ? program.title : "EMPTY/NO DATA"));


        float playVolume = currentVolume > 0 ? currentVolume : 1.0f;
        Log.d(TAG, "Starting playback with volume: " + playVolume);
        playerController.startPlayback(
                proxiedUrl,
                currentSurface,
                playVolume,
                new OttPlayerController.PlaybackCallback() {

                    @Override
                    public void onPlaybackStarted() {
                        // This is where you know the m3u8 was parsed and the first segment is ready
                        Log.i(TAG, "SUCCESS: First frame rendered for " + channel.displayName);
                        notifyVideoAvailable();
                    }

                    @Override
                    public void onBufferingStarted() {
                        Log.d(TAG, "STATE: Buffering (Network/Decoder lag)");
                        notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_BUFFERING);
                    }

                    @Override
                    public void onBufferingEnded() {
                        Log.d(TAG, "STATE: Buffering ended.");
                        notifyVideoAvailable();
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "!!! PLAYBACK ERROR for " + channel.displayName + ": " + error);
                        notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_UNKNOWN);
                    }
                });

        return true;
    }

    @Override
    public void onRelease() {
        Log.i(TAG, "--- Session Released: Cleaning up Player Resources ---");
        // This calls the logic inside your controller
        if (playerController != null) {
            playerController.release();
        }
    }

    @Override
    public void onSetCaptionEnabled(boolean enabled) {

    }
}
