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
import android.os.Build;
import android.util.Log;

import com.bottlerocketstudios.vault.keys.wrapper.AndroidKeystoreSecretKeyWrapper;
import com.bottlerocketstudios.vault.keys.wrapper.ObfuscatingSecretKeyWrapper;
import com.bottlerocketstudios.vault.keys.wrapper.SecretKeyWrapper;
import com.bottlerocketstudios.vault.salt.SaltGenerator;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.SecretKey;

/**
 * Create the appropriate version of key storage based on current API version and migrate any previous
 * version's keys to the new version.
 */
public class CompatSharedPrefKeyStorageFactory {
    private static final String TAG = CompatSharedPrefKeyStorageFactory.class.getSimpleName();

    private static final String PREF_COMPAT_FACTORY_SDK_INT_ROOT = "compatFactorySdkInt.";
    private static final String PREF_COMPAT_FACTORY_ANDROID_KEYSTORE_TEST_STATE_ROOT = "androidKeystoreTestState.";

    private static final List<String> BAD_HARDWARE_MODELS = new ArrayList<>();
    static {
        BAD_HARDWARE_MODELS.add("SGH-T889"); //Galaxy Note 2 nukes hardware keystore on PIN unlock.
    }

    /**
     * Provided with the SDK version, create or upgrade the best version for the device.
     *
     * @param currentSdkInt     The current SDK version of the device. Use android.os.Build.VERSION.SDK_INT. Left as a parameter for unit testing.
     * @param prefFileName      Preference file in which to store key material
     * @param keystoreAlias     Key alias to uniquely identify this key in the application
     * @param saltIndex         Salt index to uniquely identify this key in the application for pre 18 devices
     * @param cipherAlgorithm   Cipher algorithm that the key will be used in
     * @param presharedSecret   Secret to add depth for pre 18 devices
     * @param saltGenerator     Salt source for pre 18 devices
     *
     * @throws GeneralSecurityException
     */
    public static KeyStorage createKeyStorage(Context context, int currentSdkInt, String prefFileName, String keystoreAlias, int saltIndex, String cipherAlgorithm, String presharedSecret, SaltGenerator saltGenerator) throws GeneralSecurityException {
        KeyStorage result = null;
        int oldSdkInt = readOldSdkInt(context, prefFileName, keystoreAlias);

        //Check to see if we have crossed an upgrade boundary and attempt to upgrade if so.
        if (doesRequireKeyUpgrade(oldSdkInt, currentSdkInt)) {
            try {
                result = upgradeKeyStorage(context, oldSdkInt, currentSdkInt, prefFileName, keystoreAlias, saltIndex, cipherAlgorithm, presharedSecret, saltGenerator);
            } catch (GeneralSecurityException e) {
                Log.e(TAG, "Upgrade resulted in an exception", e);
                result = null;
            }
        }

        //Upgrade failed or was unnecessary, get the latest appropriate version of the KeyStorage.
        if (result == null) {
            result = createVersionAppropriateKeyStorage(context, currentSdkInt, prefFileName, keystoreAlias, saltIndex, cipherAlgorithm, presharedSecret, saltGenerator);
        }

        if (result != null) writeCurrentSdkInt(context, currentSdkInt, prefFileName, keystoreAlias);

        return result;
    }

    private static KeyStorage upgradeKeyStorage(Context context, int oldSdkInt, int currentSdkInt, String prefFileName, String keystoreAlias, int saltIndex, String cipherAlgorithm, String presharedSecret, SaltGenerator saltGenerator) throws GeneralSecurityException {
        KeyStorage oldKeyStorage = createVersionAppropriateKeyStorage(context, oldSdkInt, prefFileName, keystoreAlias, saltIndex, cipherAlgorithm, presharedSecret, saltGenerator);
        SecretKey secretKey = oldKeyStorage.loadKey(context);
        if (secretKey != null) {
            KeyStorage newKeyStorage = createVersionAppropriateKeyStorage(context, currentSdkInt, prefFileName, keystoreAlias, saltIndex, cipherAlgorithm, presharedSecret, saltGenerator);
            if (newKeyStorage.saveKey(context, secretKey)) {
               return newKeyStorage;
            }
        }
        return null;
    }

    private static KeyStorage createVersionAppropriateKeyStorage(Context context, int currentSdkInt, String prefFileName, String keystoreAlias, int saltIndex, String cipherAlgorithm, String presharedSecret, SaltGenerator saltGenerator) throws GeneralSecurityException {
        SecretKeyWrapper secretKeyWrapper = null;

        if (currentSdkInt >= Build.VERSION_CODES.JELLY_BEAN_MR2 && !isBadHardware() && canUseAndroidKeystore(context, prefFileName, keystoreAlias, currentSdkInt)) {
            secretKeyWrapper = new AndroidKeystoreSecretKeyWrapper(context, keystoreAlias);
        } else {
            secretKeyWrapper = new ObfuscatingSecretKeyWrapper(context, saltIndex, saltGenerator, presharedSecret);
        }
        return new SharedPrefKeyStorage(secretKeyWrapper, prefFileName, keystoreAlias, cipherAlgorithm);
    }

    private static boolean canUseAndroidKeystore(Context context, String prefFileName, String keystoreAlias, int currentSdkInt) {
        AndroidKeystoreTestState androidKeystoreTestState = readAndroidKeystoreTestState(context, prefFileName, keystoreAlias);
        if (AndroidKeystoreTestState.UNTESTED.equals(androidKeystoreTestState)) {
            androidKeystoreTestState = performAndroidKeystoreTest(context, keystoreAlias, currentSdkInt);
            writeAndroidKeystoreTestState(context, prefFileName, keystoreAlias, androidKeystoreTestState);
        }
        return AndroidKeystoreTestState.PASS.equals(androidKeystoreTestState);
    }

    private static AndroidKeystoreTestState performAndroidKeystoreTest(Context context, String keystoreAlias, int currentSdkInt) {
        AndroidKeystoreTestState androidKeystoreTestState = AndroidKeystoreTestState.FAIL;
        if (currentSdkInt >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            try {
                AndroidKeystoreSecretKeyWrapper androidKeystoreSecretKeyWrapper = new AndroidKeystoreSecretKeyWrapper(context, keystoreAlias);
                androidKeystoreTestState = androidKeystoreSecretKeyWrapper.testKey() ? AndroidKeystoreTestState.PASS : AndroidKeystoreTestState.FAIL;
            } catch (GeneralSecurityException|IOException|IllegalStateException e) {
                Log.e(TAG, "Caught an exception while creating the AndroidKeystoreSecretKeyWrapper", e);
                androidKeystoreTestState = AndroidKeystoreTestState.FAIL;
            }
        }

        if (AndroidKeystoreTestState.FAIL.equals(androidKeystoreTestState)) {
            Log.w(TAG, "This device failed the AndroidKeystoreSecretKeyWrapper test.");
        }
        return androidKeystoreTestState;
    }

    private static String getAndroidKeystoreTestStateSharedPreferenceKey(String keystoreAlias) {
        return PREF_COMPAT_FACTORY_ANDROID_KEYSTORE_TEST_STATE_ROOT + keystoreAlias;
    }

    private static void writeAndroidKeystoreTestState(Context context, String prefFileName, String keystoreAlias, AndroidKeystoreTestState androidKeystoreTestState) {
        getSharedPreferences(context, prefFileName).edit()
                .putString(getAndroidKeystoreTestStateSharedPreferenceKey(keystoreAlias), androidKeystoreTestState.toString())
                .apply();
    }

    private static AndroidKeystoreTestState readAndroidKeystoreTestState(Context context, String prefFileName, String keystoreAlias) {
        String prefValue = getSharedPreferences(context, prefFileName).getString(getAndroidKeystoreTestStateSharedPreferenceKey(keystoreAlias), AndroidKeystoreTestState.UNTESTED.toString());
        AndroidKeystoreTestState androidKeystoreTestState;
        try {
            androidKeystoreTestState = AndroidKeystoreTestState.valueOf(prefValue);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Failed to parse previous test state");
            androidKeystoreTestState = AndroidKeystoreTestState.UNTESTED;
        }
        return androidKeystoreTestState;
    }

    private static String getCurrentSdkIntSharedPreferenceKey(String keystoreAlias) {
        return PREF_COMPAT_FACTORY_SDK_INT_ROOT + keystoreAlias;
    }

    private static void writeCurrentSdkInt(Context context, int currentSdkInt, String prefFileName, String keystoreAlias) {
        SharedPreferences.Editor editor = getSharedPreferences(context, prefFileName).edit();
        editor.putInt(getCurrentSdkIntSharedPreferenceKey(keystoreAlias), currentSdkInt);
        editor.apply();
    }

    private static int readOldSdkInt(Context context, String prefFileName, String keystoreAlias) {
        SharedPreferences sharedPreferences = getSharedPreferences(context, prefFileName);
        return sharedPreferences.getInt(getCurrentSdkIntSharedPreferenceKey(keystoreAlias), 0);
    }

    /**
     * Determine if the device has just had the OS upgraded across the JELLY_BEAN_MR2 barrier.
     *
     * @return True if the KeyStorage is crossing the barrier.
     */
    private static boolean doesRequireKeyUpgrade(int oldSdkInt, int currentSdkInt) {
        return (oldSdkInt > 0 && oldSdkInt < currentSdkInt && oldSdkInt < Build.VERSION_CODES.JELLY_BEAN_MR2 && currentSdkInt >= Build.VERSION_CODES.JELLY_BEAN_MR2 && !isBadHardware());
    }

    /**
     * Return shared preference file to use for encrypted key storage
     *
     * @return Shared preference file.
     */
    protected static SharedPreferences getSharedPreferences(Context context, String prefFileName) {
        return context.getSharedPreferences(prefFileName, Context.MODE_PRIVATE);
    }

    private static boolean isBadHardware() {
        return BAD_HARDWARE_MODELS.contains(Build.MODEL);
    }
}
