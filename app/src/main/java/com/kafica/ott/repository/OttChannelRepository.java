package com.kafica.ott.repository;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.media.tv.TvContract;
import android.net.Uri;
import com.kafica.ott.model.ChannelInfo;
import com.kafica.ott.model.ProgramInfo;
import com.kafica.ott.model.ServiceInfo;

// Maps to Service Registry / DTV Control block in diagram
// All reads and writes to TV Provider / TvContract go through here
public class OttChannelRepository {

    private final ContentResolver resolver;
    private final String inputId;

    public OttChannelRepository(Context context, String inputId) {
        this.resolver = context.getContentResolver();
        this.inputId = inputId;
    }

    // insertOrUpdateChannel(...) from diagram
    // returns channelId — required before insertProgram
    public long insertChannel(ServiceInfo info, String displayNumber) {
        ContentValues v = new ContentValues();
        v.put(TvContract.Channels.COLUMN_INPUT_ID, inputId);
        v.put(TvContract.Channels.COLUMN_DISPLAY_NAME, info.name);
        v.put(TvContract.Channels.COLUMN_DISPLAY_NUMBER, displayNumber);
        v.put(TvContract.Channels.COLUMN_SERVICE_TYPE,
                TvContract.Channels.SERVICE_TYPE_AUDIO_VIDEO);
        v.put(TvContract.Channels.COLUMN_TYPE,
                TvContract.Channels.TYPE_OTHER);
        v.put(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_ID,
                info.serviceId);
        // streamUri stored as bytes — retrieved during zapping
        String uriToStore = info.streamUri != null ? info.streamUri : "";
        v.put(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_DATA, uriToStore.getBytes());
        // DVB identifiers — critical for broadcast→OTT fallback
        v.put(TvContract.Channels.COLUMN_ORIGINAL_NETWORK_ID,
                info.broadcastOnid);
        v.put(TvContract.Channels.COLUMN_TRANSPORT_STREAM_ID,
                info.broadcastTsid);
        v.put(TvContract.Channels.COLUMN_SERVICE_ID,
                info.broadcastSid);


        Uri uri = resolver.insert(TvContract.Channels.CONTENT_URI, v);
        if (uri == null) {
            throw new RuntimeException("Channel insert into TV Provider failed");
        }
        return Long.parseLong(uri.getLastPathSegment());
    }

    // insertOrUpdateProgram(...) from diagram
    // channelId must come from insertChannel()
    public long insertProgram(ProgramInfo info) {
        ContentValues v = new ContentValues();
        v.put(TvContract.Programs.COLUMN_CHANNEL_ID, info.channelId);
        v.put(TvContract.Programs.COLUMN_TITLE, info.title);
        v.put(TvContract.Programs.COLUMN_SHORT_DESCRIPTION, info.description);
        v.put(TvContract.Programs.COLUMN_START_TIME_UTC_MILLIS,
                info.startTimeUtcMillis);
        v.put(TvContract.Programs.COLUMN_END_TIME_UTC_MILLIS,
                info.endTimeUtcMillis);
        v.put(TvContract.Programs.COLUMN_CONTENT_RATING,
                info.contentRating);


        Uri uri = resolver.insert(TvContract.Programs.CONTENT_URI, v);
        if (uri == null) {
            throw new RuntimeException("Program insert into TV Provider failed");
        }
        return Long.parseLong(uri.getLastPathSegment());
    }

    // queryChannel(channelUri) — used by OttSession during zapping
    public ChannelInfo queryChannel(Uri channelUri) {
        String[] projection = {
                TvContract.Channels._ID,
                TvContract.Channels.COLUMN_DISPLAY_NAME,
                TvContract.Channels.COLUMN_DISPLAY_NUMBER,
                TvContract.Channels.COLUMN_INTERNAL_PROVIDER_DATA,
                TvContract.Channels.COLUMN_INTERNAL_PROVIDER_ID,
                TvContract.Channels.COLUMN_ORIGINAL_NETWORK_ID,
                TvContract.Channels.COLUMN_TRANSPORT_STREAM_ID,
                TvContract.Channels.COLUMN_SERVICE_ID
        };
        try (Cursor cursor = resolver.query(
                channelUri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                ChannelInfo info = new ChannelInfo();
                info.channelId = cursor.getLong(0);
                info.displayName = cursor.getString(1);
                info.displayNumber = cursor.getString(2);
                byte[] data = cursor.getBlob(3);
                info.streamUri = (data != null) ? new String(data) : null;
                info.serviceId = cursor.getString(4);
                info.originalNetworkId = cursor.getInt(5);
                info.transportStreamId = cursor.getInt(6);
                info.dvbServiceId = cursor.getInt(7);
                return info;
            }
        }
        return null;
    }
    public ProgramInfo queryCurrentProgram(long channelId) {
        long now = System.currentTimeMillis();

        // Build URI for THIS specific channel's programs
        Uri programsUri = TvContract.buildProgramsUriForChannel(channelId);

        // Keep the projection the same
        String[] projection = {
                TvContract.Programs._ID,
                TvContract.Programs.COLUMN_CHANNEL_ID,
                TvContract.Programs.COLUMN_TITLE,
                TvContract.Programs.COLUMN_SHORT_DESCRIPTION,
                TvContract.Programs.COLUMN_START_TIME_UTC_MILLIS,
                TvContract.Programs.COLUMN_END_TIME_UTC_MILLIS,
                TvContract.Programs.COLUMN_CONTENT_RATING
        };

        // FIX: PASS NULL FOR SELECTION.
        // This tells Android: "Just give me everything for THIS channel, I won't ask for specific times."
        try (Cursor cursor = resolver.query(programsUri, projection, null, null, null)) {

            if (cursor != null) {
                while (cursor.moveToNext()) {
                    long start = cursor.getLong(4);
                    long end = cursor.getLong(5);

                    // Perform the "Selection" manually in Java to avoid SecurityException
                    if (now >= start && now <= end) {
                        ProgramInfo info = new ProgramInfo(
                                cursor.getLong(1), // channelId
                                cursor.getString(2), // title
                                cursor.getString(3), // description
                                start,
                                end,
                                cursor.getString(6)  // rating
                        );
                        info.programId = cursor.getLong(0);
                        return info;
                    }
                }
            }
        } catch (Exception e) {
            android.util.Log.e("OttRepo", "Security or IO Error: " + e.getMessage());
        }
        return null;
    }
}