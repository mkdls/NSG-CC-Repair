# NSG CC Repair

Experimental standalone LSPosed module for missing carrier rows in **Network Signal Guru (NSG) qtrun 4.8.10**, observed with Pixel Shannon modems and Huawei n257 cells using 100 MHz component carriers.

**Current release: 1.1.2 — pre-release.** Fixes background refresh overriding the selected history cursor during locked Testing. The cursor fix has desktop coverage and needs phone validation. Existing downlink repair was field-tested in the target scenario; partial uplink-label problems remain unresolved.

## What it does

- Reconstructs carrier identities from Relay mapping messages and SCell state records.
- Displays additional downlink rows in NSA Data Metrics, including carriers without their own SSB.
- Adds frequencies from complete SCG RRC configurations when available.
- Provides per-CC downlink throughput and BLER with time-aware lookup for replay seeking.
- Supports Replay and live Testing through an independently opened raw log.
- Provides copyable diagnostics under **Signaling → CC repair status**.

A frequency marked `*` is an **RRC-derived resource-grid center**, not a measured SSB ARFCN. Unknown identities, frequencies, and metrics are not fabricated.

## Requirements and scope

- Root and a framework supporting modern libxposed API 101/102.
- NSG **qtrun 4.8.10**, package `com.qtrun.QuickTest`.
- Recognized Shannon profiles: **ICD9 / 1700.24.79** and **1700.26.36**.
- Target mapping format: **n257, 100 MHz, 120 kHz SCS**, verified single-stack/module 1 context.

This is not a general repair for every modem, NSG release, band, or dual-SIM configuration. Unsupported data retains native rows. Recognition of a firmware profile does not imply all radio scenarios have been validated.

## Installation

1. Install the APK from [Releases](https://github.com/mkdls/NSG-CC-Repair/releases).
2. Enable **NSG CC Repair** in LSPosed and scope it to Network Signal Guru.
3. Disable combined NSG Signaling Export **0.1.35 and earlier**, because they also modify carrier rows. Export **0.1.36-export-only and newer separate-export builds** can be enabled alongside this module. For the NR CC Metrics swipe page, update NSG Signaling Export to **0.1.39-metrics-pager** to obtain its corresponding cursor fix.
4. Fully stop and restart NSG.

### Replay

Load a supported `.log` or `.log.gz`, then view **NSA Data Metrics**. Complete SCG RRC configuration is searched automatically with a limited decode budget. If it is unavailable, manually open a complete SCG message and return to the metrics page. The module caches only details NSG actually returns.

### Live Testing

Enable **raw log saving in NSG**, then start a new test/recording. Allow the root read request when prompted. The module independently follows the regular log file for the current Testing session; it does not consume NSG's modem/socket stream or modify the log. A `Testing live log` status with increasing counters indicates the reader is active. Complete RRC may still be required for frequency labels; a late start may miss initial configuration.

### Buffered history during Testing

Tap the lock to stop following live time, then seek with the replay controls. Recording and indexing continue in the background while carrier rows follow the selected history cursor. Unlocking restores live display. The optional NR CC Metrics swipe page belongs to NSG Signaling Export; update that module separately.

## Known limitations

- Some uplink SCC labels remain unknown, especially when native row counts change. Uplink throughput and BLER remain native values.
- Brief returns to the native 3-CC display can occur; no display hold buffer is implemented.
- RRC caching is not a full add/modify/release state machine.
- RRC detail availability depends on NSG's own decoding and authorization. This module does not alter authorization or decoder state.
- Live log rotation/truncation and firmware changes may require restarting recording or extending format support.
- Homepage/Cell Table measurement rows are not the same as the configured carrier-aggregation list.

## Build

Windows, PowerShell, **JDK 21**, Android SDK Platform 36 and Build Tools 36.0.0:

```powershell
$env:JAVA_HOME = 'PATH_TO_JDK_21'
./build.ps1 -Sdk 'PATH_TO_ANDROID_SDK'
```

The script downloads the compile-only libxposed API 102.0.0 AAR from Maven Central and verifies its pinned SHA-256. Output: `dist/NSG-CC-Repair-1.0.9.apk`. Generated development keys stay under ignored `.build/`; they are not the maintainer's update key. Keep one signing key across your own updates.

For an existing private signing key, pass `-Keystore` and `-KeyAlias`, with passwords in `CC_REPAIR_STORE_PASSWORD` and `CC_REPAIR_KEY_PASSWORD`. Do not commit keys or passwords.

Run self-contained tests with `./test.ps1`. Device-log regression fixtures are deliberately excluded from this repository.

## Reporting issues

Include module version, NSG version, firmware profile, Replay/Testing mode, and a redacted copy of **CC repair status**. Review diagnostics before posting; paths, times, and serving-cell identifiers can reveal private information. Do not upload unredacted modem logs publicly.

## License and references

Copyright (C) 2026 mkdls. Licensed under **GPL-3.0-only**; see [LICENSE](LICENSE). Dependency and reference notices are in [NOTICE](NOTICE). Inspired by the NSG Tweaks approach. This is an independent project, not affiliated with NSG or its authors.
