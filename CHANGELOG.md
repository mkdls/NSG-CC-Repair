# Changelog

## 1.1.3 — pre-release

- Preserve the Signaling viewport during passive CC Repair updates. Keeping the historical cursor locked in 1.1.2 did not stop the native signaling callback from repeatedly scrolling to that cursor row.
- Mark index/RRC completion and locked live refreshes as passive, using the native signaling callback's self-origin branch. Message data updates continue without forced row selection.
- Retain manual time-axis positioning, unlocked live following and the existing carrier-repair algorithms.
- Add scroll-guard installation/deoptimization status and passive refresh counts. If guard installation fails, skip passive broadcasts rather than repeatedly selecting a row.
- Generate in-app version labels from build metadata, use fresh compile directories and add passive viewport regression tests.
- Validation: four desktop suites and APK signing checks passed. The reporting Pixel user confirmed scrolling stays in place with Signaling Export 0.1.40 enabled; other device/framework combinations still require validation.

## 1.1.2 — pre-release

- Honor NSG's display lock during Testing: background index refresh uses the selected workspace cursor while locked and returns to the latest sample only when unlocked.
- Continue acquisition and indexing while the user seeks buffered history.
- Add cursor-transition tests covering live, lock, history seek, unlock and loaded-file replay.
- Keep NR CC Metrics in the separate NSG Signaling Export module (use 0.1.39-metrics-pager for its matching fix).
- Cursor fix: desktop tests passed; phone validation pending.


## 1.0.9 — pre-release

- Standalone carrier repair module (`com.nsgcc`); no signaling export or authorization-state changes.
- Replay indexing and root-assisted live raw-log following.
- Time-aware Relay/SCell mapping, RRC-derived frequencies, native-style downlink rows and BLER.
- Added exact native throughput-window matching for uplink label identity. The supplied two-SCC numerical vector passes offline tests, but the user still reported failure on-device. This remains an experimental path, not a confirmed uplink fix.
- English UI and copyable status diagnostics.

Initial public publication preserves the existing 1.0.9 application behavior. Public build scripts, documentation, license, and synthetic tests were prepared separately for distribution.
