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
import android.os.Build;
import android.security.KeyPairGeneratorSpec;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import com.bottlerocketstudios.vault.EncryptionConstants;
import com.bottlerocketstudios.vault.keys.storage.KeyStorageType;

import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Calendar;
import java.util.GregorianCalendar;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.security.auth.x500.X500Principal;

/**
 * Created on 9/20/16.
 */
public abstract class AbstractAndroidKeystoreSecretKeyWrapper implements SecretKeyWrapper {
    protected static final String ALGORITHM = "RSA";
    protected static final int CERTIFICATE_LIFE_YEARS = 100;

    private final Cipher mCipher;
    private final Context mContext;
    private KeyPair mKeyPair;
    private final String mAlias;
    private String mDigests;

    /**
     * Create a wrapper using the public/private key pair with the given alias.
     * If no pair with that alias exists, it will be generated.
     */
    @SuppressLint("GetInstance") //Suppressing ECB mode warning because we use RSA algorithm.
    public AbstractAndroidKeystoreSecretKeyWrapper(Context context, String alias)
            throws GeneralSecurityException {
        mAlias = alias;
        mCipher = Cipher.getInstance(getTransformation());
        mContext = context.getApplicationContext();
    }

    protected abstract String getTransformation();

    private KeyPair getKeyPair() throws GeneralSecurityException, IOException {
        synchronized (mAlias) {
            if (mKeyPair == null) {
                final KeyStore keyStore = KeyStore.getInstance(EncryptionConstants.ANDROID_KEY_STORE);
                keyStore.load(null);
                if (!keyStore.containsAlias(mAlias)) {
                    generateKeyPair(mContext, mAlias);
                }
                // Even if we just generated the key, always read it back to ensure we
                // can read it successfully.
                final KeyStore.PrivateKeyEntry entry = (KeyStore.PrivateKeyEntry) keyStore.getEntry(mAlias, null);
                mKeyPair = new KeyPair(entry.getCertificate().getPublicKey(), entry.getPrivateKey());
            }
        }
        return mKeyPair;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void generateKeyPair(Context context, String alias) throws GeneralSecurityException {
        final Calendar start = new GregorianCalendar();
        final Calendar end = new GregorianCalendar();
        end.add(Calendar.YEAR, CERTIFICATE_LIFE_YEARS);
        final AlgorithmParameterSpec algorithmParameterSpec = getVersionAppropriateAlgorithmParameterSpec(context, alias, start, end, BigInteger.ONE, new X500Principal("CN=" + alias));
        final KeyPairGenerator gen = KeyPairGenerator.getInstance(ALGORITHM, EncryptionConstants.ANDROID_KEY_STORE);
        gen.initialize(algorithmParameterSpec);
        gen.generateKeyPair();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private AlgorithmParameterSpec getVersionAppropriateAlgorithmParameterSpec(Context context, String alias, Calendar start, Calendar end, BigInteger serial, X500Principal subject) {
        AlgorithmParameterSpec algorithmParameterSpec;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            algorithmParameterSpec = buildApi23AlgorithmParameterSpec(alias, start, end, serial, subject);
        } else {
            algorithmParameterSpec = buildLegacyAlgorithmParameterSpec(context, alias, start, end, serial, subject);
        }
        return algorithmParameterSpec;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private AlgorithmParameterSpec buildLegacyAlgorithmParameterSpec(Context context, String alias, Calendar start, Calendar end, BigInteger serialNumber, X500Principal subject) {
        return new KeyPairGeneratorSpec.Builder(context)
                .setAlias(alias)
                .setSubject(subject)
                .setSerialNumber(serialNumber)
                .setStartDate(start.getTime())
                .setEndDate(end.getTime())
                .build();
    }

    @TargetApi(Build.VERSION_CODES.M)
    private AlgorithmParameterSpec buildApi23AlgorithmParameterSpec(String alias, Calendar start, Calendar end, BigInteger serialNumber, X500Principal subject) {
        return new KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_DECRYPT | KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_SIGN)
                .setCertificateSubject(subject)
                .setCertificateSerialNumber(serialNumber)
                .setKeyValidityStart(start.getTime())
                .setCertificateNotBefore(start.getTime())
                .setKeyValidityEnd(end.getTime())
                .setCertificateNotAfter(end.getTime())
                .setEncryptionPaddings(getEncryptionPadding())
                .setBlockModes(getBlockModes())
                .setDigests(getDigests())
                .build();
    }

    protected abstract String[] getEncryptionPadding();

    protected abstract String[] getBlockModes();

    protected abstract String[] getDigests();

    @Override
    public synchronized byte[] wrap(SecretKey key) throws GeneralSecurityException, IOException {
        mCipher.init(Cipher.WRAP_MODE, getKeyPair().getPublic());
        return mCipher.wrap(key);
    }

    @Override
    public synchronized SecretKey unwrap(byte[] blob, String wrappedKeyAlgorithm) throws GeneralSecurityException, IOException {
        mCipher.init(Cipher.UNWRAP_MODE, getKeyPair().getPrivate());
        return (SecretKey) mCipher.unwrap(blob, wrappedKeyAlgorithm, Cipher.SECRET_KEY);
    }

    @Override
    public synchronized void clearKey(Context context) throws GeneralSecurityException, IOException {
        mKeyPair = null;
        final KeyStore keyStore = KeyStore.getInstance(EncryptionConstants.ANDROID_KEY_STORE);
        keyStore.load(null);
        keyStore.deleteEntry(mAlias);
    }

    public boolean testKey() throws GeneralSecurityException, IOException {
        KeyPair keyPair = getKeyPair();
        return keyPair != null;
    }

    @Override
    public KeyStorageType getKeyStorageType() {
        return KeyStorageType.ANDROID_KEYSTORE;
    }

}
