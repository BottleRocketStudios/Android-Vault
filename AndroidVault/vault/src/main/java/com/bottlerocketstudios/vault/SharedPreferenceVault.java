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

import android.content.SharedPreferences;

import com.bottlerocketstudios.vault.keys.storage.KeyStorageType;

import javax.crypto.SecretKey;

/**
 * Shared Preferences backed vault for storing sensitive information.
 */
public interface SharedPreferenceVault extends SharedPreferences {

    /**
     * Interface to handle atypical results from the standard {@link Editor#commit()} and {@link Editor#apply()}
     */
    interface SharedPrefVaultWriteListener {
        void onSuccess();
        void onError();
    }

    /**
     * Remove all stored values and destroy cryptographic keys associated with the vault instance.
     * <strong>This will permanently destroy all data in the preference file.</strong>
     */
    void clearStorage();

    /**
     * Remove all stored values and destroy cryptographic keys associated with the vault instance.
     * Configure the vault to use the newly provided key for future data.
     * <strong>This will permanently destroy all data in the preference file.</strong>
     */
    void rekeyStorage(SecretKey secretKey);

    /**
     * Arbitrarily set the secret key to a specific value without removing any stored values. This is primarily
     * designed for {@link com.bottlerocketstudios.vault.keys.storage.MemoryOnlyKeyStorage} and typical
     * usage would be through the {@link #rekeyStorage(SecretKey)} method.
     * <strong>If this key is not the right key, existing data may become permanently unreadable.</strong>
     */
    void setKey(SecretKey secretKey);

    /**
     * Determine if this instance of storage currently has a valid key with which to encrypt values.
     */
    boolean isKeyAvailable();

    /**
     * Enable or disable logging operations.
     */
    void setDebugEnabled(boolean enabled);

    /**
     * Determine if logging is enabled.
     */
    boolean isDebugEnabled();

    /**
     * Method to find out expected security level of KeyStorage implementation being used.
     */
    KeyStorageType getKeyStorageType();

    /**
     * Add a listener to handle atypical behavior when {@link Editor#commit()} or {@link Editor#apply()} is used
     * with Vault.
     */
    SharedPreferenceVault setSharedPrefVaultWriteListener(SharedPrefVaultWriteListener listener);
}
