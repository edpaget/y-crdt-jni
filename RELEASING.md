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

Local dry-runs normally keep `version.properties` on `-SNAPSHOT`. If you hand-edit
a module's `version.properties` to a concrete release version to inspect the
released POM, the root `build.gradle` will rewrite any sibling `-SNAPSHOT` dep
to the latest `<sibling>/<semver>` git tag. If no such tag exists (first-time
release for the sibling), `publishToMavenLocal` fails with a message naming both
modules — either release the sibling first, or restore `-SNAPSHOT` before re-running.

## Release flow (once the workflows are in place)

Release workflows are added in later phases of the `maven-central-publishing` roadmap:

- `prepare-release.yml` (phase 4): dispatchable from the Actions UI. Bumps `<module>/version.properties` for the modules you select, updates `ycrdt-bom`'s constraint versions, commits, and pushes `<module>/<version>` tags.
- `release.yml` (phase 5): triggered by `<module>/<version>` tags. Builds native libraries for JNI/Panama modules, signs all artifacts, and publishes to Central.
- `post-release.yml`: triggered by successful `release.yml` completion. For each released module, bumps the patch version and reattaches `-SNAPSHOT` in `<module>/version.properties`, then commits `chore(release): restore -SNAPSHOT for <module> (<new>)` to the default branch with the release App identity. This keeps each module ready for the next prepare-release dispatch without manual intervention.

### Inter-module dep rewriting

When a release publication is generated, the root `build.gradle` walks the POM
and rewrites any `net.carcdr:*:*-SNAPSHOT` dep to the most recent
`<artifact>/<semver>` git tag. This lets a downstream module (e.g. `ycrdt-jni`)
be dispatched without also re-releasing an unchanged upstream (`ycrdt-core`): the
published POM will pin `ycrdt-core` to its last released version rather than the
`-SNAPSHOT` that `post-release.yml` restores after each release.

If a downstream release is dispatched but its upstream has **no** `<module>/*`
tag (the upstream has never been released), the build fails fast with an error
naming both modules. The remedy is to include the upstream in the same
`prepare-release.yml` dispatch — all tags pushed by a single dispatch are visible
to every release.yml run it triggers, since each job checks out with
`fetch-tags: true`.

Gradle Module Metadata (`.module`) is disabled for release publications (kept
for `-SNAPSHOT` publishes to GitHub Packages). Released artifacts rely on the
POM alone for consumer resolution, which avoids GMM drifting from the rewritten
POM dep versions.

### If post-release does not run

`prepare-release.yml` enforces a `require-snapshot` guard: if a module's `version.properties` is at a concrete release version (no `-SNAPSHOT` suffix), the dispatch aborts with an error. If `post-release.yml` was disabled, failed, or skipped (for example, the release workflow ended in the GH-Release recovery path), manually edit the module's `version.properties` back to a `-SNAPSHOT` version before the next dispatch. The guard will otherwise fail fast and name the offending file.

## Rollback and yank

Maven Central artifacts are **immutable** once a deployment is finalized. A published `<group>:<artifact>:<version>` coordinate cannot be overwritten, redacted, or deleted. Plan for fix-forward, not rollback.

### Recovering from a failed release

The failure mode determines the recovery:

- **`release.yml` failed before `publishAndReleaseToMavenCentral`**: the tag exists but nothing hit Central. Re-dispatch by re-pushing the tag (`git push origin :<tag> && git push origin <tag>`) or by deleting and re-creating the tag from the same commit. The concurrency group `release-${{ github.ref }}` in `release.yml` serializes same-tag runs.
- **`publishAndReleaseToMavenCentral` failed mid-upload**: vanniktech stages to the Central Portal before promoting. If staging succeeded but promotion failed, the deployment appears in the Portal UI under **Deployments -> Pending**. Log in, inspect the validation report, and either drop the staged deployment (safe) or retry the promote. Re-running the workflow also works; a stale staged deployment will be replaced.
- **`publishAndReleaseToMavenCentral` succeeded but `gh release create` failed**: the artifact is live on Central. `release.yml` uploads the release notes as an artifact and prints a manual `gh release create` command in the run summary. Follow those instructions; the tag is already pushed.
- **`post-release.yml` failed**: a module's `version.properties` is stuck at a concrete release version. Edit it back to `<next-patch>-SNAPSHOT` on `main` manually and commit, otherwise the next `prepare-release.yml` dispatch will fail the `require-snapshot` guard.

### Yanking a broken release

Central does not support yanking. To retract a bad version:

1. Publish a fix-forward patch release immediately. `prepare-release.yml` with `bump=patch` is the fastest path.
2. Add a **Security** or **Fixed** entry in `CHANGELOG.md` under the new release noting that the previous version is defective and should not be used.
3. If the bad version is actively harmful (security bug, data corruption), file a Sonatype support ticket requesting the release be quarantined. Central staff can in rare cases mark a release as broken, but cannot delete it.
4. Pin the BOM to the patched version so downstream consumers who track `ycrdt-bom` get the fix automatically.

### Verifying Central propagation

After a successful `release.yml`, the artifact takes up to ~30 minutes to propagate from the Portal to `repo1.maven.org`, and longer (sometimes hours) to appear in `search.maven.org`'s index. Verify via the raw repo URL, not the search UI:

```bash
curl -sfI https://repo1.maven.org/maven2/net/carcdr/ycrdt-core/0.1.0/ycrdt-core-0.1.0.pom
# HTTP/2 200 -> synced
# HTTP/2 404 -> still propagating, retry in a few minutes
```

## Validation run

This is the sequence used to validate the release pipeline end-to-end. Re-run it after any significant change to the release workflows or the vanniktech plugin version.

1. Confirm all required repo secrets are populated and the `net.carcdr` namespace is verified on Central Portal.
2. Run a local dry-run: `./gradlew :ycrdt-core:publishToMavenLocal` and inspect the generated POM at `~/.m2/repository/net/carcdr/ycrdt-core/<version>/`.
3. Dispatch a single-module release via the Actions UI (`prepare-release.yml`, `bump=patch`, pick one module such as `ycrdt-core`). Verify the bump commit, two pushed tags (module + BOM), two `release.yml` runs, two `post-release.yml` runs, two `-SNAPSHOT` restore commits, and the module + BOM artifacts on `repo1.maven.org`.
4. Dispatch a multi-module release (two modules at once). Verify the same fan-out for the N+1 tags (N modules + BOM), and that the BOM POM on Central pins the newly released module versions.
5. Resolve the published BOM from an external Gradle project (`platform('net.carcdr:ycrdt-bom:<v>')`) and run a minimal smoke test that instantiates types from the downloaded artifacts.
6. File any rough edges encountered as rdm tasks or issues.

## Manual fallback

If the GitHub Actions workflows are unavailable (e.g. GitHub outage, Actions quota exhausted), a release can be cut manually from a clean checkout:

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
