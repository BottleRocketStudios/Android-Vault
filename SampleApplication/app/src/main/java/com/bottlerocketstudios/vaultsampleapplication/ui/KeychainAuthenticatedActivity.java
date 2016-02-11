package com.bottlerocketstudios.vaultsampleapplication.ui;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.UserNotAuthenticatedException;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.bottlerocketstudios.vault.SharedPreferenceVault;
import com.bottlerocketstudios.vault.SharedPreferenceVaultFactory;
import com.bottlerocketstudios.vaultsampleapplication.R;
import com.bottlerocketstudios.vaultsampleapplication.vault.VaultLocator;

/**
 * Created by adam.newman on 2/10/16.
 */
public class KeychainAuthenticatedActivity extends AppCompatActivity {
    private static final String TAG = KeychainAuthenticatedActivity.class.getSimpleName();

    private static final String PREF_SECRET = "theSecret";

    private static final int REQUEST_CODE_CONFIRM_FOR_LOAD = 123;
    private static final int REQUEST_CODE_CONFIRM_FOR_SAVE = 124;


    private EditText mSecretText;

    public static Intent createLaunchIntent(Context context) {
        return new Intent(context, KeychainAuthenticatedActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.keychain_authenticated_activity);
        mSecretText = (EditText) findViewById(R.id.secret_text);
        wireClick(R.id.load_secret, mClickListener);
        wireClick(R.id.save_secret, mClickListener);
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

    private void wireClick(int viewId, View.OnClickListener clickListener) {
        findViewById(viewId).setOnClickListener(clickListener);
    }

    private View.OnClickListener mClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.load_secret:
                    loadSecret();
                    break;
                case R.id.save_secret:
                    saveSecret();
                    break;
            }
        }
    };

    private void loadSecret() {
        final SharedPreferenceVault sharedPreferenceVault = VaultLocator.getKeychainAuthenticatedVault(this);
        if (sharedPreferenceVault != null) {
            String secretValue = null;
            try {
                secretValue = sharedPreferenceVault.getString(PREF_SECRET, null);
            } catch (RuntimeException e) {
                if (!handleRuntimeException(sharedPreferenceVault, REQUEST_CODE_CONFIRM_FOR_LOAD, e.getCause())) {
                    Log.e(TAG, "Failed to handle exception", e);
                }
            }
            if (secretValue != null) {
                mSecretText.setText(secretValue);
            } else {
                mSecretText.setText("");
            }
        }
    }

    private void saveSecret() {
        String secretValue = mSecretText.getText().toString();

        final SharedPreferenceVault sharedPreferenceVault = VaultLocator.getKeychainAuthenticatedVault(this);
        if (sharedPreferenceVault != null) {
            try {
                sharedPreferenceVault.edit().putString(PREF_SECRET, secretValue).apply();
            } catch (RuntimeException e) {
                if (!handleRuntimeException(sharedPreferenceVault, REQUEST_CODE_CONFIRM_FOR_SAVE, e.getCause())) {
                    Log.e(TAG, "Failed to handle exception", e);
                }
            }
        }
    }

    private boolean handleRuntimeException(SharedPreferenceVault sharedPreferenceVault, int requestCode, Throwable throwable) {
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
            case REQUEST_CODE_CONFIRM_FOR_LOAD:
                loadSecret();
                break;
            case REQUEST_CODE_CONFIRM_FOR_SAVE:
                saveSecret();
                break;
        }
    }
}
