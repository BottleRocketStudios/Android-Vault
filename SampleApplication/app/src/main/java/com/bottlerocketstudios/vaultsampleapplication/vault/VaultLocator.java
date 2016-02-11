package com.bottlerocketstudios.vaultsampleapplication.vault;

import android.content.Context;
import android.util.Log;

import com.bottlerocketstudios.vault.SharedPreferenceVault;
import com.bottlerocketstudios.vault.SharedPreferenceVaultFactory;
import com.bottlerocketstudios.vault.SharedPreferenceVaultRegistry;

import java.security.GeneralSecurityException;

/**
 * Example initialization and place to keep reference to your vaults. This example instantiates all three
 * supported types, while most applications will only need one.
 */
public class VaultLocator {
    private static final String TAG = VaultLocator.class.getSimpleName();

    private static final String KEYCHAIN_AUTHENTICATED_PREF_FILE_NAME = "keychainAuthenticatedPref";
    private static final String KEYCHAIN_AUTHENTICATED_KEY_ALIAS = "keychainAuthenticatedKey";
    private static final int KEYCHAIN_AUTHENTICATED_KEY_INDEX = 1;
    private static final int KEYCHAIN_AUTHENTICATED_AUTH_DURATION = 10;

    private static final String MANUALLY_KEYED_PREF_FILE_NAME = "manuallyKeyedPref";
    private static final String MANUALLY_KEYED_KEY_FILE_NAME = "manuallyKeyedKey";
    private static final String MANUALLY_KEYED_KEY_ALIAS = "manuallyKeyed";
    private static final int MANUALLY_KEYED_KEY_INDEX = 2;
    private static final String MANUALLY_KEYED_PRESHARED_SECRET = "This is obviously not what you want to use, come up with your own way of obscuring this value. A standard method demonstrated here will not be very useful if everyone does it.";

    private static final String AUTOMATICALLY_KEYED_PREF_FILE_NAME = "automaticallyKeyedPref";
    private static final String AUTOMATICALLY_KEYED_KEY_FILE_NAME = "automaticallyKeyedKey";
    private static final String AUTOMATICALLY_KEYED_KEY_ALIAS = "automaticallyKeyed";
    private static final int AUTOMATICALLY_KEYED_KEY_INDEX = 3;
    private static final String AUTOMATICALLY_KEYED_PRESHARED_SECRET = "This is also obviously not what you want to use, come up with your own way of obscuring this value. A standard method demonstrated here will not be very useful if everyone does it.";

    public static boolean initializeVaults(Context context) {
        try {
            initKeychainAuthenticatedVault(context);
            initAutomaticallyKeyedVault(context);
            initManuallyKeyedVault(context);
            return true;
        } catch (GeneralSecurityException e) {
            Log.e(TAG, "Failed to initialize vaults", e);
        }
        return false;
    }

    /**
     * Create a vault that requires recent user authentication.
     */
    private static synchronized void initKeychainAuthenticatedVault(Context context) throws GeneralSecurityException {
        if (SharedPreferenceVaultFactory.canUseKeychainAuthentication(context) && SharedPreferenceVaultRegistry.getInstance().getVault(KEYCHAIN_AUTHENTICATED_KEY_INDEX) == null) {
            SharedPreferenceVault sharedPreferenceVault = SharedPreferenceVaultFactory.getKeychainAuthenticatedAes256Vault(context, KEYCHAIN_AUTHENTICATED_PREF_FILE_NAME, KEYCHAIN_AUTHENTICATED_KEY_ALIAS, KEYCHAIN_AUTHENTICATED_AUTH_DURATION);
            SharedPreferenceVaultRegistry.getInstance().addVault(KEYCHAIN_AUTHENTICATED_KEY_INDEX, KEYCHAIN_AUTHENTICATED_PREF_FILE_NAME, KEYCHAIN_AUTHENTICATED_KEY_ALIAS, sharedPreferenceVault);
        }
    }

    /**
     * Encapsulates index knowledge and re-creates if user did not have device lock configured on the first attempt.
     */
    public static SharedPreferenceVault getKeychainAuthenticatedVault(Context context) {
        SharedPreferenceVault sharedPreferenceVault = SharedPreferenceVaultRegistry.getInstance().getVault(KEYCHAIN_AUTHENTICATED_KEY_INDEX);
        if (sharedPreferenceVault == null) {
            try {
                initKeychainAuthenticatedVault(context);
            } catch (GeneralSecurityException e) {
                Log.e(TAG, "Caught java.security.GeneralSecurityException", e);
            }
            sharedPreferenceVault = SharedPreferenceVaultRegistry.getInstance().getVault(KEYCHAIN_AUTHENTICATED_KEY_INDEX);
        }
        return sharedPreferenceVault;
    }

    /**
     * Create a vault that requires manual keying via PBKDF
     */
    private static void initManuallyKeyedVault(Context context) throws GeneralSecurityException {
        SharedPreferenceVault sharedPreferenceVault = SharedPreferenceVaultFactory.getCompatAes256Vault(context, MANUALLY_KEYED_PREF_FILE_NAME, MANUALLY_KEYED_KEY_FILE_NAME, MANUALLY_KEYED_KEY_ALIAS, MANUALLY_KEYED_KEY_INDEX, MANUALLY_KEYED_PRESHARED_SECRET);
        SharedPreferenceVaultRegistry.getInstance().addVault(MANUALLY_KEYED_KEY_INDEX, MANUALLY_KEYED_PREF_FILE_NAME, MANUALLY_KEYED_PREF_FILE_NAME, sharedPreferenceVault);
    }

    /**
     * Encapsulates index knowledge.
     */
    public static SharedPreferenceVault getManuallyKeyedVault() {
        return SharedPreferenceVaultRegistry.getInstance().getVault(MANUALLY_KEYED_KEY_INDEX);
    }

    /**
     * Create a vault that will automatically key itself initially with a random key.
     */
    private static void initAutomaticallyKeyedVault(Context context) throws GeneralSecurityException {
        SharedPreferenceVault sharedPreferenceVault = SharedPreferenceVaultFactory.getAppKeyedCompatAes256Vault(context, AUTOMATICALLY_KEYED_PREF_FILE_NAME, AUTOMATICALLY_KEYED_KEY_FILE_NAME, AUTOMATICALLY_KEYED_KEY_ALIAS, AUTOMATICALLY_KEYED_KEY_INDEX, AUTOMATICALLY_KEYED_PRESHARED_SECRET);
        SharedPreferenceVaultRegistry.getInstance().addVault(AUTOMATICALLY_KEYED_KEY_INDEX, AUTOMATICALLY_KEYED_PREF_FILE_NAME, AUTOMATICALLY_KEYED_KEY_ALIAS, sharedPreferenceVault);
    }

    /**
     * Encapsulates index knowledge.
     */
    public static SharedPreferenceVault getAutomaticallyKeyedVault() {
        return SharedPreferenceVaultRegistry.getInstance().getVault(AUTOMATICALLY_KEYED_KEY_INDEX);
    }

}
