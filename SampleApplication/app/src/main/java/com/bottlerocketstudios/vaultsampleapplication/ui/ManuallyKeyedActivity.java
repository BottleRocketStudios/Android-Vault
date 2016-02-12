package com.bottlerocketstudios.vaultsampleapplication.ui;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import com.bottlerocketstudios.vault.SharedPreferenceVault;
import com.bottlerocketstudios.vault.keys.generator.Aes256KeyFromPasswordFactory;
import com.bottlerocketstudios.vaultsampleapplication.vault.VaultLocator;

import java.lang.ref.WeakReference;

import javax.crypto.SecretKey;

public class ManuallyKeyedActivity extends BasePasswordActivity {
    private static final int PBKDF_ITERATIONS = 10000;

    public static Intent createLaunchIntent(Context context) {
        return new Intent(context, ManuallyKeyedActivity.class);
    }

    @Override
    protected SharedPreferenceVault getVault() {
        return VaultLocator.getManuallyKeyedVault();
    }

    protected void clearPassword() {
        getVault().rekeyStorage(null);
    }

    protected void setPassword() {
        new GeneratePasswordBasedKey(getVault(), this).execute(getPasswordText());
    }

    /**
     * Completely rekey the vault with a new randomly salted PBKDF operation on the password. The
     * same password will never generate the same key due to random salt.
     */
    private static class GeneratePasswordBasedKey extends AsyncTask<String, Void, SecretKey> {

        private final SharedPreferenceVault mSharedPreferenceVault;
        private final WeakReference<BasePasswordActivity> mActivityRef;

        public GeneratePasswordBasedKey(SharedPreferenceVault sharedPreferenceVault, BasePasswordActivity activity) {
            mSharedPreferenceVault = sharedPreferenceVault;
            mActivityRef = new WeakReference<>(activity);
        }

        @Override
        protected SecretKey doInBackground(String... strings) {
            String password = strings[0];
            return Aes256KeyFromPasswordFactory.createKey(password, PBKDF_ITERATIONS);
        }

        @Override
        protected void onPostExecute(SecretKey secretKey) {
            //Rekey because the key will never be the same.
            mSharedPreferenceVault.rekeyStorage(secretKey);

            BasePasswordActivity activity = mActivityRef.get();
            if (activity != null) {
                activity.onKeySet();
            }
        }
    }

}
