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

import android.annotation.TargetApi;
import android.content.Context;
import android.security.keystore.KeyProperties;

import java.security.GeneralSecurityException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.MGF1ParameterSpec;

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

    //Cipher Algorithm Spec Params
    protected static final String MESSAGE_DIGEST_ALGORITHM_NAME = "SHA-256";
    protected static final String MASK_GENERATION_FUNCTION_ALGORITHM_NAME = "MGF1";

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
    }

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

    @Override
    public AlgorithmParameterSpec buildCipherAlgorithmParameterSpec() {
        return new OAEPParameterSpec(
                MESSAGE_DIGEST_ALGORITHM_NAME,
                MASK_GENERATION_FUNCTION_ALGORITHM_NAME,
                MGF1ParameterSpec.SHA1,
                PSource.PSpecified.DEFAULT);
    }
}
