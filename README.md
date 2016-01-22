BR Vault - Android
============

### Purpose
This library provides a secure storage system for private information. On devices running API 18 or later it will use the Android Keystore to wrap an encryption key. On older devices, it will obfuscate the key by combining a pre-shared secret baked into the app with random data specific to the installed instance of the application.

This wrapped encryption key is stored in a SharedPreference file along with version information to upgrade the storage method when a user's device is upgraded across the v18 boundary. 

The encryption key is used to encrypt/decrypt supplied strings into base64 values which are stored in a separate SharedPreference file if you use the StandardSharedPreferenceVault.

This describes the provided behavior, but the components could be used with different storage systems. The core component, KeyStorage and the SharedPrefKeyStorage implementation can be used via the CompatSharedPrefKeyStorageFactory to retain SecretKeys for use with any cryptographic process. 

### Components
These components can be used independently of each other, but will be conveniently combined in an easy to use way if you use one of the associated factories. 

*   SharedPreferenceVault - A place to store the secret information serialized or encoded to String format which extends the SharedPreference interface, but does not necessarily need to be backed by SharedPreferences.
    *   SharedPreferenceVaultFactory - A factory that will produce a SecureVault backed by SharedPreference storage.
    *   SharedPreferenceVaultRegistry - A centralized place to keep your SecureVault instances. Guarantees the required uniqueness of values used to index the stored values.
    *   StandardSharedPreferenceVault - Implementation used by the factory that is backed by an actual SharedPreference file. 
    *   StandardSharedPreferenceVaultEditor - Extends SharedPreference.Editor and maintains the same behavior.
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
            compile 'com.bottlerocketstudios:vault:1.2.4'
        }

##### Automatically Keyed
Use automatic random keys if you need to store things like API tokens for non-user authenticated APIs or when a user password is not available or desirable to generate the initial key. 

        //Create an automatically keyed vault
        SecureVault secureVault = SharedPreferenceVaultFactory.getAppKeyedCompatAes256Vault(
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
        SharedPrefereneVaultRegistry.getInstance().getVault(VAULT_ID).rekeyStorage(secretKey);
        
##### Manually Keyed
When the key is derived from some external source you can create the keystore then rekey it later. This is typically going to be the case if you want to base the key on a user supplied password.

        SecureVault secureVault = SharedPreferenceVaultFactory.getCompatAes256Vault(
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
        SharedPrefereneVaultRegistry.getInstance().getVault(VAULT_ID).rekeyStorage(secretKey);
        
##### Rekeying
The vault can be rekeyed at any time. This will delete all values in the shared 
preference file. This is completely irreversable. 

### Build
This project must be built with gradle. 

*   Version Numbering - The version name should end with "-SNAPSHOT" for non release builds. This will cause the resulting binary, source, and javadoc files to be uploaded to the snapshot repository in Maven as a snapshot build. Removing snapshot from the version name will cause the build to replace the latest build of the same version number on jcenter. 
*   Execution - To build this libarary, associated tasks are dynamically generated by Android build tools in conjunction with Gradle. Example command for the production flavor of the release build type: 
    *   Build and upload: `./gradlew --refresh-dependencies clean uploadToMaven`
    *   Build only: `./gradlew --refresh-dependencies clean jarRelease`

### Changelog
*   1.2.4 - Open source release.
*   1.2.3 - Key Caching.
    *   Now caching the SecretKey in memory to further increase multithreaded performance with frequent reads. Better to have one SecretKey in memory than many garbage collected copies of it all over the heap.
*   1.2.2 - Fix Concurrency.
    *   The Android Keystore is not threadsafe. In rare cases, concurrent access will cause one of the operations to have an invalid handle ID. 
*   1.2.1 - Test for device keystore failure.
    *   Some devices have keystore support but will never let you use it due to either Device Administrator settings or a separate PIN that is different from the unlock PIN and probably unknown to the user.
    *   Ensure thread safety when potentially creating a wrapped key on multiple threads. 
*   1.2.0 - Acts like SharedPreference
    *   Removed API18 only key storage option.
    *   SecureVault changed to SharedPreferenceVault and extends SharedPreference interface and acts just like SharedPreference.
    *   With SharedPreference interface comes easier ability to store Float, Integer, Long, Boolean and String Set in addition to String.
*   1.1.1 - Future Proofing
    *   Deprecate API18 only storage because of hardware blacklist. 
*   1.1.0 - Samsunged + Bug
    *   Added hardware blacklist: Galaxy Note 2.
    *   Fixed rekey bug on legacy devices.
*   1.0.0 - Initial release
