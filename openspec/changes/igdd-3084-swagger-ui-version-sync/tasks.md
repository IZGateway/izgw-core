---
schema_version: '1.0'
created:
  date: '2026-08-27T02:18:24.000Z'
  user: tyoung
  agent:
    name: claude-code
    version: '2.1.238'
  llm:
    name: claude-sonnet-5
    version: '5'
  source: >-
    user request: convert the IGDD-3084 swagger-ui version-sync design and its izgw-hub
    OpenSpec change (openspec/changes/igdd-3084-swagger-ui-version-sync in izgw-hub) into a
    cross-repo izgw-core task plan, following the pattern already established in this repo by
    bc-fips-2.1.3-upgrade and spring-boot-4-framework-7-upgrade
  summary: >-
    Cross-repo task plan moving the swagger-ui webjar version-sync fix (previously duplicated
    per-service, first in izgw-transform under IGDD-2976, now recurring in izgw-hub under
    IGDD-3084) into izgw-core as shared, auto-scanned configuration, so every current and future
    consumer inherits it instead of hand-copying it. Surfaced by open task 6.4 in
    spring-boot-4-framework-7-upgrade ("Manual verification: Swagger UI renders correctly").
tags:
  - springdoc
  - swagger-ui
  - webjars
  - dependency-drift
  - izgw-bom
  - izgw-core
  - izgw-hub
  - izgw-transform
change_request: igdd-3084-swagger-ui-version-sync
ticket: IGDD-3084
document_type:
  - openspec-tasks
---
# Tasks: Swagger UI webjar version sync (izgw-core)

**Jira:** [IGDD-3084](https://izgateway.atlassian.net/browse/IGDD-3084) — mirrors the already-shipped
[IGDD-2976](https://izgateway.atlassian.net/browse/IGDD-2976) fix in `izgw-transform`, this time moving it
up into `izgw-core` instead of duplicating it a second time.
**Primary repo:** izgw-core (this change) — also touches izgw-hub, izgw-transform (non-blocking follow-up)
**Related:** `spring-boot-4-framework-7-upgrade` task 6.4 in this same `openspec/changes/` directory flagged
the manual Swagger UI check as still open; that check is what surfaced this bug. This is a separate change
from that upgrade — the underlying drift predates it and would recur on any Spring Boot version (see
Background) — but task 6.4 there should be updated to point here once this lands.
**Full design doc:** `docs/superpowers/specs/2026-08-26-swagger-ui-version-sync-design.md` in `izgw-hub`
(alternatives considered, full rationale for putting this in `izgw-core` over the alternatives).
**Overall Status:** Not started — pre-flight verification (Stage 0) is already complete, captured below.

---

## Background (why this isn't a Spring Boot upgrade bug, even though it surfaced during one)

Springdoc serves Swagger UI's static assets from `classpath:META-INF/resources/webjars/swagger-ui/<version>/`,
where `<version>` comes from the `springdoc.swagger-ui.version` property. Every current consumer of
`izgw-core` hand-pins that property to a literal in its own `application.yml`. `izgw-bom` independently
manages the `org.webjars:swagger-ui` artifact version — deliberately (see `auto-update-dependencies`'s
proposal.md in this repo: BOM-managed dependencies are explicitly excluded from `izgw-core`'s own automated
update workflow "handled separately in BOM update process", precisely so `izgw-bom`'s nightly job can move
faster on UI/CVE patches than Springdoc's own release cadence) — and a nightly automated dependency-bump
workflow keeps moving that version forward. Every time the BOM-managed version passes a hand-typed literal,
Springdoc asks for a webjar directory that no longer exists, and `/swagger/ui.html` /
`/swagger/swagger-ui/index.html` 404s.

This is not new, and not caused by the Spring Boot 4 migration: `izgw-hub`'s git history shows the exact
same drift-and-manual-repatch cycle recurring repeatedly since May 2026, months before any Boot 4 work
started. `izgw-transform` already hit it once and fixed it under IGDD-2976 — but as a **per-service** fix.
That fix's own design doc explicitly anticipated this moment: *"the fix is xform-local and does not benefit
other izgw consumers of the BOM ... when there's a second consumer the right move is to lift this into
izgw-core, not pre-emptively."* IGDD-3084 is that second consumer.

The only thing the Spring Boot 4 migration actually did here is **expose** the current instance of the drift
sooner than it otherwise would have: it bumped `izgw-hub`'s `izgw-bom` parent from `1.14.1-SNAPSHOT` to
`1.15.0-SNAPSHOT`, and that newer BOM had already advanced its `org.webjars:swagger-ui` pin past `izgw-hub`'s
stale `application.yml` literal (`5.32.13` vs. the now-resolved `5.32.14`). The bug itself, and this fix,
apply regardless of Spring Boot version.

---

## Stage 0 — Pre-flight verification (complete)

_All of this was verified directly against the current `izgw-hub` branch (`IGDD-2353_spring_upgrade`) and
this `izgw-core` checkout during design; recorded here so Stage 1 doesn't have to re-derive it._

- [x] 0.1 Confirmed the drift is live right now, not theoretical: `mvn dependency:tree` in `izgw-hub`
  resolves `org.springdoc:springdoc-openapi-starter-webmvc-ui:3.1.0` → `org.webjars:swagger-ui:5.32.14`,
  while `izgw-hub/src/main/resources/application.yml` still pins `springdoc.swagger-ui.version: 5.32.13`.
- [x] 0.2 Confirmed `org.webjars.WebJarVersionLocator.version(String)` and
  `org.springdoc.core.properties.SwaggerUiConfigProperties.setVersion(String)` both still exist with the
  same signatures the IGDD-2976 fix used, despite springdoc 3.1.0 switching its transitive webjar-locator
  artifact from classic `webjars-locator-core` to `webjars-locator-lite:1.1.4` — verified via `javap` against
  both jars directly. No adaptation of the original `izgw-transform` implementation is needed.
- [x] 0.3 Confirmed `izgw-core` already declares `springdoc-openapi-starter-webmvc-ui` as its own dependency
  (this is what brings springdoc, and the `webflux-ui`/`webmvc-ui` conflict, to every consumer in the first
  place) — no new dependency needs to be added for this change.
- [x] 0.4 Confirmed both current consumers already component-scan `gov.cdc.izgateway.configuration` (the
  package this change adds a class to) with **zero wiring changes on their side**:
  - `izgw-hub`'s `@SpringBootApplication` is rooted at `gov.cdc.izgateway` (covers it by default).
  - `izgw-transform`'s `Application.java` has an explicit
    `@ComponentScan(basePackages={"gov.cdc.izgateway.xform", ..., "gov.cdc.izgateway.configuration", ...})`
    that already lists this exact package.
- [x] 0.5 Confirmed `izgw-core` has an existing precedent for this shape of shared config:
  `gov.cdc.izgateway.configuration.NoopServicesAutoConfiguration` — a plain `@Configuration` with
  `@Bean @ConditionalOnMissingBean` methods, picked up via component scan, not a `spring.factories`/
  `AutoConfiguration.imports`-registered auto-configuration. This change follows the same shape.
- [x] 0.6 Confirmed `izgw-bom`'s `org.webjars:swagger-ui` override is deliberate policy (per its pom comment
  and `auto-update-dependencies`'s proposal in this repo), not an oversight — so removing that override was
  correctly rejected as an alternative (see the full design doc). Runtime detection makes the question moot
  regardless of what `izgw-bom` does.

---

## Stage 1 — izgw-core (this repo)

- [ ] 1.0 Create branch `IGDD-3084_swagger_ui_version_sync` from `develop`. **Not done yet** — the
  implementation below (1.1–1.6) was written directly against the local working tree, which is currently
  checked out on `IGDD-2353_spring_upgrade` (uncommitted; that branch is 7 commits ahead of
  `origin/develop` and carries other unrelated migration work). `spring-boot-4-framework-7-upgrade/tasks.md`
  also only exists on this branch, not on `develop` yet, which is what this change's task 4.1 note depends
  on. Deliberately left as a human decision rather than done automatically: **before opening 1.PR1**,
  either (a) cherry-pick/re-apply the two new files onto a fresh branch off `develop` to keep this fix
  fully decoupled from the migration as designed, or (b) accept landing it on top of the migration branch
  if that's already merging soon and separation isn't worth the extra step. Either way the code itself is
  identical and already verified working against springdoc 3.1.0 (this branch's version).
- [x] 1.1 Wrote the failing unit test file
  `src/test/java/gov/cdc/izgateway/configuration/SwaggerUiVersionConfigTests.java` — 6 tests covering: the
  aligner sets version to the actual webjar version; the resolved version is not the legacy `5.32.13` pin nor
  Springdoc's bundled default; a pre-existing bogus pin is overridden; a `null`, blank, or
  exception-throwing detector leaves the existing value untouched and does not propagate.
- [x] 1.2 Wrote the failing context test file
  `src/test/java/gov/cdc/izgateway/configuration/SwaggerUiVersionContextTests.java` using Spring's
  `ApplicationContextRunner` (not `@SpringBootTest` — this is a library module with no
  `@SpringBootApplication` of its own to bootstrap) to prove the `BeanPostProcessor` fires against a real
  `SwaggerUiConfigProperties` bean inside a live `ApplicationContext`.
- [x] 1.3 Ran `mvn test -Dtest=SwaggerUiVersionConfigTests,SwaggerUiVersionContextTests`; confirmed both
  files failed to compile (`cannot find symbol: SwaggerUiVersionConfig`) before the production class existed.
- [x] 1.4 Implemented `src/main/java/gov/cdc/izgateway/configuration/SwaggerUiVersionConfig.java` — a
  `@Configuration(proxyBeanMethods = false)` exposing a static `@Bean BeanPostProcessor
  swaggerUiVersionAligner()` that intercepts `SwaggerUiConfigProperties` and overwrites its `version` via
  `alignVersionFromWebjar`/`alignVersion(props, Supplier<String>)`. Same implementation `izgw-transform`
  already shipped under IGDD-2976, moved here verbatim with the package changed to
  `gov.cdc.izgateway.configuration`.
- [x] 1.5 Ran the tests again: all 6 unit tests and the 1 context test pass. Startup log confirms detection
  works end-to-end: `Detected swagger-ui webjar version: 5.32.14`.
- [x] 1.6 Ran `mvn clean install -DskipDependencyCheck=true` (full build, OWASP scan skipped per the same
  flag CI uses) — `BUILD SUCCESS`, no regression in `NoopServicesAutoConfiguration` or any other existing
  test in this module.
- [ ] 1.7 Run `mvn dependency-check:check`; confirm no new CVE surface. **Not run** — the OWASP/NVD scan is
  slow (dependency-check's local database sync alone commonly runs well past several minutes) and wasn't
  worth blocking on here since no new dependency was added (0.3 already confirmed both
  `WebJarVersionLocator` and `SwaggerUiConfigProperties` come from `springdoc-openapi-starter-webmvc-ui`,
  already a dependency). Let CI's normal OWASP job cover this on the actual PR.
- [ ] **1.PR1** Open PR in `izgw-core`; title referencing IGDD-3084; body links to the design doc in
  `izgw-hub` and to `izgw-transform`'s already-merged IGDD-2976 PR (`izgw-transform#264`) as the precedent
  this generalizes. Do not merge until CI passes.
- [ ] 1.8 After merge, record the published SNAPSHOT version (check the GitHub Actions deploy log, or run
  `mvn -q help:evaluate -Dexpression=project.version -DforceStdout` against the merged branch) — this value
  is needed by Stage 2.1 and Stage 3.1.

---

## Stage 2 — izgw-hub

_Already has its own OpenSpec change tracking this in detail:
`openspec/changes/igdd-3084-swagger-ui-version-sync/` in `izgw-hub` (proposal.md / design.md / tasks.md /
specs/api-documentation/spec.md). Summarized here for cross-repo visibility only — do the actual work via
that change, not this one._

- [ ] 2.1 Bump the `gov.cdc.izgw:izgw-core` dependency version in `izgw-hub/pom.xml` to the SNAPSHOT
  recorded in Stage 1.8.
- [ ] 2.2 Remove the hand-typed `springdoc.swagger-ui.version: 5.32.13` line (and its stale comment) from
  `izgw-hub/src/main/resources/application.yml`.
- [ ] 2.3 Add `SwaggerUiVersionIntegrationTests` (`@SpringBootTest`, following the existing
  `ApplicationTests.java` pattern) proving Hub's own application context inherits the fix — write it
  *before* 2.1/2.2 so it demonstrably fails first (the IGDD-3084 reproduction), then passes after.
- [ ] **2.PR1** Open PR in `izgw-hub`; do not merge until CI (build, unit tests, OWASP check, Newman
  `TC_92a`/`TC_92b` smoke tests against dev) passes.

---

## Stage 3 — izgw-transform (non-blocking follow-up, separate PR)

_Does not block Stage 2 landing first. **Correction, verified directly:** the old local copy and the new
inherited one are NOT a harmless redundant pair — both classes are named `SwaggerUiVersionConfig` (different
packages), and Spring's default `@Configuration` bean-naming collides on the simple class name. Once
`izgw-transform` bumps to a fixed `izgw-core` while its local copy still exists, boot fails with
`ConflictingBeanDefinitionException: Annotation-specified bean name 'swaggerUiVersionConfig' ... conflicts
with existing, non-compatible bean definition`. So the version bump (3.0) and the deletion (3.2) MUST land in
the same commit/PR — this stage cannot be split further than it already is from Stage 2._

- [x] 3.0 Bumped the `izgw-core` dependency version in `izgw-transform/pom.xml` — **no edit needed**, same
  situation as `izgw-hub`: it already referenced the exact SNAPSHOT coordinate
  (`3.5.1-IGDD-2353_spring_upgrade-SNAPSHOT`) that Stage 1's local `.m2` install already fixed. Revisit
  once `izgw-core`'s branch placement (Stage 1 task 1.0) is finalized, same caveat as `izgw-hub`.
- [x] 3.1 Ran the existing `SwaggerUiVersionConfigTests`/`SwaggerUiVersionContextTests` (local copy still
  present at this point) — `SwaggerUiVersionConfigTests` passed (6/6), but
  `SwaggerUiVersionContextTests.postProcessorAlignsSwaggerUiVersionInRealContext` **errored** with
  `ConflictingBeanDefinitionException`, proving the conflict described above empirically rather than just
  by code inspection.
- [x] 3.2 Deleted the local copy: `SwaggerUiVersionConfig.java`, `SwaggerUiVersionConfigTests.java`,
  `SwaggerUiVersionContextTests.java` (all under `gov.cdc.izgateway.xform.configuration`) — staged as
  deletions (`git rm --cached` + `rm -f`), left uncommitted per instruction.
- [x] 3.3 Ran `mvn clean test`: 245 tests, 0 failures, 0 errors, `BUILD SUCCESS`. Startup log for
  `XformApplicationTests` confirms `Detected swagger-ui webjar version: 5.32.14` now logged from
  `gov.cdc.izgateway.configuration.SwaggerUiVersionConfig` (the inherited class) — the `izgw-core` fix
  alone is fully sufficient.
- [ ] 3.4 Archive `izgw-transform`'s own `openspec/changes/archive/2026-05-31-auto-detect-swagger-ui-version/`
  — no requirement in `openspec/specs/api-documentation/spec.md` changes (behavior is identical; only the
  implementation's location moved), so this is a housekeeping note in the PR description, not a new
  `MODIFIED Requirements` delta. Not done — deferred to whoever actually opens the PR.
- [ ] **3.PR1** Open PR in `izgw-transform`; do not merge until CI passes.

---

## Stage 4 — Cross-cutting verification and cleanup

- [ ] 4.1 Update `spring-boot-4-framework-7-upgrade/tasks.md` task 6.4 ("Manual verification: Swagger UI
  renders correctly") in this repo's `openspec/changes/` to cross-reference this change instead of standing
  alone as an unexplained manual check.
- [ ] 4.2 Confirm no other `izgw-*` repo hand-pins `springdoc.swagger-ui.version` (grep each repo's
  `application.yml`/`application-*.yml` for the property) — if any do, they need the same Stage 2/3 treatment.
- [ ] 4.3 Update [IGDD-3084](https://izgateway.atlassian.net/browse/IGDD-3084) with final notes once all
  stages land, and cross-link IGDD-2976 as the precedent this generalized.

---

## Summary

| Stage | Repo | Description | Status |
|---|---|---|---|
| 0 | — | Pre-flight verification | Complete |
| 1 | izgw-core | Add `SwaggerUiVersionConfig` + tests, release | Code+tests done locally, uncommitted; branch/PR not started |
| 2 | izgw-hub | Consume new core version, drop yaml pin, add regression test | Done locally, uncommitted, manually verified end-to-end; PR not started |
| 3 | izgw-transform | Consume new core version, remove now-required-to-be-removed local copy | Cleanup done locally, uncommitted, verified (245 tests pass); PR not started |
| 4 | — | Cross-cutting cleanup and Jira closeout | Not Started |
