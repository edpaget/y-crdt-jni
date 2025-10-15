# GitHub Actions Workflows

This directory contains the CI/CD workflows for the y-crdt-jni project.

## Workflows

### 1. Quick Check (`check.yml`)

**Trigger:** Pull requests and pushes to main/develop branches

Fast feedback workflow that runs:
- Rust formatting check (`cargo fmt`)
- Rust linting (`cargo clippy`)
- Rust build and tests
- Java compilation check

**Purpose:** Catch common issues early in development.

### 2. CI (`ci.yml`)

**Trigger:** Pushes to main/develop, pull requests, manual dispatch

Comprehensive testing workflow that:
1. **Test Rust** - Runs Rust tests on Linux, macOS, and Windows
2. **Test Java** - Builds and tests Java code with Gradle on all platforms
3. **Build Native Libraries** - Builds native libraries for all target platforms:
   - Linux x86_64
   - macOS x86_64
   - macOS aarch64 (Apple Silicon)
   - Windows x86_64
4. **Build JAR** - Creates a single JAR with all native libraries embedded
5. **Verify JAR** - Ensures the JAR contains all required files

**Artifacts:**
- Test reports from each platform
- Native libraries for each platform (30-day retention)
- Multi-platform JAR (90-day retention)

### 3. Release (`release.yml`)

**Trigger:** Git tags matching `v*.*.*` or manual dispatch

Production release workflow that:
1. Creates a GitHub release
2. Builds native libraries for all platforms
3. Uploads individual native libraries as release assets
4. Builds and uploads a multi-platform JAR

**Release Assets:**
- `libycrdt_jni-linux-x86_64.so`
- `libycrdt_jni-macos-x86_64.dylib`
- `libycrdt_jni-macos-aarch64.dylib`
- `ycrdt_jni-windows-x86_64.dll`
- `y-crdt-jni-{version}.jar` (contains all native libraries)

## Using the Workflows

### Running Tests Locally

Before pushing, you can run the same checks locally:

```bash
# Rust checks
cargo fmt --check
cargo clippy
cargo test

# Java checks
./gradlew build
```

### Creating a Release

To create a new release:

1. Update the version in relevant files
2. Commit your changes
3. Create and push a tag:
   ```bash
   git tag v0.1.0
   git push origin v0.1.0
   ```
4. The release workflow will automatically build and publish the release

Alternatively, use the manual workflow dispatch:
- Go to Actions → Release → Run workflow
- Enter the version (e.g., `v0.1.0`)

### Downloading Artifacts

CI build artifacts can be downloaded from:
- [GitHub Actions](https://github.com/edpaget/y-crdt-jni/actions) → CI workflow → Select a run → Artifacts section

Artifacts include:
- `test-reports-{os}` - Test results and reports
- `native-lib-{os}-{arch}` - Individual platform libraries
- `y-crdt-jni-jar` - Complete JAR with all platforms

### Caching

The workflows use GitHub Actions caching to speed up builds:
- **Cargo cache** - Rust dependencies and build artifacts
- **Gradle cache** - Java dependencies and Gradle wrapper

Caches are keyed by OS and lock file hashes, so they automatically invalidate when dependencies change.

## Platform Support

### Tested Platforms

| OS      | Architecture | Status |
|---------|-------------|--------|
| Linux   | x86_64      | ✅     |
| macOS   | x86_64      | ✅     |
| macOS   | aarch64     | ✅     |
| Windows | x86_64      | ✅     |

### Cross-Compilation Notes

- **macOS cross-compilation** from Linux requires additional tooling (not yet implemented)
- **Windows cross-compilation** from Linux uses `x86_64-pc-windows-msvc` target
- **aarch64 macOS** builds run natively on GitHub's macOS runners

## Troubleshooting

### Workflow Failures

1. **Rust compilation errors**
   - Check that `Cargo.toml` dependencies are up to date
   - Ensure code compiles on all target platforms

2. **Java compilation errors**
   - Verify Java 11 compatibility
   - Check that native method signatures match between Rust and Java

3. **JAR verification failures**
   - Ensure native libraries are copied to correct resource paths
   - Check `NativeLoader.java` platform detection logic

4. **Cache issues**
   - Cache can be manually cleared from GitHub repository settings
   - Caches automatically expire after 7 days of inactivity

### Local Testing

To test the multi-platform JAR creation locally:

```bash
# Build Rust library
cargo build --release

# Build JAR
./gradlew jar

# Verify JAR contents
jar tf build/libs/y-crdt-jni-*.jar | grep native
```

## Future Improvements

- [ ] Add code coverage reporting
- [ ] Add performance benchmarking
- [ ] Set up Maven Central publishing
- [ ] Add security scanning (Dependabot, CodeQL)
- [ ] Add documentation generation and deployment
- [ ] Implement proper macOS cross-compilation from Linux
