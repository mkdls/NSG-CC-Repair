# Changelog

## 1.0.9 — pre-release

- Standalone carrier repair module (`com.nsgcc`); no signaling export or authorization-state changes.
- Replay indexing and root-assisted live raw-log following.
- Time-aware Relay/SCell mapping, RRC-derived frequencies, native-style downlink rows and BLER.
- Added exact native throughput-window matching for uplink label identity. The supplied two-SCC numerical vector passes offline tests, but the user still reported failure on-device. This remains an experimental path, not a confirmed uplink fix.
- English UI and copyable status diagnostics.

Initial public publication preserves the existing 1.0.9 application behavior. Public build scripts, documentation, license, and synthetic tests were prepared separately for distribution.
