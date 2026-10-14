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
      client cert into a `@TempDir` and points `server.ssl.*`/`client.ssl.*` at
      them; the test connects over HTTPS, captures the served certificate's
      serial, atomically rotates the keystore file, calls
      `POST /rest/trust?reload=true` (mTLS, admin client cert) to force
      detection, reconnects, and asserts the newly served serial matches the
      rotated keystore. Verify: compiles (`mvn test-compile`) — confirmed.
- [ ] 1.4 Run `TlsCertHotReloadTests` against the current, unmodified `develop`
      tip of both repos. **Checkpoint 1 (red):** attempted locally — confirmed
      it fails for the expected reason (`Application.main()`'s own
      `System.exit(1)` on the real app's `BeanCreationException` when DynamoDB
      credentials aren't available locally, the same requirement
      `ApplicationTests` already has — no surefire report/dump written,
      consistent with an abrupt JVM exit rather than a bug in the new test).
      This specific checkpoint's clean red signal (assertion failure showing the
      serial unchanged, not an environment crash) requires CI, which already has
      the AWS/Elastic access this needs — record the actual CI run result here
      once the PR in Group 6 is open.

## 2. Implement the izgw-core fix

- [ ] 2.1 Add `sslReloadConnectorCustomizer()` `@Bean` to `ClientTlsSupport`
      (guard on `"https".equals(connector.getScheme())`, cast
      `connector.getProtocolHandler()` to `AbstractHttp11Protocol<?>`, call
      `SslReloader.setProtocol(...)`), with Javadoc per `java-maven-style`.
      Verify: `mvn clean package` compiles.
- [ ] 2.2 Add a unit test asserting the customizer calls
      `SslReloader.setProtocol(...)` with the connector's protocol handler when
      given an HTTPS connector, and does nothing for a non-HTTPS connector.
      Verify: `mvn test -Dtest=<NewTestClass>` passes.
- [ ] 2.3 `mvn install` this branch locally so `izgw-hub` can build against the
      fixed snapshot. Verify: updated jar timestamp in the local `.m2` repo.

## 3. Prove the core fix alone (izgw-hub, cleanup not yet applied)

- [ ] 3.1 Point `izgw-hub`'s `pom.xml`/dependency resolution at the fixed
      `izgw-core` snapshot from 2.3, without touching `Application.java` yet.
      Verify: `mvn clean package` succeeds.
- [ ] 3.2 Re-run `TlsCertHotReloadTests` (via CI, same as 1.4). **Checkpoint 2
      (green):** verify the test passes — the post-rotation serial now matches
      the rotated keystore — record the CI run result as evidence.

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

## 6. Wire the verification into CI as a standalone step

- [ ] 6.1 Add a new, separate job/step (not folded into the existing `verify`
      job) in `izgw-hub`'s CI that runs `tls_hot_reload_test.sh` against an
      ephemeral local/Docker instance. Verify: the new job runs green on this
      branch's own PR.

## Workflow follow-up

- Archive this change in `izgw-core` once both PRs (`izgw-core`, `izgw-hub`)
  are merged and CI is green on both.
- Update `projects-in-progress.md`'s IGDD-3582 entry to reflect merged/archived
  status; leave the separately-flagged OpenSpec-hygiene note for the other
  stale `izgw-core` changes untouched — that is tracked independently.
