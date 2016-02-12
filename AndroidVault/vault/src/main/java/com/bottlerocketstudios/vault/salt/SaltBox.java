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
    private static final String DEFAULT_SHARED_PREF_FILE = "NaCl";

    @SuppressWarnings("unused")
    private static final String TAG = SaltBox.class.getSimpleName();

    private static SparseArray<byte[]> sStoredBits = new SparseArray<>();

    /**
     * This method will use the default shared preference file. Not recommended for external use.
     * @see SaltBox#getStoredBits(Context, int, int, String)
     */
    @Deprecated
    public static byte[] getStoredBits(Context context, int saltIndex, int requestedSize) {
        return getStoredBits(context, saltIndex, requestedSize, DEFAULT_SHARED_PREF_FILE);
    }

    /**
     * Return previously stored byte array. If the default constructor is used, care must be taken
     * to avoid collision with Vault/Key indices with the saltIndex value. In other words, if you
     * use this class directly external to the factories provided by the Vault library, use a
     * different constructor.
     *
     * @param context                   Application context
     * @param saltIndex                 Preference file-wide unique index the bytes are stored in.
     * @param requestedSize             Number of bytes to be read, must match number of bytes stored exactly.
     * @param sharedPreferenceFileName  Preference file to store the salt in.
     * @return                          Byte array or null.
     */
    public static byte[] getStoredBits(Context context, int saltIndex, int requestedSize, String sharedPreferenceFileName) {
        String settingName = getSettingName(saltIndex);

        byte[] storedBits = getStoredBitsCache(saltIndex);

        if (isByteArrayInvalid(storedBits, requestedSize)) {
            //Try to load existing
            storedBits = loadStoredBitsFromPreferences(context, settingName, requestedSize, sharedPreferenceFileName);
            setStoredBitsCache(saltIndex, storedBits);
        }

        return storedBits;
    }

    private static String getSettingName(int saltIndex) {
        return String.format(Locale.US, SETTING_NAME_FORMAT, saltIndex);
    }

    /**
     * This method will use the default shared preference file. Not recommended for external use.
     * @see SaltBox#writeStoredBits(Context, int, byte[], int, String)
     */
    public static void writeStoredBits(Context context, int saltIndex, byte[] storedBits, int requestedSize) {
        writeStoredBits(context, saltIndex, storedBits, requestedSize, DEFAULT_SHARED_PREF_FILE);
    }

    /**
     * Write a byte array to storage. If the default constructor is used, care must be taken
     * to avoid collision with Vault/Key indices with the saltIndex value. In other words, if you
     * use this class directly external to the factories provided by the Vault library, use a
     * different sharedPrefFileName.
     * @param context               Application context
     * @param saltIndex             Preference file-wide unique index the bytes are stored in.
     * @param storedBits            Byte array to store or null to erase bytes at this index.
     * @param requestedSize         Number of bytes to be read, must match number of bytes stored exactly.
     * @param sharedPrefFileName    Preference file to store the salt in.
     */
    public static void writeStoredBits(Context context, int saltIndex, byte[] storedBits, int requestedSize, String sharedPrefFileName) {
        saveStoredBitsToPreferences(context, requestedSize, getSettingName(saltIndex), storedBits, sharedPrefFileName);
        if (isByteArrayInvalid(storedBits, requestedSize)) {
            setStoredBitsCache(saltIndex, null);
        } else {
            setStoredBitsCache(saltIndex, storedBits);
        }
    }

    private static boolean isByteArrayInvalid(byte[] storedBits, int requestedSize) {
        return storedBits == null || storedBits.length != requestedSize;
    }

    private static void saveStoredBitsToPreferences(Context context, int requestedSize, String settingName, byte[] storedBits, String sharedPrefFileName) {
        SharedPreferences.Editor sharedPrefsEditor = getSharedPreferences(context, sharedPrefFileName).edit();
        if (isByteArrayInvalid(storedBits, requestedSize)) {
            sharedPrefsEditor.remove(settingName);
        } else {
            String base64 = Base64.encodeToString(storedBits, Base64.DEFAULT);
            sharedPrefsEditor.putString(settingName, base64);
        }
        sharedPrefsEditor.apply();
    }

    private static byte[] loadStoredBitsFromPreferences(Context context, String settingName, int requestedSize, String sharedPrefFileName) {
        SharedPreferences sharedPrefs = getSharedPreferences(context, sharedPrefFileName);
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

    private static SharedPreferences getSharedPreferences(Context context, String sharedPrefFileName) {
        return context.getSharedPreferences(sharedPrefFileName, Context.MODE_PRIVATE);
    }

    private static byte[] getStoredBitsCache(int saltIndex) {
        return sStoredBits.get(saltIndex);
    }

    private static void setStoredBitsCache(int saltIndex, byte[] storedBits) {
        sStoredBits.put(saltIndex, storedBits);
    }
}
