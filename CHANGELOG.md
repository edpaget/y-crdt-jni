# Changelog

All notable changes to y-crdt-jni modules are recorded here. The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html) per-module.

While work is in flight, add entries under `[Unreleased]` in the appropriate category (**Added**, **Changed**, **Deprecated**, **Removed**, **Fixed**, **Security**). The `prepare-release.yml` workflow inserts a dated release section after `[Unreleased]`; hand-edit the tag list it writes into the proper categories and clear `[Unreleased]` ahead of the next cycle.

## [Unreleased]

### Changed

- Release POMs now rewrite each `net.carcdr:*:*-SNAPSHOT` sibling dep to the latest `<module>/<semver>` git tag at publish time. A downstream module (e.g. `ycrdt-jni`) can be released without its upstream (`ycrdt-core`) in the same `prepare-release.yml` dispatch, as long as the upstream has at least one release tag. Releases fail fast with a named-module error if no upstream tag exists.
- Gradle Module Metadata (`.module`) publication is disabled for release publications (kept for `-SNAPSHOT` publishes to GitHub Packages). Gradle consumers of released artifacts now resolve via the POM, which avoids GMM drifting from the rewritten POM dep versions.

### Fixed

- Released POM `<license>` metadata now declares Apache License 2.0, matching the project's `LICENSE` file and every module README. Previous releases advertised GPLv3 in POM metadata due to stale `gradle.properties` defaults; already-published artifacts on Maven Central are immutable and must be superseded by new releases.
- Root `build.gradle` POM `withXml` SNAPSHOT-rewrite filters `depthFirst()` results to `Node` instances before invoking `.name()`. Under Gradle 9.4.1 (Groovy 4) the traversal can emit bare `String` characters when a leaf element's text is stored as a raw `String`, which caused `generatePomFileForMavenPublication` to abort on POMs with multiple dependency coordinate groups (e.g. `yprosemirror/0.1.1`).

## 2026-04-22

- yprosemirror/0.1.3
- ycrdt-bom/0.1.13

## 2026-04-22

- yprosemirror/0.1.1
- ycrdt-bom/0.1.11

## 2026-04-22

- yhocuspocus-websocket/0.1.1
- yhocuspocus-spring-websocket/0.1.1
- yhocuspocus-redis/0.1.1
- ycrdt-bom/0.1.9

## 2026-04-21

- ycrdt-jni/0.1.3
- ycrdt-panama/0.1.3
- ycrdt-bom/0.1.7

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
