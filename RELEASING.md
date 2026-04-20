# Releasing y-crdt-jni

This document describes how y-crdt-jni modules are released to Maven Central, and the one-time setup that is required before the first release.

## Artifact layout

- **Namespace**: `net.carcdr`
- **Publishable modules** (8): `ycrdt-core`, `ycrdt-jni`, `ycrdt-panama`, `yprosemirror`, `yhocuspocus`, `yhocuspocus-websocket`, `yhocuspocus-spring-websocket`, `yhocuspocus-redis`
- **BOM**: `ycrdt-bom` — pins versions for all 8 modules
- **Snapshots**: GitHub Packages (`https://maven.pkg.github.com/edpaget/y-crdt-jni`), published on every push to `main`
- **Releases**: Maven Central Portal (`https://central.sonatype.com`), published per-module on module-scoped git tags (`<module>/<version>`)

## Per-module versioning

Each publishable module owns a `<module>/version.properties` file. The root `build.gradle` reads it during configuration. Non-publishable modules (`examples/*`, `ycrdt-benchmarks`) inherit the root fallback.

## One-time setup

The following steps are required before the first Maven Central release. They are not reversible without coordination with Sonatype and GitHub, so take care.

### 1. Verify the `net.carcdr` namespace on Central Portal

1. Sign in to https://central.sonatype.com with the account that owns the `carcdr.net` DNS record.
2. Under **Namespaces**, confirm `net.carcdr` shows **Verified**.
3. If it is still pending, follow the DNS TXT record instructions Sonatype displays and wait for verification.

### 2. Generate a release GPG key

All Maven Central artifacts must be signed with a GPG key whose public half is published to a keyserver.

```bash
# Generate a dedicated release key (do not reuse a personal key).
gpg --full-generate-key
#   Kind: RSA and RSA
#   Size: 4096
#   Expires: 2y (or longer)
#   Name:  y-crdt-jni release
#   Email: your-release-email@example.com

# Note the key's long ID (8 hex digits) from:
gpg --list-secret-keys --keyid-format=long

# Publish the public key to the keyservers Sonatype queries.
gpg --keyserver keys.openpgp.org --send-keys <LONG_KEY_ID>
gpg --keyserver keyserver.ubuntu.com --send-keys <LONG_KEY_ID>

# Export the ASCII-armored private key for GitHub Actions. Write it outside the
# working tree so it cannot be accidentally committed.
gpg --armor --export-secret-keys <LONG_KEY_ID> > /tmp/release-signing-key.asc
```

Paste the file's contents into the `SIGNING_KEY` secret (step 3), then delete it:

```bash
shred -u /tmp/release-signing-key.asc 2>/dev/null || rm -P /tmp/release-signing-key.asc
```

### 3. Add GitHub Actions secrets

In the repo's **Settings -> Secrets and variables -> Actions -> Repository secrets**, create:

| Secret | Value | Source |
|---|---|---|
| `MAVEN_CENTRAL_USERNAME` | Central Portal user token name | https://central.sonatype.com -> **View Account** -> **Generate User Token** |
| `MAVEN_CENTRAL_PASSWORD` | Central Portal user token password | same page |
| `SIGNING_KEY` | contents of `release-signing-key.asc` | step 2 |
| `SIGNING_PASSWORD` | passphrase for the GPG key | step 2 |

The release workflow remaps these secrets to the Gradle properties the `com.vanniktech.maven.publish` plugin reads. The mapping (done by `env:` blocks in `release.yml`) is:

| GitHub Actions secret | Environment variable the workflow sets |
|---|---|
| `MAVEN_CENTRAL_USERNAME` | `ORG_GRADLE_PROJECT_mavenCentralUsername` |
| `MAVEN_CENTRAL_PASSWORD` | `ORG_GRADLE_PROJECT_mavenCentralPassword` |
| `SIGNING_KEY` | `ORG_GRADLE_PROJECT_signingInMemoryKey` |
| `SIGNING_PASSWORD` | `ORG_GRADLE_PROJECT_signingInMemoryKeyPassword` |

### 4. Create a release GitHub App

Releases are pushed by a GitHub App rather than a user PAT, so that the commits and tags the `prepare-release.yml` workflow creates can trigger the downstream `release.yml` workflow. Tokens minted from the built-in `GITHUB_TOKEN` do not trigger other workflows.

1. Go to **Settings -> Developer settings -> GitHub Apps -> New GitHub App**.
2. Name: `y-crdt-jni release bot` (or similar). Homepage URL can point to the repo.
3. Uncheck **Webhook - Active**.
4. Permissions (repository):
   - **Contents**: Read and write (needed to push commits and tags)
   - **Actions**: Read (needed to inspect workflow status)
5. Leave all other permissions and events at default (none).
6. Create the app, then **Install App** on the `edpaget/y-crdt-jni` repository only.
7. Under **General -> Private keys**, generate a private key (`.pem` file) and save it.
8. Note the **App ID** (shown on the app's General page).

Add two more GitHub Actions secrets:

| Secret | Value |
|---|---|
| `RELEASE_APP_ID` | the App ID number |
| `RELEASE_APP_PRIVATE_KEY` | the full `.pem` file contents |

## Local dry-run

To verify the publishing configuration without pushing to Central:

```bash
# Publish to the local Maven cache (no signing, no network).
./gradlew publishToMavenLocal

# Inspect the generated POM for a module:
cat ~/.m2/repository/net/carcdr/ycrdt-core/<version>/ycrdt-core-<version>.pom

# With signing keys exported as env vars, publishToMavenLocal produces .asc files.
ORG_GRADLE_PROJECT_signingInMemoryKey="$(cat /tmp/release-signing-key.asc)" \
ORG_GRADLE_PROJECT_signingInMemoryKeyPassword="<passphrase>" \
    ./gradlew :ycrdt-core:publishToMavenLocal
ls ~/.m2/repository/net/carcdr/ycrdt-core/<version>/
#   ...jar.asc, ...pom.asc, ...sources.jar.asc, ...javadoc.jar.asc
```

## Release flow (once the workflows are in place)

Release workflows are added in later phases of the `maven-central-publishing` roadmap:

- `prepare-release.yml` (phase 4): dispatchable from the Actions UI. Bumps `<module>/version.properties` for the modules you select, updates `ycrdt-bom`'s constraint versions, commits, and pushes `<module>/<version>` tags.
- `release.yml` (phase 5): triggered by `<module>/<version>` tags. Builds native libraries for JNI/Panama modules, signs all artifacts, and publishes to Central.

Until those workflows are in place, releases can be cut manually:

```bash
# From a clean checkout on the release commit, edit the module's
# version.properties to the release version (drop the -SNAPSHOT suffix),
# then run:
./gradlew :ycrdt-core:publishAndReleaseToMavenCentral \
    -PmavenCentralUsername=<token-name> \
    -PmavenCentralPassword=<token-password> \
    -PsigningInMemoryKey="$(cat /tmp/release-signing-key.asc)" \
    -PsigningInMemoryKeyPassword=<passphrase>
```
