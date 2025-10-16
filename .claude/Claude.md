# Development Guidelines for y-crdt-jni

## Project Structure

This is a **multi-module Gradle project**:

```
y-crdt-jni/
├── ycrdt/              # Core Y-CRDT JNI bindings (Java + Rust)
│   ├── build.gradle    # Gradle build for Java layer
│   ├── Cargo.toml      # Rust build for native library
│   └── src/
│       ├── main/java/  # Java classes
│       ├── main/rust/  # Rust JNI bindings
│       └── test/java/  # Java tests
├── yprosemirror/       # ProseMirror integration (Java only)
│   ├── build.gradle
│   └── src/
│       ├── main/java/
│       └── test/java/
├── yhocuspocus/        # Hocuspocus server (planned, Java only)
│   └── ...
└── build.gradle        # Root project configuration
```

## After Making Changes

### Rust Files (`*.rs`) in ycrdt module

When you modify any Rust source files in `ycrdt/src/`, **always** run:

```bash
cd ycrdt
cargo fmt
```

This ensures consistent code formatting across the project.

Then run tests to verify your changes:

```bash
cargo test
```

**Note:** Rust code only exists in the `ycrdt` module. Other modules (yprosemirror, yhocuspocus) are pure Java.

### Java Files (`*.java`)

When you modify any Java source files, **always** run checkstyle and tests.

#### For specific module:

```bash
# For ycrdt module
./gradlew :ycrdt:checkstyleMain :ycrdt:checkstyleTest :ycrdt:test

# For yprosemirror module
./gradlew :yprosemirror:checkstyleMain :yprosemirror:checkstyleTest :yprosemirror:test
```

#### For all modules:

```bash
./gradlew checkstyleMain checkstyleTest test
```

Or simply run the full check on all modules:

```bash
./gradlew check
```

### Any Other Files

After making changes to:
- Build configuration (`build.gradle`, `Cargo.toml`)
- Documentation files
- GitHub Actions workflows
- Any other project files

**Always** run the full test suite to ensure nothing broke:

```bash
./gradlew test
```

Or run the full build (all modules):

```bash
./gradlew build
```

## Quick Reference

| Change Type | Location | Commands to Run |
|-------------|----------|-----------------|
| Rust code (`*.rs`) | `ycrdt/src/` | `cd ycrdt && cargo fmt && cargo test` |
| Java code (ycrdt) | `ycrdt/src/main/java/` | `./gradlew :ycrdt:checkstyleMain :ycrdt:test` |
| Java code (yprosemirror) | `yprosemirror/src/main/java/` | `./gradlew :yprosemirror:checkstyleMain :yprosemirror:test` |
| Java code (all modules) | Any module | `./gradlew checkstyleMain checkstyleTest test` |
| Build config | Any `build.gradle` or `Cargo.toml` | `./gradlew build` |
| Documentation | Any `*.md` | `./gradlew test` (verify examples) |

## Pre-Commit Checklist

Before committing changes:

- [ ] Run `cd ycrdt && cargo fmt` if Rust files changed (ycrdt module only)
- [ ] Run `cargo test` in ycrdt directory - all Rust tests passing
- [ ] Run `./gradlew test` - all Java tests passing (all modules)
- [ ] Run `./gradlew checkstyleMain checkstyleTest` - no style violations
- [ ] Check `git status` - no unintended files staged
- [ ] Review `git diff` - changes are intentional
- [ ] Update relevant documentation (see "Update Project Documentation" section)

## CI/CD Validation

Remember that GitHub Actions will automatically:
- Check Rust formatting with `cargo fmt --check`
- Run `cargo clippy` for linting
- Run all Rust tests
- Run Checkstyle on Java code
- Run all Java tests
- Build on multiple platforms

Make sure your changes pass locally before pushing!

## Common Commands

### Rust (ycrdt module only)
```bash
# Navigate to ycrdt module first
cd ycrdt

# Format code
cargo fmt

# Check formatting without changes
cargo fmt --check

# Run linter
cargo clippy

# Run tests
cargo test

# Build release
cargo build --release

# Return to root
cd ..
```

### Java/Gradle (all modules)
```bash
# Run checkstyle on all modules
./gradlew checkstyleMain checkstyleTest

# Run checkstyle on specific module
./gradlew :ycrdt:checkstyleMain :ycrdt:checkstyleTest
./gradlew :yprosemirror:checkstyleMain :yprosemirror:checkstyleTest

# Run tests on all modules
./gradlew test

# Run tests on specific module
./gradlew :ycrdt:test
./gradlew :yprosemirror:test

# Run checkstyle and tests (all modules)
./gradlew check

# Full build (includes checkstyle and tests, all modules)
./gradlew build

# Build specific module
./gradlew :ycrdt:build
./gradlew :yprosemirror:build

# Clean build
./gradlew clean build

# Run example (ycrdt module)
./gradlew :ycrdt:run

# Generate Javadoc (all modules)
./gradlew javadoc

# Generate Javadoc for specific module
./gradlew :ycrdt:javadoc
./gradlew :yprosemirror:javadoc

# List all modules
./gradlew projects
```

### Combined Workflows
```bash
# Format Rust and run all tests (Rust + Java)
cd ycrdt && cargo fmt && cargo test && cd .. && ./gradlew test

# Full validation before committing
cd ycrdt && cargo fmt && cargo clippy && cargo test && cd .. && ./gradlew check

# Quick check (formatting + tests only)
cd ycrdt && cargo fmt --check && cargo test && cd .. && ./gradlew test
```

## Update Project Documentation

After making significant changes to the codebase, **always** update the relevant documentation files:

### When to Update Documentation

1. **PLAN.md** - Update when:
   - Completing a phase or milestone
   - Changing implementation approach
   - Adding/removing major features
   - Updating project status or timelines

2. **IMPLEMENTATION.md** - Update when:
   - Adding new JNI bindings or native methods
   - Changing build system or architecture
   - Adding new classes or modules
   - Modifying memory management patterns

3. **CHANGELOG.md** - Update when:
   - Adding new features
   - Fixing bugs
   - Changing APIs (breaking or non-breaking)
   - Updating dependencies
   - Releasing versions

4. **README.md** - Update when:
   - Adding new features that users should know about
   - Changing installation or usage instructions
   - Updating examples
   - Modifying project structure

5. **plans/*.md** - Update when:
   - Completing planned features
   - Changing implementation status
   - Making design decisions
   - Identifying new requirements

### Documentation Update Checklist

After implementing changes:

- [ ] Update test counts in PLAN.md if tests were added
- [ ] Update implementation status in PLAN.md if phases completed
- [ ] Add entry to CHANGELOG.md describing changes
- [ ] Update IMPLEMENTATION.md if architecture changed
- [ ] Update README.md if user-facing features changed
- [ ] Update relevant plans/*.md files if design evolved
- [ ] Update JavaDoc/code comments for API changes

## Understanding Test Output

### What You'll See When Running Tests

When you run `./gradlew test`, you'll see:

1. **Rust tests** run first (from `testRust` task in ycrdt):
   - Shows `running 36 tests` with individual test names
   - Uses cargo's test output format
   - Located in `ycrdt/src/`

2. **Java tests** run after Rust tests:
   - Shows each test class and method: `net.carcdr.ycrdt.YTextTest > testInsert PASSED`
   - Summary at end: `Java Test Results: SUCCESS (290 tests, 290 passed, 0 failed, 0 skipped)`
   - Located in `ycrdt/src/test/java/` and `yprosemirror/src/test/java/`

3. **Build summary**:
   - `BUILD SUCCESSFUL in Xs`
   - List of tasks executed

**Note:** If tests are `UP-TO-DATE`, Gradle thinks nothing changed and skips them. Use `--rerun-tasks` to force execution:

```bash
./gradlew test --rerun-tasks
```

### Test Output Configuration

Both ycrdt and yprosemirror modules are configured to show:
- Individual test pass/fail status
- Test count summary after each test suite
- Full exception details on failures

## Notes

- **cargo fmt** is essential for Rust code - the project uses standard Rust formatting
- **Both Rust AND Java tests run** when you execute `./gradlew test` (Rust first, then Java)
- Tests must pass before pushing to ensure CI/CD succeeds
- The GitHub Actions "Quick Check" workflow will catch formatting issues
- If tests fail, fix them before committing
- **Keep documentation in sync** - outdated docs are worse than no docs
