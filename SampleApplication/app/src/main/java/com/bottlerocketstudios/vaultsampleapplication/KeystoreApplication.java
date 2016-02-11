package com.bottlerocketstudios.vaultsampleapplication;

import android.app.Application;

import com.bottlerocketstudios.vaultsampleapplication.vault.VaultLocator;

/**
 * Created by adam.newman on 2/10/16.
 */
public class KeystoreApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        VaultLocator.initializeVaults(this);
    }

}
