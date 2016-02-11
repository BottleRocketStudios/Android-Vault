package com.bottlerocketstudios.vaultsampleapplication.ui;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import com.bottlerocketstudios.vault.SharedPreferenceVault;
import com.bottlerocketstudios.vault.keys.generator.Aes256KeyFromPasswordFactory;
import com.bottlerocketstudios.vault.salt.SaltGenerator;
import com.bottlerocketstudios.vaultsampleapplication.vault.VaultLocator;

import java.lang.ref.WeakReference;

import javax.crypto.SecretKey;

public class PbkdfActivity extends BasePasswordActivity {
    private static final int PBKDF_ITERATIONS = 10000;

    public static Intent createLaunchIntent(Context context) {
        return new Intent(context, PbkdfActivity.class);
    }

    @Override
    protected SharedPreferenceVault getVault() {
        return VaultLocator.getPbkdfVault();
    }

    @Override
    protected void clearPassword() {
        getVault().setKey(null);
    }

    protected void setPassword() {
        new GeneratePasswordBasedKey(getVault(), VaultLocator.getPbkdfVaultSaltGenerator(this), this).execute(getPasswordText());
    }

    /**
     * Set the key on the vault using the provided salt generator. In this case we use the
     * SpecificSaltGenerator so that the same password will generate the same key dependably.
     * @see VaultLocator#getPbkdfVaultSaltGenerator(Context)
     */
    private static class GeneratePasswordBasedKey extends AsyncTask<String, Void, SecretKey> {

        private final SharedPreferenceVault mSharedPreferenceVault;
        private final SaltGenerator mSaltGenerator;
        private final WeakReference<BasePasswordActivity> mActivityRef;

        public GeneratePasswordBasedKey(SharedPreferenceVault sharedPreferenceVault, SaltGenerator saltGenerator, BasePasswordActivity activity) {
            mSharedPreferenceVault = sharedPreferenceVault;
            mSaltGenerator = saltGenerator;
            mActivityRef = new WeakReference<>(activity);
        }

        @Override
        protected SecretKey doInBackground(String... strings) {
            String password = strings[0];
            return Aes256KeyFromPasswordFactory.createKey(password, PBKDF_ITERATIONS, mSaltGenerator);
        }

        @Override
        protected void onPostExecute(SecretKey secretKey) {
            //Setkey because the key can be the same as already used.
            mSharedPreferenceVault.setKey(secretKey);

            BasePasswordActivity activity = mActivityRef.get();
            if (activity != null) {
                activity.onKeySet();
            }
        }
    }

}
