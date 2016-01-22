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

import android.util.Log;

import com.bottlerocketstudios.vault.salt.SaltGenerator;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Generate a SecretKey given a user supplied password string.
 */
public class PbkdfKeyGenerator {
    private static final String TAG = PbkdfKeyGenerator.class.getSimpleName();

    private static final String PBE_ALGORITHM = "PBKDF2WithHmacSHA1";

    private final int mPbkdf2Iterations;
    private final SaltGenerator mSaltGenerator;
    private final int mSaltSize;
    private final int mKeyLengthBits;

    public PbkdfKeyGenerator(int pbkdf2Iterations, int keyLengthBits, SaltGenerator saltGenerator, int saltSizeBytes) {
        mPbkdf2Iterations = pbkdf2Iterations;
        mSaltGenerator = saltGenerator;
        mSaltSize = saltSizeBytes;
        mKeyLengthBits = keyLengthBits;
    }

    public SecretKey generateKey(String keySource) {
        return createKeyWithPassword(keySource);
    }

    private SecretKey createKeyWithPassword(String password) {
        byte[] passwordSalt = mSaltGenerator.createSaltBytes(mSaltSize);
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), passwordSalt, mPbkdf2Iterations, mKeyLengthBits);
        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance(PBE_ALGORITHM);
            return skf.generateSecret(spec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            Log.e(TAG, "Failed to process key", e);
        }
        return null;
    }
}
