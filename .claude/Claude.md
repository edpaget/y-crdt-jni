# Development Guidelines for y-crdt-jni

## After Making Changes

### Rust Files (`*.rs`)

When you modify any Rust source files, **always** run:

```bash
cargo fmt
```

This ensures consistent code formatting across the project.

Then run tests to verify your changes:

```bash
cargo test
```

### Java Files (`*.java`)

When you modify any Java source files, **always** run checkstyle and tests:

```bash
./gradlew checkstyleMain checkstyleTest test
```

Or simply run the full check:

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

Or run the full build:

```bash
./gradlew build
```

## Quick Reference

| Change Type | Commands to Run |
|-------------|-----------------|
| Rust code (`src/*.rs`) | `cargo fmt` then `cargo test` |
| Java code (`src/**/*.java`) | `./gradlew checkstyleMain checkstyleTest test` |
| Build config | `./gradlew build` |
| Documentation | `./gradlew test` (verify examples) |

## Pre-Commit Checklist

Before committing changes:

- [ ] Run `cargo fmt` if Rust files changed
- [ ] Run `cargo test` - all tests passing
- [ ] Run `./gradlew test` - all tests passing
- [ ] Check `git status` - no unintended files staged
- [ ] Review `git diff` - changes are intentional

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

### Rust
```bash
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
```

### Java/Gradle
```bash
# Run checkstyle
./gradlew checkstyleMain checkstyleTest

# Run tests
./gradlew test

# Run checkstyle and tests
./gradlew check

# Full build (includes checkstyle and tests)
./gradlew build

# Clean build
./gradlew clean build

# Run example
./gradlew run
```

### Combined
```bash
# Format Rust and run all tests
cargo fmt && cargo test && ./gradlew test
```

## Notes

- **cargo fmt** is essential for Rust code - the project uses standard Rust formatting
- Tests must pass before pushing to ensure CI/CD succeeds
- The GitHub Actions "Quick Check" workflow will catch formatting issues
- If tests fail, fix them before committing
