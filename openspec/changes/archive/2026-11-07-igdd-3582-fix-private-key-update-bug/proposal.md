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
  - date: '2026-10-19T14:34:43.818Z'
    user: boonek
    agent:
      name: claude-code
      version: '2.0'
    llm:
      name: claude-sonnet-5
      version: claude-sonnet-5
    prompt_uri: >-
      prompt:/claude-code/7b62e482-c255-4291-b97d-980994f07e6c/~77f4fc9a-9006-4c57-9c5e-372b98907ac2
    summary: >-
      Correct proposal.md Why section: reload-on-update also doesn't work, due
      to inotify not detecting EFS cross-client changes; Correct proposal.md
      What Changes: document the poller-based reload mechanism replacing
      reload-on-update; Correct Consumers impact bullet: consumers also need
      their own scheduled invocation, not just bundle config; Correct Impact
      section Code bullets with the full set of files actually changed
  - date: '2026-10-15T00:50:59.945Z'
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
      Rewrite proposal.md to describe the SslBundle/reload-on-update fix
      replacing the architecturally-insufficient SslReloader approach, and
      correct the now-false zero-consumer-action claim; Update proposal.md
      Impact section to describe the application.yml SslBundle migration and
      corrected izgw-core change
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

**Confirmed while implementing the straightforward fix (wiring `setProtocol(...)`):**
`reloadSslHostConfigs()` could never have worked here regardless of wiring. Spring
Boot's own `SslConnectorCustomizer` always configures the embedded Tomcat
connector's certificate from an in-memory `KeyStore` object built once at startup
(`certificate.setCertificateKeystore(stores.getKeyStore())`), never from a file
path — `reloadSslHostConfigs()` just re-applies that same stale in-memory object.
The fix has to use Spring Boot's own `SslBundle` machinery (named bundles), the
only mechanism Spring Boot wires an update handler for at all.

**Confirmed while implementing *that* fix: Spring Boot's own `reload-on-update`
(the obvious way to drive that bundle update) also does not work here.** It is
backed by `java.nio.file.WatchService` — inotify on Linux — which cannot detect a
file change made by a different client of the same network filesystem. This
application's keystore lives on an AWS EFS (NFSv4.1) volume in every real
deployment (confirmed directly against `iz-gateway-terraform`'s ECS task
definition), written to by a process other than the Hub container's own JVM. The
actual reload mechanism instead polls the bundle's keystore/truststore file mtime
and pushes the update itself — the same technique already proven correct on this
application's EFS mount for the outbound client TLS path — while still reusing
Spring Boot's own bundle-update wiring to reach the live connector.

## What Changes

- Remove `izgw-core`'s now-fully-dead `SslReloader` mechanism (it could never have
  worked under Spring Boot 4, independent of the missing `setProtocol(...)` wiring
  — see Why). Keep only the one part of its `TomcatConnectorCustomizer` bean that
  was never about reload: wiring the connector's protocol handler to this
  library's OCSP-aware `SSLImplementation` for inbound client-cert validation
  (renamed `sslImplementationConnectorCustomizer()`).
- Migrate `izgw-hub`'s `server.ssl.key-store`/`trust-store` properties to a named
  Spring Boot SSL bundle (`server.ssl.bundle: izgw-server` +
  `spring.ssl.bundle.jks.izgw-server.*`, `reload-on-update: false`). This
  migration is required regardless: Spring Boot only wires a bundle-update
  handler for named bundles at all, never for legacy `server.ssl.key-store`-style
  properties. `reload-on-update` itself is left disabled because its
  `WatchService`-based detection does not work on this application's EFS-mounted
  keystore (see Why).
- Add `ServerSslBundleReloader` (izgw-core): polls the named bundle's
  keystore/truststore file mtime and calls `SslBundleRegistry.updateBundle(...)`
  directly when it changes — reusing Spring Boot's own bundle-update wiring, just
  with a detection mechanism proven to work on this application's actual
  filesystem. `izgw-hub` adds `ServerSslBundleReloadScheduler`, a `@Scheduled`
  bean that owns the polling cadence and invokes it; `GET /rest/trust?reload=true`
  also invokes it directly, for an immediate, on-demand reload.
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

- **Code (`izgw-core`)**: `ClientTlsSupport.java` — removes the dead `SslReloader`
  class and simplifies its `TomcatConnectorCustomizer` bean to just the
  `SSLImplementation` wiring. New `ServerSslBundleReloader.java` — the poller that
  actually drives server-bundle reload. `TrustManagerProvider.java` — sources its
  trust store from the same named bundle, with a legacy-property fallback.
  `TrustController.java` — `reload=true` also invokes the new reloader.
  `RevocationChecker.java` — `@Lazy(false)`, to keep OCSP eager-init working now
  that the old (unused) dependency that incidentally forced it is gone. No
  API/contract changes, no breaking changes, no new runtime dependencies.
- **Code (`izgw-hub`)**: `Application.java` — remove the dead `protocol` field,
  `reloadSsl()` method, `customizeConnector()` method, and
  `reloadConnectorCustomizer` bean. `application.yml` — migrate
  `server.ssl.key-store`/`trust-store`/`*-password`/`*-provider`/`*-type` and the
  protocol/cipher allowlist to a named `spring.ssl.bundle.jks.izgw-server` bundle
  (`reload-on-update: false` — see Why); no change to `enabled`/`client-auth`. New
  `ServerSslBundleReloadScheduler.java` — the `@Scheduled` bean that invokes the
  core reloader. Adds test classes under `src/test/java/gov/cdc/izgateway/`:
  `TlsTestKeystoreFactory` (ephemeral BC-FIPS cert/keystore generation helper),
  `TlsTestKeystoreFactoryTests` (standalone unit test of that helper, no AWS
  dependency), and `TlsCertHotReloadTests` (the regression test, forcing reload
  via `GET /rest/trust?reload=true`).
- **Consumers**: any embedded-Tomcat app built on `izgw-core` still needs to (1)
  configure its own `server.ssl.*` as a named Spring Boot SSL bundle, and (2)
  schedule its own periodic call to `ServerSslBundleReloader.reloadIfChanged()`
  (e.g. a one-line `@Scheduled` bean, matching `izgw-hub`'s
  `ServerSslBundleReloadScheduler`) to get this behavior — neither is automatic,
  since bundle configuration is Spring Boot's own property-based
  auto-configuration and polling cadence is a deployment decision, not anything
  `izgw-core` can inject on a consumer's behalf. `izgw-hub`'s own migration is in
  scope as part of this CR (see Code impact above); `izgw-transform` (a second,
  older-pinned consumer with the same legacy configuration and dead-code pattern)
  is explicitly out of scope, tracked separately. The `Application.java`
  dead-code removal remains in scope by choice, not necessity.
- **Branches**: `IGDD-3582_Fix_Private_Key_Update_Bug` in both `izgw-core` and
  `izgw-hub`. Verification requires building `izgw-hub` against this `izgw-core`
  branch's snapshot (e.g. local `mvn install` of `izgw-core`, then building `izgw-hub`
  against that snapshot version) to exercise the fix end-to-end.
- **Jira**: [IGDD-3582](https://izgateway.atlassian.net/browse/IGDD-3582).
