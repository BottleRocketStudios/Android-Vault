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
import android.annotation.TargetApi;
import android.content.Context;
import android.security.keystore.KeyProperties;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.spec.MGF1ParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;

/**
 * Wraps {@link javax.crypto.SecretKey} instances using a public/private key pair stored in
 * the platform {@link java.security.KeyStore}. This allows us to protect symmetric keys with
 * hardware-backed crypto, if provided by the device.
 * <p>
 * This version uses OAEP padding which is only supported on API 23+
 * </p>
 * <p>
 * See <a href="http://en.wikipedia.org/wiki/Key_Wrap">key wrapping</a> for more
 * details.
 * </p>
 */
@TargetApi(23)
public class AndroidOaepKeystoreSecretKeyWrapper extends AbstractAndroidKeystoreSecretKeyWrapper {
    protected static final String TRANSFORMATION = "RSA/ECB/OAEPwithSHA-256andMGF1Padding";
    protected static final String[] ENCRYPTION_PADDING;
    protected static final String[] BLOCK_MODES;
    protected static final String[] DIGESTS;

    private OAEPParameterSpec sp;

    static {
        ENCRYPTION_PADDING = new String[] {KeyProperties.ENCRYPTION_PADDING_RSA_OAEP};
        BLOCK_MODES = new String[] {KeyProperties.BLOCK_MODE_ECB};
        DIGESTS = new String[] {KeyProperties.DIGEST_SHA256};
    }

    /**
     * Create a wrapper using the public/private key pair with the given alias.
     * If no pair with that alias exists, it will be generated.
     *
     * @param context
     * @param alias
     */
    public AndroidOaepKeystoreSecretKeyWrapper(Context context, String alias) throws GeneralSecurityException {
        super(context, alias);
        sp = new OAEPParameterSpec(KeyProperties.DIGEST_SHA256, "MGF1", new MGF1ParameterSpec(KeyProperties.DIGEST_SHA1), PSource.PSpecified.DEFAULT);
    }

    // Temporary workaround for https://github.com/BottleRocketStudios/Android-Vault/issues/5
    @Override
    public synchronized byte[] wrap(SecretKey key) throws GeneralSecurityException, IOException {
        mCipher.init(Cipher.WRAP_MODE, getKeyPair().getPublic(), sp);
        return mCipher.wrap(key);
    }

    @Override
    public synchronized SecretKey unwrap(byte[] blob, String wrappedKeyAlgorithm) throws GeneralSecurityException, IOException {
        mCipher.init(Cipher.UNWRAP_MODE, getKeyPair().getPrivate(), sp);
        return (SecretKey) mCipher.unwrap(blob, wrappedKeyAlgorithm, Cipher.SECRET_KEY);
    }
    // End of workaround

    @Override
    protected String getTransformation() {
        return TRANSFORMATION;
    }

    @Override
    protected String[] getEncryptionPadding() {
        return ENCRYPTION_PADDING;
    }

    @Override
    protected String[] getBlockModes() {
        return BLOCK_MODES;
    }

    @Override
    protected String[] getDigests() {
        return DIGESTS;
    }
}
