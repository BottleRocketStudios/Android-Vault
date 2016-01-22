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

/**
 * Constant values used by the various encryption methods.
 */
public class EncryptionConstants {
    public static final String AES_CIPHER = "AES";
    public static final String AES_CBC_PADDED_TRANSFORM = "AES/CBC/PKCS5Padding";
    public static final int AES_256_KEY_LENGTH_BITS = 256;

    public static final String DIGEST_SHA256 = "SHA256";
}
