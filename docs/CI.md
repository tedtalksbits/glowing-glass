# CI

GitHub Actions, `.github/workflows/ci.yml`. Runs on every push to `main`, on pull requests, and by hand (Actions tab >
CI > Run workflow). Pushes that only touch Markdown, `docs/` or `run/` are skipped. Nothing is deployed anywhere.

| Job | Does | Fails when |
|---|---|---|
| Plugin build and unit tests | `./gradlew build` at the root: plugin, self-test plugin, MockBukkit unit tests | Compile error or a unit test fails |
| Self-test on a real Paper server | Downloads the pinned Paper build, checks its SHA-256, runs `ci/run-selftest.sh` | The report doesn't end `SELFTEST PASSED`, or the server hangs (10 minutes) |
| Release (tags `v*` only) | All of the above, then publishes the plugin jar (see Releases) | Any CI job fails, or the tag doesn't match the plugin version |

The client mod job (build, unit tests, mixin audit) arrives with the mod in slice 2.

Each run keeps the jars as artifacts (`glowing-plugin`, `paper-selftest`) plus `selftest-report` (report and server
log). The self-test report is also printed on the run's summary page.

## Pins

The workflow's `env` block: `JAVA_VERSION`, `PAPER_VERSION`, `PAPER_BUILD`, `PAPER_SHA256` (the same Paper 26.3-211
as Shulker Styles). To move to a new Paper build, take the hash of the jar you tested locally
(`certutil -hashfile paper-<v>-<b>.jar SHA256` on Windows, `sha256sum` elsewhere) and change all three together.

## Running the self-test by hand

On Linux or in Git Bash, after `./gradlew build`:

```
ci/run-selftest.sh path/to/paper-26.3-211.jar /tmp/selftest-run
```

The second argument must be a folder that doesn't exist yet. Without internet access, add
`PAPER_SEED_DIR=path/to/an/existing/paper/server` so its `cache/`, `versions/` and `libraries/` are reused.
`SELFTEST_ONLY=piston,TNT` runs only the steps whose names start that way. About 25 seconds.

## Releases

`.github/workflows/release.yml`: the tag is the **plugin** version. Once the mod exists it keeps its own version and
both jars go in every release (slice 2 adds that).

1. Bump `version` in `build.gradle.kts`.
2. Commit and push to `main`; wait for CI to go green.
3. Tag and push: `git tag v0.1.0` then `git push origin v0.1.0`.

The release workflow runs all of CI again, checks the tag matches the plugin version, then publishes
"Glowing Glass v0.1.0" with `glowing-plugin-<version>.jar`. If anything fails, nothing is published; fix it, delete
the tag (`git push origin :v0.1.0`, `git tag -d v0.1.0`) and tag again. The self-test plugin is never released.
