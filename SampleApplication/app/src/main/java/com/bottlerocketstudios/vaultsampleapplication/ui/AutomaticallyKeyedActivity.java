package com.bottlerocketstudios.vaultsampleapplication.ui;

import android.content.Context;
import android.content.Intent;

import com.bottlerocketstudios.vault.SharedPreferenceVault;
import com.bottlerocketstudios.vaultsampleapplication.R;
import com.bottlerocketstudios.vaultsampleapplication.vault.VaultLocator;

public class AutomaticallyKeyedActivity extends BaseSecretActivity {
    public static Intent createLaunchIntent(Context context) {
        return new Intent(context, AutomaticallyKeyedActivity.class);
    }

    @Override
    protected int getContentViewLayoutId() {
        return R.layout.secret_only_activity;
    }

    @Override
    protected SharedPreferenceVault getVault() {
        return VaultLocator.getAutomaticallyKeyedVault();
    }
}
