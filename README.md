BR Vault - Android
============

### Purpose
This library provides a secure storage system for private information. 

### Modes of Key Storage
Vault provides several key storage mechanisms out of the box. This is the key functionality that secures the information encrypted with this key. 

#### Persistent Key Storage

On devices running API 18 or later it will use the Android Keystore to wrap an encryption key. On older devices, blacklisted hardware or devices which fail an initial test; it will obfuscate the key by combining a pre-shared secret baked into the app with random data specific to the installed instance of the application.

This wrapped encryption key is stored in a SharedPreference file along with version information to upgrade the storage method when a user's device is upgraded across the v18 boundary. 

The key is always available to the application once it has been set until cleared or replaced.

#### Authenticated Key Storage - API 23+

The Android Keystore is used in authenticated mode so that the user must have a secured lock screen then use the lock screen unlock within a timeframe that you specify. If that time has elapsed you can use OS functionality to show the lock prompt. 

#### Memory Only Key Storage

The SecretKey is not actually stored anywhere, this would be useful for keys based on the user's password where you want the user to re-enter the password to unlock the vault. 

### Handling encrypted data

The encryption key is used to encrypt/decrypt supplied strings into base64 values which are stored in a separate SharedPreference file if you use the StandardSharedPreferenceVault.

### Components
These components can be used independently of each other, but will be conveniently combined in an easy to use way if you use one of the associated factories. 

*   SharedPreferenceVault - A place to store the secret information serialized or encoded to String format which extends the SharedPreference interface, but does not necessarily need to be backed by SharedPreferences.
    *   SharedPreferenceVaultFactory - A factory that will produce a SharedPreferenceVault backed by SharedPreference storage.
    *   SharedPreferenceVaultRegistry - A centralized place to keep your SharedPreferenceVault instances. Guarantees the required uniqueness of values used to index the stored values.
    *   StandardSharedPreferenceVault - Implementation used by the factory that is backed by an actual SharedPreference file. 
    *   StandardSharedPreferenceVaultEditor - Implements SharedPreference.Editor and provides the same behavior.
    *   SharedPrefVaultWriteListener - Improves the functionality of the standard commit and apply associated with SharedPreference
*   KeyStorage - Secure method to store your SecretKey objects.
    *   CompatSharedPrefKeyStorageFactory - Self-upgrading SharedPreferences backed KeyStorage factory.
    *   SharedPrefKeyStorage - Implementation used by the factory. 
*   Key Generation - You probably don't have to mess with this.
    *   Aes256KeyFromPasswordFactory - Creates a key for use with the AES256 cipher using a supplied password.
    *   Aes256RandomKeyFactory - Creates a key for use with the AES256 cipher using a SecureRandom source.
    *   PbkdfKeygenerator, RandomKeyGenerator, SecretKeySpecGenerator - implementations that can be used separately, but are largely for use by other components.

### Usage
Add the jcenter repository and include the library in your project with the compile directive in your dependencies section of your build.gradle.

        repositories {
            ...
            jcenter()
        }
        
        ...

        dependencies {
            ...
            compile 'com.bottlerocketstudios:vault:1.4.2'
        }

In rare cases where you need to pull a snapshot build to help troubleshoot the develop branch, snapshots are hosted by JFrog. You should not ship a release using the snapshot library as the actual binary referenced by snapshot is going to change with every build of the develop branch. In the best case you will have irreproducible builds. In the worst case, human extinction. In some more likely middle case, you will have buggy or experimental code in your released app.

         repositories {
            ...
            jcenter()
            maven {
               url "https://oss.jfrog.org/artifactory/oss-snapshot-local"
            }
         }
         
         dependencies {
            ...
            compile 'com.bottlerocketstudios:vault:1.4.3-SNAPSHOT'
         }

#### Sample Application
In this repository there is a Sample Application project which demonstrates usage of the various standard modes of operation for this library. This is the best source for an idea of how to build using the various factory methods described below. 

#### Automatically Persistently Keyed
Use automatic random keys if you need to store things like API tokens for non-user authenticated APIs or when a user password is not available or desirable to generate the initial key. Once created, the key can be used at any time. 

        //Create an automatically keyed vault
        SharedPreferenceVault secureVault = SharedPreferenceVaultFactory.getAppKeyedCompatAes256Vault(
                context,
                PREF_FILE_NAME,     //Preference file name to store content
                KEY_FILE_NAME,      //Preference file to store key material
                KEY_ALIAS,          //App-wide unique key alias
                VAULT_ID,           //App-wide unique vault id
                PRESHARED_SECRET    //Random string for pre v18
        );
        //Store the created vault in an application-wide store to prevent collisions
        SharedPreferenceVaultRegistry.getInstance().addVault(
                VAULT_ID,
                PREF_FILE_NAME,
                KEY_ALIAS,
                secureVault
        );
        ...
        //They logged out or you want to clear it for some reason later
        SecretKey secretKey = Aes256RandomKeyFactory.createKey();
        SharedPreferenceVaultRegistry.getInstance().getVault(VAULT_ID).rekeyStorage(secretKey);

#### Manually Persistently Keyed
When the key is derived from some external source you can create the keystore then rekey it later. This is typically going to be the case if you want to base the key on a user supplied password. Once created, the key can be used at any time. 

        SharedPreferenceVault secureVault = SharedPreferenceVaultFactory.getCompatAes256Vault(
                context,
                PREF_FILE_NAME,
                KEY_FILE_NAME,
                KEY_ALIAS,
                VAULT_ID,
                PRESHARED_SECRET
        );
        SharedPreferenceVaultRegistry.getInstance().addVault(
                VAULT_ID,
                PREF_FILE_NAME,
                KEY_ALIAS,
                secureVault
        );
        ...
        //Later when you have the password, create a key using 10000 PKDBF iterations
        //Avoid doing this on the UI thread, it is designed to be CPU intensive. 
        SecretKey secretKey = Aes256KeyFromPasswordFactory.createKey("password", 10000);
        SharedPreferenceVaultRegistry.getInstance().getVault(VAULT_ID).rekeyStorage(secretKey);

#### Authentication Based Key
Starting with API 23, devices can store a key and require the user to provide a password/pin/pattern/fingerprint unlock in order to use the key. If the device has not been unlocked in a timeframe that you specify, the user is prompted to provide their unlock authentication. The user must have a secure lock screen enabled.

		SharedPreferenceVault secureVault = SharedPreferenceVaultFactory.getKeychainAuthenticatedAes256Vault(
				context, 
				PREF_FILE_NAME, 
				KEY_ALIAS,
				AUTH_DURATION_SECONDS);
        SharedPreferenceVaultRegistry.getInstance().addVault(
		        VAULT_ID, 
		        PREF_FILE_NAME, 
		        KEY_ALIAS, 
		        secureVault);

#### Memory Only Key
You can use the SharedPreferenceVault with SecretKey generated entirely from the user password and requiring the user password to be entered before the vault is readable. Without both the salt and the user provided password, the PBKDF will not output the same key. Since only the salt should be stored, an adversary must have the password to unlock the vault. See the sample app for a demonstration of creating PRNG salt then storing it so that it can be used later in combination with the password. 

		SharedPreferenceVault secureVault = SharedPreferenceVaultFactory.getMemoryOnlyKeyAes256Vault(
				context, 
				PREF_FILE_NAME, 
				true);
        SharedPreferenceVaultRegistry.getInstance().addVault(
		        VAULT_ID, 
		        PREF_FILE_NAME, 
		        KEY_ALIAS, 
		        secureVault);
        ...
        //Later when you have the password, create a key using 10000 PKDBF iterations
        //Avoid doing this on the UI thread, it is designed to be CPU intensive. 
        //You must use the same salt used to generate the key to generate it again later. 
        //See the sample app.
        SecretKey secretKey = Aes256KeyFromPasswordFactory.createKey("password", 10000, specificSaltGenerator);
        SharedPreferenceVaultRegistry.getInstance().getVault(VAULT_ID).rekeyStorage(secretKey);

#### Rekeying
The vault can be rekeyed at any time. This will delete all values in the shared preference file. This is completely irreversible.

#### API Changes to Commit and Apply
Typical results of the SharedPreferenc commit and apply are slightly modified in Vault resulting of the extra encryption that is taking place. Because of this, commit and apply can both fail for reasons other than the SharedPreference write failing. The return type for commit is now more important. The recommendation is to **enable exceptions** when creating a SharedPreferenceVault.

* `SharedPreferenceVaultFactory.getCompatAes256Vault()`
* `SharedPreferenceVaultFactory.getAppKeyedCompatAes256Vault()`
* `SharedPreferenceVaultFactory.getMemoryOnlyKeyAes256Vault()`

These all have the ability to throw `GeneralSecurityException` or `UnsupportedEncodingException` with **exceptions enabled**. These exceptions can occur before the commit or apply is actually attempted and can result in a failure to write data to the SharedPreference file.

1. If using `commit()` consider **enabling exceptions** to allow handling of these errors. Also, pay attention to the return type from `commit()` to make sure data was successfully written.
2. If using `apply()`, there is a new `SharedPrefVaultWriteListener` interface that can be used to handle callback should any of the errors mentioned above. The `SharedPreferenceVault` interface now supports a new method to add this Listener to the Vault. `setSharedPrefVaultWriteListener(SharedPrefVaultWriteListener listener)` This method can be chained but must be called **before** an editor is created.
    * The actual result of the write is still ignored when using the new `apply()` with a listener. The listener is only to show potential encryption issues that happen *before* the write.
    * Setting no listener will behave like the standard `SharedPreference.apply()`. All error are ignored.
    * The listener will only provide basic information on success and fail, it is still recommended that if more information is required, to **enable exceptions**.

Adding a listener is easy. Simply add an implementation of the `SharedPrefVaultWriteListener` to the `SharedPreferenceVault` **before** and editor is created from the Vault.

```java
//Adding a listener to a SharedPreferenceVault Instance
SharedPreferenceVault secureVault = SharedPreferenceVaultFactory.getAppKeyedCompatAes256Vault(
            context,
            PREF_FILE_NAME,
            KEY_FILE_NAME,
            KEY_ALIAS,
            VAULT_ID,
            PRESHARED_SECRET
    ).setSharedPrefVaultWriteListener(new SharedPrefVaultWriteListener() {
        @Override
        public void onSuccess() {
            //On Success code
        }

        @Override
        public void onError() {
            //On Error code
        }
    });

```
### Auditor Notes
Automated testing tools are often built to trigger on potential cryptographic mishaps. That is fine and sunlight is often the best disinfectant, especially in crypto. That is part of why this is an open source library. However, this library will cause two irrelevant reports to occur. 

*   Using an ECB block mode - Some tools will trigger if they see the letters ECB in any transform. This is not a problem because ECB is only used along with RSA to wrap a key. The short version is that a block cipher mode is a bit of a misnomer on a key wrap operation where the payload is smaller than the RSA key itself. Ultimately the RSA/ECB/PKCS1Padding transform is the only useful asymmetric transform in the Android Keystore for API 18-22, so there isn't any wiggle room here anyway.
*   Using RSA without OAEP padding - This one isn't exactly wrong, but it cannot be avoided. The library had previously only used PKCS1 with the Android Keystore. Starting with 1.4.0, the library is using the best security that it can based on API level. For API 18-22 that means RSA/ECB/PKCS1Padding and for API 23+ that means RSA/ECB/OAEPWithSHA-256AndMGF1Padding. API 23+ devices will be automatically migrated to OAEP. API support matrix: https://developer.android.com/training/articles/keystore.html#SupportedCiphers 

### Build
This project must be built with gradle. 

*   Version Numbering - The version name should end with "-SNAPSHOT" for non release builds. This will cause the resulting binary, source, and javadoc files to be uploaded to the snapshot repository in Maven as a snapshot build. Removing snapshot from the version name will publish the build on jcenter. If that version is already published, it will not overwrite it.
*   Execution - To build this library, associated tasks are dynamically generated by Android build tools in conjunction with Gradle. Example command for the production flavor of the release build type: 
    *   Build and upload: `./gradlew --refresh-dependencies clean uploadToMaven`
    *   Build only: `./gradlew --refresh-dependencies clean jarRelease`
