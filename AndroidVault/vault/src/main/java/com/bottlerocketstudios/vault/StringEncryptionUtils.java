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

import android.util.Base64;
import android.util.Pair;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

/**
 * Tools to perform cryptographic transformations on strings resulting in Base64 encoded strings.
 */
public class StringEncryptionUtils {
    private static final String TAG = StringEncryptionUtils.class.getSimpleName();

    private static final byte HEADER_MAGIC_NUMBER = 121;
    private static final byte HEADER_VERSION = 1;
    private static final int HEADER_IV_OFFSET = 2;
    private static final int INTEGER_SIZE_BYTES = Integer.SIZE / 8;
    private static final int HEADER_METADATA_SIZE = HEADER_IV_OFFSET + INTEGER_SIZE_BYTES;

    /**
     * Generate a Base64 encoded string containing an AES encrypted version of cleartext using the provided seed to generate a key.
     */
    public static String encrypt(SecretKey key, String clearText, String charset, String transform) throws UnsupportedEncodingException, IllegalBlockSizeException, InvalidKeyException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException {
        if (clearText == null) return null;

        byte[] result = encrypt(key, clearText.getBytes(charset), transform);
        return Base64.encodeToString(result, Base64.DEFAULT);
    }

    /**
     * Decode a Base64 encoded string into a cleartext string using the provided key and charset.
     * @throws UnencryptedException
     */
    public static String decrypt(SecretKey key, String encrypted, String charset, String transform) throws UnencryptedException, GeneralSecurityException, UnsupportedEncodingException {
        if (encrypted == null) return null;

        try {
            byte[] enc = Base64.decode(encrypted, Base64.DEFAULT);
            byte[] result = decrypt(key, enc, transform);
            if (result != null) {
                return new String(result, charset);
            }
        } catch (IllegalArgumentException e) {
            throw new UnencryptedException("Encrypted String was not base64 encoded.", e);
        }
        return null;
    }

    private static byte[] createIvHeader(byte[] iv) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + HEADER_METADATA_SIZE);
        byteBuffer.put(HEADER_MAGIC_NUMBER);
        byteBuffer.put(HEADER_VERSION);
        byteBuffer.putInt(iv.length);
        byteBuffer.put(iv);
        return byteBuffer.array();
    }

    private static Pair<byte[], byte[]> readIvFromHeader(byte[] encrypted) throws GeneralSecurityException {
        if (encrypted == null) return null;
        ByteBuffer encryptedBuffer = ByteBuffer.wrap(encrypted);
        if (encrypted.length <= HEADER_METADATA_SIZE) {
            throw new GeneralSecurityException("Not enough data");
        } else if (encryptedBuffer.get() != HEADER_MAGIC_NUMBER) {
            throw new GeneralSecurityException("Invalid header");
        } else if (encryptedBuffer.get() != HEADER_VERSION) {
            throw new GeneralSecurityException("Incorrect header version");
        }

        int ivSize = encryptedBuffer.getInt();
        byte[] iv = null;
        if (ivSize > 0) {
            iv = new byte[ivSize];
            encryptedBuffer.get(iv, 0, ivSize);
        }

        int dataSize = encrypted.length - (HEADER_METADATA_SIZE + ivSize);
        byte[] data = new byte[dataSize];
        encryptedBuffer.get(data, 0, dataSize);

        return new Pair<>(iv, data);
    }

    private static byte[] encrypt(SecretKey key, byte[] clearText, String transform) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Cipher cipher = Cipher.getInstance(transform);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] data = cipher.doFinal(clearText);
        byte[] header = createIvHeader(cipher.getIV());
        return concatByteArrays(header, data);
    }

    private static byte[] decrypt(SecretKey key, byte[] encrypted, String transform) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(transform);
        Pair<byte[], byte[]> dataPair = readIvFromHeader(encrypted);
        if (dataPair != null) {
            if (dataPair.first == null) {
                cipher.init(Cipher.DECRYPT_MODE, key);
            } else {
                cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(dataPair.first));
            }
            return cipher.doFinal(dataPair.second);
        }
        return null;
    }

    /**
     * Exception thrown when content that was provided for decryption was not encrypted.
     */
    public static class UnencryptedException extends Throwable {

        public UnencryptedException() {
            super();
        }

        public UnencryptedException(String detailMessage) {
            super(detailMessage);
        }

        public UnencryptedException(String message, Throwable cause) {
            super(message, cause);
        }

        public UnencryptedException(Throwable cause) {
            super(cause);
        }
    }

    /**
     * Combine two byte arrays into one and return it.
     * @return A byte array with a then b
     */
    public static byte[] concatByteArrays(byte[] a, byte[] b) {
        byte[] c= new byte[a.length + b.length];
        System.arraycopy(a, 0, c, 0, a.length);
        System.arraycopy(b, 0, c, a.length, b.length);

        return c;
    }
}
