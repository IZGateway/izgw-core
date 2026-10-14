---
schema_version: '1.0'
created:
  date: '2026-10-14T18:56:09.241Z'
  user: boonek
  agent:
    name: claude-code
    version: '2.0'
  llm:
    name: claude-sonnet-5
    version: claude-sonnet-5
  prompt_uri: >-
    prompt:/claude-code/7b62e482-c255-4291-b97d-980994f07e6c/~8759d077-531e-42a4-b1b3-c248a0e26c8f
  summary: >-
    IGDD-3582 proposal: wire dead SslReloader to fix Tomcat hot-reload of
    rotated TLS keystore
updated:
  - date: '2026-10-14T22:26:54.349Z'
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
      Proposal: verification is a JUnit test (TlsCertHotReloadTests) modeled on
      ApplicationTests, not a shell script; Impact section: list the actual
      three new test classes added
  - date: '2026-10-14T21:57:31.208Z'
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
      Proposal: verification material is generated fresh every run, not reused
      from COMMON_PASS/TESTING_CERT-dependent fixtures
  - date: '2026-10-14T20:28:56.237Z'
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
      Switch verification mechanism from Newman/Postman to curl+openssl single
      script
  - date: '2026-10-14T19:12:42.206Z'
    user: boonek
    agent:
      name: claude-code
      version: '2.0'
    llm:
      name: claude-sonnet-5
      version: claude-sonnet-5
    prompt_uri: >-
      prompt:/claude-code/7b62e482-c255-4291-b97d-980994f07e6c/~be94d555-c2c0-47d0-ae15-1b052b80abde
    summary: Drop unconfirmed isOutOfDate mtime fragility bullet from proposal
  - date: '2026-10-14T19:01:41.732Z'
    user: boonek
    agent:
      name: claude-code
      version: '2.0'
    llm:
      name: claude-sonnet-5
      version: claude-sonnet-5
    prompt_uri: >-
      prompt:/claude-code/7b62e482-c255-4291-b97d-980994f07e6c/~e9ab2472-7818-476c-99ff-d3dd6645990f
    summary: >-
      Expand proposal scope: izgw-hub dead-code removal in-scope, verification
      test relocated to izgw-hub; Update Impact section for two-repo scope
      (izgw-core fix + izgw-hub cleanup/tests)
change_request: igdd-3582-fix-private-key-update-bug
ticket: IGDD-3582
document_type:
  - openspec-proposal
---
# Proposal

## Why

The Hub's embedded Tomcat HTTPS listener loads its TLS server certificate once at
startup and never reloads it. `ClientTlsSupport` already contains a background
scheduler that detects when the on-disk keystore file changes and a `SslReloader`
helper designed to force Tomcat to reload (`AbstractHttp11Protocol.reloadSslHostConfigs()`),
but `SslReloader.setProtocol(...)` is never called anywhere in the codebase, so
`SslReloader.protocol` is permanently `null` and the reload is a silent no-op. This
has been true since the class was introduced in the initial commit (2024-05-25) —
confirmed independent of the later Spring Boot 4 / Tomcat 11 migration (Aug–Sep 2026,
IGDD-2353), which only renamed the field's type (`AbstractHttp11JsseProtocol` →
`AbstractHttp11Protocol`, because Tomcat 11 removed the former) without touching the
missing wiring.

Net effect: whenever the Hub's server keystore is rotated on disk (certificate
renewal), the running process keeps presenting the old certificate — including after
it expires — until someone restarts it. This caused IGDD-3582: the APHL-mirrored dev
environment's server certificate expired 2026-10-10 and connectivity/status checks to
`dev.phiz-project.org` failed with `certificate_unknown(46)` until the ECS task was
restarted. A restart "fixes" it only because Spring Boot builds a brand-new
`SSLHostConfig` from whatever is on disk at cold-start — it never exercises the
broken reload path at all.

## What Changes

- Add a `TomcatConnectorCustomizer` bean, defined entirely in `izgw-core`'s
  `ClientTlsSupport`, that captures the live embedded HTTPS connector's protocol
  handler at startup and registers it with `ClientTlsSupport.SslReloader` — closing
  the wiring gap. Spring Boot's `TomcatServletWebServerFactory` already collects
  every `TomcatConnectorCustomizer` bean in the application context (confirmed in
  `izgw-hub/Application.java:521`, `factory.getConnectorCustomizers().addAll(...)`),
  so this bean is picked up automatically by any consumer (`izgw-hub`, and any other
  app built on `izgw-core`'s embedded-Tomcat setup) with **no code change required
  in any consuming repository**.
- No change to the trust-change detection logic itself (`KeyStoreLoader.isOutOfDate()`)
  or to the client-side keystore/truststore reload path used for outbound calls
  (`MessageSender`) — that path already works correctly today, which is how we
  diagnosed this: `/rest/trust` (reporting on the client-side store) showed the
  correct, rotated certificate while the server's actual TLS handshake did not.
- Remove `izgw-hub`'s own parallel, equally-dead `Application.protocol` /
  `Application.reloadSsl()` / `reloadConnectorCustomizer` bean (`Application.java`).
  It was an independent, never-invoked attempt at the same reload hook and is
  provably superfluous once `izgw-core` wires the real one — keeping it around
  would just be confusing dead code next to a now-working mechanism. **This CR
  spans two repos**: the fix lives in `izgw-core`, this cleanup lives in
  `izgw-hub`, both on a branch named `IGDD-3582_Fix_Private_Key_Update_Bug`.
- Add a regression test in `izgw-hub` — not a Postman/Newman collection or a shell
  script, but a plain JUnit test (`TlsCertHotReloadTests`, matching the project's
  own `**/*Tests.java` Surefire convention, modeled on the existing
  `ApplicationTests` pattern of booting the real application via
  `@SpringBootTest(useMainMethod = ALWAYS)`) that: (1) demonstrates the bug against
  an unfixed build, and (2) demonstrates the fix against a fixed build, without
  requiring a process restart in either case. All keystore and client-certificate
  material is generated fresh in pure Java on every run (BC-FIPS is already a
  project dependency, so `JcaX509v3CertificateBuilder` generates the self-signed
  certs directly — no `keytool`/`openssl` subprocess needed), written to a JUnit
  `@TempDir`, and wired in via `@DynamicPropertySource` — never checked into the
  repo, and not dependent on the real `awsdev_keystore.bcfks`/`COMMON_PASS` or
  `TESTING_CERT`/`TESTING_PASS` secrets. This avoids reintroducing this CR's own
  bug class into the test: a checked-in fixture with fixed validity would, given
  enough time, expire and fail the test for the wrong reason. The test makes the
  actual rotate-and-recheck HTTPS calls via the JDK's own `HttpsURLConnection`/
  `SSLSession` APIs rather than shelling out to `curl`/`openssl s_client`. This has
  to live in `izgw-hub`, not `izgw-core`: `izgw-core` is a library with no
  standalone embedded server of its own, and `izgw-hub` already has the runnable
  application this verification needs. Because it's a normal `*Tests.java` class,
  it runs automatically as part of the existing `mvn clean package`/CI build —
  no separate standalone CI job is needed. Like `ApplicationTests`, it requires
  the same real AWS DynamoDB / Elastic access CI already provides for that
  existing test; this is not a new environmental requirement.

## Capabilities

### New Capabilities
- `tls-cert-hot-reload`: the Hub's embedded TLS listener keeps its certificate in
  sync with the on-disk keystore file without requiring a process restart.

### Modified Capabilities
_None — no existing specs exist yet in this repo (`openspec list --specs` returns
none); this is the first capability registered._

## Impact

- **Code (`izgw-core`)**: `src/main/java/gov/cdc/izgateway/security/ClientTlsSupport.java`
  only (new `@Bean` method + minor supporting changes). No API/contract changes, no
  breaking changes, no new runtime dependencies (Tomcat/Spring Boot classes already
  imported in this file).
- **Code (`izgw-hub`)**: `src/main/java/gov/cdc/izgateway/Application.java` — remove
  the dead `protocol` field, `reloadSsl()` method, and `reloadConnectorCustomizer`
  bean. No production behavior changes other than removing unreachable code. Adds
  three new test classes under `src/test/java/gov/cdc/izgateway/`:
  `TlsTestKeystoreFactory` (ephemeral BC-FIPS cert/keystore generation helper),
  `TlsTestKeystoreFactoryTests` (standalone unit test of that helper, no AWS
  dependency), and `TlsCertHotReloadTests` (the actual regression test).
- **Consumers**: `izgw-hub` (and any other embedded-Tomcat app built on `izgw-core`)
  gets the reload fix automatically on the next `izgw-core` version bump — no source
  change is *required* there for the fix itself to take effect; the `Application.java`
  cleanup in this CR is in scope by choice, not by necessity.
- **Branches**: `IGDD-3582_Fix_Private_Key_Update_Bug` in both `izgw-core` and
  `izgw-hub`. Verification requires building `izgw-hub` against this `izgw-core`
  branch's snapshot (e.g. local `mvn install` of `izgw-core`, then building `izgw-hub`
  against that snapshot version) to exercise the fix end-to-end.
- **Jira**: [IGDD-3582](https://izgateway.atlassian.net/browse/IGDD-3582).
