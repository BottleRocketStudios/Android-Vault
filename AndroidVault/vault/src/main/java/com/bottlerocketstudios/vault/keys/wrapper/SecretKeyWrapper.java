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

package com.bottlerocketstudios.vault.keys.wrapper;

import android.content.Context;

import com.bottlerocketstudios.vault.keys.storage.KeyStorageType;

import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.crypto.SecretKey;

/**
 * Interface that implementations of secret key wrapping operations must implement.
 */
public interface SecretKeyWrapper {
    /**
     * Wrap a {@link javax.crypto.SecretKey} using the public key assigned to this wrapper.
     * Use {@link #unwrap(byte[], String)} to later recover the original
     * {@link javax.crypto.SecretKey}.
     *
     * @return a wrapped version of the given {@link javax.crypto.SecretKey} that can be
     * safely stored on untrusted storage.
     */
    byte[] wrap(SecretKey key) throws GeneralSecurityException, IOException;

    /**
     * Unwrap a {@link javax.crypto.SecretKey} using the private key assigned to this
     * wrapper.
     *
     * @param blob a wrapped {@link javax.crypto.SecretKey} as previously returned by
     *             {@link #wrap(javax.crypto.SecretKey)}.
     */
    SecretKey unwrap(byte[] blob, String wrappedKeyAlgorithm) throws GeneralSecurityException, IOException;

    /**
     * Change key material so that next wrapping will use a different key pair.
     * @throws GeneralSecurityException
     */
    void clearKey(Context context) throws GeneralSecurityException, IOException;

    public KeyStorageType getKeyStorageType();
}
