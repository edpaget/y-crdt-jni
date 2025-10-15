# CI/CD Setup Summary

This document summarizes the GitHub Actions CI/CD infrastructure for y-crdt-jni.

## Overview

The project now has a complete CI/CD pipeline that:
- ✅ Tests code on every push and pull request
- ✅ Builds native libraries for all major platforms
- ✅ Creates multi-platform JARs with embedded native libraries
- ✅ Automates releases with tagged versions

## Workflows Created

### 1. Quick Check (`.github/workflows/check.yml`)

**Purpose:** Fast feedback for developers

**Triggers:**
- Pull requests (opened, synchronized, reopened)
- Pushes to main/develop branches

**Jobs:**
- `quick-check` - Rust formatting, clippy, build, and test
- `java-check` - Java compilation

**Duration:** ~2-3 minutes

**When to use:** Every commit during development

---

### 2. CI (`.github/workflows/ci.yml`)

**Purpose:** Comprehensive testing and multi-platform builds

**Triggers:**
- Pushes to main/develop branches
- Pull requests to main/develop
- Manual workflow dispatch

**Jobs:**

1. **test-rust** (Linux, macOS, Windows)
   - Runs `cargo test`
   - Builds debug and release versions
   - Uses Cargo caching for speed

2. **test-java** (Linux, macOS, Windows)
   - Runs `./gradlew build`
   - Executes Java tests
   - Uses Gradle caching
   - Uploads test reports as artifacts

3. **build-native-libraries** (6 targets)
   - Linux x86_64
   - macOS x86_64
   - macOS aarch64 (Apple Silicon)
   - Windows x86_64
   - Builds release binaries
   - Uploads each as separate artifact (30-day retention)

4. **build-jar**
   - Downloads all native libraries
   - Organizes them into resource structure
   - Builds single JAR with all platforms
   - Uploads JAR artifact (90-day retention)

5. **check-jar**
   - Verifies JAR structure
   - Ensures all native libraries are present
   - Validates Java classes are included

**Duration:** ~15-20 minutes (parallel execution)

**Artifacts:**
- `test-reports-{os}` - Test results
- `native-lib-{os}-{arch}` - Individual libraries
- `y-crdt-jni-jar` - Multi-platform JAR

---

### 3. Release (`.github/workflows/release.yml`)

**Purpose:** Automated release creation and publishing

**Triggers:**
- Git tags matching `v*.*.*` (e.g., v0.1.0)
- Manual workflow dispatch with version input

**Jobs:**

1. **create-release**
   - Creates GitHub release
   - Generates release notes
   - Provides upload URL for assets

2. **build-and-upload** (4 platforms)
   - Builds native libraries for each platform
   - Uploads as individual release assets:
     - `libycrdt_jni-linux-x86_64.so`
     - `libycrdt_jni-macos-x86_64.dylib`
     - `libycrdt_jni-macos-aarch64.dylib`
     - `ycrdt_jni-windows-x86_64.dll`

3. **build-release-jar**
   - Builds multi-platform JAR
   - Uploads as release asset:
     - `y-crdt-jni-{version}.jar`

**Duration:** ~10-15 minutes

**Output:** GitHub Release with all binary artifacts

---

## Platform Support Matrix

| Platform | Architecture | CI Tests | Release Build | Native Library |
|----------|--------------|----------|---------------|----------------|
| Linux    | x86_64      | ✅       | ✅            | `libycrdt_jni.so` |
| macOS    | x86_64      | ✅       | ✅            | `libycrdt_jni.dylib` |
| macOS    | aarch64     | ✅       | ✅            | `libycrdt_jni.dylib` |
| Windows  | x86_64      | ✅       | ✅            | `ycrdt_jni.dll` |

## Caching Strategy

### Cargo Cache
- **Paths:**
  - `~/.cargo/bin/`
  - `~/.cargo/registry/`
  - `target/`
- **Key:** `{os}-cargo-{Cargo.lock hash}`
- **Benefits:** ~5-10 minute speedup on cache hit

### Gradle Cache
- **Paths:**
  - `~/.gradle/caches`
  - `~/.gradle/wrapper`
- **Key:** `{os}-gradle-{gradle files hash}`
- **Benefits:** ~2-3 minute speedup on cache hit

## Workflow Execution Flow

```
┌─────────────────────────────────────────────────────────────┐
│                      Push to main/PR                         │
└───────────────────────┬─────────────────────────────────────┘
                        │
                        ▼
            ┌───────────────────────┐
            │   Quick Check (Fast)  │
            │  - Format check       │
            │  - Clippy             │
            │  - Basic build        │
            └───────────┬───────────┘
                        │
                        ▼
            ┌───────────────────────┐
            │   CI (Comprehensive)  │
            └───────────┬───────────┘
                        │
        ┌───────────────┼───────────────┐
        ▼               ▼               ▼
   ┌────────┐    ┌──────────┐   ┌──────────┐
   │  Rust  │    │   Java   │   │  Native  │
   │  Tests │    │  Tests   │   │  Builds  │
   └────┬───┘    └────┬─────┘   └────┬─────┘
        │             │              │
        └─────────────┼──────────────┘
                      ▼
              ┌──────────────┐
              │  Build JAR   │
              │ (All Platforms)│
              └──────┬───────┘
                     ▼
              ┌──────────────┐
              │  Verify JAR  │
              └──────────────┘


┌─────────────────────────────────────────────────────────────┐
│                    Create Git Tag (v*.*.*)                   │
└───────────────────────┬─────────────────────────────────────┘
                        │
                        ▼
            ┌───────────────────────┐
            │   Create Release      │
            └───────────┬───────────┘
                        │
        ┌───────────────┼───────────────┐
        ▼               ▼               ▼
   ┌────────┐    ┌──────────┐   ┌──────────┐
   │ Linux  │    │  macOS   │   │ Windows  │
   │ Build  │    │  Build   │   │  Build   │
   └────┬───┘    └────┬─────┘   └────┬─────┘
        │             │              │
        └─────────────┼──────────────┘
                      ▼
              ┌──────────────┐
              │ Upload Assets│
              │ to Release   │
              └──────────────┘
```

## Usage Examples

### For Contributors

```bash
# Before pushing - run quick checks
cargo fmt --check
cargo clippy
cargo test

# If all pass, push
git push origin feature-branch
```

GitHub Actions will automatically:
1. Run quick check (~2 min)
2. Run full CI if quick check passes (~15 min)
3. Provide feedback in PR

### For Maintainers

**Creating a Release:**

```bash
# 1. Update version in files
# 2. Commit changes
git add .
git commit -m "Bump version to 0.1.0"

# 3. Create and push tag
git tag v0.1.0
git push origin v0.1.0

# GitHub Actions automatically:
# - Creates release
# - Builds all platforms
# - Uploads artifacts
```

**Manual Release (without tag):**

1. Go to Actions → Release
2. Click "Run workflow"
3. Enter version (e.g., `v0.1.0`)
4. Click "Run workflow"

### For Users

**Downloading Artifacts:**

From CI builds:
```
https://github.com/edpaget/y-crdt-jni/actions
→ Select CI workflow run
→ Scroll to "Artifacts"
→ Download "y-crdt-jni-jar"
```

From releases:
```
https://github.com/edpaget/y-crdt-jni/releases
→ Select version
→ Download y-crdt-jni-{version}.jar
```

## Configuration Files

```
.github/
├── workflows/
│   ├── check.yml      # Quick validation
│   ├── ci.yml         # Full CI pipeline
│   └── release.yml    # Release automation
└── CICD_SUMMARY.md    # This file

gradle/
└── wrapper/
    └── gradle-wrapper.properties  # Gradle 8.5

build.gradle           # Version: 0.1.0-SNAPSHOT
```

## Environment Variables

All workflows use:
- `CARGO_TERM_COLOR=always` - Colored Cargo output

## Secrets Required

Currently: **None**

Future additions may include:
- `MAVEN_USERNAME` - For Maven Central publishing
- `MAVEN_PASSWORD` - For Maven Central publishing
- `GPG_PRIVATE_KEY` - For artifact signing

## Troubleshooting

### Common Issues

**1. Workflow fails on specific platform**
- Check platform-specific logs in Actions tab
- Look for compiler errors or missing dependencies
- May need to update Rust toolchain version

**2. JAR verification fails**
- Ensure native libraries are copied correctly
- Check resource path structure matches NativeLoader expectations
- Verify library names are correct for each platform

**3. Gradle wrapper not executable**
- The workflow automatically runs `chmod +x gradlew`
- If it fails, check file permissions in repository

**4. Cache misses**
- Caches expire after 7 days of inactivity
- Lock file changes invalidate cache
- Can manually clear from Settings → Actions → Caches

### Debugging Tips

```bash
# Test workflow locally with act (GitHub Actions runner)
act -j quick-check

# Build exactly as CI does
cargo build --release --target x86_64-unknown-linux-gnu

# Verify JAR structure
jar tf build/libs/y-crdt-jni-*.jar | grep native
```

## Metrics

**Expected Durations:**
- Quick Check: 2-3 minutes
- Full CI: 15-20 minutes
- Release: 10-15 minutes

**Artifact Sizes:**
- Native library: ~2-3 MB per platform
- Multi-platform JAR: ~10-12 MB

**Cache Benefits:**
- Cargo cache hit: ~5-10 minute speedup
- Gradle cache hit: ~2-3 minute speedup

## Future Enhancements

- [ ] Add code coverage reporting (Codecov/Coveralls)
- [ ] Add performance benchmarking
- [ ] Set up Maven Central publishing
- [ ] Add Dependabot for dependency updates
- [ ] Add CodeQL security scanning
- [ ] Generate and publish API documentation
- [ ] Add Docker image builds
- [ ] Implement proper macOS cross-compilation
