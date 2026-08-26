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
- [ ] 2.2 Run full build (`mvn clean install`) and unit test suite; pay particular attention to `src/main/java/gov/cdc/izgateway/security/crypto/CryptoSupport.java` and any other BC-FIPS provider registration/self-test code for behavior changes flagged in Stage 0.3
- [ ] 2.3 Run `mvn dependency-check:check`; compare CVE results against the stale suppression notes in `dependency-suppression.xml` (several currently reference `bc-fips-1.0.2.4`/`bcpkix-fips-1.0.7.jar` — leftovers from a much older major-version upgrade). For each suppressed BC-FIPS CVE, confirm it's still applicable to 2.1.3 or remove the now-unnecessary suppression; update stale notes either way
- [x] 2.4 Bump `izgw-core`'s own project `<version>`. This repo documents its own convention in a `pom.xml` comment above the `<version>` tag (working branch = `<major>.<minor>.<patch>-IGDD-<ticket#>_<ticket-title>-SNAPSHOT`). Since `develop`'s current tip is already `3.5.0-SNAPSHOT` (no release cut yet — this work lands as part of that same upcoming release, not a separate one), used **`3.5.0-IGDD-3254_Upgrade_to_bcfips_213-SNAPSHOT`** — note dots dropped from the embedded `2.1.3` (written `213`) to avoid confusing semver parsers that might try to read dots inside the qualifier as version components.
- [ ] **2.PR1** Open and merge PR for the `izgw-core` version bump before starting Stage 3

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
- [ ] **3.PR1** Open PR in `izgw-hub`; do not merge until CI (build, unit tests, OWASP check, Docker build, Newman integration tests against dev) passes
- [ ] 3.8 After merge, monitor the `develop`/dev ECS deployment through the `verify` CI job (Newman integration tests, `:good` tag) before considering this repo done

---

## Stage 4 — izgw-transform

- [ ] 4.0 Create branch `IGDD-3254_Upgrade_to_bcfips_2.1.3` from `develop` in `izgw-transform`
- [ ] 4.1 Bump `izgw-transform/pom.xml` `<parent>` (`izgw-bom`) version to the Stage 1 release
- [ ] 4.2 Bump the `izgw-core` dependency version in `izgw-transform/pom.xml` to `3.5.0-IGDD-3254_Upgrade_to_bcfips_213-SNAPSHOT` (Stage 2's working version). **Follow-up required at merge:** same as 3.2 — update this reference again once `izgw-core`'s branch merges to a real shipped version.
- [ ] 4.3 Replace the jar(s) in `izgw-transform/docker/data/lib/bcfips/` with the same certified binaries used in Stage 3.3; verify checksums match
- [ ] 4.4 Update the three hardcoded `-providerpath /usr/share/izg-transform/lib/bcfips/bc-fips-2.1.2.jar` references in `izgw-transform/Dockerfile` (lines ~79, ~89, ~99) to `bc-fips-2.1.3.jar`
- [ ] 4.5 Grep the rest of `izgw-transform` for any other hardcoded `bc-fips-2.1.2` string literals outside the Dockerfile (e.g., `docs/KEYSTORE_FILES.md` references an even older `bc-fips-2.0.0.jar` example filename — confirm whether that doc should be refreshed as part of this change or tracked separately, since it's illustrative rather than executed)
- [ ] 4.6 Run `mvn clean package` and the full unit test suite
- [ ] 4.7 Build the Docker image locally and start the container; confirm the self-signed BCFKS keystore generation step in the Dockerfile succeeds with the new jar filename, the app starts, and BC-FIPS self-test passes
- [ ] 4.8 Run `mvn dependency-check:check`; review/update `izgw-transform/dependency-suppression.xml` BC-FIPS suppression notes (same stale `1.0.2.4`/`1.0.7` references as izgw-core — see Stage 2.3)
- [ ] **4.PR1** Open PR in `izgw-transform`; do not merge until CI passes
- [ ] 4.9 After merge, monitor the dev ECS deployment before considering this repo done

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
| 0 | — | Pre-flight / technical verification | Not Started |
| 1 | izgw-bom | Bump version property + release | Not Started |
| 2 | izgw-core | Consume new BOM, code/CVE review, release | Not Started |
| 3 | izgw-hub | Consume new core/BOM, swap jars, verify deploy | Not Started |
| 4 | izgw-transform | Consume new core/BOM, swap jars, fix hardcoded filenames, verify deploy | Not Started |
| 5 | — | Cross-cutting cleanup and Jira closeout | Not Started |
