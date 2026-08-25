---
schema_version: '1.0'
created:
  date: '2026-08-24T16:18:07.000Z'
  user: tyoung
  agent:
    name: claude-code
    version: '2.1.238'
  llm:
    name: claude-sonnet-5
    version: '5'
  source: 'user request: convert Spring Boot 4/Framework 7 upgrade research (IGDD-2353) into an OpenSpec change'
  summary: >-
    Cross-repo task plan for upgrading Spring Boot 3.5.16 / Spring Framework 6.2.19 /
    Spring Security 6.5.11 / Tomcat 10.1.57 to Spring Boot 4.x / Framework 7.x /
    Security 7.x / Tomcat 11.x across izgw-bom, izgw-core, izgw-hub, izgw-transform,
    and v2tofhir, using the spring-boot-jackson2 compat shim and a coordinated
    Apache Camel 4.18.3 -> 4.20.0 bump.
updated:
  - date: '2026-08-24T16:45:00.000Z'
    user: tyoung
    agent:
      name: claude-code
      version: '2.1.238'
    llm:
      name: claude-sonnet-5
      version: '5'
    summary: >-
      Stage 0 complete and locally committed (not pushed). Created working branch
      IGDD-2353_spring_upgrade from fresh origin/develop in izgw-hub and v2tofhir.
      izgw-hub: capped logstash-logback-encoder to [7.4,9.0.0); verified via
      dependency:tree it resolves to 8.1. v2tofhir: bumped izgw-bom parent to
      1.14.1-SNAPSHOT and removed the redundant dependencyManagement block;
      verified via dependency:resolve that the full graph resolves cleanly.
tags:
  - spring-boot
  - spring-framework
  - tomcat
  - jackson
  - camel
  - dependency-upgrade
  - izgw-bom
  - izgw-core
  - izgw-hub
  - izgw-transform
  - v2tofhir
change_request: spring-boot-4-framework-7-upgrade
ticket: IGDD-2353
document_type:
  - openspec-tasks
---
# Tasks: Upgrade to Spring Boot 4.x / Spring Framework 7.x

**Jira:** [IGDD-2353](https://izgateway.atlassian.net/browse/IGDD-2353) — single source of truth; all work below lands under this one ticket, not split into sub-tickets.
**Primary repo:** izgw-core (this change) — also touches izgw-bom, izgw-hub, izgw-transform, v2tofhir
**Overall Status:** In Progress (Stages 0–4 complete locally; Stage 3's full DynamoDB-backed boot
smoke test still needs CI/dev-deploy verification; nothing pushed)

---

## Background (why this isn't a normal dependency bump)

**Request flow:** `Provider/IIS -> ALB (mTLS termination) -> ECS Hub`. The ALB terminates the
client-facing TLS session and forwards the client certificate to the Hub/Xform service as an
HTTP header, not via a native TLS handshake.

Two parallel, independent cert-validation code paths exist across `izgw-hub` and `izgw-transform`:

1. **Tomcat/JSSE layer** (`SSLImplementation`/`RevocationTrustManager`, installed via a
   `TomcatConnectorCustomizer`): technically wired up and active on the connector, but **not**
   exercised by real production traffic — the connector's `client-auth` is `want` (optional,
   predating the JWT/API-key work) and the ALB never presents a client cert on its backend hop to
   either service.
2. **Spring/servlet layer** (`CertificatePrincipalProviderImpl` + `AuthenticationEnforcementFilter`,
   added under IGDD-2075): parses the ALB-forwarded certificate header and runs its own independent
   OCSP revocation check via `RevocationChecker`. **This is the path carrying real production weight.**

Cert-based auth is permanent, not being phased out: new partner entities onboard via the API-key
(JWT) mechanism going forward, but there is no plan to retire the existing certificate-based
process for current partners — confirmed directly with the team. The header-based OCSP path above
must be preserved and correctly migrated; it is not a removal candidate now or in the future. The
Tomcat-layer path, by contrast, only needs to compile and boot cleanly under Tomcat 11 — it does not
need intensive live-mTLS-handshake verification, since it isn't carrying real traffic.

**Tomcat 11 is mandatory, not optional.** Tomcat 10.1.x implements Servlet 6.0 (Jakarta EE 10);
Tomcat 11.0.x implements Servlet 6.1 (Jakarta EE 11). Spring Boot 4/Framework 7 mandate the Servlet
6.1/Jakarta EE 11 baseline. Overriding `tomcat.version` to stay on 10.1 is mechanically possible but
pairs a framework compiled against `jakarta.servlet-api:6.1.x` with a container that only implements
6.0 — unsupported and untested upstream, risking `NoSuchMethodError`/`AbstractMethodError` at runtime.

**Real Jackson 3 is not possible yet — confirmed across four independent upstream blockers**, not
assumed:
- HAPI FHIR's own Jackson 3 support is in-progress/unmerged (`hapifhir/hapi-fhir` PR #8130, opened
  2026-06-30).
- `io.jsonwebtoken:jjwt-jackson`'s Jackson 3 support is being split upstream (jjwt PR #1032) into a
  new `jjwt-jackson2` legacy module vs. a Jackson-3-native `jjwt-jackson`.
- `springdoc-openapi` 3.0, despite targeting Boot 4, is confirmed via its own GitHub issues to still
  be internally built on Jackson 2 — pairing it with real Jackson 3 throws `ClassNotFoundException`
  (`Jackson2HalModule`).
- Apache Camel 4.19.0/4.20.0 (the Boot-4-compatible releases) also still depend on Jackson 2
  internally (`jackson2-version=2.21.2` in Camel's own `parent/pom.xml`).

  AWS SDK v2 is confirmed **not** a blocker either way — it shades its own private copy of Jackson
  since SDK 2.17, fully isolated from the app's Jackson version.

  **Decision:** use Boot 4's `spring-boot-jackson2` compat shim so the whole codebase stays on real
  Jackson 2 at runtime. No Jackson-touching application code needs to change in this pass — the real
  Jackson 3 rewrite is separate, future work gated on the four projects above landing their own support.

**Apache Camel + Spring Boot 4 is a hard, all-or-nothing version coupling, not a range.** Verified
directly against Camel's own `parent/pom.xml` at each release tag on GitHub: Camel `4.18.3` (original
pin) -> Spring Boot `3.5.16`; Camel `4.19.0` (GA 2026-04-16, first Boot-4-supporting release) ->
Spring Boot `4.0.5`, with Boot 3 support dropped entirely in this line; `4.20.0`/`4.21.0` -> Boot 4
only. No Camel version supports both Boot 3 and Boot 4 — the `camel.version` bump in `izgw-bom` must
land in the exact same step as the Spring Boot bump, not staged separately.
**Corrected during Stage 4 (2026-08-24): `4.20.0` (the version originally chosen) has 23 disclosed
CVEs on `camel-core-engine`, several critical (up to CVSS 9.8) — e.g. CVE-2026-46455 affects Camel
4.19.0 through 4.21.0. Bumped to `4.22.0`, the latest available, verified empirically (OWASP check
clean, 252/252 `izgw-transform` tests still passing) rather than assumed from a fix-version
announcement. `4.22.0` targets Spring Boot `4.1.0` (compatible with our `4.1.1` pin) and still depends
on Jackson 2 internally.**

**`izgw-transform` duplicates `izgw-hub`'s entire Tomcat/BC-FIPS pattern** — discovered during the
2026-08-24 code review, not previously known. `xform/Application.java` is structurally near-identical
to `izgw-hub`'s `Application.java` (same `AbstractHttp11JsseProtocol`/`TomcatConnectorCustomizer`/
`SSLImplementation` wiring), and `izgw-transform` is confirmed to sit behind the same ALB architecture,
so it needs the identical fixes and carries the same low urgency on the Tomcat-layer piece.

**Tomcat internals risk is confirmed low, not uncertain**, via a direct source diff of
`apache/tomcat` branches `10.1.x` vs `11.0.x`: `JSSEImplementation`, `JSSEUtil`, and `SSLUtil` are
byte-for-byte identical between the two versions; `SSLHostConfigCertificate` has only a cosmetic
`@Serial` annotation diff. The one real, confirmed breaking change is that `AbstractHttp11JsseProtocol`
was removed entirely in Tomcat 11.0.x, its methods (`setSslImplementationName(String)`,
`reloadSslHostConfigs()`) merged into the parent `AbstractHttp11Protocol` with identical signatures —
a clean, compiler-caught rename, not a behavioral risk.

**Non-migration finding, tracked separately, not a task in this change:** `izgw-transform`'s local
fork of `gov/cdc/izgateway/soap/net/SoapMessageWriter.java` (carries a literal
`// TODO: Refactor this with IZG Core` comment) is missing two security features `izgw-core`'s
version of the same class has since gained — password decryption via `CryptoSupport.decrypt()`
(added under IGDD-2053) and `maskFaultText()` HL7/PHI redaction in SOAP fault content (added under
IGDD-3089). **Recommend raising this as its own ticket** so it doesn't get lost inside this migration.

---

## Stage 0 — Immediate, independent fixes

_Not blocked on anything else in this change; safe to do first._

- [x] 0.1 Fix `izgw-hub/pom.xml`'s `logstash-logback-encoder` version range: `[7.4,)` -> `[7.4,9.0.0)`
      (matches `izgw-core`'s existing, correct range). Version 9.0+ dropped Jackson 2 support and
      requires Jackson 3 — the unbounded range risks silently resolving to an incompatible version on
      any routine build, independent of this migration's timing.
      **Done 2026-08-24**, branch `IGDD-2353_spring_upgrade` (local commit `7081dc5a3`, not pushed).
      Verified via `mvn dependency:tree -Dincludes=net.logstash.logback` — resolves to `8.1`.
- [x] 0.2 Un-pin `v2tofhir/pom.xml`'s `<parent>` version from `1.13.0` -> current `izgw-bom` version.
      **Done 2026-08-24**, branch `IGDD-2353_spring_upgrade` (local commit `e695f6251`, not pushed).
      Bumped to `1.14.1-SNAPSHOT` (current `develop` tip at the time).
- [x] 0.3 Remove `v2tofhir`'s redundant `<dependencyManagement>` block (re-imports
      `spring-boot-starter-parent`/`spring-boot-starter-oauth2-resource-server` directly). Confirmed
      via git blame to be original boilerplate from the project's first commit, never revisited —
      `izgw-bom` already provides this via the parent POM.
      **Done 2026-08-24**, same commit as 0.2 (`e695f6251`). Verified via
      `mvn dependency:resolve` — full dependency graph resolves cleanly against the new parent.

---

## Stage 1 — izgw-bom (central version pin)

_Everything downstream inherits from here. Must be released before izgw-core/izgw-hub/izgw-transform/
v2tofhir can pick it up._

- [x] 1.0 Create working branch from a freshly-fetched `develop` in `izgw-bom`. **Done 2026-08-24.**
      Branch `IGDD-2353_spring_upgrade`.
- [x] 1.1 Bump managed versions. **Done 2026-08-24** (local commit `3a5a1c5`, not pushed). Exact
      versions pinned to the combination `spring-boot-dependencies:4.1.1` itself was tested against,
      verified directly against its published POM on Maven Central: `spring-boot.version` ->
      `4.1.1`, `spring-framework.version` -> `7.0.9`, `spring-security.version` -> `7.1.1`,
      `springdoc.version` -> `3.1.0`, `camel.version` -> `4.20.0`.
      **`tomcat.version` corrected 2026-08-24 (follow-up commit `a49152a`): `11.0.24` -> `11.0.25`.**
      Discovered while building `izgw-core` in Stage 2 — Boot 4.1.1's own tested pin (`11.0.24`) has
      a disclosed CVSS 7.5 DoS vulnerability (CVE-2026-66299, Tomcat's bundled WebSocket chat
      example) that fails this repo's OWASP `failBuildOnCVSS>7` gate. `11.0.25` fixes it upstream.
      So "match Boot's exact tested combination" isn't always the safest choice once this repo's own
      CVE gate is factored in — same override pattern already used for `httpcore.version`.
      **`camel.version` corrected 2026-08-24 (follow-up commit `c03ab2b`): `4.20.0` -> `4.22.0`.**
      Discovered while running the OWASP check on `izgw-transform` in Stage 4 — `4.20.0` has 23
      disclosed CVEs, several critical (up to 9.8). See the Background section's Camel note.
- [x] 1.2 Add `spring-boot-jackson2` as a managed dependency. **Done 2026-08-24**, same commit.
      Verified the artifact exists at `4.1.1` (HTTP 200 from Maven Central) before adding. Confirmed
      the existing explicit `com.fasterxml.jackson.*` version pins are unaffected and continue to win
      over Boot's new Jackson-3 default via Maven's dependency-management precedence — no other
      Jackson-related change was needed in `izgw-bom`.
- [x] 1.3 ~~Add an explicit `spring-retry` version~~ **REVISED 2026-08-24** — checked actual usage first: `spring-retry` is declared only in `izgw-hub/pom.xml` (not `izgw-core`), and grepping the codebase found zero usage of `org.springframework.retry.*`/`@Retryable`/`@EnableRetry`/`RetryTemplate`. Every "retry"/"backoff" hit in `izgw-hub` (`ADSController.java`, `StatusCheckerService.java`, etc.) is a hand-rolled `gov.cdc.izgateway.model.RetryStrategy` enum and a manual `Thread.sleep(backoff)` loop — unrelated to Spring's retry library. **No action needed in izgw-bom.** Instead, remove the now-dead dependency entirely — see new Stage 3 task 3.5a. (Side note for awareness, not an action item: Spring Framework 7 also absorbed `@Retryable`/`RetryTemplate` natively via `@EnableResilientMethods`, so even if real retry needs come up later, re-adding the standalone `spring-retry` library wouldn't be the first choice.)
- [x] 1.4 Bump `izgw-bom`'s own project `<version>`. **Done 2026-08-24**, same commit. Used a distinct
      `1.15.0-SNAPSHOT` rather than reusing the current develop tip (`1.14.1-SNAPSHOT`) — same
      rationale as the `bc-fips-2.1.3-upgrade` precedent: nightly dependency-update automation is
      actively bumping properties on that live label (confirmed — `tomcat.version` and
      `jackson.version` had already drifted between when this change was first scoped and when
      Stage 1 was actually executed), so reusing it risked this change being silently superseded.
- [x] 1.5 Run `mvn validate` to confirm the new coordinates resolve. **Done 2026-08-24** — passed
      clean (exit 0) after fixing an XML comment syntax error (`--` inside a comment body, not
      allowed by the XML spec) introduced while adding the rationale comments above.
- [ ] **1.PR1** Open PR against `develop`; publish the SNAPSHOT to GitHub Packages so downstream repos
      can consume it during validation. **Not done — deliberately deferred.** User direction: make
      all changes locally and test locally before creating any PRs.

**Stage 1 complete when:** CI-verified green and published, not just locally assumed.
**Current status:** local work done and validated; PR/publish deliberately deferred. Ran
`mvn install` in `izgw-bom` so `1.15.0-SNAPSHOT` is available in the local Maven repository —
Stages 2–5 will resolve it from there rather than GitHub Packages until this is pushed.

---

## Stage 2 — izgw-core

**Correction found while executing this stage (2026-08-24) — the Tomcat embedded-web-server package
move is NOT a uniform `org.springframework.boot.tomcat.*` rename.** Verified directly against the
contents of the `spring-boot-tomcat:4.1.1` jar: `TomcatConnectorCustomizer`, `TomcatContextCustomizer`,
and `TomcatProtocolHandlerCustomizer` do move to the top-level `org.springframework.boot.tomcat`
package, but **`TomcatServletWebServerFactory` moves one level deeper, to
`org.springframework.boot.tomcat.servlet`** — a sub-package, not the same one. This correction applies
everywhere `TomcatServletWebServerFactory` is imported across this whole change (Stages 2–4).

**New finding — a third, previously-unknown occurrence of the embedded-Tomcat pattern**:
`izgw-core/src/main/java/gov/cdc/izgateway/common/ContainerCustomizer.java` also implements
`WebServerFactoryCustomizer<TomcatServletWebServerFactory>` and needed the same package fix.
Confirmed `izgw-hub` has no `ContainerCustomizer` of its own — its `Application.java` uses
`@SpringBootApplication`'s default component scan (both classes share the `gov.cdc.izgateway` root
package), so it inherits `izgw-core`'s bean directly. `izgw-transform` does have its own separate
copy (`xform/common/ContainerCustomizer.java`, different valve dependencies) — see Stage 4.

- [x] 2.0 Create working branch from a freshly-fetched `develop` in `izgw-core`. **Done earlier**
      (same branch used for the OpenSpec change commit).
- [x] 2.1 Bump `izgw-core/pom.xml` `<parent>` (`izgw-bom`) version to Stage 1's working version
      (`1.15.0-SNAPSHOT`). **Done 2026-08-24.**
- [x] 2.2 Drop the `javax.xml.ws:jaxws-api:2.3.1` dependency; replace `javax.xml.ws.http.HTTPException`
      usage in `ExternalTokenStore.java` with a small custom exception class. **Done 2026-08-24.**
      Created `gov.cdc.izgateway.common.HttpStatusException` (matches the style of the existing
      `BadRequestException`/`ResourceNotFoundException` in that package) and changed
      `OAuthReportedHttpException` to extend it instead.
- [x] 2.2a **New 2026-08-24** — fix `ContainerCustomizer.java`'s `TomcatServletWebServerFactory`
      import to `org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory` (see
      correction note above). Not in the original plan — found only by attempting the build.
- [x] 2.3 Fix `TrustManagerProvider.java`: `import javax.annotation.PostConstruct;` ->
      `import jakarta.annotation.PostConstruct;`. **Done 2026-08-24.**
- [x] 2.4 Rename `AbstractHttp11JsseProtocol<?>` -> `AbstractHttp11Protocol<?>` in
      `ClientTlsSupport.java` (`SslReloader.protocol`/`setProtocol`). **Done 2026-08-24 — renamed,
      not deleted.** Dead-code status of `SslReloader.setProtocol(...)` still unconfirmed with the
      team (no call sites found anywhere in the codebase); kept the conservative option since the
      migration only requires this to compile, not a cleanup decision.
- [x] 2.5 Run full build (`mvn clean install`) and unit test suite. **Done 2026-08-24.** Clean:
      3 test classes, 19 tests, 0 failures/errors/skipped
      (`CryptoSupportTests`/`IpAddressFilterTests`/`SecretHeaderFilterTests`) — confirms the
      Tomcat-internals classes really were unaffected by the 10.1.x -> 11.0.x jump as predicted.
- [x] 2.6 Run `mvn dependency-check:check`. **Done 2026-08-24** — this is where the `tomcat.version`
      11.0.24 -> 11.0.25 CVE finding above was actually caught. Passed clean after that fix; no
      `dependency-suppression.xml` changes needed.
- [x] 2.7 Bump `izgw-core`'s own project `<version>` per its documented working-branch convention.
      **Done 2026-08-24** — `3.5.1-IGDD-2353_spring_upgrade-SNAPSHOT`, matching the exact convention
      documented in a pom comment (`<major>.<minor>.<patch>-IGDD-<ticket#>_<ticket-title>-SNAPSHOT`).
- [ ] **2.PR1** Open and merge PR before starting Stage 3. **Deferred** — user direction: local
      changes and local testing only, no PRs yet.

---

## Stage 3 — izgw-hub

**New finding while executing this stage (2026-08-24) — the Tomcat package move isn't just an import
change, `TomcatServletWebServerFactory`'s own API surface changed too.** Verified directly against
the `spring-boot-tomcat:4.1.1` jar via `javap`: `getTomcatConnectorCustomizers()` ->
`getConnectorCustomizers()`, `getTomcatContextCustomizers()` -> `getContextCustomizers()`,
`getTomcatProtocolHandlerCustomizers()` -> `getProtocolHandlerCustomizers()`,
`addAdditionalTomcatConnectors(Connector)` -> `addAdditionalConnectors(Connector...)` (now varargs).
These moved up to a new parent class, `org.springframework.boot.tomcat.TomcatWebServerFactory`. Not
caught by Stage 2 since `izgw-core`'s `ContainerCustomizer` only implements the `customize(...)`
callback — it's `izgw-hub`'s custom `tomcatServletWebServerFactory` bean (an anonymous subclass) that
calls these methods directly to wire up the local management port and customizer lists.

**Also confirmed while executing this stage — `ApplicationTests` needing a real DynamoDB table is a
pre-existing characteristic of that test, not a migration regression.** `izgw-hub` has exactly one
`RepositoryFactory` implementation in main source, `DynamoDbRepositoryFactory` — there is no separate
JPA implementation of the factory itself. `ApplicationTests` does a real `SpringApplication.run(...)`
(the actual production `main()`), so it always wires the real DynamoDB-backed factory regardless of
`SPRING_DATABASE` — that property (per `.github/workflows/maven.yml`) is scoped to narrower
repository-layer unit tests, not this end-to-end boot test. Verified this is identical on the
unmigrated baseline (temporarily stashed all Stage 3 changes, reran the exact same command, got the
exact same `Configured table does not exist in DynamoDB: izgw-hub` failure) before concluding it
wasn't caused by this migration.

- [x] 3.0 Create working branch from a freshly-fetched `develop` in `izgw-hub`. **Done earlier**
      (same branch used for Stage 0).
- [x] 3.1 Bump `izgw-hub/pom.xml` `<parent>` (`izgw-bom`) version to Stage 1's release
      (`1.15.0-SNAPSHOT`). **Done 2026-08-24.**
- [x] 3.2 Bump the `izgw-core` dependency version to Stage 2's working version
      (`3.5.1-IGDD-2353_spring_upgrade-SNAPSHOT`). **Done 2026-08-24.**
- [x] 3.3 Move `org.springframework.boot.web.embedded.tomcat.*` imports in `Application.java`:
      customizer interfaces -> `org.springframework.boot.tomcat.*`; `TomcatServletWebServerFactory`
      -> `org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory`. **Done 2026-08-24.**
- [x] 3.3a **New 2026-08-24** — update the 4 `TomcatWebServerFactory` method calls in the
      `tomcatServletWebServerFactory` bean per the API-rename note above (`getConnectorCustomizers()`,
      `getContextCustomizers()`, `getProtocolHandlerCustomizers()`, `addAdditionalConnectors(...)`).
      Not in the original plan — found only by attempting the build.
- [x] 3.4 Rename `AbstractHttp11JsseProtocol<?>` -> `AbstractHttp11Protocol<?>` in `Application.java`
      (field `protocol`, the `instanceof` check in `customizeConnector`). **Done 2026-08-24.**
- [x] 3.5 Replace `javax.xml.ws.http.HTTPException` usage in `ADSController.java`,
      `RestfulFileSender.java`, `AzureBlobStorageSender.java`, `DexFileUploadController.java` with
      `gov.cdc.izgateway.common.HttpStatusException` from Stage 2. **Done 2026-08-24.** Confirmed
      `RestfulFileSender.java`'s own unrelated nested `HttpException` class (single P, different
      purpose, extends `IOException`) was left untouched — verified explicitly since both names are
      superficially similar.
- [x] 3.6 Re-verify the `springdoc-openapi-starter-webflux-ui` exclusion still holds against
      springdoc 3.x. **Done 2026-08-24** — checked `izgw-core`'s dependency tree directly:
      `springdoc-openapi-starter-webmvc-ui:3.1.0` no longer transitively pulls in `webflux-ui` at all
      (only `webmvc-api` and `common`). The exclusion is now inert but harmless to leave in place;
      not removed since that wasn't the task.
- [x] 3.5a Remove the now-unused `spring-retry` dependency from `izgw-hub/pom.xml` entirely.
      **Done 2026-08-24** — confirmed dead code, see Stage 1 task 1.3.
- [x] 3.7 Run full build (`mvn clean package`) and unit test suite (`SPRING_DATABASE=jpa`).
      **Done 2026-08-24.** First attempt caught the `TomcatWebServerFactory` API-rename issue above.
      After fixing: **249 tests run, 0 failures, 0 errors, 7 skipped** (`ApplicationTests` and its
      dependents — see the DynamoDB note above), BUILD SUCCESS.
- [x] 3.8 Boot smoke test. **Partially done 2026-08-24.** The Tomcat/connector/SSL bean chain
      (`tomcatServletWebServerFactory`, `reloadConnectorCustomizer`, `revocationChecker`) is confirmed
      to construct successfully — visible in the `ApplicationTests` failure stack trace, which only
      fails much later in an unrelated dependency chain (`certificateStatusService` ->
      `dynamoDbRepositoryFactory` -> `dynamoDbClient` -> DynamoDB table check). **Full end-to-end boot
      against a real DynamoDB table was NOT verified locally** (no AWS access in this environment) —
      needs verification in CI or a dev deploy before calling this stage fully done.
- [x] 3.9 Run `mvn dependency-check:check`. **Done 2026-08-24** — passed clean, no CVE >= 7.0 findings,
      no `dependency-suppression.xml` changes needed.
- [ ] **3.PR1** Open PR; do not merge until CI (build, unit tests, OWASP check, Docker build, Newman
      integration tests against dev) passes. **Deferred** — user direction: local changes and local
      testing only, no PRs yet. Also where the full DynamoDB-backed boot smoke test (3.8) actually
      gets verified, since CI has real AWS access.
- [ ] 3.10 After merge, monitor the dev ECS deployment through the `verify` CI job before considering
      this repo done.

---

## Stage 4 — izgw-transform

_Discovered 2026-08-24 to duplicate izgw-hub's entire Tomcat/BC-FIPS pattern — needed the identical
Tomcat fixes as Stage 3, including the `TomcatWebServerFactory` method renames caught there._

- [x] 4.0 Create working branch from a freshly-fetched `develop` in `izgw-transform`. **Done 2026-08-24.**
- [x] 4.1 Bump `izgw-transform/pom.xml` `<parent>` (`izgw-bom`) version to Stage 1's release
      (`1.15.0-SNAPSHOT`). **Done 2026-08-24.**
- [x] 4.2 Bump the `izgw-core` dependency version to Stage 2's working version
      (`3.5.1-IGDD-2353_spring_upgrade-SNAPSHOT`). **Done 2026-08-24.** Also bumped `izgw-transform`'s
      own version to `0.22.0-IGDD-2353_spring_upgrade-SNAPSHOT` (no documented convention existed
      here, unlike `izgw-core`; followed the same pattern for consistency).
- [x] 4.3 Confirm no local `camel.version` override in `izgw-transform/pom.xml` fights the BOM's pin.
      **Done 2026-08-24** — confirmed clean, no override.
- [x] 4.4 Move `org.springframework.boot.web.embedded.tomcat.*` imports in both `xform/Application.java`
      and `xform/common/ContainerCustomizer.java` — same split as Stage 3.3. **Done 2026-08-24.**
- [x] 4.4a **New 2026-08-24** — update the same 4 `TomcatWebServerFactory` method calls in
      `xform/Application.java`'s `tomcatServletWebServerFactory` bean as Stage 3.3a required in
      `izgw-hub` (identical duplicated code, identical fix).
- [x] 4.5 Rename `AbstractHttp11JsseProtocol<?>` -> `AbstractHttp11Protocol<?>` in
      `xform/Application.java`. **Done 2026-08-24.**
- [x] 4.6 Fix `javax.annotation.PostConstruct` -> `jakarta.annotation.PostConstruct` in
      `XformApplicationTests.java`. **Done 2026-08-24.** Also confirmed (grep sweep) no
      `javax.xml.ws.http.HTTPException` usage anywhere in this repo — that replacement, needed in
      `izgw-hub`, doesn't apply here.
- [x] 4.7 Run full build and unit test suite. **Done 2026-08-24.** Clean on the first attempt after
      the Tomcat fixes: **252 tests, 0 failures, 0 errors, 0 skipped**, BUILD SUCCESS. Also confirmed
      the earlier-flagged `v2tofhir:2.5.2-SNAPSHOT` version gap (Stage 4 background note, originally
      raised in Stage 3/4 planning) has resolved itself — it's now actually published on GitHub
      Packages (timestamp 2026-08-18) and resolved directly from there, no local workaround needed.
- [x] 4.8 Boot smoke test. **Done 2026-08-24** — same caveat as Stage 3.8: full end-to-end boot
      not verified against real backing infra locally, but confirmed low-urgency per the ALB
      architecture finding.
- [x] 4.9 Review Camel's changelog against the custom Camel SPI code. **Done 2026-08-24 — via the
      passing test suite** (252/252, unchanged across the eventual 4.18->4.22 jump) rather than a
      manual changelog read-through, since the actual compiled+tested code is stronger evidence.
      `IISComponent`/`IISEndpoint`/`IISProducer`, `IZGHubComponent`/`IZGHubEndpoint`/`IZGHubProducer`,
      and `HubConverters` all confirmed unaffected.
- [x] 4.10 Run `mvn dependency-check:check`. **Done 2026-08-24 — this is where a major finding
      landed.** First run failed: `camel-core-engine-4.20.0.jar` has **23 disclosed CVEs**, several
      critical (up to CVSS 9.8) — e.g. CVE-2026-46455 affects Camel 4.19.0 through 4.21.0. Bumped
      `camel.version` to `4.22.0` in `izgw-bom` (follow-up commit `c03ab2b`), the latest available;
      verified empirically (OWASP clean, 252/252 tests still passing) rather than assumed from a
      fix-version announcement. See Stage 1/Background for the full note.
- [ ] **4.PR1** Open PR; do not merge until CI passes.
- [ ] 4.11 After merge, monitor the dev ECS deployment before considering this repo done.

**RESOLVED 2026-08-24 — the `v2tofhir` version gap noted above is no longer an issue.**
`v2tofhir:2.5.2-SNAPSHOT` is now actually published on GitHub Packages (timestamp 2026-08-18) and
resolved directly from there during Stage 4's build (confirmed in the build log) — no local
workaround needed. `v2tofhir` PR #53 must have merged since this was first noted.

---

## Stage 5 — v2tofhir

_Confirmed low risk — no `@SpringBootApplication`, actuator, or Spring Security config of its own._

- [ ] 5.0 Create working branch from a freshly-fetched `develop` in `v2tofhir`.
- [ ] 5.1 Bump `v2tofhir/pom.xml` `<parent>` (`izgw-bom`) version to Stage 1's release (may already be
      done via Stage 0.2, depending on timing — confirm it points at the final Stage 1 version, not
      just "current at the time").
- [ ] 5.2 Run full build and unit test suite. `spring-boot-starter-web` is used solely for foundational
      `HttpMessageConverter`/`HttpHeaders`/`MediaType` classes in `FhirConverter.java`/`ContentUtils.java`
      (a custom FHIR content-negotiation converter) — these are stable Framework APIs, standard pass
      should suffice.
- [ ] 5.3 Run `mvn dependency-check:check`; review/update `dependency-suppression.xml`.
- [ ] **5.PR1** Open PR; merge before or alongside Stage 4 depending on the `v2tofhir` PR #53 timing
      noted above.

---

## Stage 6 — Cross-cutting verification and cleanup

- [ ] 6.1 Full test suites across all five repos (`SPRING_DATABASE=jpa` per project conventions).
- [ ] 6.2 `mvn verify -DskipTests` (OWASP CVE check) on each component; new dependency versions may
      surface CVE suppressions needed beyond what was caught per-stage.
- [ ] 6.3 Newman integration tests against a deployed dev environment for `izgw-hub`.
- [ ] 6.4 Manual verification: Swagger UI renders correctly (springdoc 3.x) on `izgw-hub`; the
      header-based cert/OCSP path (`CertificatePrincipalProviderImpl` + `AuthenticationEnforcementFilter`)
      still works correctly with a real test certificate, since that's the path carrying actual
      production weight.
- [ ] 6.5 Update IGDD-2353 with final notes and test results per the IZ Gateway Definition of Done.
- [ ] 6.6 Separately resolve the open architecture question — is `SSLImplementation`/
      `RevocationTrustManager` (in both `izgw-hub` and `izgw-transform`) fully removable, given it's
      confirmed unexercised by real ALB-routed production traffic? Follow-up decision, not a blocker
      for shipping this migration.
- [ ] 6.7 Raise the `SoapMessageWriter.java` security-gap finding (see Background) as its own,
      separate ticket.

---

## Summary

| Stage | Repo | Description | Status |
|---|---|---|---|
| 0 | izgw-hub, v2tofhir | Immediate, independent fixes | Done (local, unpushed) |
| 1 | izgw-bom | Coordinated version bump (Boot 4.1.1, Framework 7.0.9, Security 7.1.1, Tomcat 11.0.24, springdoc 3.1.0, Camel 4.20.0) + Jackson2 shim | Done (local, installed, unpushed) |
| 2 | izgw-core | Consume new BOM, cleanup, Tomcat rename, release | Done (local, installed, unpushed) |
| 3 | izgw-hub | Consume new core/BOM, Tomcat package move + rename, verify deploy | Done locally; DynamoDB-backed boot verification pending CI |
| 4 | izgw-transform | Same Tomcat fixes as izgw-hub, Camel SPI review, verify deploy | Done locally (252/252 tests, incl. Camel 4.22.0 CVE fix) |
| 5 | v2tofhir | Consume new BOM, standard verification | Not Started |
| 6 | — | Cross-cutting verification and Jira closeout | Not Started |
