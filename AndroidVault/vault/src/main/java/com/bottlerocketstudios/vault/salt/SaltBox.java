/*
 * Copyright (c) 2016. Bottle Rocket LLC
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bottlerocketstudios.vault.salt;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;
import android.util.SparseArray;

import java.util.Locale;

/**
 * Disk and in-memory cache of random salt used to aid obfuscated storage on older devices.
 */
public class SaltBox {
    private static final String SETTING_NAME_FORMAT = "NaCl-%1$d";
    private static final String SHARED_PREF_FILE = "NaCl";

    @SuppressWarnings("unused")
    private static final String TAG = SaltBox.class.getSimpleName();

    private static SparseArray<byte[]> sStoredBits = new SparseArray<>();

    public static byte[] getStoredBits(Context context, int saltIndex, int requestedSize) {

        String settingName = getSettingName(saltIndex);

        byte[] storedBits = getStoredBitsCache(saltIndex);

        if (isByteArrayInvalid(storedBits, requestedSize)) {
            //Try to load existing
            storedBits = loadStoredBitsFromPreferences(context, settingName, requestedSize);
            setStoredBitsCache(saltIndex, storedBits);
        }

        return storedBits;
    }

    private static String getSettingName(int saltIndex) {
        return String.format(Locale.US, SETTING_NAME_FORMAT, saltIndex);
    }

    public static void writeStoredBits(Context context, int saltIndex, byte[] storedBits, int requestedSize) {
        saveStoredBitsToPreferences(context, getSettingName(saltIndex), storedBits, requestedSize);
        if (isByteArrayInvalid(storedBits, requestedSize)) {
            setStoredBitsCache(saltIndex, null);
        } else {
            setStoredBitsCache(saltIndex, storedBits);
        }
    }

    private static boolean isByteArrayInvalid(byte[] storedBits, int requestedSize) {
        return storedBits == null || storedBits.length != requestedSize;
    }

    private static void saveStoredBitsToPreferences(Context context, String settingName, byte[] storedBits, int requestedSize) {
        SharedPreferences.Editor sharedPrefsEditor = getSharedPreferences(context).edit();
        if (isByteArrayInvalid(storedBits, requestedSize)) {
            sharedPrefsEditor.remove(settingName);
        } else {
            String base64 = Base64.encodeToString(storedBits, Base64.DEFAULT);
            sharedPrefsEditor.putString(settingName, base64);
        }
        sharedPrefsEditor.apply();
    }

    private static byte[] loadStoredBitsFromPreferences(Context context, String settingName, int requestedSize) {
        SharedPreferences sharedPrefs = getSharedPreferences(context);
        String base64 = sharedPrefs.getString(settingName, null);
        if (base64 != null) {
            try {
                byte[] storedBits = Base64.decode(base64, Base64.DEFAULT);
                if (isByteArrayInvalid(storedBits, requestedSize)) {
                    return null;
                } else {
                    return storedBits;
                }
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "Stored bits were not properly encoded", e);
            }
        }
        return null;
    }

    private static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(SHARED_PREF_FILE, Context.MODE_PRIVATE);
    }

    private static byte[] getStoredBitsCache(int saltIndex) {
        return sStoredBits.get(saltIndex);
    }

    private static void setStoredBitsCache(int saltIndex, byte[] storedBits) {
        sStoredBits.put(saltIndex, storedBits);
    }
}
