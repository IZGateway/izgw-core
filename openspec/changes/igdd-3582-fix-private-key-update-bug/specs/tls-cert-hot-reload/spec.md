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
updated: []
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

### Requirement: Certificate hot-reload requires no action from consuming applications
Any application that embeds this library's TLS support and starts an embedded
HTTPS server through the standard mechanism SHALL receive the certificate
hot-reload behavior automatically, without registering, configuring, or wiring
anything itself.

#### Scenario: Consuming application performs no explicit registration
- **WHEN** an application embeds this library's TLS support and starts its own
  embedded HTTPS server through the standard mechanism, without any
  application-specific registration code for certificate reload
- **THEN** the certificate hot-reload behavior described above is active for that
  application's embedded HTTPS listener

### Requirement: Outbound client TLS behavior is unaffected
This capability SHALL NOT change how outbound client connections select, cache,
or reload the key/trust material used to authenticate as a client or validate a
remote server when making outbound calls.

#### Scenario: Outbound call behavior is unchanged
- **WHEN** an outbound call is made to a remote destination after this capability
  is introduced
- **THEN** the key/trust material reload behavior governing that outbound call is
  identical to its behavior before this capability was introduced
