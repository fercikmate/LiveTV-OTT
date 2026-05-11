package com.kafica.ott.util;

import android.content.Context;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

public class JsonResourceHelper {

    /**
     * Reads a JSON file from the res/raw folder.
     */
    public static String getJsonFromRaw(Context context, int resourceId) {
        try {
            InputStream is = context.getResources().openRawResource(resourceId);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}