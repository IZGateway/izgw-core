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
**Overall Status:** In Progress (Stage 0 complete, locally committed, not pushed)

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
directly against Camel's own `parent/pom.xml` at each release tag on GitHub: Camel `4.18.3` (current
pin) -> Spring Boot `3.5.16`; Camel `4.19.0` (GA 2026-04-16, first Boot-4-supporting release) ->
Spring Boot `4.0.5`, with Boot 3 support dropped entirely in this line; Camel `4.20.0` (security-fix
release on 4.19) -> Boot 4 only. No Camel version supports both Boot 3 and Boot 4 — the `camel.version`
bump in `izgw-bom` must land in the exact same step as the Spring Boot bump, not staged separately.

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

- [ ] 1.0 Create working branch from a freshly-fetched `develop` in `izgw-bom`.
- [ ] 1.1 Bump managed versions: `spring-boot.version` -> 4.x, `spring-framework.version` -> 7.x,
      `spring-security.version` -> 7.x, `tomcat.version` -> 11.x, `springdoc.version` -> 3.x,
      `camel.version` -> `4.20.0` (must land in this same commit — see Background).
- [ ] 1.2 Add `spring-boot-jackson2` as a managed dependency.
- [ ] 1.3 Add an explicit `spring-retry` version (Boot 4 removed it from its own dependency management).
- [ ] 1.4 Bump `izgw-bom`'s own project `<version>`.
- [ ] 1.5 Run `mvn validate` to confirm the new coordinates resolve.
- [ ] **1.PR1** Open PR against `develop`; publish the SNAPSHOT to GitHub Packages so downstream repos
      can consume it during validation.

**Stage 1 complete when:** CI-verified green and published, not just locally assumed.

---

## Stage 2 — izgw-core

- [ ] 2.0 Create working branch from a freshly-fetched `develop` in `izgw-core`.
- [ ] 2.1 Bump `izgw-core/pom.xml` `<parent>` (`izgw-bom`) version to Stage 1's working version.
- [ ] 2.2 Drop the `javax.xml.ws:jaxws-api:2.3.1` dependency; replace `javax.xml.ws.http.HTTPException`
      usage in `ExternalTokenStore.java` with a small custom exception class.
- [ ] 2.3 Fix `TrustManagerProvider.java`: `import javax.annotation.PostConstruct;` ->
      `import jakarta.annotation.PostConstruct;`.
- [ ] 2.4 Rename `AbstractHttp11JsseProtocol<?>` -> `AbstractHttp11Protocol<?>` in
      `ClientTlsSupport.java` (`SslReloader.protocol`/`setProtocol`). Confirm with the team first
      whether `SslReloader.setProtocol(...)` is dead code — no call sites were found anywhere in the
      codebase during this review — it may be deletable instead of renamed.
- [ ] 2.5 Run full build (`mvn clean install`) and unit test suite. Expect a mostly clean pass —
      `JSSEImplementation`/`JSSEUtil`/`SSLUtil` are confirmed byte-for-byte identical between Tomcat
      10.1.x and 11.0.x (see Background).
- [ ] 2.6 Run `mvn dependency-check:check`; review/update `dependency-suppression.xml` for any new
      CVEs surfaced by the version bumps.
- [ ] 2.7 Bump `izgw-core`'s own project `<version>` per its documented working-branch convention.
- [ ] **2.PR1** Open and merge PR before starting Stage 3.

---

## Stage 3 — izgw-hub

- [ ] 3.0 Create working branch from a freshly-fetched `develop` in `izgw-hub`.
- [ ] 3.1 Bump `izgw-hub/pom.xml` `<parent>` (`izgw-bom`) version to Stage 1's release.
- [ ] 3.2 Bump the `izgw-core` dependency version to Stage 2's working version.
- [ ] 3.3 Move `org.springframework.boot.web.embedded.tomcat.*` imports in `Application.java`
      (`TomcatConnectorCustomizer`, `TomcatContextCustomizer`, `TomcatProtocolHandlerCustomizer`,
      `TomcatServletWebServerFactory`) to `org.springframework.boot.tomcat.*`.
- [ ] 3.4 Rename `AbstractHttp11JsseProtocol<?>` -> `AbstractHttp11Protocol<?>` in `Application.java`
      (field `protocol`, the `instanceof` check in `customizeConnector`).
- [ ] 3.5 Replace `javax.xml.ws.http.HTTPException` usage in `ADSController.java`,
      `RestfulFileSender.java`, `AzureBlobStorageSender.java`, `DexFileUploadController.java` with the
      custom exception from 2.2.
- [ ] 3.6 Re-verify the `springdoc-openapi-starter-webflux-ui` exclusion (added to stop WebFlux
      auto-config from breaking the MVC Swagger UI / TC_92a) still holds against springdoc 3.x.
- [ ] 3.7 Run full build (`mvn clean package`) and unit test suite (`SPRING_DATABASE=jpa`).
- [ ] 3.8 Boot smoke test — confirm the app starts cleanly (this is where any missed Tomcat-11
      incompatibility would surface loudly, since the Tomcat-internals code isn't otherwise exercised
      by real production traffic — see Background) and `ApplicationTests`/`AccessControlTests` pass.
- [ ] 3.9 Run `mvn dependency-check:check`; review/update `dependency-suppression.xml`.
- [ ] **3.PR1** Open PR; do not merge until CI (build, unit tests, OWASP check, Docker build, Newman
      integration tests against dev) passes.
- [ ] 3.10 After merge, monitor the dev ECS deployment through the `verify` CI job before considering
      this repo done.

---

## Stage 4 — izgw-transform

_Discovered 2026-08-24 to duplicate izgw-hub's entire Tomcat/BC-FIPS pattern — needs the identical
Tomcat fixes as Stage 3, plus its own straggler and Camel-specific verification._

- [ ] 4.0 Create working branch from a freshly-fetched `develop` in `izgw-transform`.
- [ ] 4.1 Bump `izgw-transform/pom.xml` `<parent>` (`izgw-bom`) version to Stage 1's release.
- [ ] 4.2 Bump the `izgw-core` dependency version to Stage 2's working version.
- [ ] 4.3 Confirm no local `camel.version` override in `izgw-transform/pom.xml` fights the BOM's new
      `4.20.0` pin.
- [ ] 4.4 Move `org.springframework.boot.web.embedded.tomcat.*` imports in both `xform/Application.java`
      **and** `xform/common/ContainerCustomizer.java` (implements
      `WebServerFactoryCustomizer<TomcatServletWebServerFactory>`) to `org.springframework.boot.tomcat.*`.
- [ ] 4.5 Rename `AbstractHttp11JsseProtocol<?>` -> `AbstractHttp11Protocol<?>` in `xform/Application.java`.
- [ ] 4.6 Fix `javax.annotation.PostConstruct` -> `jakarta.annotation.PostConstruct` in
      `src/test/java/gov/cdc/izgateway/xform/XformApplicationTests.java`.
- [ ] 4.7 Run full build and unit test suite.
- [ ] 4.8 Boot smoke test — same low-urgency compile/boot treatment as Stage 3 applies here too
      (confirmed `izgw-transform` sits behind the same ALB architecture, so its `SSLImplementation`
      wiring isn't exercised by real production traffic either).
- [ ] 4.9 Review Camel's 4.19/4.20 changelogs specifically against the custom Camel SPI code in this
      repo — `IISComponent`/`IISEndpoint`/`IISProducer` and
      `IZGHubComponent`/`IZGHubEndpoint`/`IZGHubProducer` (extend
      `DefaultComponent`/`DefaultEndpoint`/`DefaultProducer`), plus the `HubConverters` custom
      `TypeConverters` registration. This SPI is generally stable across Camel minor versions, but
      hand-written component code deserves a specific test pass, not just a generic compile check.
- [ ] 4.10 Run `mvn dependency-check:check`; review/update `dependency-suppression.xml`.
- [ ] **4.PR1** Open PR; do not merge until CI passes.
- [ ] 4.11 After merge, monitor the dev ECS deployment before considering this repo done.

**Known, non-blocking caveat:** a local multi-module build of `izgw-transform` against a
locally-checked-out `v2tofhir` will currently fail dependency resolution — `izgw-transform` (via PR
#283, merged 2026-08-19) expects `v2tofhir:2.5.2-SNAPSHOT`, which depends on `v2tofhir` PR #53
(Location labeling fix), still open/unmerged as of this writing. Not something to fix as part of this
change; just be aware of it when testing locally.

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
| 1 | izgw-bom | Coordinated version bump (Boot 4, Framework 7, Security 7, Tomcat 11, springdoc 3, Camel 4.20) + Jackson2 shim | Not Started |
| 2 | izgw-core | Consume new BOM, cleanup, Tomcat rename, release | Not Started |
| 3 | izgw-hub | Consume new core/BOM, Tomcat package move + rename, verify deploy | Not Started |
| 4 | izgw-transform | Same Tomcat fixes as izgw-hub, Camel SPI review, verify deploy | Not Started |
| 5 | v2tofhir | Consume new BOM, standard verification | Not Started |
| 6 | — | Cross-cutting verification and Jira closeout | Not Started |
