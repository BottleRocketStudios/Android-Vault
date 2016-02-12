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
            compile 'com.bottlerocketstudios:vault:1.3.0'
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
            compile 'com.bottlerocketstudios:vault:1.3.1-SNAPSHOT'
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
The vault can be rekeyed at any time. This will delete all values in the shared 
preference file. This is completely irreversible.

### Build
This project must be built with gradle. 

*   Version Numbering - The version name should end with "-SNAPSHOT" for non release builds. This will cause the resulting binary, source, and javadoc files to be uploaded to the snapshot repository in Maven as a snapshot build. Removing snapshot from the version name will publish the build on jcenter. If that version is already published, it will not overwrite it.
*   Execution - To build this libarary, associated tasks are dynamically generated by Android build tools in conjunction with Gradle. Example command for the production flavor of the release build type: 
    *   Build and upload: `./gradlew --refresh-dependencies clean uploadToMaven`
    *   Build only: `./gradlew --refresh-dependencies clean jarRelease`
