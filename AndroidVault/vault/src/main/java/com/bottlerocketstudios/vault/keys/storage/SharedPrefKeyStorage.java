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

package com.bottlerocketstudios.vault.keys.storage;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import com.bottlerocketstudios.vault.keys.wrapper.SecretKeyWrapper;

import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.crypto.SecretKey;

/**
 * Storage system using SharedPreference file to retain SecretKeys.
 */
public class SharedPrefKeyStorage implements KeyStorage {

    private static final String TAG = SharedPrefKeyStorage.class.getSimpleName();

    private static final String PREF_ROOT = "vaultedBlobV2.";

    private final SecretKeyWrapper mSecretKeyWrapper;
    private final String mPrefFileName;
    private final String mKeystoreAlias;
    private final String mCipherAlgorithm;
    private SecretKey mCachedSecretKey;
    private final String mKeyLock = "keyLock";

    public SharedPrefKeyStorage(SecretKeyWrapper secretKeyWrapper, String prefFileName, String keystoreAlias, String cipherAlgorithm) {
        mSecretKeyWrapper = secretKeyWrapper;
        mPrefFileName = prefFileName;
        mKeystoreAlias = keystoreAlias;
        mCipherAlgorithm = cipherAlgorithm;
    }

    @Override
    public SecretKey loadKey(Context context) {
        if (mCachedSecretKey == null) {
            //Only allow one thread at a time load the key.
            synchronized (mKeyLock) {
                //If the other thread updated the key, don't re-load it.
                if (mCachedSecretKey == null) {
                    mCachedSecretKey = loadSecretKey(context, mKeystoreAlias, mCipherAlgorithm);
                }
            }
        }
        return mCachedSecretKey;
    }

    @Override
    public boolean saveKey(Context context, SecretKey secretKey) {
        boolean success;
        synchronized (mKeyLock) {
            success = storeSecretKey(context, mKeystoreAlias, secretKey);
            //Clear the cached key upon failure to save.
            mCachedSecretKey = success ? secretKey : null;
        }
        return success;
    }

    @Override
    public void clearKey(Context context) {
        mCachedSecretKey = null;
        storeSecretKey(context, mKeystoreAlias, null);
        try {
            mSecretKeyWrapper.clearKey(context);
        } catch (GeneralSecurityException | IOException e) {
            Log.e(TAG, "Failed to clearKey in wrapper", e);
        }
    }

    /**
     * Return shared preference file to use for encrypted key storage
     */
    protected SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(mPrefFileName, Context.MODE_PRIVATE);
    }

    /**
     * Return key used to store in shared preferences.
     */
    protected String getSharedPreferenceKey(String keystoreAlias) {
        return PREF_ROOT + keystoreAlias;
    }

    /**
     * Use the SecretKeyWrapper secure storage to read the key in a securely wrapped format
     * @return Secret key loaded from storage or null
     */
    protected SecretKey loadSecretKey(Context context, String keystoreAlias, String cipherAlgorithm) {
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        String encrypted = sharedPreferences.getString(getSharedPreferenceKey(keystoreAlias), null);
        if (encrypted != null) {
            try {
                byte[] enc = Base64.decode(encrypted, Base64.DEFAULT);
                return mSecretKeyWrapper.unwrap(enc, cipherAlgorithm);
            } catch (GeneralSecurityException | RuntimeException | IOException e) {
                Log.e(TAG, "load failed", e);
            }
        }
        return null;
    }

    /**
     * Use the SecretKeyWrapper secure storage to write the key in a securely wrapped format
     * @return True if save was successful
     */
    protected boolean storeSecretKey(Context context, String keystoreAlias, SecretKey secretKey) {
        SharedPreferences.Editor editor = getSharedPreferences(context).edit();
        if (secretKey == null) {
            editor.remove(getSharedPreferenceKey(keystoreAlias));
            editor.apply();
            return true;
        } else {
            try {
                byte[] wrappedKey = mSecretKeyWrapper.wrap(secretKey);
                String encoded = Base64.encodeToString(wrappedKey, Base64.DEFAULT);
                editor.putString(getSharedPreferenceKey(keystoreAlias), encoded);
                editor.apply();
                return true;
            } catch (GeneralSecurityException | IOException | RuntimeException e) {
                Log.e(TAG, "save failed", e);
            }
        }
        return false;
    }

    @Override
    public boolean hasKey(Context context) {
        SecretKey secretKey = loadKey(context);
        return secretKey != null;
    }

    @Override
    public KeyStorageType getKeyStorageType() {
        return mSecretKeyWrapper.getKeyStorageType();
    }
}
