---
schema_version: '1.0'
created:
  date: '2026-10-14T19:21:37.127Z'
  user: boonek
  agent:
    name: claude-code
    version: '2.0'
  llm:
    name: claude-sonnet-5
    version: claude-sonnet-5
  prompt_uri: >-
    prompt:/claude-code/7b62e482-c255-4291-b97d-980994f07e6c/~25fad386-db4e-40e6-80e2-df970b3d4527
  summary: tls-cert-hot-reload capability spec for IGDD-3582
updated:
  - date: '2026-10-15T00:48:16.652Z'
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
      Correct requirement: reload needs standard SslBundle config from
      consumers, not literally zero config, since legacy server.ssl.* properties
      get no Spring Boot reload wiring at all
change_request: igdd-3582-fix-private-key-update-bug
ticket: IGDD-3582
document_type:
  - openspec-spec-delta
---
# Spec Delta

## Purpose

Keeps the embedded HTTPS listener's TLS server certificate synchronized with
whatever certificate is currently on disk, so a certificate rotation takes effect
without requiring the process to be restarted.

## ADDED Requirements

### Requirement: Embedded HTTPS listener picks up a rotated certificate without a restart
When the on-disk keystore file backing the embedded HTTPS listener's TLS identity
is replaced with a new certificate, the system SHALL make that new certificate
available to new TLS connections within the system's existing certificate-change
detection interval, without requiring the process to be restarted.

#### Scenario: Keystore file is rotated with a new certificate
- **WHEN** the on-disk keystore file used by the embedded HTTPS listener is replaced
  with a file containing a new certificate for the same hostname
- **THEN** TLS handshakes negotiated by the listener after the next detection cycle
  present the new certificate, and no process restart occurs or is required

#### Scenario: Keystore file is unchanged
- **WHEN** the on-disk keystore file has not been modified since it was last loaded
- **THEN** the embedded HTTPS listener continues presenting the certificate already
  in use, and no reload is performed

### Requirement: Certificate hot-reload requires only standard Spring Boot SSL configuration, no custom code
Any application that embeds this library's TLS support and configures its
embedded HTTPS server's TLS material as a named Spring Boot SSL bundle with
reload enabled SHALL receive the certificate hot-reload behavior automatically,
without writing, registering, or wiring any application-specific reload code.

#### Scenario: Consuming application uses standard SSL bundle configuration
- **WHEN** an application embeds this library's TLS support and configures its
  embedded HTTPS server's TLS material via a named `spring.ssl.bundle.jks.<name>`
  (or `.pem.<name>`) bundle with reload enabled, without any
  application-specific registration code for certificate reload
- **THEN** the certificate hot-reload behavior described above is active for that
  application's embedded HTTPS listener

#### Scenario: Consuming application uses legacy, non-bundle SSL configuration
- **WHEN** an application embeds this library's TLS support but configures its
  embedded HTTPS server's TLS material via legacy, non-bundle properties
  (e.g. `server.ssl.key-store`) instead of a named SSL bundle
- **THEN** the certificate hot-reload behavior described above is NOT active for
  that application's embedded HTTPS listener, since Spring Boot does not wire a
  reload mechanism for legacy, non-bundle SSL configuration

### Requirement: Outbound client TLS behavior is unaffected
This capability SHALL NOT change how outbound client connections select, cache,
or reload the key/trust material used to authenticate as a client or validate a
remote server when making outbound calls.

#### Scenario: Outbound call behavior is unchanged
- **WHEN** an outbound call is made to a remote destination after this capability
  is introduced
- **THEN** the key/trust material reload behavior governing that outbound call is
  identical to its behavior before this capability was introduced
