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

import com.bottlerocketstudios.vault.keys.storage.hardware.AndroidKeystoreTester;
import com.bottlerocketstudios.vault.keys.storage.hardware.BadHardware;
import com.bottlerocketstudios.vault.keys.storage.hardware.LegacyAndroidKeystoreTester;
import com.bottlerocketstudios.vault.keys.storage.hardware.OaepAndroidKeystoreTester;
import com.bottlerocketstudios.vault.keys.storage.hardware.Pkcs1AndroidKeystoreTester;
import com.bottlerocketstudios.vault.keys.wrapper.AndroidKeystoreSecretKeyWrapper;
import com.bottlerocketstudios.vault.keys.wrapper.AndroidOaepKeystoreSecretKeyWrapper;
import com.bottlerocketstudios.vault.keys.wrapper.ObfuscatingSecretKeyWrapper;
import com.bottlerocketstudios.vault.keys.wrapper.SecretKeyWrapper;
import com.bottlerocketstudios.vault.salt.SaltGenerator;

import java.security.GeneralSecurityException;

import javax.crypto.SecretKey;

/**
 * Create the appropriate version of key storage based on current API version and migrate any previous
 * version's keys to the new version.
 */
public class CompatSharedPrefKeyStorageFactory {
    private static final String TAG = CompatSharedPrefKeyStorageFactory.class.getSimpleName();

    private static final String PREF_COMPAT_FACTORY_WRAPPER_TYPE = "compatFactoryWrapperType.";
    private static final String PREF_COMPAT_FACTORY_SDK_INT_ROOT = "compatFactorySdkInt.";

    static final int WRAPPER_TYPE_INVALID = 0;
    static final int WRAPPER_TYPE_OBFUSCATED = 1;
    static final int WRAPPER_TYPE_RSA_PKCS1 = 2;
    static final int WRAPPER_TYPE_RSA_OAEP = 3;

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
        int oldWrapperType = determineCurrentWrapperType(context, prefFileName, keystoreAlias);
        int bestSupportedWrapperType = determineBestSupportedWrapperType(context, currentSdkInt, prefFileName, keystoreAlias);

        return createKeyStorage(context, currentSdkInt, prefFileName, keystoreAlias, saltIndex, cipherAlgorithm, presharedSecret, saltGenerator, oldWrapperType, bestSupportedWrapperType);
    }

    /*
     * Default visibility for use when integration testing upgrade path.
     */
    static KeyStorage createKeyStorage(Context context, int currentSdkInt, String prefFileName, String keystoreAlias, int saltIndex, String cipherAlgorithm, String presharedSecret, SaltGenerator saltGenerator, int oldWrapperType, int newWrapperType) throws GeneralSecurityException {
        KeyStorage result = null;
        //If we are not using the best supported wrapper type, attempt an upgrade.
        if (doesRequireWrapperUpgrade(oldWrapperType, newWrapperType)) {
            try {
                result = upgradeKeyWrapper(context, oldWrapperType, newWrapperType, prefFileName, keystoreAlias, saltIndex, cipherAlgorithm, presharedSecret, saltGenerator);
            } catch (GeneralSecurityException e) {
                Log.e(TAG, "Upgrade resulted in an exception", e);
                result = null;
            }
        }

        //Upgrade failed or was unnecessary, get the latest appropriate version of the KeyStorage.
        if (result == null) {
            result = createKeyStorageForWrapperType(context, newWrapperType, prefFileName, keystoreAlias, saltIndex, cipherAlgorithm, presharedSecret, saltGenerator);
        }

        if (result != null) writeMetaInformation(context, prefFileName, keystoreAlias, newWrapperType, currentSdkInt);

        return result;
    }

    private static void writeMetaInformation(Context context, String prefFileName, String keystoreAlias, int wrapperType, int currentSdkInt) {
        writeWrapperType(context, prefFileName, keystoreAlias, wrapperType);
        writeCurrentSdkInt(context, prefFileName, keystoreAlias, currentSdkInt);
    }

    private static int determineCurrentWrapperType(Context context, String prefFileName, String keystoreAlias) {
        int currentWrapperType = readWrapperType(context, prefFileName, keystoreAlias);
        if (currentWrapperType == WRAPPER_TYPE_INVALID) {
            currentWrapperType = determineLegacyWrapperType(context, prefFileName, keystoreAlias);
        }
        return currentWrapperType;
    }

    private static int determineBestSupportedWrapperType(Context context, int currentSdkInt, String prefFileName, String keystoreAlias) {
        if (currentSdkInt >= Build.VERSION_CODES.JELLY_BEAN_MR2 && !BadHardware.isBadHardware()) {
            if (currentSdkInt >= Build.VERSION_CODES.M && canUseAndroidKeystore(new OaepAndroidKeystoreTester(context, keystoreAlias, currentSdkInt), getSharedPreferences(context, prefFileName))) {
                return WRAPPER_TYPE_RSA_OAEP;
            } else if (currentSdkInt < Build.VERSION_CODES.M && canUseAndroidKeystore(new Pkcs1AndroidKeystoreTester(context, keystoreAlias, currentSdkInt), getSharedPreferences(context, prefFileName))) {
                return WRAPPER_TYPE_RSA_PKCS1;
            }
        }
        return WRAPPER_TYPE_OBFUSCATED;
    }

    private static boolean canUseAndroidKeystore(AndroidKeystoreTester androidKeystoreTester, SharedPreferences sharedPreferences) {
        return androidKeystoreTester.canUseAndroidKeystore(sharedPreferences);
    }

    private static boolean doesRequireWrapperUpgrade(int oldWrapperType, int bestSupportedWrapperType) {
        return oldWrapperType != WRAPPER_TYPE_INVALID && oldWrapperType != bestSupportedWrapperType;
    }

    private static KeyStorage upgradeKeyWrapper(Context context, int oldWrapperType, int bestSupportedWrapperType, String prefFileName, String keystoreAlias, int saltIndex, String cipherAlgorithm, String presharedSecret, SaltGenerator saltGenerator) throws GeneralSecurityException {
        KeyStorage oldKeyStorage = createKeyStorageForWrapperType(context, oldWrapperType, prefFileName, keystoreAlias, saltIndex, cipherAlgorithm, presharedSecret, saltGenerator);
        SecretKey secretKey = oldKeyStorage.loadKey(context);
        if (secretKey != null) {
            oldKeyStorage.clearKey(context);
            KeyStorage newKeyStorage = createKeyStorageForWrapperType(context, bestSupportedWrapperType, prefFileName, keystoreAlias, saltIndex, cipherAlgorithm, presharedSecret, saltGenerator);
            if (newKeyStorage.saveKey(context, secretKey)) {
                return newKeyStorage;
            }
        }
        return null;
    }

    /**
     * In a previous version of the library, the key wrapper types were either obfuscated or not obfuscated, but the specific type
     * used was not stored. This method will determine which type was previously in use. It should only run on fresh installations
     * or migrations.
     */
    private static int determineLegacyWrapperType(Context context, String prefFileName, String keystoreAlias) {
        int oldSdkInt = readOldSdkInt(context, prefFileName, keystoreAlias);
        if (oldSdkInt == 0) {
            //This is a fresh installation.
            return WRAPPER_TYPE_INVALID;
        } else if (oldSdkInt > 0 && oldSdkInt < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            //This device is too old to have used the Android Keystore.
            return WRAPPER_TYPE_OBFUSCATED;
        } else if (LegacyAndroidKeystoreTester.hasAlreadyPassedTest(context, keystoreAlias, oldSdkInt, getSharedPreferences(context, prefFileName))) {
            //This device has a record of passing the Android Keystore test and must have been using the Android Keystore.
            return WRAPPER_TYPE_RSA_PKCS1;
        }
        //This device was new enough to take the AndroidKeystore test, but failed the test.
        return WRAPPER_TYPE_OBFUSCATED;
    }

    private static KeyStorage createKeyStorageForWrapperType(Context context, int wrapperType, String prefFileName, String keystoreAlias, int saltIndex, String cipherAlgorithm, String presharedSecret, SaltGenerator saltGenerator) throws GeneralSecurityException {
        SecretKeyWrapper secretKeyWrapper = null;

        switch (wrapperType) {
            case WRAPPER_TYPE_RSA_OAEP:
                secretKeyWrapper = new AndroidOaepKeystoreSecretKeyWrapper(context, keystoreAlias);
                break;
            case WRAPPER_TYPE_RSA_PKCS1:
                secretKeyWrapper = new AndroidKeystoreSecretKeyWrapper(context, keystoreAlias);
                break;
            case WRAPPER_TYPE_OBFUSCATED:
                secretKeyWrapper = new ObfuscatingSecretKeyWrapper(context, saltIndex, saltGenerator, presharedSecret);
                break;
            default:
                throw new IllegalArgumentException("Wrapper type " + wrapperType + " is invalid.");
        }
        return new SharedPrefKeyStorage(secretKeyWrapper, prefFileName, keystoreAlias, cipherAlgorithm);
    }

    private static String getCurrentWrapperTypeSharedPreferenceKey(String keystoreAlias) {
        return PREF_COMPAT_FACTORY_WRAPPER_TYPE + keystoreAlias;
    }

    private static void writeWrapperType(Context context, String prefFileName, String keystoreAlias, int wrapperType) {
        SharedPreferences.Editor editor = getSharedPreferences(context, prefFileName).edit();
        editor.putInt(getCurrentWrapperTypeSharedPreferenceKey(keystoreAlias), wrapperType);
        editor.apply();
    }

    private static int readWrapperType(Context context, String prefFileName, String keystoreAlias) {
        SharedPreferences sharedPreferences = getSharedPreferences(context, prefFileName);
        return sharedPreferences.getInt(getCurrentWrapperTypeSharedPreferenceKey(keystoreAlias), WRAPPER_TYPE_INVALID);
    }

    private static String getCurrentSdkIntSharedPreferenceKey(String keystoreAlias) {
        return PREF_COMPAT_FACTORY_SDK_INT_ROOT + keystoreAlias;
    }

    private static void writeCurrentSdkInt(Context context, String prefFileName, String keystoreAlias, int currentSdkInt) {
        SharedPreferences.Editor editor = getSharedPreferences(context, prefFileName).edit();
        editor.putInt(getCurrentSdkIntSharedPreferenceKey(keystoreAlias), currentSdkInt);
        editor.apply();
    }

    private static int readOldSdkInt(Context context, String prefFileName, String keystoreAlias) {
        SharedPreferences sharedPreferences = getSharedPreferences(context, prefFileName);
        return sharedPreferences.getInt(getCurrentSdkIntSharedPreferenceKey(keystoreAlias), 0);
    }

    /**
     * Return shared preference file to use for encrypted key storage
     *
     * @return Shared preference file.
     */
    protected static SharedPreferences getSharedPreferences(Context context, String prefFileName) {
        return context.getSharedPreferences(prefFileName, Context.MODE_PRIVATE);
    }

}
