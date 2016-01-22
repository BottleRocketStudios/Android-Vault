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

package com.bottlerocketstudios.vault.keys.generator;

import java.security.NoSuchAlgorithmException;

import javax.crypto.spec.SecretKeySpec;

/**
 * Generate a secret key spec by performing a digest on a supplied set of bytes.
 */
public class SecretKeySpecGenerator {

    /**
     * Perform digest on seed to get key that will always be the same for any given seed.
     *
     * @throws java.security.NoSuchAlgorithmException
     */
    public static SecretKeySpec getFullKey(String seed, String digest, String cipherAlgorithm) throws NoSuchAlgorithmException {
        return getFullKey(seed.getBytes(), digest, cipherAlgorithm);
    }

    /**
     * Perform digest on seed to get key that will always be the same for any given seed.
     *
     * @throws java.security.NoSuchAlgorithmException
     */
    public static SecretKeySpec getFullKey(byte[] seed, String digest, String cipherAlgorithm) throws NoSuchAlgorithmException {
        java.security.MessageDigest md = java.security.MessageDigest.getInstance(digest);
        return new SecretKeySpec(md.digest(seed), cipherAlgorithm);
    }

    /**
     * Combine two byte arrays into one and return it.
     *
     * @return Concatenated byte array of a then b.
     */
    public static byte[] concatByteArrays(byte[] a, byte[] b) {
        byte[] c= new byte[a.length + b.length];
        System.arraycopy(a, 0, c, 0, a.length);
        System.arraycopy(b, 0, c, a.length, b.length);

        return c;
    }
}
