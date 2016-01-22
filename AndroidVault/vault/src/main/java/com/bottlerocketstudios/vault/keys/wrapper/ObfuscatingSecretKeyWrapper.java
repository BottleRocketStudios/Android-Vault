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

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import com.bottlerocketstudios.vault.CharacterEncodingConstants;
import com.bottlerocketstudios.vault.EncryptionConstants;
import com.bottlerocketstudios.vault.keys.generator.SecretKeySpecGenerator;
import com.bottlerocketstudios.vault.salt.SaltBox;
import com.bottlerocketstudios.vault.salt.SaltGenerator;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

/**
 * Creates an AES encrypted wrapped version of the key. This should only be used on less secure
 * devices with API &lt; 18. This combines some information built into the application (presharedSecret)
 * along with some data randomly generated upon first use. This will generate a unique key per
 * installation of the application.
 *

 */
public class ObfuscatingSecretKeyWrapper implements SecretKeyWrapper {
    private static final String TAG = ObfuscatingSecretKeyWrapper.class.getSimpleName();

    private static final int SALT_SIZE_BYTES = 512;
    private static final String WRAPPED_KEY_HASH = EncryptionConstants.DIGEST_SHA256;
    private static final String WRAPPED_KEY_ALGORITHM = EncryptionConstants.AES_CIPHER;
    private static final String WRAPPED_KEY_TRANSFORMATION = "AES/ECB/PKCS5Padding";

    private final Context mContext;
    private final int mSaltIndex;
    private final SaltGenerator mSaltGenerator;
    private final String mPresharedSecret;

    private SecretKey mWrappingKey;
    private byte[] mSalt;
    private Cipher mCipher;

    /**
     * Create a new instance of an Obfuscating SecretKey Wrapper.
     */
    @SuppressLint("GetInstance") //Suppressed ECB warning as we only wrap another AES key with it.
    public ObfuscatingSecretKeyWrapper(Context context, int saltIndex, SaltGenerator saltGenerator, String presharedSecret) throws NoSuchPaddingException, NoSuchAlgorithmException {
        mContext = context.getApplicationContext();
        mSaltIndex = saltIndex;
        mSaltGenerator = saltGenerator;
        mPresharedSecret = presharedSecret;
        mCipher = Cipher.getInstance(WRAPPED_KEY_TRANSFORMATION);
    }

    private SecretKey getWrappingKey(Context context) {
        if (mWrappingKey == null) {
            byte[] salt = getSalt(context);
            try {
                mWrappingKey = SecretKeySpecGenerator.getFullKey(SecretKeySpecGenerator.concatByteArrays(mPresharedSecret.getBytes(CharacterEncodingConstants.UTF_8), salt), WRAPPED_KEY_HASH, WRAPPED_KEY_ALGORITHM);
            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, "Caught java.io.UnsupportedEncodingException", e);
            } catch (NoSuchAlgorithmException e) {
                Log.e(TAG, "Caught java.security.NoSuchAlgorithmException", e);
            }
        }
        return mWrappingKey;
    }

    @Override
    public byte[] wrap(SecretKey key) throws GeneralSecurityException {
        mCipher.init(Cipher.WRAP_MODE, getWrappingKey(mContext));
        return mCipher.wrap(key);
    }

    @Override
    public SecretKey unwrap(byte[] blob, String wrappedKeyAlgorithm) throws GeneralSecurityException {
        mCipher.init(Cipher.UNWRAP_MODE, getWrappingKey(mContext));
        return (SecretKey) mCipher.unwrap(blob, wrappedKeyAlgorithm, Cipher.SECRET_KEY);
    }

    @Override
    public void clearKey(Context context) {
        mWrappingKey = null;
        mSalt = null;
        SaltBox.writeStoredBits(context, mSaltIndex, null, SALT_SIZE_BYTES);
    }

    private byte[] getSalt(Context context) {
        if (mSalt == null) {
            mSalt = SaltBox.getStoredBits(context, mSaltIndex, SALT_SIZE_BYTES);
            if (mSalt == null) {
                mSalt = createSalt(context);
            }
        }
        return mSalt;
    }

    private byte[] createSalt(Context context) {
        byte[] salt = mSaltGenerator.createSaltBytes(SALT_SIZE_BYTES);
        SaltBox.writeStoredBits(context, mSaltIndex, salt, SALT_SIZE_BYTES);
        return salt;
    }
}
