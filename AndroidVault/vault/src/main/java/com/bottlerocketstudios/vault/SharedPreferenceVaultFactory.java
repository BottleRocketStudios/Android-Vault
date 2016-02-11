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

package com.bottlerocketstudios.vault;

import android.annotation.TargetApi;
import android.app.KeyguardManager;
import android.content.Context;
import android.os.Build;
import android.text.TextUtils;

import com.bottlerocketstudios.vault.keys.generator.Aes256RandomKeyFactory;
import com.bottlerocketstudios.vault.keys.storage.CompatSharedPrefKeyStorageFactory;
import com.bottlerocketstudios.vault.keys.storage.KeyStorage;
import com.bottlerocketstudios.vault.keys.storage.KeychainAuthenticatedKeyStorage;
import com.bottlerocketstudios.vault.salt.PrngSaltGenerator;

import java.security.GeneralSecurityException;


/**
 * Factory to generate SharedPreference backed secure storage vaults.
 */
public class SharedPreferenceVaultFactory {

    /**
     * Create an unkeyed vault. Use this when you wish to set the key later based on a user provided password
     * or when using a specific key strategy. The vault will be unusable until the SecretKey is set using
     * {@link SharedPreferenceVault#rekeyStorage(javax.crypto.SecretKey)}. You must verify
     * that the vault is not already keyed using {@link SharedPreferenceVault#isKeyAvailable()}
     *
     * <p>
     *     For devices running Android below 18, an obfuscated storage system will be used to store the key.
     *     For devices using Android 18+ the AndroidKeystore secure storage will be used. If an application
     *     was running on a device that was below 18 and the device was upgraded to 18+ the key
     *     storage will be upgraded automatically and migrate the key.
     * </p>
     *
     * @param context           Application context
     * @param prefFileName      Preference file name to be used for storage of key and data
     * @param keyFileName       Preference file name to store keys in, must be different than prefFileName
     * @param keyAlias          Alias of preference key, must be unique within application.
     * @param keyIndex          Index of salt used in obfuscation storage.
     * @param presharedSecret   Application provided information in obfuscation storage. Must remain constant through app upgrades. Should be unique to the app.
     * @param enableExceptions  Allow wrapping and rethrowing of checked exceptions as RuntimeExceptions to maintain compatibility with SharedPreference Interface.
     * @throws GeneralSecurityException
     */
    public static SharedPreferenceVault getCompatAes256Vault(Context context, String prefFileName, String keyFileName, String keyAlias, int keyIndex, String presharedSecret, boolean enableExceptions) throws GeneralSecurityException {
        if (TextUtils.equals(prefFileName, keyFileName)) {
            throw new IllegalArgumentException("Pref file and key file cannot be the same file.");
        }
        KeyStorage keyStorage = CompatSharedPrefKeyStorageFactory.createKeyStorage(context, Build.VERSION.SDK_INT, keyFileName, keyAlias, keyIndex, EncryptionConstants.AES_CIPHER, presharedSecret, new PrngSaltGenerator());
        return new StandardSharedPreferenceVault(context, keyStorage, prefFileName, EncryptionConstants.AES_CBC_PADDED_TRANSFORM, enableExceptions);
    }

    /**
     * @see SharedPreferenceVaultFactory#getCompatAes256Vault(Context, String, String, String, int, String, boolean)
     */
    public static SharedPreferenceVault getCompatAes256Vault(Context context, String prefFileName, String keyFileName, String keyAlias, int keyIndex, String presharedSecret) throws GeneralSecurityException {
        return getCompatAes256Vault(context, prefFileName, keyFileName, keyAlias, keyIndex, presharedSecret, false);
    }

    /**
     * Create an application keyed pseudo random vault for storage of secure information. Use this when
     * there is no ability to secure the information using the user's password e.g. API client tokens and sensitive app configuration.
     *
     * @see com.bottlerocketstudios.vault.SharedPreferenceVaultFactory#getCompatAes256Vault(android.content.Context, String, String, String, int, String)
     *
     * @param context           Application context
     * @param prefFileName      Preference file name to be used for storage of key and data
     * @param keyFileName       Preference file name to store keys in, must be different than prefFileName
     * @param keyAlias          Alias of preference key, must be unique within application.
     * @param keyIndex          Index of salt used in obfuscation storage.
     * @param presharedSecret   Application provided information in obfuscation storage. Must remain constant through app upgrades. Should be unique to the app.
     * @param enableExceptions  Allow wrapping and rethrowing of checked exceptions as RuntimeExceptions to maintain compatibility with SharedPreference Interface.
     * @throws GeneralSecurityException
     */
    public static SharedPreferenceVault getAppKeyedCompatAes256Vault(Context context, String prefFileName, String keyFileName, String keyAlias, int keyIndex, String presharedSecret, boolean enableExceptions) throws GeneralSecurityException {
        SharedPreferenceVault sharedPreferenceVault = getCompatAes256Vault(context, prefFileName, keyFileName, keyAlias, keyIndex, presharedSecret, enableExceptions);
        if (!sharedPreferenceVault.isKeyAvailable()) {
            sharedPreferenceVault.rekeyStorage(Aes256RandomKeyFactory.createKey());
        }
        return sharedPreferenceVault;
    }

    /**
     * @see SharedPreferenceVaultFactory#getAppKeyedCompatAes256Vault(Context, String, String, String, int, String, boolean)
     */
    public static SharedPreferenceVault getAppKeyedCompatAes256Vault(Context context, String prefFileName, String keyFileName, String keyAlias, int keyIndex, String presharedSecret) throws GeneralSecurityException {
        return getAppKeyedCompatAes256Vault(context, prefFileName, keyFileName, keyAlias, keyIndex, presharedSecret, false);
    }

    /**
     * Create a vault that uses the operating system's built in keystore locking mechanism. Whenever
     * the device has not been unlocked in a specified amount of time, reading from this vault will
     * throw a {@link android.security.keystore.KeyPermanentlyInvalidatedException} or {@link android.security.keystore.UserNotAuthenticatedException}.
     *
     * @param context                Application context.
     * @param prefFileName           Preference file name to be used for storage of data.
     * @param keyAlias               Alias of Keystore key, must be unique within application.
     * @param authDurationSeconds    Time in seconds to allow use of the key without requiring authentication.
     * @throws GeneralSecurityException
     */
    @TargetApi(Build.VERSION_CODES.M)
    public static SharedPreferenceVault getKeychainAuthenticatedAes256Vault(Context context, String prefFileName, String keyAlias, int authDurationSeconds) throws GeneralSecurityException {
        KeyStorage keyStorage = new KeychainAuthenticatedKeyStorage(keyAlias, EncryptionConstants.AES_CIPHER, EncryptionConstants.BLOCK_MODE_CBC, EncryptionConstants.ENCRYPTION_PADDING_PKCS7, authDurationSeconds);

        SharedPreferenceVault sharedPreferenceVault = new StandardSharedPreferenceVault(context, keyStorage, prefFileName, EncryptionConstants.AES_CBC_PADDED_TRANSFORM_ANDROID_M, true);
        if (!sharedPreferenceVault.isKeyAvailable()) {
            sharedPreferenceVault.rekeyStorage(null);
        }
        return sharedPreferenceVault;
    }

    @TargetApi(Build.VERSION_CODES.M)
    public static boolean canUseKeychainAuthentication(Context context) {
        KeyguardManager keyguardManager = context.getSystemService(KeyguardManager.class);
        return keyguardManager.isKeyguardSecure();
    }

}
