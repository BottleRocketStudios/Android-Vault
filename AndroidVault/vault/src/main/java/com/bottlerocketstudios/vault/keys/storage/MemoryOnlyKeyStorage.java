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
 * Key storage that only stores the key in memory. No key is persisted to storage. This is really only
 * useful if the key is provided by the user via PBKDF or from some other secure source.
 */
public class MemoryOnlyKeyStorage implements KeyStorage {
    private SecretKey mSecretKey;

    @Override
    public SecretKey loadKey(Context context) {
        return mSecretKey;
    }

    @Override
    public boolean saveKey(Context context, SecretKey secretKey) {
        mSecretKey = secretKey;
        return true;
    }

    @Override
    public void clearKey(Context context) {
        mSecretKey = null;
    }

    @Override
    public boolean hasKey(Context context) {
        return mSecretKey != null;
    }
}
