# Changelog

All notable changes to y-crdt-jni modules are recorded here. The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html) per-module.

While work is in flight, add entries under `[Unreleased]` in the appropriate category (**Added**, **Changed**, **Deprecated**, **Removed**, **Fixed**, **Security**). The `prepare-release.yml` workflow inserts a dated release section after `[Unreleased]`; hand-edit the tag list it writes into the proper categories and clear `[Unreleased]` ahead of the next cycle.

## [Unreleased]

## 2026-04-21 — natives release

### Added

- `net.carcdr:ycrdt-jni:0.1.1` — first Maven Central release of the JNI binding. Native libraries bundled for linux/x86_64, macos/aarch64, windows/x86_64.
- `net.carcdr:ycrdt-panama:0.1.1` — first Maven Central release of the Panama/FFM binding. Native libraries bundled for linux/x86_64, macos/aarch64.
- `net.carcdr:ycrdt-bom:0.1.5` — BOM now also pins `ycrdt-jni:0.1.1` and `ycrdt-panama:0.1.1`.

## 2026-04-21 — multi-module release

### Added

- `net.carcdr:ycrdt-core:0.1.3` — second patch release.
- `net.carcdr:yhocuspocus:0.1.1` — first Maven Central release of the collaborative editing server.
- `net.carcdr:ycrdt-bom:0.1.3` — BOM now pins `ycrdt-core:0.1.3` and `yhocuspocus:0.1.1`.

### Fixed

- `ycrdt-bom` pins constituents from the most recent `<module>/<version>` git tag instead of `version.properties`. The previous logic pinned `-SNAPSHOT` versions after `post-release.yml` restored them, which produced unresolvable BOM coordinates. Modules without a release tag are now skipped so single-module releases can publish a coherent BOM.
- `release.yml` no longer blocks BOM publication when constituent modules are still at `-SNAPSHOT`. The check was redundant after the BOM pin-from-tags fix.

## 2026-04-21 — first Maven Central release

### Added

- `net.carcdr:ycrdt-core:0.1.1` — first public release of the Y-CRDT Java core interfaces.
- `net.carcdr:ycrdt-bom:0.1.1` — first BOM release (pins `ycrdt-core:0.1.1`).
