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
import android.os.Build;
import android.security.keystore.KeyProperties;

import java.security.GeneralSecurityException;

/**
 * Wraps {@link javax.crypto.SecretKey} instances using a public/private key pair stored in
 * the platform {@link java.security.KeyStore}. This allows us to protect symmetric keys with
 * hardware-backed crypto, if provided by the device.
 * <p>
 * See <a href="http://en.wikipedia.org/wiki/Key_Wrap">key wrapping</a> for more
 * details.
 * </p>
 */
public class AndroidKeystoreSecretKeyWrapper extends AbstractAndroidKeystoreSecretKeyWrapper {
    protected static final String TRANSFORMATION = "RSA/ECB/PKCS1Padding";
    protected static final String[] ENCRYPTION_PADDING;
    protected static final String[] BLOCK_MODES;
    protected static final String[] DIGESTS = {};

    static {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            BLOCK_MODES = new String[] {KeyProperties.BLOCK_MODE_ECB};
            ENCRYPTION_PADDING = new String[] {KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1};
        } else {
            BLOCK_MODES = new String[] {"ECB"};
            ENCRYPTION_PADDING = new String[] {"PKCS1Padding"};
        }
    }

    /**
     * Create a wrapper using the public/private key pair with the given alias.
     * If no pair with that alias exists, it will be generated.
     *
     * @param context
     * @param alias
     */
    public AndroidKeystoreSecretKeyWrapper(Context context, String alias) throws GeneralSecurityException {
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
}
