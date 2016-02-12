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

package com.bottlerocketstudios.vault.salt;

import java.util.Arrays;

/**
 * Provides a predefined salt. Primarily useful for using a pre-calculated salt for a PBKDF.
 */
public class SpecificSaltGenerator implements SaltGenerator {

    private final byte[] mSaltBytes;

    public SpecificSaltGenerator(byte[] saltBytes) {
        mSaltBytes = saltBytes;
    }

    @Override
    public byte[] createSaltBytes(int size) {
        if (size > mSaltBytes.length) throw new IndexOutOfBoundsException("Requested salt size exceeds amount available");
        return Arrays.copyOf(mSaltBytes, size);
    }
}
