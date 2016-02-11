package com.bottlerocketstudios.vaultsampleapplication.ui;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.bottlerocketstudios.vault.SharedPreferenceVault;
import com.bottlerocketstudios.vaultsampleapplication.R;

/**
 * Basis for every activity that contains a secret text field.
 */
public abstract class BaseSecretActivity extends AppCompatActivity {
    private static final String TAG = BaseSecretActivity.class.getSimpleName();

    protected static final int REQUEST_CODE_FIX_LOAD = 123;
    protected static final int REQUEST_CODE_FIX_SAVE = 124;

    private static final String PREF_SECRET = "theSecret";

    protected EditText mSecretText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(getContentViewLayoutId());

        mSecretText = (EditText) findViewById(R.id.secret_text);
        wireClick(R.id.load_secret, mClickListener);
        wireClick(R.id.save_secret, mClickListener);
    }

    protected abstract int getContentViewLayoutId();

    protected abstract SharedPreferenceVault getVault();

    protected void wireClick(int viewId, View.OnClickListener clickListener) {
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

    protected void loadSecret() {
        final SharedPreferenceVault sharedPreferenceVault = getVault();
        if (sharedPreferenceVault != null) {
            String secretValue = null;
            try {
                secretValue = sharedPreferenceVault.getString(PREF_SECRET, null);
            } catch (RuntimeException e) {
                if (!handleRuntimeException(sharedPreferenceVault, REQUEST_CODE_FIX_LOAD, e.getCause())) {
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

    protected void saveSecret() {
        String secretValue = mSecretText.getText().toString();

        final SharedPreferenceVault sharedPreferenceVault = getVault();
        if (sharedPreferenceVault != null) {
            try {
                sharedPreferenceVault.edit().putString(PREF_SECRET, secretValue).apply();
            } catch (RuntimeException e) {
                if (!handleRuntimeException(sharedPreferenceVault, REQUEST_CODE_FIX_SAVE, e.getCause())) {
                    Log.e(TAG, "Failed to handle exception", e);
                }
            }
        }
    }

    protected boolean handleRuntimeException(SharedPreferenceVault sharedPreferenceVault, int requestCode, Throwable throwable) {
        return false;
    }

}
