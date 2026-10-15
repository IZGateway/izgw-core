---
schema_version: '1.0'
created:
  date: '2026-10-14T20:20:15.488Z'
  user: boonek
  agent:
    name: claude-code
    version: '2.0'
  llm:
    name: claude-sonnet-5
    version: claude-sonnet-5
  prompt_uri: >-
    prompt:/claude-code/7b62e482-c255-4291-b97d-980994f07e6c/~2b892336-02b1-4e11-a4ad-14402756c12a
  summary: >-
    tasks.md for IGDD-3582: 3-checkpoint verification sequence interleaved with
    fix and cleanup
updated:
  - date: '2026-10-15T00:47:56.628Z'
    user: boonek
    agent:
      name: claude-code
      version: '2.0'
    llm:
      name: claude-sonnet-5
      version: claude-sonnet-5
    prompt_uri: >-
      prompt:/claude-code/7b62e482-c255-4291-b97d-980994f07e6c/~ea48f90f-a9b3-4f92-b1de-1d1ddda10af8
    summary: >-
      Rewrite tasks 1.3-1.4 to describe the rewritten SslBundle-based test and
      un-check checkpoint 1, which needs re-confirmation against the rewritten
      test; Document that the SslReloader approach was removed and replaced with
      Spring Boot's native SslBundle reload mechanism in tasks 2.1-2.2; Add task
      3.1a for the application.yml SslBundle migration and update checkpoint 2
      description
  - date: '2026-10-14T23:16:27.242Z'
    user: boonek
    agent:
      name: claude-code
      version: '2.0'
    llm:
      name: claude-sonnet-5
      version: claude-sonnet-5
    prompt_uri: >-
      prompt:/claude-code/7b62e482-c255-4291-b97d-980994f07e6c/~d5068f81-ab71-437d-9280-82b52d488ea5
    summary: >-
      Check off tasks 2.1-2.2: core fix implemented and tested, with the
      SSLImplementation scope correction documented; Remove Group 6: superfluous
      now that TlsCertHotReloadTests runs automatically via Surefire's
      **/*Tests.java convention, confirmed by CI already running it with no
      extra wiring
  - date: '2026-10-14T23:01:44.681Z'
    user: boonek
    agent:
      name: claude-code
      version: '2.0'
    llm:
      name: claude-sonnet-5
      version: claude-sonnet-5
    prompt_uri: >-
      prompt:/claude-code/7b62e482-c255-4291-b97d-980994f07e6c/~f23c6b4e-d333-4cbb-b6c8-2137fd86d225
    summary: >-
      Document real versioning/publishing mechanism for checkpoint 2/3:
      ticket-qualified SNAPSHOT per repo convention, izgw-hub pom bump, standard
      cross-repo merge-core-first pattern
  - date: '2026-10-14T22:51:31.008Z'
    user: boonek
    agent:
      name: claude-code
      version: '2.0'
    llm:
      name: claude-sonnet-5
      version: claude-sonnet-5
    prompt_uri: >-
      prompt:/claude-code/7b62e482-c255-4291-b97d-980994f07e6c/~22667ddd-2adf-4255-bb6b-cb94ef730d23
    summary: >-
      Correct HTTP method: /rest/trust?reload=true is GET, not POST; Check off
      task 1.4: checkpoint 1 (red) confirmed cleanly in CI after fixing 3 real
      test bugs
  - date: '2026-10-14T22:29:05.055Z'
    user: boonek
    agent:
      name: claude-code
      version: '2.0'
    llm:
      name: claude-sonnet-5
      version: claude-sonnet-5
    prompt_uri: >-
      prompt:/claude-code/7b62e482-c255-4291-b97d-980994f07e6c/~cf4981a1-f38c-496b-bea0-368dfcf65448
    summary: >-
      Rewrite Group 1 tasks to reflect the actual JUnit test classes built and
      what was verified locally vs needs CI; Update task 3.2 wording to
      reference TlsCertHotReloadTests and CI
  - date: '2026-10-14T21:56:59.136Z'
    user: boonek
    agent:
      name: claude-code
      version: '2.0'
    llm:
      name: claude-sonnet-5
      version: claude-sonnet-5
    prompt_uri: >-
      prompt:/claude-code/7b62e482-c255-4291-b97d-980994f07e6c/~683a8ad9-e2fd-4fba-9233-39507b022f67
    summary: >-
      Collapse Group 1 (static fixtures) into Group 2's script since fixtures
      are now generated fresh every run; renumber groups 1-6
  - date: '2026-10-14T21:54:28.143Z'
    user: boonek
    agent:
      name: claude-code
      version: '2.0'
    llm:
      name: claude-sonnet-5
      version: claude-sonnet-5
    prompt_uri: >-
      prompt:/claude-code/7b62e482-c255-4291-b97d-980994f07e6c/~fcbb8bc8-798f-48e6-897a-d74364cdfa1a
    summary: >-
      Check off task 1.1: baseline+rotated test keystores generated and
      verified; Add and check off task 1.3: admin test client cert generated and
      trusted by both server keystores
  - date: '2026-10-14T20:30:03.830Z'
    user: boonek
    agent:
      name: claude-code
      version: '2.0'
    llm:
      name: claude-sonnet-5
      version: claude-sonnet-5
    prompt_uri: >-
      prompt:/claude-code/7b62e482-c255-4291-b97d-980994f07e6c/~16a51eec-9dd0-4e1c-bb41-91aed6cfce40
    summary: >-
      Tasks Group 2: drop Newman collection task, fold curl trigger into the
      single script task; Tasks Group 7: remove last Newman collection reference
change_request: igdd-3582-fix-private-key-update-bug
ticket: IGDD-3582
document_type:
  - openspec-tasks
---
# Tasks

## 1. Verification test — prove the bug first (izgw-hub)

- [x] 1.1 Add `TlsTestKeystoreFactory` (`src/test/java/gov/cdc/izgateway/`): pure
      Java/BC-FIPS generation of self-signed server keystores and the admin
      client cert, no `keytool`/`openssl` subprocess, no files ever checked in.
      Verify: compiles (`mvn test-compile`) — confirmed.
- [x] 1.2 Add `TlsTestKeystoreFactoryTests`: standalone unit test of the factory
      (generate two certs, confirm same DN/different serial, build + write +
      reload a BCFKS keystore, confirm round-trip). No Spring context, no AWS
      dependency. Verify: `mvn test -Dtest=TlsTestKeystoreFactoryTests` passes —
      confirmed (1 test, 0 failures, 3.1s).
- [x] 1.3 Add `TlsCertHotReloadTests`, modeled on `ApplicationTests`
      (`@SpringBootTest(webEnvironment = RANDOM_PORT, useMainMethod = ALWAYS)`):
      `@DynamicPropertySource` generates the baseline/rotated keystores + admin
      client cert into a manually-created temp dir and points
      `spring.ssl.bundle.jks.izgw-server.keystore/truststore.location`/`.password`
      and `.key.alias` at them (type/provider stay `bcfks`/`BCFIPS` from
      `application.yml`), plus shortens
      `spring.ssl.bundle.watch.file.quiet-period` to 200ms; the test connects
      over HTTPS, captures the served certificate's serial, atomically rotates
      the keystore file, then polls (no manual trigger exists anymore --
      Spring's own `FileWatcher` picks up the change asynchronously) until the
      served serial changes or a 10s timeout elapses, and asserts it matches
      the rotated keystore. **Rewritten from an earlier version that overrode
      legacy `server.ssl.*`/`client.ssl.*` properties and called
      `GET /rest/trust?reload=true`** once the in-memory-`KeyStore` root cause
      (see design.md Context) made that mechanism provably unfixable; see
      task 1.4 for why checkpoint 1 had to be re-run against this rewritten
      test. Verify: compiles (`mvn test-compile`) — confirmed.
- [ ] 1.4 Run the rewritten `TlsCertHotReloadTests` (task 1.3) against the
      current, unmodified `develop` tip of both repos (legacy
      `server.ssl.key-store` properties, no bundle). **Checkpoint 1 (red) --
      needs re-confirmation**: an earlier checkpoint-1 run was recorded against
      the pre-rewrite test (the `server.ssl.*`/`client.ssl.*` +
      `GET /rest/trust?reload=true` version), which did correctly reproduce
      IGDD-3582 under its own (also-broken) mechanism, but that evidence no
      longer applies to the test as it will actually ship -- it must be
      re-established against the rewritten test described in 1.3. Expect the
      post-rotation serial to still match the baseline even after the full
      10s poll timeout, since legacy `server.ssl.key-store` properties get no
      update-handler wiring from Spring Boot at all. Verify: CI run recorded
      as evidence.

## 2. Implement the izgw-core fix

- [x] 2.1 **Superseded design, then corrected:** originally added a
      `sslReloadConnectorCustomizer()` `@Bean` that called
      `SslReloader.setProtocol(...)`. Confirmed afterward (see design.md
      Context) that `reloadSslHostConfigs()` cannot work under Spring Boot 4
      regardless of wiring, since `SslConnectorCustomizer` always configures
      Tomcat from an in-memory `KeyStore`, never a file path. Removed
      `SslReloader` entirely (the class, its trust-changed-listener
      registration in `afterPropertiesSet()`, and the `setProtocol(...)` call)
      and renamed/simplified the bean to `sslImplementationConnectorCustomizer()`,
      which now only calls
      `jsse.setSslImplementationName(SSLImplementation.class.getName())` --
      the one responsibility of the old customizer that was never about
      reload (OCSP-aware inbound client-cert validation, still needed; see
      design.md Decision 3 for why `izgw-hub`'s `Application.customizeConnector()`
      remains fully, safely removable in Group 4). The actual reload mechanism
      is now entirely Spring Boot's own `SslBundle`/`reload-on-update`
      machinery, configured in `izgw-hub`'s `application.yml` (task 3.1), not
      any code in `izgw-core`. Verify: `mvn compile` succeeds — confirmed.
- [x] 2.2 Rewrote `ClientTlsSupportTests` (3 cases, Mockito) for the
      simplified customizer from 2.1: HTTPS connector gets
      `setSslImplementationName(...)` wired; non-HTTPS connector is ignored
      (`getProtocolHandler()` never called); HTTPS connector with a
      non-`AbstractHttp11Protocol` handler is ignored without throwing. Dropped
      the reload-specific assertion (`SslReloader` no longer exists). Verify:
      `mvn test -Dtest=ClientTlsSupportTests` passes — confirmed (3/3, 0
      failures).
- [x] 2.3 Bump `izgw-core`'s `pom.xml` `<version>` to
      `3.9.1-IGDD-3582_Fix_Private_Key_Update_Bug-SNAPSHOT`, per this repo's own
      documented convention (`pom.xml` lines 13-15: working branches are
      labeled `<major>.<minor>.<patch>-IGDD-<ticket#>_<ticket-title>-SNAPSHOT`).
      `izgw-hub` pins an explicit released version (`3.8.0`), not a floating
      SNAPSHOT, so a local `mvn install` alone can never make CI pick up the
      fix — CI resolves `izgw-core` fresh from GitHub Packages on every run.
      `izgw-core`'s own CI (`maven.yml`) runs `mvn ... deploy` on every `push`
      *and* `pull_request` against `develop`, so opening a PR for this branch
      publishes this exact, uniquely-qualified SNAPSHOT coordinate to GitHub
      Packages automatically — distinct from `develop`'s own `3.9.0-SNAPSHOT`,
      so it can't collide with that or any other ticket branch's build. Verify:
      PR opened for this branch; confirm the new version appears under
      `gov.cdc.izgw.izgw-core` in GitHub Packages after CI runs.

## 3. Prove the core fix alone (izgw-hub, cleanup not yet applied)

- [ ] 3.1 Point `izgw-hub`'s `pom.xml` `izgw-core` dependency at
      `3.9.1-IGDD-3582_Fix_Private_Key_Update_Bug-SNAPSHOT` (from 2.3), without
      touching `Application.java` yet. This is a temporary pin for this CR's
      verification only — per this project's established cross-repo pattern
      (see `IGDD-3089` in `projects-in-progress.md`: merge `izgw-core` first,
      then point `izgw-hub` at the real released version, then merge `izgw-hub`),
      `izgw-hub`'s dependency gets reset to a proper released version once
      `izgw-core`'s fix is actually merged and released. Verify: `mvn clean
      package` succeeds, and CI resolves the new coordinate successfully.
- [ ] 3.1a Migrate `izgw-hub`'s `src/main/resources/application.yml`: replace
      `server.ssl.key-store`/`trust-store`/`*-password`/`*-provider`/`*-type`
      with `server.ssl.bundle: izgw-server` and a new
      `spring.ssl.bundle.jks.izgw-server` block (`reload-on-update: true`,
      same keystore/truststore `location`/`password`/`provider`/`type` values,
      same `PHIZ_CRYPTO_STORE_*_FILE`/`SSL_SHARE` env var indirection).
      `server.ssl.enabled`, `client-auth`, `enabled-protocols`, and `ciphers`
      stay where they are — behaviorally a no-op outside of enabling reload.
      This is the change that actually makes reload possible at all; the core
      fix in Group 2 only restores the (separate) `SSLImplementation` wiring.
      Verify: `mvn clean package` succeeds; `ApplicationTests` and the existing
      Newman suite still pass (no regression to the pre-rotation case).
- [ ] 3.2 Re-run `TlsCertHotReloadTests` (via CI, same as 1.4). **Checkpoint 2
      (green):** verify the test passes — the post-rotation serial now matches
      the rotated keystore, picked up by Spring's `FileWatcher` with no manual
      trigger — record the CI run result as evidence.

## 4. Remove izgw-hub's dead code

- [ ] 4.1 Remove `Application.protocol`, `Application.reloadSsl()`,
      `Application.customizeConnector()`, and the `reloadConnectorCustomizer`
      `@Bean` method, plus any now-unused imports. Verify: `mvn clean package`
      compiles with no unused-import warnings.
- [ ] 4.2 Run the full existing unit test suite and the existing
      `IZGW_2.0_Integration_Test.postman_collection.json` Newman suite. Verify:
      both green, no regressions from the removal.

## 5. Prove the cleanup didn't break anything

- [ ] 5.1 Re-run the harness from Group 1 a third time, now with both the core
      fix and the hub cleanup applied. **Checkpoint 3 (green):** verify the
      post-rotation serial still matches the rotated keystore's serial —
      record the result as evidence that the cleanup is safe.

## Workflow follow-up

- Archive this change in `izgw-core` once both PRs (`izgw-core`, `izgw-hub`)
  are merged and CI is green on both.
- Update `projects-in-progress.md`'s IGDD-3582 entry to reflect merged/archived
  status; leave the separately-flagged OpenSpec-hygiene note for the other
  stale `izgw-core` changes untouched — that is tracked independently.
