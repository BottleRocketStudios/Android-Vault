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

import com.bottlerocketstudios.vault.salt.SaltGenerator;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * Generate a random key using the supplied random source.
 */
public class RandomKeyGenerator {

    private final SaltGenerator mSaltGenerator;
    private final int mKeyLengthBits;

    public RandomKeyGenerator(SaltGenerator saltGenerator, int keyLengthBits) {
        mSaltGenerator = saltGenerator;
        mKeyLengthBits = keyLengthBits;
    }

    public SecretKey generateKey(String cipher) {
        return new SecretKeySpec(mSaltGenerator.createSaltBytes(mKeyLengthBits / 8), cipher);
    }
}
