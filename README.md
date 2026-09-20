# JJK Diagnostics

Android-first vehicle diagnostics for ThinkDiag TKD01.

Target Bluetooth identifier: 979869028107

## Current foundation
- Android application structure
- TKD01 BLE discovery and GATT service probing
- Standard OBD-II PID definitions
- Generic OBD response and DTC parsing layer
- Automated installable APK build through GitHub Actions

## Important hardware note
ThinkDiag documentation describes the TKD01 as a Bluetooth device and says generic OBD-II functions are available. The exact application-level transport used by this particular TKD01 must be identified against the user's physical unit before raw OBD commands are enabled. The app therefore probes the Bluetooth/GATT layer first rather than assuming an ELM327 protocol.

## Build trigger
- Manual workflow trigger is enabled for the installable APK build.
- A repository push to `main` also triggers the build.
