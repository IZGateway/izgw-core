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
  - date: '2026-10-19T14:35:34.472Z'
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
      Correct spec: reload-on-update does not work on network filesystems;
      document the actual poller mechanism and its consumer-scheduling
      requirement; Shorten network-filesystem requirement text to satisfy
      openspec validate length warning; Shorten scheduled-check requirement text
      to satisfy openspec validate length warning
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

### Requirement: Certificate hot-reload works on network filesystems, not just local disk
The reload mechanism SHALL detect a certificate rotation regardless of whether the
keystore file lives on local disk or a network filesystem (e.g. NFS/AWS EFS)
shared with the process that performs the rotation, by polling file modification
time rather than relying on a filesystem change-event API (see design.md Context
for why the latter does not reliably work on a network filesystem).

#### Scenario: Keystore file is rotated by a different process sharing the same network filesystem
- **WHEN** the on-disk keystore file is replaced by a process other than the one
  running the embedded HTTPS listener, on a filesystem shared over a network
  protocol (e.g. NFS/EFS)
- **THEN** the embedded HTTPS listener still picks up the new certificate within
  the system's existing certificate-change detection interval

### Requirement: Certificate hot-reload requires a named SSL bundle and a scheduled check; no other custom code
Any application that embeds this library's TLS support, configures its embedded
HTTPS server's TLS material as a named Spring Boot SSL bundle, and schedules a
periodic call to this library's reload-check operation SHALL receive the
certificate hot-reload behavior described above, with no other consumer-side
implementation (the detect-and-rebuild logic is entirely this library's).

#### Scenario: Consuming application uses a named SSL bundle and schedules the reload check
- **WHEN** an application embeds this library's TLS support, configures its
  embedded HTTPS server's TLS material via a named `spring.ssl.bundle.jks.<name>`
  bundle, and periodically invokes this library's reload-check operation (e.g. via
  its own `@Scheduled` bean)
- **THEN** the certificate hot-reload behavior described above is active for that
  application's embedded HTTPS listener, without the application implementing any
  change-detection or bundle-rebuilding logic itself

#### Scenario: Consuming application uses legacy, non-bundle SSL configuration
- **WHEN** an application embeds this library's TLS support but configures its
  embedded HTTPS server's TLS material via legacy, non-bundle properties
  (e.g. `server.ssl.key-store`) instead of a named SSL bundle
- **THEN** the certificate hot-reload behavior described above is NOT active for
  that application's embedded HTTPS listener, since there is no named bundle for
  this library's reload-check operation to update

#### Scenario: Consuming application never schedules the reload check
- **WHEN** an application configures a named SSL bundle but never invokes this
  library's reload-check operation on any schedule
- **THEN** the certificate hot-reload behavior described above is NOT active for
  that application's embedded HTTPS listener, since nothing ever calls it

### Requirement: Outbound client TLS behavior is unaffected
This capability SHALL NOT change how outbound client connections select, cache,
or reload the key/trust material used to authenticate as a client or validate a
remote server when making outbound calls.

#### Scenario: Outbound call behavior is unchanged
- **WHEN** an outbound call is made to a remote destination after this capability
  is introduced
- **THEN** the key/trust material reload behavior governing that outbound call is
  identical to its behavior before this capability was introduced
