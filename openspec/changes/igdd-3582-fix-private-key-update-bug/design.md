---
schema_version: '1.0'
created:
  date: '2026-10-14T20:13:59.577Z'
  user: boonek
  agent:
    name: claude-code
    version: '2.0'
  llm:
    name: claude-sonnet-5
    version: claude-sonnet-5
  prompt_uri: >-
    prompt:/claude-code/7b62e482-c255-4291-b97d-980994f07e6c/~db1fbf8e-b4dc-4285-bc3b-6630207c8d42
  summary: >-
    design.md for IGDD-3582: connector-customizer approach, izgw-hub cleanup
    scope, 3-checkpoint verification
updated:
  - date: '2026-10-14T23:16:16.419Z'
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
      Document the SSLImplementation correction in Decision 4: why hub's cleanup
      is still fully safe; Retry stale @TempDir reference fix that silently
      failed in background earlier
  - date: '2026-10-14T23:02:04.005Z'
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
      Document the concrete ticket-qualified SNAPSHOT versioning mechanism in
      Migration Plan
  - date: '2026-10-14T22:48:50.258Z'
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
      Correct design.md: only override server.ssl.*, not client.ssl.* which
      broke the Elastic status checker in CI; Correct design.md: manual temp dir
      creation, not @TempDir annotation, due to DynamicPropertySource timing;
      Correct HTTP method: /rest/trust?reload=true is GET, not POST
  - date: '2026-10-14T22:27:59.529Z'
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
      Rewrite Decisions 5-7 to document the actual JUnit-based implementation
      (TlsCertHotReloadTests) replacing the earlier bash/curl/openssl script
      design
  - date: '2026-10-14T21:56:34.964Z'
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
      Decision 6: generate test keystores and client cert fresh every run
      instead of checking in static fixtures that would eventually expire;
      Decision 5 lifecycle: generate keystores into scratch dir directly, not
      copied from static fixtures
  - date: '2026-10-14T21:54:19.179Z'
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
      Design Decision 6: document the actual self-contained baseline+rotated
      keystore pair generated, decoupled from COMMON_PASS; Design Decision 5
      lifecycle steps: reference the actual initial/rotated fixture filenames;
      Design Decision 6: document the admin client cert and mTLS trust setup
      needed for /rest/trust
  - date: '2026-10-14T20:29:37.056Z'
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
      Design: replace Newman collection with curl+openssl single script; Design:
      update file-update rationale to reference curl instead of Newman; Design:
      replace Newman step reference with curl in the lifecycle list; Design:
      Decision 7 now names a single curl/openssl script, no Postman collection
  - date: '2026-10-14T20:18:53.717Z'
    user: boonek
    agent:
      name: claude-code
      version: '2.0'
    llm:
      name: claude-sonnet-5
      version: claude-sonnet-5
    prompt_uri: >-
      prompt:/claude-code/7b62e482-c255-4291-b97d-980994f07e6c/~a3e5e581-0a0a-40a3-af83-25f0958ee909
    summary: >-
      Drop Open Questions section - neither item was a genuine unresolved
      question
  - date: '2026-10-14T20:14:47.236Z'
    user: boonek
    agent:
      name: claude-code
      version: '2.0'
    llm:
      name: claude-sonnet-5
      version: claude-sonnet-5
    prompt_uri: >-
      prompt:/claude-code/7b62e482-c255-4291-b97d-980994f07e6c/~db1fbf8e-b4dc-4285-bc3b-6630207c8d42
    summary: >-
      Resolve how the verification test actually updates the keystore file
      (local ephemeral instance, not shared env)
change_request: igdd-3582-fix-private-key-update-bug
ticket: IGDD-3582
document_type:
  - openspec-design
---
# Design

## Context

See `proposal.md` - Why for the root-cause narrative. Current state, in brief:
`ClientTlsSupport` (izgw-core) already has a background scheduler, change-detection
logic, and an `SslReloader` helper whose `reloadSsl()` calls
`AbstractHttp11Protocol.reloadSslHostConfigs()` — but `SslReloader.setProtocol(...)`
is never called, so `reloadSsl()` is permanently a no-op. `izgw-hub` separately has
its own parallel, equally dead `Application.protocol` / `Application.reloadSsl()` /
`reloadConnectorCustomizer` bean — a second, independent, never-invoked attempt at
the same thing. Spring Boot's `TomcatServletWebServerFactory` already collects every
`TomcatConnectorCustomizer` bean present in the application context via
`ObjectProvider<TomcatConnectorCustomizer>` and applies all of them
(`izgw-hub/Application.java:521`, `factory.getConnectorCustomizers().addAll(...)`).

## Goals / Non-Goals

**Goals:**
- Wire `SslReloader.setProtocol(...)` to the real embedded HTTPS connector's
  protocol handler, automatically, with zero required code in any consuming repo.
- Remove `izgw-hub`'s redundant dead code with no behavior change.
- Produce one repeatable regression test (Newman collection + companion script)
  that demonstrably fails before the fix and passes after it, runnable at the
  three checkpoints agreed on (see Decisions).

**Non-Goals:**
- Changing `KeyStoreLoader.isOutOfDate()` or its `File.lastModified()`-based
  change-detection logic — dropped from scope entirely (unconfirmed as relevant;
  see proposal history).
- Changing the client-side keystore/truststore reload path used for outbound calls
  (`ClientTlsSupport.getSSLContext()` / `getSNIEnabledConnection()`) — already
  correct today (see `tls-cert-hot-reload` spec, "Outbound client TLS behavior is
  unaffected").
- Adopting Spring Boot 4's native SSL-bundle hot-reload as a replacement mechanism
  (considered and rejected for this CR — see Decisions).
- Reconciling `izgw-core`'s other stale/un-archived OpenSpec changes — tracked
  separately in `projects-in-progress.md`.

## Decisions

### 1. The fix lives entirely in `izgw-core`, as a self-registering bean
Add `@Bean public TomcatConnectorCustomizer sslReloadConnectorCustomizer()` directly
inside `ClientTlsSupport`. Because Spring Boot already gathers every
`TomcatConnectorCustomizer` bean from the context (confirmed call site above), a
bean defined in `izgw-core` is picked up automatically by any consumer with an
embedded Tomcat servlet container — no consumer-side registration needed.
**Alternative considered:** keep requiring each consumer to explicitly call
`SslReloader.setProtocol(...)` from its own customizer. Rejected — that's the exact
per-consumer manual-wiring pattern that silently went unwired for over two years;
moving it into the library removes the possibility of a future consumer forgetting
to wire it again.

### 2. Reuse the existing `SslReloader` mechanism; don't replace it
Keep `ClientTlsSupport.SslReloader` and its existing trust-changed-listener
registration in `afterPropertiesSet()` as-is; add only the missing
`setProtocol(...)` call. The scheduler/listener machinery itself already works
correctly — this is how we diagnosed the bug in the first place (the client-side
store, served by this same machinery, showed the correct rotated certificate via
`/rest/trust`). **Alternative considered:** adopt Spring Boot 4's native SSL-bundle
hot-reload (`spring.ssl.bundle...reload-on-update=true`), replacing this mechanism
outright. Rejected for this CR — it only covers the embedded-server side (not the
client-side outbound path), requires migrating every consumer's `server.ssl.*`
config to the SSL-bundle style, and needs separate verification against this
project's BCFKS/BC-FIPS provider setup before it could be trusted. Worth a future,
separately-scoped CR; not pursued here to keep this fix minimal and scoped to the
confirmed bug.

### 3. Protocol-handler capture pattern matches Spring Boot's own fix for this exact break
Guard on `"https".equals(connector.getScheme())`, then
`connector.getProtocolHandler() instanceof AbstractHttp11Protocol<?> jsse` before
calling `SslReloader.setProtocol(jsse)` — the same cast `izgw-hub`'s (soon-removed)
`Application.customizeConnector()` already used, and the same widening Spring Boot
itself adopted in 3.4.2 for the identical Tomcat 11 removal of
`AbstractHttp11JsseProtocol` (spring-boot#43849). Reusing an already-validated
pattern rather than inventing a new one.

### 4. `izgw-hub` cleanup scope is narrow and mechanical — with one correction
Remove exactly: the `protocol` field, `reloadSsl()` method, `customizeConnector()`
method, and the `reloadConnectorCustomizer` `@Bean` method in `Application.java`,
plus any imports that become unused as a result. Leave the rest of the Tomcat
factory wiring (`getConnectorCustomizers().addAll(...)` and friends) untouched —
that machinery is exactly what picks up `izgw-core`'s new bean.

**Correction found while implementing Decision 1's bean (not while planning
this one):** `customizeConnector()` is not purely dead code — alongside the
dead `Application.protocol = jsse` assignment, it also calls
`jsse.setSslImplementationName(SSLImplementation.class.getName())`, the only
place that wires the server's connector to this library's OCSP-aware
`SSLImplementation`/`RevocationTrustManager` for inbound client-cert
validation. Deleting the whole method as originally scoped would have quietly
regressed that live behavior. Since `SSLImplementation` already lives in
`izgw-core` (same package as `ClientTlsSupport`), `sslReloadConnectorCustomizer()`
absorbs both responsibilities — it sets the SSL implementation name *and*
registers the reload hook — so `izgw-hub`'s version really is now fully,
safely removable exactly as this decision originally described; the removal
scope itself didn't change, only the justification for why it's safe.

### 5. Verification is a JUnit test, not a script — modeled on `ApplicationTests`
`izgw-hub` already has a proven pattern for booting the *real* application in a
test (`ApplicationTests`, `@SpringBootTest(webEnvironment = RANDOM_PORT,
useMainMethod = UseMainMethod.ALWAYS)`, full `@ComponentScan`), including real
Tomcat/SSL wiring. The new regression test, `TlsCertHotReloadTests`, follows the
exact same pattern rather than shelling out to `curl`/`openssl`/`keytool` from a
bash script: a real embedded Tomcat really starts, with the real
`ClientTlsSupport`/`SslReloader` wiring under test, and the test drives it
entirely in-process via the JDK's own `HttpsURLConnection`/`SSLSession` APIs.
This was a direct pivot away from an earlier bash-script design once attempting
to actually run it locally surfaced that booting the real application (needed
either way, to exercise the real Tomcat wiring) pulls in `RevocationChecker` →
`CertificateStatusService` → a real DynamoDB client — a requirement that exists
regardless of script-vs-JUnit, but a JUnit test lets `izgw-hub` reuse exactly the
mechanism `ApplicationTests` already has for it (see Risks: this test needs the
same real AWS/Elastic access `ApplicationTests` already requires — confirmed by
attempting a local run, which aborts the JVM via `Application.main()`'s own
`System.exit(1)` on the resulting `BeanCreationException`, not a bug in the new
test).

Sequence, performed inside the single `@Test` method:
1. `@DynamicPropertySource` (runs once, before context startup) generates two
   self-signed server keystores and one self-signed admin client cert — all
   pure Java via BC-FIPS (`TlsTestKeystoreFactory`, backed by
   `JcaX509v3CertificateBuilder`/`JcaContentSignerBuilder`, no `keytool`/`openssl`
   subprocess) — and overrides only `server.ssl.key-store`/`trust-store` to
   point at the one generated baseline keystore file in a manually-created temp
   directory, with a harness-local password. Deliberately *not* overriding
   `client.ssl.*`: that governs the application's own outbound calls to other
   services (confirmed the hard way — overriding it broke
   `ElasticStatusRepository`'s unrelated background status check, which the
   app treated as fatal). `client.ssl.*` keeps using its real, CI-provided
   configuration, same as `ApplicationTests` already relies on.
2. The real application boots against that file (same mechanism `ApplicationTests`
   already relies on for everything else, including whatever lets it avoid
   needing a *second* real environment's worth of config).
3. Connect over HTTPS to the running embedded server (`local.server.port`) and
   capture the presented certificate's serial via
   `HttpsURLConnection.getServerCertificates()` — the baseline.
4. **Rotate**: atomically replace the keystore file (write-to-temp-name, then
   `Files.move(..., ATOMIC_MOVE, REPLACE_EXISTING)`) with a second, freshly
   generated keystore (same DN, new key pair/serial) — a plain local file
   operation, since the JUnit process itself owns the temp directory.
5. Call `GET /rest/trust?reload=true` (mTLS, presenting the generated admin
   client cert) to force immediate change-detection, exactly as a real operator
   forcing a reload would — this calls `tlsSupport.updateTrust()` directly
   rather than waiting out the scheduler's monitoring period.
6. Reconnect and re-capture the served certificate's serial; assert it now
   matches the rotated keystore's serial (not just "changed," to rule out a
   false pass from an unrelated restart) — the behavior under test is reload
   *without* a restart.

Run manually at three points during implementation (evidence recorded in
`tasks.md`, not a feature the shipped test needs to preserve):
1. **Checkpoint 1 (red)** — current `develop` tip, both repos, unmodified → expect
   the post-rotation serial to still match the *baseline* (reproduces the bug).
2. **Checkpoint 2 (green)** — `izgw-hub` built against the fixed `izgw-core`
   snapshot, `Application.java` not yet cleaned up → expect it to match the
   *rotated* serial.
3. **Checkpoint 3 (green)** — same, plus the `izgw-hub` dead-code removal applied
   → expect it to still match the rotated serial (proves the cleanup didn't
   regress anything).

Because none of this depends on filesystem semantics outside the JVM's own temp
directory, there's no cross-platform file-locking/atomic-rename risk to manage
separately (`Files.move` with `ATOMIC_MOVE` behaves consistently across the
Windows dev loop and Linux CI containers).

### 6. Test key material is generated fresh on every run, never checked in
No new binary fixture files are added to `conf/ssl/`, and nothing is written
outside a temp directory created and cleaned up by the test itself (`Files.createTempDirectory`,
not JUnit's `@TempDir` annotation — that field-injection extension runs too late
to be available to `@DynamicPropertySource`, which executes earlier during
Spring's context-preparation phase; confirmed by an earlier version of this test
writing its keystore into the project's working directory instead). Checking in
static pre-generated keystores would
just reintroduce this CR's own bug class into the test itself — a fixed-validity
cert that, given enough time, expires and starts failing the test for the wrong
reason, with nobody noticing until it does.

- `TlsTestKeystoreFactory` generates two independent key pairs/self-signed certs
  (RSA 2048 / SHA256withRSA, 30-day validity — short is fine, nothing persists
  past the run), both with the same DN as the real `awsdev_keystore.bcfks`
  fixture (`CN=dev.izgateway.org, OU=Federal and Strategic Advisory, O=Audacious
  Inquiry, L=Baltimore, ST=Maryland, C=US`; the real fixture's `EMAILADDRESS`
  attribute is omitted as not behaviorally relevant), so each run gets fresh,
  distinct serials.
- Both share one harness-local, intentionally trivial password (`password`) —
  used only within that run's temp directory, never the real `awsdev_keystore.bcfks`'s
  `COMMON_PASS` (a CI-only secret this test has no business needing).
- `TlsTestKeystoreFactoryTests` is a standalone JUnit test (no Spring context, no
  AWS dependency) that exercises the factory directly — generate, write, reload,
  and compare — so the cert-generation logic itself has fast, independently
  verifiable local coverage even though the full integration test cannot be run
  without real AWS/Elastic access.

**Admin client trust (mutual TLS):** `server.ssl.client-auth: need` means the
embedded server also requires and validates a client certificate on every
connection, including the test's own call to `/rest/trust?reload=true`.
`CertificatePrincipalProviderImpl` reads that client cert's CN as the principal
name, and the H2/JPA test seed data (`src/test/resources/data.sql`) grants the
`admin` group to any CN matching `*.testing.izgateway.org` — so a client cert
with that CN suffix gets ADMIN access under the test backend with no extra role
wiring. `TlsTestKeystoreFactory` generates one such self-signed client cert/key
pair (CN `tls-hot-reload-test.testing.izgateway.org`) and its public cert is
added as a `trustedCertEntry` to *both* generated server keystores — the same
client stays trusted across the simulated rotation, exactly as a real admin's
client cert would not need to change just because the server renewed its own.
The client key/cert are held purely in memory (an in-memory PKCS12 `KeyStore`,
never written to disk) and used to build the `SSLSocketFactory` passed to
`HttpsURLConnection` directly.

### 7. Verification artifact location
Three new classes under `izgw-hub/src/test/java/gov/cdc/izgateway/`:
`TlsTestKeystoreFactory`, `TlsTestKeystoreFactoryTests`, and
`TlsCertHotReloadTests`. No shell script, no Postman collection. Because the
class name matches the project's `**/*Tests.java` Surefire include pattern, it
runs automatically as part of the existing `mvn clean package`/CI build —
alongside, but independent of, the existing
`testing/scripts/IZGW_2.0_Integration_Test.postman_collection.json` Newman
suite, which it does not touch.

## Risks / Trade-offs

- **[Risk]** Removing `izgw-hub`'s dead `Application.protocol`/`reloadSsl()` could
  theoretically miss some other caller → **Mitigation:** already grep-confirmed
  zero other references repo-wide; full existing Newman suite plus this CR's new
  test must stay green after removal (checkpoint 3 explicitly re-proves it).
- **[Risk]** A future consumer of `izgw-core` could add its own redundant
  `TomcatConnectorCustomizer` calling `SslReloader.setProtocol(...)` again →
  **Mitigation:** harmless — the setter is a plain last-write-wins static field;
  redundant calls from multiple customizers don't conflict.
- **[Risk]** The dropped `KeyStoreLoader.isOutOfDate()` mtime-staleness concern
  could still be a real, separate bug → **Mitigation:** explicitly not addressed
  here; if it resurfaces, it gets diagnosed and scoped on its own, not folded into
  this fix after the fact.

## Migration Plan

- Code-only change. No data migration, no new config/env vars, no schema changes.
- Versioning during this CR's verification: `izgw-core`'s `pom.xml` follows an
  existing documented convention (`pom.xml` lines 13-15) where a working branch
  is versioned `<major>.<minor>.<patch>-IGDD-<ticket#>_<ticket-title>-SNAPSHOT`.
  This branch is bumped to `3.9.1-IGDD-3582_Fix_Private_Key_Update_Bug-SNAPSHOT`
  (develop is currently `3.9.0-SNAPSHOT`). `izgw-core`'s CI (`maven.yml`) runs
  `mvn ... deploy` on every `push` *and* `pull_request` against `develop`, so
  opening a PR for this branch publishes that exact coordinate to GitHub
  Packages — uniquely qualified, so it can't collide with `develop`'s own
  SNAPSHOT or any other ticket branch's build. `izgw-hub` pins an explicit
  released `izgw-core` version (`3.8.0`), not a floating SNAPSHOT, so a local
  `mvn install` can never substitute for this — CI always resolves fresh from
  GitHub Packages.
- Deploy order (matches this project's established cross-repo pattern, e.g.
  `IGDD-3089`): merge `izgw-core` first, then point `izgw-hub` at the real
  released version (not the ticket-qualified verification SNAPSHOT, which gets
  reset once `izgw-core`'s fix is actually merged/released) before merging the
  `izgw-hub` cleanup + verification-test PR.
- Rollback: the two PRs can be reverted independently if needed — the removed
  `izgw-hub` dead code was never reachable, so reverting just the `izgw-core`
  change (if a problem is found there) doesn't require reverting the `izgw-hub`
  cleanup, and vice versa. Reviewed and merged together under this CR regardless.
