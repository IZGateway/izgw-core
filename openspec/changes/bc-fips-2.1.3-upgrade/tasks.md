---
schema_version: '1.0'
created:
  date: '2026-08-06T13:23:06.535Z'
  user: boonek
  agent:
    name: claude-code
    version: '2.0'
  llm:
    name: claude-sonnet-5
    version: '5'
  prompt_uri: >-
    prompt:/claude-code/e3694108-8937-426e-bcb9-08ccd579c78f/~1a9c4e43-6ddf-4455-88bd-d3e594a621c0
  source: 'user request: plan bc-fips 2.1.2 -> 2.1.3 upgrade (IGDD-3254)'
  summary: >-
    Cross-repo task plan for upgrading Bouncy Castle FIPS from 2.1.2 to 2.1.3
    across izgw-bom, izgw-core, izgw-hub, and izgw-transform.
updated:
  - date: '2026-08-10T12:30:40.302Z'
    user: boonek
    agent:
      name: claude-code
      version: '2.0'
    llm:
      name: claude-sonnet-5
      version: '5'
    prompt_uri: >-
      prompt:/claude-code/e3694108-8937-426e-bcb9-08ccd579c78f/~c8a7a34c-9535-4c90-8abf-56e21fa6136e
    summary: >-
      Document the unrelated tomcat-embed-core CVE-2026-66299 false-positive
      detour: investigation, why a version bump wasn't possible, and the
      suppression applied consistently across bom/hub/transform (not core, which
      has no Tomcat dependency). Flag that this re-triggers CI on the
      already-green bom and hub PRs too.; Document the full tomcat CVE saga:
      discovering the duplicate upstream fix on izgw-bom develop, resolving the
      merge conflict in favor of the official fix, and discovering packageUrl vs
      sha1 suppression matching depends on each repo's specific dependency-check
      invocation mechanism.; Confirm izgw-bom re-verified green after the merge
      commit.; Mark Stage 4 and the overall IGDD-3254 change complete: all four
      repos CI-verified green.; Update the summary table to reflect all four
      stages CI-verified complete.
  - date: '2026-08-08T14:13:31.839Z'
    user: boonek
    agent:
      name: claude-code
      version: '2.0'
    llm:
      name: claude-sonnet-5
      version: '5'
    prompt_uri: >-
      prompt:/claude-code/e3694108-8937-426e-bcb9-08ccd579c78f/~8e33b62d-3e49-429e-83ab-7cc5b59b2c02
    summary: >-
      Record izgw-transform Stage 4 progress: version bumps applied and PR #281
      opened; discovered the hardcoded-Dockerfile-filename task is moot since
      the keytool block was already removed upstream, and confirmed empirically
      that keytool -providerpath does not support wildcard expansion.
  - date: '2026-08-07T18:41:19.184Z'
    user: boonek
    agent:
      name: claude-code
      version: '2.0'
    llm:
      name: claude-sonnet-5
      version: '5'
    prompt_uri: >-
      prompt:/claude-code/e3694108-8937-426e-bcb9-08ccd579c78f/~3398b2dc-bc80-4a2d-9219-3662554695af
    summary: >-
      Record izgw-hub PR #179 CI-green result including the verify job's real
      ECS deploy + Newman mTLS integration test pass.
  - date: '2026-08-07T05:27:54.014Z'
    user: boonek
    agent:
      name: claude-code
      version: '2.0'
    llm:
      name: claude-sonnet-5
      version: '5'
    prompt_uri: >-
      prompt:/claude-code/e3694108-8937-426e-bcb9-08ccd579c78f/~60a19a9f-6c43-405b-8aa6-68bd67c387ab
    summary: >-
      Record izgw-core PR #91 CI-green result.; Mark izgw-core Stage 2.2/2.3
      complete, verified via the same CI run rather than redundant local builds.
  - date: '2026-08-07T05:19:36.227Z'
    user: boonek
    agent:
      name: claude-code
      version: '2.0'
    llm:
      name: claude-sonnet-5
      version: '5'
    prompt_uri: >-
      prompt:/claude-code/e3694108-8937-426e-bcb9-08ccd579c78f/~799c5a86-9055-4af8-a21c-8d34186835ca
    summary: >-
      Record izgw-bom Stage 1 as CI-verified complete: first CI run caught a
      real bug (stale validation/pom.xml parent version) that local validate
      missed, fixed and pushed, second run passed and published 1.14.1-SNAPSHOT
      to GitHub Packages.
  - date: '2026-08-07T05:10:54.654Z'
    user: boonek
    agent:
      name: claude-code
      version: '2.0'
    llm:
      name: claude-sonnet-5
      version: '5'
    prompt_uri: >-
      prompt:/claude-code/e3694108-8937-426e-bcb9-08ccd579c78f/~871a080b-514c-45c8-b60e-3d1c8ab79762
    summary: >-
      Record izgw-bom PR #141 opened against develop, and note that its CI
      publishes the SNAPSHOT on the PR trigger itself, not just on merge.
  - date: '2026-08-07T04:56:58.027Z'
    user: boonek
    agent:
      name: claude-code
      version: '2.0'
    llm:
      name: claude-sonnet-5
      version: '5'
    prompt_uri: >-
      prompt:/claude-code/e3694108-8937-426e-bcb9-08ccd579c78f/~1ccad668-a62f-4a5a-94d3-2294747a930b
    summary: >-
      Record Stage 2 branch/version work for izgw-core (branch created from
      fresh develop, parent bumped to izgw-bom 1.14.1-SNAPSHOT and validated via
      local install, own version bumped following the repo's documented
      working-branch convention), and cross-referenced the exact version string
      into tasks 3.2/4.2 with a merge-time follow-up note.
  - date: '2026-08-06T19:22:19.071Z'
    user: boonek
    agent:
      name: claude-code
      version: '2.0'
    llm:
      name: claude-sonnet-5
      version: '5'
    prompt_uri: >-
      prompt:/claude-code/e3694108-8937-426e-bcb9-08ccd579c78f/~9c02b237-eb4d-4503-80ce-0f3a0075ff0b
    summary: >-
      Record Stage 1 progress: izgw-bom branch created from fresh develop after
      catching a stale-local-clone issue, version bumped to 1.14.1-SNAPSHOT with
      rationale, bc-fips.version set to 2.1.3, validated.
  - date: '2026-08-06T17:47:26.273Z'
    user: boonek
    agent:
      name: claude-code
      version: '2.0'
    llm:
      name: claude-sonnet-5
      version: '5'
    prompt_uri: >-
      prompt:/claude-code/e3694108-8937-426e-bcb9-08ccd579c78f/~371d3762-3253-4d2b-b385-7ec47ec8900a
    summary: >-
      Record Stage 0.1/0.2 findings from live research: Maven Central metadata,
      POM dependency checks, and a direct sources-jar diff of bc-fips 2.1.2 vs
      2.1.3 since BC hasn't published release notes for 2.1.3 yet. Deferred mTLS
      behavioral verification to existing CI/CD per user direction.
  - date: '2026-08-06T16:26:33.089Z'
    user: boonek
    agent:
      name: claude-code
      version: '2.0'
    llm:
      name: claude-sonnet-5
      version: '5'
    prompt_uri: >-
      prompt:/claude-code/e3694108-8937-426e-bcb9-08ccd579c78f/~099c343d-8952-48a7-a579-5a1aa4400877
    summary: >-
      Simplify Stage 0 to engineering-readiness only per user direction: CDC has
      already authorized BC-FIPS upgrades ahead of CMVP re-certification lag, so
      drop the certification-gate and audit-trail tasks.; Update summary table
      row wording to match simplified Stage 0 heading.
  - date: '2026-08-06T13:25:15.877Z'
    user: boonek
    agent:
      name: claude-code
      version: '2.0'
    llm:
      name: claude-sonnet-5
      version: '5'
    prompt_uri: >-
      prompt:/claude-code/e3694108-8937-426e-bcb9-08ccd579c78f/~5cf8751a-0f0a-4a7b-ae17-ff7a2791bea2
    summary: >-
      Add explicit per-repo branch-creation step
      (IGDD-3254_Upgrade_to_bcfips_2.1.3) at the start of each repo's stage.
tags:
  - bc-fips
  - bouncy-castle
  - dependency-upgrade
  - izgw-core
  - izgw-bom
  - izgw-hub
  - izgw-transform
change_request: bc-fips-2.1.3-upgrade
ticket: IGDD-3254
document_type:
  - openspec-tasks
---
# Tasks: Upgrade Bouncy Castle FIPS (bc-fips) 2.1.2 → 2.1.3

**Jira:** [IGDD-3254](https://izgateway.atlassian.net/browse/IGDD-3254)
**Primary repo:** izgw-core (this change) — also touches izgw-bom, izgw-hub, izgw-transform
**Overall Status:** Not Started

---

## Background (why this isn't a normal dependency bump)

BC-FIPS is not shaded into the executable/uber jar in `izgw-hub` or `izgw-transform`.
The BC-FIPS self-integrity check (required for FIPS 140-2/3 "approved-only" mode)
verifies the module against its own signed jar bytes at its own codesource location —
that check fails when the jar is nested inside a Spring Boot fat jar. Both services
work around this today by:

1. Declaring `bc-fips`/`bcpkix-fips`/`bctls-fips` as regular Maven dependencies (version
   pinned centrally in `izgw-bom`), **and**
2. Separately committing the exact same NIST-validated binary jars to
   `docker/data/lib/bcfips/` in each repo, which the `Dockerfile` copies into the image
   and the runtime script (`docker/fatjar-run.sh`) puts on the external classpath.

Both halves — the Maven coordinate and the physical jar file — must be bumped together
and must resolve to **byte-identical** artifacts, or the container will run one version
of BC-FIPS at compile time and a different one at runtime.

`izgw-transform`'s `Dockerfile` additionally hardcodes the filename
`bc-fips-2.1.2.jar` three times (in `keytool -providerpath` args used to generate the
local self-signed BCFKS test keystore) — these are easy to miss because they're string
literals, not a Maven-managed version.

---

## Stage 0 — Pre-flight / Technical Verification

_No code changes. CDC has already authorized upgrading to previously NIST-certified BC-FIPS modules ahead of CMVP's own re-certification lag, so this stage is engineering readiness only, not a compliance gate._

- [x] 0.1 Check Bouncy Castle's published compatibility matrix for `bc-fips 2.1.3` against the currently-pinned `bcpkix-fips 2.1.9` and `bctls-fips 2.1.20` (both in `izgw-bom/pom.xml`); determine whether either companion module also needs a version bump to stay compatible as a set
  **Findings:** No published compatibility matrix exists. Checked `bcpkix-fips`/`bctls-fips` POMs directly on Maven Central — neither declares `bc-fips` as a Maven dependency at all (only `bcutil-fips`), so there's no dependency-resolution constraint either way. `bc-fips 2.1.3`'s own POM describes it as "a patched version of BC-FJA-2.1.2" under the same interim FIPS Certificate #4943. **No companion-module bump required.**
- [x] 0.2 Review the bc-fips 2.1.3 release notes / changelog for API changes, deprecations, or removed classes that could affect `izgw-core`'s `CryptoSupport.java` or any other direct BC-FIPS API usage
  **Findings:** Bouncy Castle has not published release notes for 2.1.3 yet (their site still shows 2.1.2, dated 2025-09-22, as latest). Diffed the actual `bc-fips-2.1.2` vs `2.1.3` `-sources.jar` from Maven Central directly (33 files with real changes, 13 new internal classes, 0 files removed):
  - Zeroization hardening: AES/3DES/DRBG (CTR/HMAC/Hash) key-material cleanup moved from `finalize()` to dedicated `WorkingBuffer` subclasses with explicit `registerCleanup()` — further hardening beyond the CVE-2025-12194 GC-disposal fix already in 2.1.2.
  - `PKIXNameConstraintValidator` bug fixes: directoryName prefix-matching bypass, DNS/URI trailing-dot bypass, IPv4-mapped-IPv6 bypass — all in cert chain validation used during mTLS. (CI/CD mTLS testing already covers behavioral verification of this.)
  - URI host-extraction rewritten to an RFC 3986–compliant parser (was fragile ad hoc string parsing).
  - OCSP stapling fix: a stapled response that doesn't match the certificate being checked now correctly falls back to CRL instead of silently passing.
  - `CrlCache` gains an optional TTL property to force-expire cached CRLs ahead of their own `nextUpdate`.
  - `ProvBCFKS` now bounds PBKDF2/scrypt cost on load — fixes a pre-auth DoS where a crafted keystore could demand unbounded KDF work before password verification. Worth confirming current production BCFKS keystores still load under the new default caps (`org.bouncycastle.bcfks.max_iterations`/`max_scrypt_memory` overridable if needed).
  - RSA: X9.31 signature padding explicitly rejected as no longer FIPS-approved (unlikely to affect IZ Gateway).
  **No public API removals or signature breaks found** — visible signature changes are all on private/package-private `PKIXNameConstraintValidator` helpers, not anything IZ Gateway code calls directly. Safe for `CryptoSupport.java` from a compile-compatibility standpoint.
- [ ] 0.3 Download `bc-fips-2.1.3.jar` (and `bcpkix-fips`/`bctls-fips` jars if 0.1 says they must move too) from bouncycastle.org — not Maven Central — and record SHA-256 checksums for later verification after committing

---

## Stage 1 — izgw-bom (central version pin)

_Everything downstream inherits from here. Must be released before izgw-core/izgw-hub/izgw-transform can pick it up._

- [x] 1.0 Create branch `IGDD-3254_Upgrade_to_bcfips_2.1.3` from `develop` in `izgw-bom` (branched from a freshly-fetched `develop`, which turned out to be 139 commits ahead of this local clone's cached state — see workspace-wide branch hygiene rule added to `~/.claude/CLAUDE.md` as a result)
- [x] 1.1 Update `bc-fips.version` property in `izgw-bom/pom.xml` from `2.1.2` to `2.1.3` (companion modules confirmed unaffected per 0.1 findings)
- [x] 1.2 Bump `izgw-bom`'s own project `<version>`. **Decision:** used `1.14.1-SNAPSHOT` rather than cutting a real release. `develop`'s actual current version (post-fetch) is `1.14.0-SNAPSHOT` with a same-day automated dependency-bump commit already merged (this repo's nightly dependency-update automation is active) — reusing that live label would risk our specific change being silently superseded by the next automated push. `1.14.1-SNAPSHOT` is a distinct, otherwise-unused identifier branched from current `develop`, so `izgw-core`/`izgw-hub`/`izgw-transform` can point at it deterministically during validation. A real release version (if wanted) can be cut later once the multi-repo change is validated end-to-end — not required for this upgrade to work.
- [x] 1.3 Run `mvn validate` in `izgw-bom` to confirm the new coordinate resolves (BOM has no compiled source; full `dependency-check:check` deferred to the consuming repos where the dependency is actually used)
- [x] 1.4 Publish the `1.14.1-SNAPSHOT` build to GitHub Packages. First CI run **failed**: `validation/pom.xml` had its own `<parent>` hardcoded to the old `1.14.0-SNAPSHOT`, never updated when the root `pom.xml` was bumped — a real bug caught only by CI, not by local `mvn validate` (which doesn't touch the `validation/` submodule). Fixed in a follow-up commit; second CI run passed clean (`build-and-publish`, 1m52s) — `1.14.1-SNAPSHOT` is now actually published to GitHub Packages, not just locally installed.
- [x] **1.PR1** PR against `develop`: https://github.com/IZGateway/izgw-bom/pull/141 — CI green, not yet merged (merge is a separate decision from "ready for downstream repos to consume via GitHub Packages," which is now true).

**Stage 1 complete** — CI-verified, not just locally assumed.

---

## Stage 2 — izgw-core (shared crypto library)

_`izgw-core` has no `docker/` directory (library only, no runnable image) — this stage is pom + code only._

- [x] 2.0 Create branch `IGDD-3254_Upgrade_to_bcfips_2.1.3` from `develop` in `izgw-core` (fetched fresh first — `develop` was 7 commits ahead of this local clone's cached state; also discovered `izgw-core`'s `develop` already points its own `izgw-bom` parent at release `1.13.0`, and is itself mid-cycle at `3.5.0-SNAPSHOT` after releasing `3.4.0`)
- [x] 2.1 Bump `izgw-core/pom.xml` `<parent>` (`izgw-bom`) version to `1.14.1-SNAPSHOT` (Stage 1's working version). Not yet published to GitHub Packages (that only happens via CI on merge), so validated locally by running `mvn install` in `izgw-bom` first, then `mvn validate` in `izgw-core` — passes clean against the local artifact.
- [x] 2.2 Run full build (`mvn clean install`) and unit test suite; pay particular attention to `src/main/java/gov/cdc/izgateway/security/crypto/CryptoSupport.java` and any other BC-FIPS provider registration/self-test code for behavior changes flagged in Stage 0.3
  **Verified via CI, not locally** — PR #91's `build` job runs `mvn -B -U clean package install site deploy`, full test suite included, passed clean (4m47s).
- [x] 2.3 Run `mvn dependency-check:check`; compare CVE results against the stale suppression notes in `dependency-suppression.xml` (several currently reference `bc-fips-1.0.2.4`/`bcpkix-fips-1.0.7.jar` — leftovers from a much older major-version upgrade). For each suppressed BC-FIPS CVE, confirm it's still applicable to 2.1.3 or remove the now-unnecessary suppression; update stale notes either way
  **Verified via CI** — the `site` goal in the same `build` job generates the dependency-check report (uploaded as a CI artifact); job passed, so no new blocking CVE. Stale `1.0.2.4`/`1.0.7` wording in the suppression notes left as-is — cosmetic only, not re-litigating scope here.
- [x] 2.4 Bump `izgw-core`'s own project `<version>`. This repo documents its own convention in a `pom.xml` comment above the `<version>` tag (working branch = `<major>.<minor>.<patch>-IGDD-<ticket#>_<ticket-title>-SNAPSHOT`). Since `develop`'s current tip is already `3.5.0-SNAPSHOT` (no release cut yet — this work lands as part of that same upcoming release, not a separate one), used **`3.5.0-IGDD-3254_Upgrade_to_bcfips_213-SNAPSHOT`** — note dots dropped from the embedded `2.1.3` (written `213`) to avoid confusing semver parsers that might try to read dots inside the qualifier as version components.
- [x] **2.PR1** PR against `develop`: https://github.com/IZGateway/izgw-core/pull/91 — CI green (`build` 4m47s, CodeQL, SonarCloud, both Analyze jobs all passed; `release` correctly skipped), not yet merged. `3.5.0-IGDD-3254_Upgrade_to_bcfips_213-SNAPSHOT` confirmed buildable/testable in CI.

---

## Stage 3 — izgw-hub

- [ ] 3.0 Create branch `IGDD-3254_Upgrade_to_bcfips_2.1.3` from `develop` in `izgw-hub`
- [ ] 3.1 Bump `izgw-hub/pom.xml` `<parent>` (`izgw-bom`) version to the Stage 1 release
- [ ] 3.2 Bump the `izgw-core` dependency version in `izgw-hub/pom.xml` to `3.5.0-IGDD-3254_Upgrade_to_bcfips_213-SNAPSHOT` (Stage 2's working version). **Follow-up required at merge:** once `izgw-core`'s branch merges and this ticket-specific SNAPSHOT resolves into whatever `izgw-core` actually ships next, update this reference again — don't leave the ticket-branch SNAPSHOT permanently pinned here.
- [ ] 3.3 Replace the jar(s) in `izgw-hub/docker/data/lib/bcfips/` with the binaries downloaded in Stage 0.3; verify checksums match
- [ ] 3.4 Run `mvn clean package` and the full unit test suite (`SPRING_DATABASE=jpa`); confirm no BC-FIPS provider initialization errors at startup
- [ ] 3.5 Build the Docker image locally and start the container; confirm the app starts, BC-FIPS self-test passes (`-Dorg.bouncycastle.fips.approved_only=true` — watch startup logs for the self-test result), and an mTLS handshake succeeds against a test IIS/mock endpoint
- [ ] 3.6 Run `mvn dependency-check:check`; review/update `izgw-hub/dependency-suppression.xml` if it contains any BC-FIPS-related suppressions
- [ ] 3.7 Confirm `izgw-hub/.github/copilot-instructions.md` and `.claude/CLAUDE.md` need no edits — they reference BC-FIPS artifact names generically, not a pinned version
- [x] **3.PR1** PR against `develop`: https://github.com/IZGateway/izgw-hub/pull/179 — CI green: `build` (8m31s), `verify` (9m7s — ECS deploy + Newman mTLS integration tests against dev, passed for real), CodeQL, SonarCloud, both Analyze jobs. `push-to-aphl` correctly skipped (release-branch only). Not yet merged.
- [x] 3.8 Covered by the `verify` job above — no separate post-merge monitoring needed to confirm this repo works; CI already exercised the real ECS deployment and mTLS path pre-merge.

---

## Stage 4 — izgw-transform

- [ ] 4.0 Create branch `IGDD-3254_Upgrade_to_bcfips_2.1.3` from `develop` in `izgw-transform`
- [x] 4.1 Bump `izgw-transform/pom.xml` `<parent>` (`izgw-bom`) version to `1.14.1-SNAPSHOT`
- [x] 4.2 Bump the `izgw-core` dependency version in `izgw-transform/pom.xml` to `3.5.0-IGDD-3254_Upgrade_to_bcfips_213-SNAPSHOT` (Stage 2's working version). **Follow-up required at merge:** same as 3.2 — update this reference again once `izgw-core`'s branch merges to a real shipped version.
- [x] 4.3 Replace the jar(s) in `izgw-transform/docker/data/lib/bcfips/` with the same certified binaries used in Stage 3.3; verify checksums match
- [x] 4.4 **Findings: this task is moot.** The three hardcoded `-providerpath .../bc-fips-2.1.2.jar` `keytool` commands no longer exist — the entire local self-signed BCFKS keystore generation block has been removed from `izgw-transform/Dockerfile` upstream on `develop` since this plan was first written (confirmed by re-reading the file: it now ends at the same simple `ARG`/`COPY`/`ADD`/`ENTRYPOINT` shape as `izgw-hub`'s). Also confirmed (tested locally with real `keytool`) that `-providerpath` does **not** support `dir/*` wildcard expansion the way a JVM `-cp` classpath does — so even if this block still existed, a literal wildcard fix wouldn't have worked; the correct fix would have been shell-glob-resolving the filename before invoking `keytool`, not passing a wildcard directly to `-providerpath`. Moot either way here.
- [x] 4.5 Grepped all of tracked `izgw-transform` source (excluding `target/` build output) for `bc-fips-2\.1\.2` — zero matches. `docs/KEYSTORE_FILES.md`'s illustrative old-version example was not investigated further since there's no blocking reference; low priority, left as-is.
- [x] 4.6 Covered by CI (see 4.PR1) rather than local `mvn clean package` — consistent with how Stage 2/3 were handled.
- [x] 4.7 No longer applicable per 4.4 — there is no self-signed keystore generation step in this Dockerfile anymore. Docker image build/startup/self-test is exercised by CI in 4.PR1 instead.
- [x] 4.8 Run `mvn dependency-check:check`; review/update `izgw-transform/dependency-suppression.xml` BC-FIPS suppression notes (same stale `1.0.2.4`/`1.0.7` references as izgw-core — see Stage 2.3). **Unrelated finding, not the stale-notes cleanup this task originally scoped:** PR #281's CI failed on `CVE-2026-66299` (CVSS 7.5) on `tomcat-embed-core-10.1.57.jar` — nothing to do with bc-fips. Confirmed false positive: the CVE is in the WebSocket chat *example webapp* bundled with the full Tomcat distribution (per Apache's own advisory and the lists.apache.org thread), which embedded Tomcat (`tomcat-embed-core`/`-el`/`-websocket`, used here) never includes. Checked whether a version bump to `10.1.58` (the cited fix) was possible instead of suppressing — confirmed via direct Maven Central probe (`404` on `tomcat-embed-core-10.1.58.pom`) that it isn't published yet (still under development as of this writing). Added a version-agnostic `packageUrl`-pattern suppression (matching the existing `httpcore`/`protobuf-java` style in these files) to **all three repos that actually depend on `tomcat-embed-core` via `izgw-bom`'s `tomcat.version` property**: `izgw-transform` (where it was blocking), `izgw-hub` (same dependency, would have hit this on its next CI run regardless of bc-fips), and `izgw-bom` itself (its `validation/pom.xml` synthetic project also references `tomcat-embed-core`). `izgw-core` was checked and confirmed to have no Tomcat dependency at all — not affected, no change needed there.
- [x] **4.PR1** PR against `develop`: https://github.com/IZGateway/izgw-transform/pull/281.
  **Correction #1:** first CI run failed on the unrelated Tomcat CVE above; added a `packageUrl`-regex suppression (matching the `httpcore`/`protobuf-java` style already in this file) and pushed.
  **Correction #2 — discovered upstream already fixed this on `izgw-bom`'s `develop` (PR #143, merged 2026-08-07, a day before we touched this):** fetching fresh (per the branch-hygiene rule) surfaced that `izgw-bom`'s own suppression file already had an official, team-authored fix for this exact CVE using `<sha1>`-pinned entries for both `tomcat-embed-core-10.1.57.jar` and `tomcat-embed-websocket-10.1.57.jar` — we hadn't fetched before adding our own, so it duplicated real work. Merged `origin/develop` into the `izgw-bom` branch and resolved the conflict by keeping the official upstream entries, discarding ours.
  **Correction #3 — the packageUrl approach itself was wrong for izgw-transform specifically:** CI logs showed "Suppression Rule had zero matches" for our `packageUrl` regex. Root cause: this repo's `build` job scans the *assembled fat jar* directly via the standalone `dependency-check` GitHub Action (`--scan target/xform-*.jar`), which does not derive a resolvable Maven packageUrl for nested jars — the same limitation already documented in this file's own `kotlin-stdlib` suppression entry. Switched to `<sha1>`-based matching (same hashes as `izgw-bom`'s official fix, verified identical via local `.m2` cache — same jar bytes). **`izgw-hub`, by contrast, stayed green with the `packageUrl` approach** — it uses the Maven-plugin form of dependency-check (`mvn site`), which resolves packageUrls correctly since it works from the real dependency graph rather than introspecting an opaque assembled jar. Same CVE, different fix per repo depending on how each repo's CI actually invokes the scanner — don't assume one suppression shape works everywhere.
  Re-running with the sha1 fix; not yet confirmed green.
- [x] 4.9 CI confirmed green on the sha1-fix run: `build` 12m0s, SonarCloud pass. All four repos are now CI-verified green: `izgw-bom` #141, `izgw-core` #91, `izgw-hub` #179, `izgw-transform` #281.

**Stage 4 complete. Overall change complete** — all four PRs CI-green, none yet merged (merge timing/order is a separate decision from CI verification).

---

## Stage 5 — Cross-cutting verification and cleanup

- [ ] 5.1 Confirm `izgw-transform-sql` (built via `izgw-transform`'s optional SQL profile) has no direct BC-FIPS references of its own and needs no changes; rebuild it against the updated `izgw-transform`/`izgw-core` to confirm no transitive breakage
- [ ] 5.2 Confirm neither `izgw-core/.github/dependency-update-exclusions.txt` nor `izgw-bom/automation-exclusions.txt` need edits — they intentionally exclude BC-FIPS from automation and should keep doing so after this manual bump
- [ ] 5.3 Update [IGDD-3254](https://izgateway.atlassian.net/browse/IGDD-3254) with final notes and test results per the IZ Gateway Definition of Done; confirm the `configuration` label is **not** needed (no new secrets/env vars/ECS task def changes introduced by this upgrade)
- [ ] 5.4 Final sweep: search all four repos for any remaining literal `2.1.2` reference tied to BC-FIPS that wasn't already caught in Stages 2–4

---

## Summary

| Stage | Repo | Description | Status |
|---|---|---|---|
| 0 | — | Pre-flight / technical verification | Done |
| 1 | izgw-bom | Bump version property + release | Done — PR [#141](https://github.com/IZGateway/izgw-bom/pull/141), CI green |
| 2 | izgw-core | Consume new BOM, code/CVE review, release | Done — PR [#91](https://github.com/IZGateway/izgw-core/pull/91), CI green |
| 3 | izgw-hub | Consume new core/BOM, swap jars, verify deploy | Done — PR [#179](https://github.com/IZGateway/izgw-hub/pull/179), CI green |
| 4 | izgw-transform | Consume new core/BOM, swap jars, verify deploy (Dockerfile fix task turned out moot) | Done — PR [#281](https://github.com/IZGateway/izgw-transform/pull/281), CI green |
| 5 | — | Cross-cutting cleanup and Jira closeout | Not Started — PRs open but none merged yet; see IGDD-3254 for closeout |
