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

package com.bottlerocketstudios.vault.keys.storage.hardware;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import com.bottlerocketstudios.vault.keys.storage.AndroidKeystoreTestState;
import com.bottlerocketstudios.vault.keys.wrapper.AbstractAndroidKeystoreSecretKeyWrapper;

import java.security.GeneralSecurityException;

/**
 * Created on 9/21/16.
 */
public abstract class AndroidKeystoreTester {
    private static final String TAG = AndroidKeystoreTester.class.getSimpleName();

    protected static final String PREF_COMPAT_FACTORY_ANDROID_KEYSTORE_TEST_STATE_ROOT = "androidKeystoreTestState.";

    private final Context mContext;
    private final String mKeystoreAlias;
    private final int mCurrentSdkInt;
    private final String mTestKeystoreAlias;

    public AndroidKeystoreTester(Context context, String keystoreAlias, int currentSdkInt) {
        mContext = context;
        mKeystoreAlias = keystoreAlias;
        mTestKeystoreAlias = keystoreAlias + "___TEST___";
        mCurrentSdkInt = currentSdkInt;
    }

    public boolean canUseAndroidKeystore(SharedPreferences sharedPreferences) {
        AndroidKeystoreTestState androidKeystoreTestState = readAndroidKeystoreTestState(sharedPreferences);
        if (AndroidKeystoreTestState.UNTESTED.equals(androidKeystoreTestState)) {
            androidKeystoreTestState = performAndroidKeystoreTest();
            writeAndroidKeystoreTestState(sharedPreferences, androidKeystoreTestState);
        }
        return AndroidKeystoreTestState.PASS.equals(androidKeystoreTestState);
    }

    private AndroidKeystoreTestState performAndroidKeystoreTest() {
        AndroidKeystoreTestState androidKeystoreTestState = AndroidKeystoreTestState.FAIL;
        if (mCurrentSdkInt >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            AbstractAndroidKeystoreSecretKeyWrapper androidKeystoreSecretKeyWrapper = null;
            try {
                androidKeystoreSecretKeyWrapper = createKeystoreSecretKeyWrapper(mContext, mTestKeystoreAlias);
                androidKeystoreTestState = androidKeystoreSecretKeyWrapper.testKey() ? AndroidKeystoreTestState.PASS : AndroidKeystoreTestState.FAIL;
            } catch (Throwable t) {
                Log.e(TAG, "Caught an exception while creating the AndroidKeystoreSecretKeyWrapper", t);
                androidKeystoreTestState = AndroidKeystoreTestState.FAIL;
            } finally {
                if (androidKeystoreSecretKeyWrapper != null) {
                    try {
                        androidKeystoreSecretKeyWrapper.clearKey(mContext);
                    } catch (Throwable t) {
                        Log.e(TAG, "Caught an exception while cleaning up the AndroidKeystoreSecretKeyWrapper", t);
                    }
                }
            }
        }

        if (AndroidKeystoreTestState.FAIL.equals(androidKeystoreTestState)) {
            Log.w(TAG, "This device failed the AndroidKeystoreSecretKeyWrapper test.");
        }
        return androidKeystoreTestState;
    }

    private void writeAndroidKeystoreTestState(SharedPreferences sharedPreferences, AndroidKeystoreTestState androidKeystoreTestState) {
        sharedPreferences.edit()
                .putString(getAndroidKeystoreTestStateSharedPreferenceKey(mKeystoreAlias), androidKeystoreTestState.toString())
                .apply();
    }

    protected AndroidKeystoreTestState readAndroidKeystoreTestState(SharedPreferences sharedPreferences) {
        String prefValue = sharedPreferences.getString(getAndroidKeystoreTestStateSharedPreferenceKey(mKeystoreAlias), AndroidKeystoreTestState.UNTESTED.toString());
        return parseAndroidKeystoreTestState(prefValue);
    }

    private AndroidKeystoreTestState parseAndroidKeystoreTestState(String prefValue) {
        AndroidKeystoreTestState androidKeystoreTestState;
        try {
            androidKeystoreTestState = AndroidKeystoreTestState.valueOf(prefValue);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Failed to parse previous test state");
            androidKeystoreTestState = AndroidKeystoreTestState.UNTESTED;
        }
        return androidKeystoreTestState;
    }

    protected abstract String getAndroidKeystoreTestStateSharedPreferenceKey(String keystoreAlias);

    protected abstract AbstractAndroidKeystoreSecretKeyWrapper createKeystoreSecretKeyWrapper(Context context, String testKeystoreAlias) throws GeneralSecurityException;
}
