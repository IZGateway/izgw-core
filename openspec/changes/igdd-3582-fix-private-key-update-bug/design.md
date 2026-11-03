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
  - date: '2026-10-19T14:31:54.543Z'
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
      Rewrite design.md: document the EFS/inotify discovery that invalidated
      reload-on-update, and the
      ServerSslBundleReloader/ServerSslBundleReloadScheduler poller replacing it
  - date: '2026-10-15T00:45:48.161Z'
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
      Rewrite design.md to describe the SslBundle/reload-on-update mechanism
      replacing the architecturally-insufficient
      SslReloader/reloadSslHostConfigs approach
  - date: '2026-10-14T23:39:37.385Z'
    user: boonek
    agent:
      name: claude-code
      version: '2.0'
    llm:
      name: claude-sonnet-5
      version: claude-sonnet-5
    prompt_uri: >-
      prompt:/claude-code/7b62e482-c255-4291-b97d-980994f07e6c/~c595235b-b98f-4f25-aece-579343c5c226
    summary: >-
      Document the client.ssl.key-store fix: updateTrust() only checks
      client.ssl.*, so rotating only server.ssl.* was invisible to it
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

See `proposal.md` for the root-cause narrative. Current state, in brief:
`ClientTlsSupport` (izgw-core) has a background scheduler, change-detection logic,
and an `SslReloader` helper whose `reloadSsl()` calls
`AbstractHttp11Protocol.reloadSslHostConfigs()` — but `SslReloader.setProtocol(...)`
is never called, so `reloadSsl()` is permanently a no-op. `izgw-hub` separately has
its own parallel, equally dead `Application.protocol` / `Application.reloadSsl()` /
`reloadConnectorCustomizer` bean — a second, independent, never-invoked attempt at
the same thing.

**A deeper problem surfaced while implementing and testing the `SslReloader` fix
directly:** even with `setProtocol(...)` correctly wired, `reloadSslHostConfigs()`
cannot work under Spring Boot 4. Spring Boot's own
`org.springframework.boot.tomcat.SslConnectorCustomizer` always configures the
Tomcat connector's `SSLHostConfigCertificate` via
`certificate.setCertificateKeystore(stores.getKeyStore())` — an in-memory `KeyStore`
object built once at startup, never a file path. `reloadSslHostConfigs()` just
re-applies that same in-memory object; it has no way to re-read the keystore file
from disk. The `SslReloader`/connector-customizer approach, however correctly
wired, was never going to fix this bug in production — only in a test that doesn't
actually exercise Spring Boot 4's real SSL configuration path faithfully enough to
notice.

**First replacement mechanism considered, then also found insufficient:** Spring
Boot's own `SslBundle` machinery (`org.springframework.boot.ssl`, since 3.1.0).
`TomcatWebServerFactory.customizeSsl()` wires an
`SslBundles.addBundleUpdateHandler(name, ...)` when the connector is configured via
a **named bundle** (`server.ssl.bundle: <name>` + `spring.ssl.bundle.jks.<name>.*`).
Setting `spring.ssl.bundle.jks.<name>.reload-on-update: true` causes
`SslPropertiesBundleRegistrar` to register a `FileWatcher` — a real NIO
`WatchService` on the keystore/truststore's parent directory — that rebuilds the
bundle and calls `SslBundleRegistry.updateBundle(name, freshBundle)` automatically
whenever the watched directory reports a change.

**That mechanism does not work in this application's real deployment.** `WatchService`
on Linux is backed by inotify, which operates at the local VFS layer and has no
visibility into NFS protocol traffic — it cannot detect a file change made by a
*different* client of the same network filesystem, because NFS does not push change
notifications to other clients' kernels. Confirmed against this application's actual
AWS configuration, not just general NFS behavior: `iz-gateway-terraform/hub/service/ecs.tf`
mounts the Hub container's `/usr/share/phiz-web-ws/conf` (where
`security.ssl-path`/`awsdev_keystore.bcfks` live) from an `efs_volume_configuration`
block (`file_system_id = "fs-0c76fe796cfc1d1e8"`, `access_point_id =
"fsap-0c8cf40dbde770a68"` — the same EFS volume `izgw-hub/.claude/CLAUDE.md` already
documents). AWS EFS uses NFSv4.1; research confirms AWS has not finished
implementing NFSv4.1 delegation/callback support for Linux clients on EFS, so
`reload-on-update`'s `FileWatcher` would very likely never fire for a rotation
performed by whatever external process actually writes the renewed certificate onto
EFS (almost certainly not the Hub container's own JVM). This is not a multi-task
fan-out problem either — `terraform.tfvars` sets `ecs_desired_count = 1` — it is
simpler and worse: even a single Hub task's `WatchService` would not see a write
made by a different EFS client.

**The actual fix reuses the mechanism already proven correct on this exact
filesystem.** `ClientTlsSupport`'s existing outbound-client reload path (the one
that was *already* correctly picking up a rotated certificate — this is how this
bug was first diagnosed: `/rest/trust`'s client-side report showed the correct,
rotated certificate while the server's actual TLS handshake did not) works by
polling file modification time (`KeyStoreLoader.isOutOfDate()`), not by any
filesystem-event API. Polling mtime works identically whether the file lives on
local disk or EFS, because it is just a stat() call re-issued on a timer, not a
push notification. The fix for the server listener applies the same technique: a
new `ServerSslBundleReloader` (izgw-core) polls the bundle's keystore/truststore
mtime and, on a change, rebuilds the bundle and calls
`SslBundleRegistry.updateBundle(...)` directly — bypassing `reload-on-update`
entirely (disabled in `application.yml`) but reusing 100% of its downstream
machinery: `DefaultSslBundleRegistry` is an ordinary injectable bean implementing
both `SslBundleRegistry` and `SslBundles`, and `TomcatWebServerFactory.customizeSsl()`
registers its connector-update handler on that registry at startup independent of
who calls `updateBundle(...)` — Spring's own `FileWatcher` was always just one
caller of a public API, not something with privileged access to the connector.

Spring Boot's `TomcatServletWebServerFactory` already collects every
`TomcatConnectorCustomizer` bean present in the application context via
`ObjectProvider<TomcatConnectorCustomizer>` and applies all of them
(`izgw-hub/Application.java:521`, `factory.getConnectorCustomizers().addAll(...)`).
That collection point is unrelated to bundle reload (it runs once, at connector
creation) but remains the correct way to wire this library's OCSP-aware
`SSLImplementation` into the connector's protocol handler — a separate concern
this CR must not regress while removing the ineffective reload code.

## Goals / Non-Goals

**Goals:**
- Make the embedded Tomcat HTTPS listener pick up a rotated server keystore file
  without a process restart, **in this application's real deployment** (AWS EFS),
  not just in a test running against local disk.
- Remove the now-fully-dead `SslReloader` mechanism from `izgw-core` (confirmed
  architecturally incapable of working under Spring Boot 4's in-memory-`KeyStore`
  connector configuration, independent of whether `setProtocol(...)` is wired).
- Remove `izgw-hub`'s redundant dead code with no behavior change.
- Produce one repeatable regression test (`TlsCertHotReloadTests`) that
  demonstrably fails before the fix and passes after it.

**Non-Goals:**
- Changing `KeyStoreLoader.isOutOfDate()` or its `File.lastModified()`-based
  change-detection logic — dropped from scope entirely (unconfirmed as relevant;
  see proposal history). It remains in use only for the still-unrelated,
  already-correct client-side outbound reload path below, and its pattern is what
  the new `ServerSslBundleReloader` reuses.
- Changing the client-side keystore/truststore reload path used for outbound calls
  (`ClientTlsSupport.getSSLContext()` / `getSNIEnabledConnection()` /
  `updateTrust()`) — already correct today, and not driven by `SslBundle` at all
  (it builds its own `SSLContext` directly from the keystore files for outbound
  HTTP calls, not through Tomcat). See `tls-cert-hot-reload` spec, "Outbound
  client TLS behavior is unaffected."
- Giving `izgw-transform` (a second, currently older-pinned consumer of
  `izgw-core`, also on legacy `server.ssl.*` properties with the same dead
  `customizeConnector`/`protocol` duplication izgw-hub had) this same reload
  capability — tracked separately; out of scope for this CR.
- Reconciling `izgw-core`'s other stale/un-archived OpenSpec changes — tracked
  separately in `projects-in-progress.md`.

## Decisions

### 1. Named `SslBundle` for configuration, but `reload-on-update: false`
Migrate `izgw-hub`'s `server.ssl.key-store`/`trust-store`/`*-password`/`*-provider`/
`*-type` properties to a named bundle (`spring.ssl.bundle.jks.izgw-server`), and
point `server.ssl.bundle: izgw-server` at it — this part is unchanged from the
original plan, and still required: it is the only way to get `TomcatWebServerFactory`
to register an update handler on the bundle registry at all (see Context).
`reload-on-update` itself is set to `false`: its `FileWatcher` does not work on
this application's EFS-mounted keystore (see Context), so leaving it enabled would
be dead weight at best. `server.ssl.enabled`, `client-auth`, and the TLS
protocol/cipher allowlist (moved into the bundle's own `options` — see Decision 2)
are unaffected by this change.

### 2. Bundle `options` must carry the protocol/cipher allowlist, not `server.ssl.*`
Found during Copilot review: once `server.ssl.bundle` names a bundle,
`SslConnectorCustomizer.applySslBundle()` reads enabled protocols and ciphers
*exclusively* from that bundle's own `options` — it never falls back to
`server.ssl.enabled-protocols`/`server.ssl.ciphers`. Leaving the existing TLSv1.2/
TLSv1.3-only, curated-cipher-list properties under `server.ssl.*` (as the original
migration did) would have them silently ignored, weakening the listener to Tomcat/
provider defaults with no startup error to surface it. Fixed by moving the
`enabled-protocols`/`ciphers` lists into
`spring.ssl.bundle.jks.izgw-server.options.*`.

### 3. `ServerSslBundleReloader` (izgw-core) + `ServerSslBundleReloadScheduler` (izgw-hub)
The actual reload mechanism (see Context for why `reload-on-update` was rejected).
Split across repos deliberately:
- **`ServerSslBundleReloader`** (izgw-core, `gov.cdc.izgateway.security`) is a plain,
  schedule-agnostic class exposing `reloadIfChanged()`: reads `server.ssl.bundle`
  (no-ops if blank — legacy, non-bundle configuration has no reload path, by
  design), polls the named bundle's keystore/truststore `location` mtime via
  `Environment`, and on a change, builds a fresh `JksSslStoreBundle`/`SslBundle`
  (preserving the current bundle's `key`/`options`/`protocol`, which don't change
  between rotations) and calls `SslBundleRegistry.updateBundle(name, freshBundle)`.
  Covered by focused Mockito unit tests (`ServerSslBundleReloaderTests`) — no Spring
  context needed.
- **`ServerSslBundleReloadScheduler`** (izgw-hub, `gov.cdc.izgateway.hub.security`)
  owns the scheduling policy: a `@Scheduled` bean (same shape as the existing
  `GracePeriodRevocationScheduler`) that calls `reloader.reloadIfChanged()` on a
  fixed delay (`server.ssl.bundle-reload.interval`, default `PT10S`).

**Why split this way, rather than putting the scheduling inside `ClientTlsSupport`'s
existing `ScheduledExecutorService` (which already runs in izgw-core):** a shared
library consumed by multiple applications (izgw-hub, and potentially izgw-transform
later) should provide the reusable *mechanism*, not unilaterally decide *when* (or
whether) it runs for every consumer — polling cadence is a deployment policy
decision for the application, not something the library should dictate. This also
keeps the new class trivially unit-testable (call `reloadIfChanged()` directly, no
scheduler/thread involved) and avoids any coupling to `ClientTlsSupport`'s
client-side scheduler, which exists for a different (though related) concern.
**Alternative considered:** reuse `ClientTlsSupport`'s existing
`trustChangedListeners` registration, adding this as another listener invoked by
its already-running scheduler. Rejected per the above — mixes a
library-internal scheduling decision with an application-level one, and
conflates two independently-evolving concerns (outbound client trust vs. the
server's own listener identity) that happen to share a physical file today but
aren't guaranteed to.

### 4. `GET /rest/trust?reload=true` also forces an immediate server bundle reload
`TrustController.getTrust(reload=true)` already force-reloads the *client-side*
trust material synchronously (via `touch()` + `ClientTlsSupport.updateTrust()`)
rather than waiting out that scheduler's own poll interval. Extended to also call
`ServerSslBundleReloader.reloadIfChanged()` in the same request — giving an
operator (or this CR's own test) a way to force the server listener's reload
immediately rather than waiting up to `server.ssl.bundle-reload.interval`. Composes
naturally with the existing `touch()` calls: in this application's actual
configuration, the client-side files `touch()` already bumps are the same physical
file the `izgw-server` bundle watches, so by the time `reloadIfChanged()` runs, it
already observes a changed mtime.

### 5. Remove `SslReloader` entirely; keep only the `SSLImplementation` wiring
Delete `ClientTlsSupport.SslReloader` and its `addSslTrustChangedListener(
SslReloader::reloadSsl)` registration. Simplify the `TomcatConnectorCustomizer`
bean (renamed `sslImplementationConnectorCustomizer()`, from
`sslReloadConnectorCustomizer()`) down to only
`jsse.setSslImplementationName(SSLImplementation.class.getName())` — the one part
of the old customizer unrelated to reload, still needed for OCSP-aware revocation
checking on inbound client certs.

### 6. `izgw-hub` cleanup scope is narrow and mechanical — with one correction
Remove exactly: the `protocol` field, `reloadSsl()` method, `customizeConnector()`
method, and the `reloadConnectorCustomizer` `@Bean` method in `Application.java`,
plus any imports that become unused as a result. Leave the rest of the Tomcat
factory wiring (`getConnectorCustomizers().addAll(...)` and friends) untouched —
that machinery is exactly what picks up `izgw-core`'s `SSLImplementation`-wiring
bean.

**Correction found while implementing Decision 5's original (now-superseded)
bean:** `customizeConnector()` is not purely dead code — alongside the dead
`Application.protocol = jsse` assignment, it also calls
`jsse.setSslImplementationName(SSLImplementation.class.getName())`, the only place
that wires the server's connector to this library's OCSP-aware
`SSLImplementation`/`RevocationTrustManager` for inbound client-cert validation.
Deleting the whole method as originally scoped would have quietly regressed that
live behavior. Since `SSLImplementation` already lives in `izgw-core` (same package
as `ClientTlsSupport`), `sslImplementationConnectorCustomizer()` absorbs that
responsibility, so `izgw-hub`'s version really is fully, safely removable exactly
as this decision describes.

**Second correction found via Copilot review:** removing `Application.customizeConnector()`
entirely also removed the only thing forcing Spring to eagerly construct the
`RevocationChecker` bean under this application's `spring.main.lazy-initialization:
true` (the `@Autowired RevocationChecker checker` constructor parameter on the old
`reloadConnectorCustomizer` bean was never used in the method body, but its mere
presence as a dependency forced eager construction). `RevocationChecker.getInstance()`
is a plain static accessor with no Spring context behind it; both
`RevocationTrustManager` and `CertificatePrincipalProviderImpl` silently treat a
`null` checker as "skip OCSP," with no error. Fixed by marking
`RevocationChecker` `@Lazy(false)` directly — tying the eager-init requirement to
the class that actually needs it, not to an unrelated consumer bean's incidental
constructor parameter.

### 7. Verification is a JUnit test, not a script — modeled on `ApplicationTests`
`izgw-hub` already has a proven pattern for booting the *real* application in a
test (`ApplicationTests`, `@SpringBootTest(webEnvironment = RANDOM_PORT,
useMainMethod = UseMainMethod.ALWAYS)`, full `@ComponentScan`), including real
Tomcat/SSL wiring. The regression test, `TlsCertHotReloadTests`, follows the exact
same pattern rather than shelling out to `curl`/`openssl`/`keytool` from a bash
script: a real embedded Tomcat really starts, with the real `izgw-server`
`SslBundle` under test, and the test drives it entirely in-process via the JDK's
own `HttpsURLConnection` APIs. Booting the real application (needed either way, to
exercise the real Tomcat wiring) pulls in `RevocationChecker` →
`CertificateStatusService` → a real DynamoDB client — a requirement that exists
regardless of script-vs-JUnit (see Risks: this test needs the same real AWS/Elastic
access `ApplicationTests` already requires).

Sequence, performed inside the single `@Test` method:
1. `@DynamicPropertySource` (runs once, before context startup) generates two
   self-signed server keystores and one self-signed admin client cert — all pure
   Java via BC-FIPS (`TlsTestKeystoreFactory`, backed by
   `JcaX509v3CertificateBuilder`/`JcaContentSignerBuilder`, no `keytool`/`openssl`
   subprocess) — and overrides `spring.ssl.bundle.jks.izgw-server.keystore.location`/
   `.password` and `.truststore.location`/`.password` (type/provider stay
   `bcfks`/`BCFIPS` from `application.yml`) to point at one generated baseline
   keystore file in a manually-created temp directory, plus
   `spring.ssl.bundle.jks.izgw-server.key.alias`. `client.ssl.*` is left completely
   untouched — that's a separate, hand-rolled property namespace `ClientTlsSupport`
   uses only for the application's own *outbound* calls (e.g.
   `ElasticStatusRepository`'s background status check), unrelated to
   `SslBundle`/Tomcat and already confirmed working; overriding it broke that
   unrelated subsystem in an earlier version of this test.
2. The real application boots against that file (same mechanism `ApplicationTests`
   already relies on for everything else). `reload-on-update: false` (production
   default) applies here too — nothing in the test re-enables Spring's own
   `FileWatcher`.
3. Connect over HTTPS to the running embedded server (`local.server.port`) and
   capture the presented certificate's serial via
   `HttpsURLConnection.getServerCertificates()` — the baseline.
4. **Rotate**: atomically replace the keystore file (write-to-temp-name, then
   `Files.move(..., ATOMIC_MOVE, REPLACE_EXISTING)`) with a second, freshly
   generated keystore (same DN, new key pair/serial) — a plain local file
   operation, since the JUnit process itself owns the temp directory.
5. **Force an immediate reload** via `GET /rest/trust?reload=true` (mTLS, admin
   client cert) — the same endpoint an operator would use (Decision 4). This call
   is synchronous: `ServerSslBundleReloader.reloadIfChanged()` runs inline in the
   request thread, so the live connector is already updated by the time the HTTP
   response returns. The test asserts the very next capture already matches the
   rotated serial, then re-confirms on a fresh connection shortly after (a short,
   bounded defensive poll — insurance against any subtle propagation nuance, not
   the primary detection mechanism; the natural background-scheduler path is
   covered by `ServerSslBundleReloaderTests` exercising the same
   `reloadIfChanged()` method directly, not by waiting out this test's own
   scheduler interval).
6. Assert the served certificate's serial matches the rotated keystore's exact
   serial (not just "changed," to rule out a false pass from an unrelated restart)
   — the behavior under test is reload *without* a restart.

### 8. Test key material is generated fresh on every run, never checked in
No new binary fixture files are added to `conf/ssl/`, and nothing is written
outside a temp directory created and cleaned up by the test itself
(`Files.createTempDirectory`, not JUnit's `@TempDir` annotation — that
field-injection extension runs too late to be available to `@DynamicPropertySource`,
which executes earlier during Spring's context-preparation phase; confirmed by an
earlier version of this test writing its keystore into the project's working
directory instead). Checking in static pre-generated keystores would just
reintroduce this CR's own bug class into the test itself — a fixed-validity cert
that, given enough time, expires and starts failing the test for the wrong reason,
with nobody noticing until it does.

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
connection, including the test's own call to `/rest/trust`.
`CertificatePrincipalProviderImpl` reads that client cert's CN as the principal
name, and the H2/JPA test seed data (`src/test/resources/data.sql`) grants the
`admin` group to any CN matching `*.testing.izgateway.org` — so a client cert with
that CN suffix gets ADMIN access under the test backend with no extra role wiring.
`TlsTestKeystoreFactory` generates one such self-signed client cert/key pair (CN
`tls-hot-reload-test.testing.izgateway.org`) and its public cert is added as a
`trustedCertEntry` to *both* generated server keystores — the same client stays
trusted across the simulated rotation, exactly as a real admin's client cert would
not need to change just because the server renewed its own. The client key/cert are
held purely in memory (BCFKS, never written to disk, provider pinned to BCJSSE to
avoid any ambiguity under this application's process-wide
`org.bouncycastle.fips.approved_only=true`) and used to build the `SSLSocketFactory`
passed to `HttpsURLConnection` directly.

### 9. Verification artifact location
Three classes under `izgw-hub/src/test/java/gov/cdc/izgateway/`:
`TlsTestKeystoreFactory`, `TlsTestKeystoreFactoryTests`, and
`TlsCertHotReloadTests`; one class under `izgw-core/src/test/java/gov/cdc/izgateway/security/`:
`ServerSslBundleReloaderTests`. No shell script, no Postman collection. Because the
class names match each project's `**/*Tests.java` Surefire include pattern, they run
automatically as part of the existing `mvn clean package`/CI build — alongside, but
independent of, the existing
`testing/scripts/IZGW_2.0_Integration_Test.postman_collection.json` Newman suite,
which they do not touch.

## Risks / Trade-offs

- **[Risk]** In production, a rotated keystore file takes up to
  `server.ssl.bundle-reload.interval` (default 10s) to be picked up via the
  background scheduler, not instantly → **Mitigation:** an operator who needs it
  sooner can force it via `GET /rest/trust?reload=true` (Decision 4); a bounded
  delay is still a strict improvement over the prior behavior (full process
  restart required). Deliberately not using Spring's `reload-on-update` to get a
  faster, event-driven reload, because that mechanism does not work at all on this
  application's EFS-mounted keystore (see Context) — a mechanism that reacts
  instantly but never fires is worse than one with a bounded delay that always
  works.
- **[Risk]** Removing `izgw-hub`'s dead `Application.protocol`/`reloadSsl()` could
  theoretically miss some other caller → **Mitigation:** already grep-confirmed
  zero other references repo-wide; full existing Newman suite plus this CR's new
  test must stay green after removal.
- **[Risk]** Migrating `server.ssl.*` to a named bundle changes the shape of
  production configuration (`application.yml`), not just adding new code →
  **Mitigation:** `server.ssl.enabled` and `client-auth` are left exactly as they
  were; the keystore/truststore location/password/provider/type fields move into
  the bundle block with identical values (including the existing
  `PHIZ_CRYPTO_STORE_*_FILE`/`SSL_SHARE` env var indirection), and the
  protocol/cipher allowlist moves into the bundle's `options` (Decision 2) —
  behaviorally a no-op outside of enabling reload.
- **[Risk]** `izgw-transform` is a second, real consumer of `izgw-core` (pinned to
  an older release, still on legacy `server.ssl.*` properties, with the same dead
  `customizeConnector`/`protocol` duplication izgw-hub had) → **Mitigation:**
  explicitly out of scope for this CR (see Non-Goals); not broken today since it
  hasn't bumped past this fix yet, but will need an analogous migration
  (`application.yml` to a named bundle, its own `@Scheduled` invocation of
  `ServerSslBundleReloader`) whenever it does. Flagged for follow-up, not silently
  dropped.
- **[Risk]** The dropped `KeyStoreLoader.isOutOfDate()` mtime-staleness concern
  could still be a real, separate bug → **Mitigation:** explicitly not addressed
  here; if it resurfaces, it gets diagnosed and scoped on its own, not folded into
  this fix after the fact.

## Migration Plan

- Code and configuration change; no data migration, no new required env vars, no
  schema changes. The `SSL_SHARE`/`PHIZ_CRYPTO_STORE_*_FILE` env vars that already
  govern the keystore file's location are unchanged — the bundle config reads the
  same values through the same indirection. New, optional properties with
  sensible defaults: `server.ssl.bundle-reload.interval`/`.initial-delay` (default
  `PT10S` each).
- Versioning during this CR's verification: `izgw-core` used a ticket-qualified
  SNAPSHOT during active verification, per this repo's documented branch-versioning
  convention, then dropped the qualifier once verification was complete — the
  existing cross-repo release process (not this CR) handles synchronizing
  `izgw-hub`'s pinned version to a real release once `izgw-core` merges.
- Deploy order (matches this project's established cross-repo pattern, e.g.
  `IGDD-3089`): merge `izgw-core` first, then `izgw-hub`.
- Rollback: the two PRs can be reverted independently if needed — the removed
  `izgw-hub` dead code was never reachable, and the `application.yml` bundle
  migration is behaviorally a no-op outside of reload, so reverting just the
  `izgw-core` change (if a problem is found there) doesn't require reverting the
  `izgw-hub` cleanup, and vice versa. Reviewed and merged together under this CR
  regardless.
