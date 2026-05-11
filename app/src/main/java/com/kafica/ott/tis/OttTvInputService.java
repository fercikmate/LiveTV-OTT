package com.kafica.ott.tis;

import android.media.tv.TvInputService;
import androidx.annotation.Nullable;

// Registered with Android TIF via AndroidManifest
// TvInputManager discovers this on boot via intent filter
public class OttTvInputService extends TvInputService {

    @Nullable
    @Override
    public Session onCreateSession(String inputId) {
        return new OttSession(this, inputId);
    }
}