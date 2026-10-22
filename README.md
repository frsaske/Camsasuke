# Sasuke RetroCam

## Automated Android releases

Every push to `main` and every manual workflow run executes unit tests, builds the release APK, uploads it as a workflow artifact, and publishes it to a GitHub Release.

For a persistent signing identity (required for installing future updates over an existing APK), add these repository secrets:

- `KEYSTORE_BASE64`: Base64-encoded `.jks` keystore
- `STORE_PASSWORD`: Keystore password
- `KEY_PASSWORD`: `upload` key password

If the secrets are not configured, GitHub Actions creates a temporary keystore so the build and release can still complete. APKs signed this way should be treated as test releases and cannot be used for seamless updates after the keystore changes.
