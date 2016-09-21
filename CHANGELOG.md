# Vault Changelog #

*   1.4.0 - Make OAEP padding default for wrapped keys.
*   1.3.1 - Catch any test failure
*	1.3.0 - Lock Screen and Memory Only
	*	Create a vault that can only be opened on API23+ devices which have been unlocked recently.
	*	Create a vault that requires a user supplied password to unlock.
	*   Create a method to determine which type of key storage a vault is using. 
*   1.2.5 - Open source release.
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
