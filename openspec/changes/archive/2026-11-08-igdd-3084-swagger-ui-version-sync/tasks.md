---
schema_version: '1.0'
created:
  date: '2026-08-27T02:18:24.000Z'
  user: tyoung
  agent:
    name: claude-code
    version: 2.1.238
  llm:
    name: claude-sonnet-5
    version: '5'
  source: >-
    user request: convert the IGDD-3084 swagger-ui version-sync design and its
    izgw-hub OpenSpec change (openspec/changes/igdd-3084-swagger-ui-version-sync
    in izgw-hub) into a cross-repo izgw-core task plan, following the pattern
    already established in this repo by bc-fips-2.1.3-upgrade and
    spring-boot-4-framework-7-upgrade
  summary: >-
    Cross-repo task plan moving the swagger-ui webjar version-sync fix
    (previously duplicated per-service, first in izgw-transform under IGDD-2976,
    now recurring in izgw-hub under IGDD-3084) into izgw-core as shared,
    auto-scanned configuration, so every current and future consumer inherits it
    instead of hand-copying it. Surfaced by open task 6.4 in
    spring-boot-4-framework-7-upgrade ("Manual verification: Swagger UI renders
    correctly").
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
updated:
  - date: '2026-11-08T15:58:37.955Z'
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
      Reconcile 1.0: the branch/PR decision resolved in favor of a dedicated PR,
      confirmed via PR #95; Reconcile Stage 1 remaining tasks: 1.7 satisfied via
      PR #95's CI, 1.PR1 confirmed merged, 1.8 confirmed via downstream
      consumption; Reconcile Stage 2 (izgw-hub): confirmed done via izgw-hub's
      own archived CR; Reconcile Stage 3 (izgw-transform): 3.4 archive and 3.PR1
      confirmed done via commit evidence; Reconcile Stage 4: 4.2 confirmed via
      grep sweep, 4.3 confirmed Jira already Resolved; Reconcile summary table:
      all 4 stages confirmed complete, ready to archive; Close 4.1:
      cross-reference added to spring-boot-4-framework-7-upgrade task 6.4
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

- [x] 1.0 ~~Create branch, decide separate-PR-vs-riding-the-migration~~ **resolved 2026-11-08**: landed as
  its own dedicated PR after all — [#95](https://github.com/IZGateway/izgw-core/pull/95), merged
  2026-09-01 by Todd Young, decoupled from the Spring Boot migration as originally designed.
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
- [x] 1.7 ~~Run `mvn dependency-check:check` locally~~ **satisfied via CI**: covered by PR #95's required
  OWASP job before merge, as anticipated.
- [x] **1.PR1** Opened as [#95](https://github.com/IZGateway/izgw-core/pull/95), merged 2026-09-01.
- [x] 1.8 **done**: downstream consumption confirms the published version propagated correctly —
  `izgw-transform`'s `220a0522b IGDD-3084: sync swagger-ui version via izgw-core` and merged PR #264
  both consume the released `izgw-core` version with the aligner active.

---

## Stage 2 — izgw-hub

_Already has its own OpenSpec change tracking this in detail:
`openspec/changes/igdd-3084-swagger-ui-version-sync/` in `izgw-hub` (proposal.md / design.md / tasks.md /
specs/api-documentation/spec.md). Summarized here for cross-repo visibility only — do the actual work via
that change, not this one._

- [x] 2.1 **done via izgw-hub's own CR** — confirmed archived at
  `izgw-hub/openspec/changes/archive/2026-09-21-igdd-3084-swagger-ui-version-sync/`.
- [x] 2.2 **confirmed**: the hand-pinned `springdoc.swagger-ui.version` literal is gone from `izgw-hub`'s
  current `application.yml`/`application-*.yml` files.
- [x] 2.3 **done via izgw-hub's own CR** — tracked and verified in that change's own tasks.md, not
  duplicated here.
- [x] **2.PR1** **done via izgw-hub's own CR** — landed and archived as above.

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
- [x] 3.4 **done**: confirmed via `izgw-transform` commit `dfed2dc20 chore: Archive openspec change for
  swagger auto-detect`.
- [x] **3.PR1** Opened as [#264](https://github.com/IZGateway/izgw-transform/pull/264)
  (`afdef68e8 Merge pull request #264 from IZGateway/swagger-ui-auto-detect`), merged.

---

## Stage 4 — Cross-cutting verification and cleanup

- [x] 4.1 **done**: `spring-boot-4-framework-7-upgrade/tasks.md` task 6.4 updated to cross-reference this
  change.
- [x] 4.2 **done**: swept `izgw-hub`, `izgw-transform`, `izgw-core` — no remaining hand-pinned
  `springdoc.swagger-ui.version` literal anywhere. Other `izgw-*`/`izg-*` repos (`izg-transformation-ui`,
  `izg-configuration-console`, etc.) are Next.js/TypeScript, not Spring/springdoc consumers — not applicable.
- [x] 4.3 **done**: [IGDD-3084](https://izgateway.atlassian.net/browse/IGDD-3084) confirmed Resolved in Jira;
  IGDD-2976 cross-linked as the precedent this generalized.

---

## Summary

| Stage | Repo | Description | Status |
|---|---|---|---|
| 0 | — | Pre-flight verification | Complete |
| 1 | izgw-core | Add `SwaggerUiVersionConfig` + tests, release | Done — PR #95 merged 2026-09-01 |
| 2 | izgw-hub | Consume new core version, drop yaml pin, add regression test | Done — via izgw-hub's own archived CR (`archive/2026-09-21-igdd-3084-swagger-ui-version-sync/`) |
| 3 | izgw-transform | Consume new core version, remove now-required-to-be-removed local copy | Done — PR #264 merged, own OpenSpec change archived |
| 4 | — | Cross-cutting cleanup and Jira closeout | Done |

**Reconciled 2026-11-08** — all 4 stages confirmed complete via commit/PR/archive evidence; ready to archive
this change. See `updated[]` entries above for the evidence trail.
