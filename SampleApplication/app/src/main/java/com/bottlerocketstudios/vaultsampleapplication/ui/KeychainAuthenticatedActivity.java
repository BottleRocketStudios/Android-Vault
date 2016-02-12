package com.bottlerocketstudios.vaultsampleapplication.ui;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.UserNotAuthenticatedException;
import android.util.Log;
import android.widget.Toast;

import com.bottlerocketstudios.vault.SharedPreferenceVault;
import com.bottlerocketstudios.vault.SharedPreferenceVaultFactory;
import com.bottlerocketstudios.vaultsampleapplication.R;
import com.bottlerocketstudios.vaultsampleapplication.vault.VaultLocator;

public class KeychainAuthenticatedActivity extends BaseSecretActivity {
    private static final String TAG = KeychainAuthenticatedActivity.class.getSimpleName();


    public static Intent createLaunchIntent(Context context) {
        return new Intent(context, KeychainAuthenticatedActivity.class);
    }

    @Override
    protected void onResume() {
        super.onResume();
        notifyKeychainState();
    }

    private void notifyKeychainState() {
        if (!SharedPreferenceVaultFactory.canUseKeychainAuthentication(this)) {
            Toast.makeText(this, R.string.secure_lock_not_setup, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected int getContentViewLayoutId() {
        return R.layout.secret_only_activity;
    }

    @Override
    protected SharedPreferenceVault getVault() {
        return VaultLocator.getKeychainAuthenticatedVault(this);
    }

    @Override
    protected boolean handleRuntimeException(SharedPreferenceVault sharedPreferenceVault, int requestCode, Throwable throwable) {
        boolean handled = false;
        if (throwable instanceof UserNotAuthenticatedException) {
            Log.w(TAG, "User authentication expired");
            showAuthenticationScreen(requestCode);
            handled = true;
        } else if (throwable instanceof KeyPermanentlyInvalidatedException) {
            Log.w(TAG, "User changed unlock code and permanently invalidated the key");
            sharedPreferenceVault.rekeyStorage(null);
            completeOperationForRequestCode(requestCode);
            handled = true;
        }
        return handled;
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void showAuthenticationScreen(int requestCode) {
        KeyguardManager keyguardManager = getSystemService(KeyguardManager.class);
        Intent intent = keyguardManager.createConfirmDeviceCredentialIntent(getString(R.string.please_login_title), getString(R.string.please_login_message));
        if (intent != null) {
            startActivityForResult(intent, requestCode);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            completeOperationForRequestCode(requestCode);
        }
    }

    private void completeOperationForRequestCode(int requestCode) {
        switch (requestCode) {
            case REQUEST_CODE_FIX_LOAD:
                loadSecret();
                break;
            case REQUEST_CODE_FIX_SAVE:
                saveSecret();
                break;
        }
    }
}
