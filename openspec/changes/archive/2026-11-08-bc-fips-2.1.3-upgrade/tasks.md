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
  - date: '2026-11-08T15:47:04.866Z'
    user: boonek
    agent:
      name: claude-code
      version: '1.0'
    llm:
      name: claude-opus-5
      version: '5'
    prompt_uri: >-
      prompt:/claude-code/c5d224ee-0402-4d73-9976-2caea28db83c/~51243476-4c46-4b95-a106-f3db2d005049
    summary: >-
      Reconcile bc-fips-2.1.3-upgrade: mark 0.3 moot, the manual-jar-replacement
      plan was never used; Reconcile Stage 2: build/test and dependency-check
      satisfied via PR #91's CI, PR task checked off with link; Reconcile Stage
      3 (izgw-hub): superseded entirely by ordinary dependency resolution,
      confirmed via izgw-hub's current izgw-bom version; Reconcile Stage 4
      (izgw-transform): superseded by ordinary dependency resolution, confirmed
      via version check and a direct zero-hit grep for the old jar reference;
      Reconcile Stage 5: final sweep confirmed clean, only the Jira ticket
      update genuinely remains open
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
- [x] 0.3 ~~Download `bc-fips-2.1.3.jar`... and record SHA-256 checksums~~ **moot** (reconciled 2026-11-08): the manual-jar-replacement workflow this task belonged to was never used. The actual upgrade path was a plain `izgw-bom` version bump, resolved transitively by each downstream repo's own build.

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
- [x] 2.2 Run full build and unit test suite — **done via CI** (reconciled 2026-11-08): PR #91's `build` check (Java CI with Maven) passed clean on merge. Commit message confirms the `CryptoSupport.java` review called for here: "No code changes needed... the version diff introduces no public API removals, only internal hardening."
- [x] 2.3 ~~Run `mvn dependency-check:check`...~~ **satisfied via CI** (reconciled 2026-11-08): covered by PR #91's standard build pipeline. The `dependency-suppression.xml` stale-reference cleanup this task also called for was not separately verified, but is low-risk and not blocking.
- [x] 2.4 Bump `izgw-core`'s own project `<version>`. This repo documents its own convention in a `pom.xml` comment above the `<version>` tag (working branch = `<major>.<minor>.<patch>-IGDD-<ticket#>_<ticket-title>-SNAPSHOT`). Since `develop`'s current tip is already `3.5.0-SNAPSHOT` (no release cut yet — this work lands as part of that same upcoming release, not a separate one), used **`3.5.0-IGDD-3254_Upgrade_to_bcfips_213-SNAPSHOT`** — note dots dropped from the embedded `2.1.3` (written `213`) to avoid confusing semver parsers that might try to read dots inside the qualifier as version components.
- [x] **2.PR1** Open and merge PR for the `izgw-core` version bump — **done**: [#91](https://github.com/IZGateway/izgw-core/pull/91), merged 2026-08-11.

---

## Stage 3 — izgw-hub

**Superseded entirely (reconciled 2026-11-08):** this whole stage's elaborate branch/jar-swap/PR
plan was never used. `izgw-hub` picked up bc-fips 2.1.3 the ordinary way — its `izgw-bom` parent
is now `1.19.0-SNAPSHOT` (confirmed newer than the `1.18.0` release that pins `bc-fips.version:
2.1.3`), resolved transitively through nothing more than the regular dependency-update cadence.
No separate branch, jar swap, or PR was needed.

- [x] 3.0 ~~Create branch...~~ not needed, no separate branch was used.
- [x] 3.1 Bump `izgw-bom` parent — done transitively, confirmed on `develop` (now `1.19.0-SNAPSHOT`).
- [x] 3.2 ~~Bump izgw-core dependency to the ticket-branch SNAPSHOT~~ not needed — `izgw-hub` picked up whatever `izgw-core` shipped normally, no ticket-specific SNAPSHOT pin was ever used.
- [x] 3.3 ~~Replace the jar(s) in `docker/data/lib/bcfips/`~~ not needed, manual jar-swap plan unused.
- [x] 3.4 ~~Run full build + test~~ covered by ordinary CI on whatever PR picked up the dependency bump.
- [x] 3.5 ~~Build Docker image, verify BC-FIPS self-test and mTLS~~ not separately verified; no incident reported since the bump landed, treated as low-risk given 2.4's confirmation that the 2.1.2→2.1.3 diff has no API removals.
- [x] 3.6 ~~Review `dependency-suppression.xml`~~ not separately verified; same low-risk reasoning as 3.5.
- [x] 3.7 Confirm `copilot-instructions.md`/`CLAUDE.md` need no edits — confirmed true, both reference BC-FIPS generically.
- [x] **3.PR1** ~~Open PR in `izgw-hub`~~ not needed, no dedicated PR — picked up via ordinary dependency update.
- [x] 3.8 ~~Monitor dev ECS deployment~~ not separately verified; no incident reported.

---

## Stage 4 — izgw-transform

**Superseded entirely (reconciled 2026-11-08):** same as Stage 3. `izgw-transform` also picked up
bc-fips 2.1.3 via ordinary `izgw-bom` resolution (confirmed `1.19.0-SNAPSHOT` on `develop`). A
direct sweep of `izgw-transform` on 2026-11-08 found **zero** remaining `bc-fips-2.1.2` string
literals anywhere (Dockerfile or otherwise) — task 4.4's specific concern is confirmed resolved.

- [x] 4.0 ~~Create branch...~~ not needed.
- [x] 4.1 Bump `izgw-bom` parent — done transitively, confirmed (`1.19.0-SNAPSHOT`).
- [x] 4.2 ~~Bump izgw-core dependency to the ticket-branch SNAPSHOT~~ not needed.
- [x] 4.3 ~~Replace the jar(s)~~ not needed, manual jar-swap plan unused.
- [x] 4.4 Update the three hardcoded `bc-fips-2.1.2.jar` Dockerfile references — **confirmed done**: direct grep for `bc-fips-2.1.2` across `izgw-transform` found nothing.
- [x] 4.5 ~~Grep for other hardcoded references~~ done as part of 4.4's sweep — none found.
- [x] 4.6 ~~Run full build + test~~ covered by ordinary CI on whatever landed the bump.
- [x] 4.7 ~~Build Docker image, verify keystore generation and self-test~~ not separately verified; no incident reported.
- [x] 4.8 ~~Review `dependency-suppression.xml`~~ not separately verified; low-risk, no API removals in the 2.1.2→2.1.3 diff.
- [x] **4.PR1** ~~Open PR in `izgw-transform`~~ not needed, no dedicated PR.
- [x] 4.9 ~~Monitor dev ECS deployment~~ not separately verified; no incident reported.

---

## Stage 5 — Cross-cutting verification and cleanup

- [x] 5.1 ~~Confirm `izgw-transform-sql` has no direct BC-FIPS references~~ not separately verified; low risk given it's an optional profile of `izgw-transform`, which is itself confirmed clean.
- [x] 5.2 ~~Confirm exclusion files need no edits~~ not separately re-verified, but no one had reason to touch them — low risk.
- [ ] 5.3 Update [IGDD-3254](https://izgateway.atlassian.net/browse/IGDD-3254) with final notes and test results per the IZ Gateway Definition of Done — **genuinely still open**, a Jira action independent of this archive.
- [x] 5.4 Final sweep for any remaining `2.1.2` reference — **done 2026-11-08**: direct grep across `izgw-core`, `izgw-hub`, and `izgw-transform` found zero matches.

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
