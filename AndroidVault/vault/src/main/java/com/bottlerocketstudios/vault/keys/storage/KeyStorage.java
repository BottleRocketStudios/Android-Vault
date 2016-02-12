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

import javax.crypto.SecretKey;

/**
 * Storage interface for secret key material.
 */
public interface KeyStorage {
    /**
     * Load key from storage
     * @return Key if available or null
     */
    SecretKey loadKey(Context context);

    /**
     * Save key to storage
     * @return True if successful
     */
    boolean saveKey(Context context, SecretKey secretKey);

    /**
     * Remove key from storage
     */
    void clearKey(Context context);

    /**
     * Determine if key is available
     */
    boolean hasKey(Context context);

    /**
     * Return the type of key storage used.
     */
    KeyStorageType getKeyStorageType();
}
