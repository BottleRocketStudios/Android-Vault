package com.bottlerocketstudios.vaultsampleapplication;

import android.app.Application;

import com.bottlerocketstudios.vaultsampleapplication.vault.VaultLocator;

public class VaultSampleApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        VaultLocator.initializeVaults(this);
    }

}
