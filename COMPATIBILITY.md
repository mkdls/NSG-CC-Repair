# Vector 3080 and qtrun 4.8.12 compatibility

Inspected the supplied Vector-v2.2-3080-Release.zip offline. Its module id is zygisk_vector. Its bundled framework exposes libxposed API 102, default-constructed XposedModule entries, onPackageReady, hook/intercept and deoptimize. These match the modules' modern API entry points; no legacy bridge or bundled framework API classes are added.

CC Repair 1.1.4: runtime diagnostics identify the actual host version/code, framework name/version and API. Java entry-point mappings were compared with the supplied qtrun 4.8.12 decompilation. Existing carrier/index logic and the locked Signaling scroll fix are retained. The verified raw-log profiles remain Shannon ICD9 1700.24.79 / 1700.26.36, n257/100 MHz/120 kHz; this is not a claim of arbitrary modem or mmWave-band support.

Signaling Export 0.1.42-vector-4812: export accepts qtrun 4.8.10 and 4.8.12 and records the actual host version in TXT/JSON. Diagnostics include the framework identity. Native detail handling selects the exact audited ARM64 profile for each version and checks six instruction words; unknown hashes are rejected. The old decode-counter address is read only on its audited 4.8.10 profile. A normal NSG launch button does not register compiled caches. Legacy LSPosed cache writes are explicitly refused if the Vector module is present; the old database workflow has not been ported by guessing its schema.

Idle SCC 1.0.13: retains existing 4.8.10 / 4.8.12 audited profiles and the paired DisplayOnly/DecodeRepair boundary; runtime status now identifies Vector/LSPosed and API. Existing release-aware fallback, marker settings and overflow diagnostics are retained.

Validation is offline: signed APK builds, CC regression suites, both Idle SCC regression suites, and independent native ELF hash/six-instruction/pointer audits against the supplied host APKs. Vector injection and qtrun 4.8.12 live modem/replay behavior have not been handset-tested. The handset remains under the user's control; no installation or restart was performed.

Install only one Idle SCC variant. If Export owns native truncation repair, prefer DisplayOnly rather than stacking two native repair owners. Scope enabled modules to com.qtrun.QuickTest in Vector, then fully restart NSG. Raw log recording remains required for CC Repair live Testing indexing.
