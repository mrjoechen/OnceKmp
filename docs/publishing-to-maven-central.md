# Publishing To Maven Central

This repository includes a GitHub Actions workflow at `.github/workflows/publish-maven-central.yml`.

## Recommended Setup

Create a GitHub Environment named `maven-central`, then add these secrets:

- `MAVEN_CENTRAL_USERNAME`
- `MAVEN_CENTRAL_PASSWORD`
- `SIGNING_IN_MEMORY_KEY`
- `SIGNING_IN_MEMORY_KEY_PASSWORD`
- `SIGNING_IN_MEMORY_KEY_ID` (optional)

For the repository owner, the default POM metadata already points to:

- GitHub URL: `https://github.com/mrjoechen/OnceKmp`
- Group: `io.github.mrjoechen`
- Artifact: `oncekmp`

If those values change later, update:

- `scripts/publish-oncekmp.sh`
- `onceKmp/build.gradle.kts`

## Release Triggers

The workflow supports two release modes:

1. Push a tag like `v0.1.0`
2. Run `Publish Maven Central` manually from GitHub Actions and provide `0.1.0` or `v0.1.0`

The workflow will:

1. Build and verify the library on `macos-latest`
2. Run `:onceKmp:allTests`
3. Compile the sample module
4. Publish and release `onceKmp` to Maven Central
5. Create a GitHub Release for the tag on successful publish

## Recommended Versioning

Use simple SemVer tags:

1. First public release: `0.1.0`
2. Backward-compatible fixes: `0.1.1`, `0.1.2`
3. Backward-compatible features: `0.2.0`
4. Breaking API changes before `1.0.0`: still bump the minor version, for example `0.3.0`
5. Stable public API milestone: `1.0.0`

Use Git tags in the form `vX.Y.Z`, for example:

- `v0.1.0`
- `v0.1.1`
- `v0.2.0`

## Notes

- The workflow publishes the version from the tag or manual input. It strips a leading `v`.
- Central release runs with `--no-configuration-cache` because this path is more reliable for `publishAndReleaseToMavenCentral`.
- `onceKmp` includes iOS targets, so the workflow uses macOS instead of Linux.

## First Release Checklist

1. Verify the Sonatype Central account and token are active
2. Verify the GPG private key is ASCII-armored and matches the published public key
3. Confirm the artifact coordinates are `io.github.mrjoechen:oncekmp:0.1.0`
4. Push `v0.1.0`
5. Watch the `Publish Maven Central` workflow until it succeeds

## First Release Commands

```bash
git tag v0.1.0
git push origin v0.1.0
```
