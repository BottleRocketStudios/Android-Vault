package com.bottlerocketstudios.vaultsampleapplication.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import com.bottlerocketstudios.vaultsampleapplication.R;

/**
 * Base activity for storage types that require a password.
 */
public abstract class BasePasswordActivity extends BaseSecretActivity {

    protected EditText mPasswordText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPasswordText = (EditText) findViewById(R.id.password);
        wireClick(R.id.clear_password, mPasswordClickListener);
        wireClick(R.id.set_password, mPasswordClickListener);
    }

    @Override
    protected int getContentViewLayoutId() {
        return R.layout.password_activity;
    }

    private View.OnClickListener mPasswordClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.clear_password:
                    clearPassword();
                    break;
                case R.id.set_password:
                    setPassword();
                    break;
            }
        }
    };

    protected abstract void clearPassword();

    protected abstract void setPassword();

    protected String getPasswordText() {
        return mPasswordText.getText().toString();
    }

    protected void onKeySet() {
        mPasswordText.setText("");
    }
}
