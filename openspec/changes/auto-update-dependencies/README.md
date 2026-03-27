# auto-update-dependencies

**Project:** izgw-core  
**Status:** In Progress — Tasks 1 complete, Tasks 2–12 not started  
**Created:** 2026-03-20  
**Effort:** 18.5 hours

Automated nightly Maven dependency updates (patch/minor only) with CVE prioritization,
minimal `pom.xml` changes, and PR-based workflow.

## Documents

| File | Purpose |
|------|---------|
| [proposal.md](./proposal.md) | Why this is needed, what changes, impact analysis, non-goals |
| [design.md](./design.md) | Full technical architecture, workflow steps, code examples, scheduling, glossary |
| [tasks.md](./tasks.md) | 12 implementation tasks with acceptance criteria and status |

## Update Rules (Quick Reference)

| Change | Auto? | Notes |
|--------|-------|-------|
| Patch (1.2.3 → 1.2.4) | ✅ Yes | |
| Minor (1.2.3 → 1.3.0) | ✅ Yes | |
| Major (1.2.3 → 2.0.0) | ❌ No | **Even for CVE fixes** |
| Bouncy Castle FIPS | ❌ No | Certification implications |
| Spring Boot / izgw-bom parent | ❌ No | BOM / ecosystem coordination |

⚠️ **`allowMajorUpdates=false` must be set in both `pom.xml` plugin config and all `mvn versions:*` commands.**

## Secrets (All Pre-Configured ✅)

| Secret | Purpose |
|--------|---------|
| `GITHUB_TOKEN` | Built-in — GitHub Packages + PR creation |
| `OSS_INDEX_USERNAME` / `OSS_INDEX_PASSWORD` | OWASP CVE scanning |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | Email via AWS SES |

## Review Decisions

1. **CVE + major version** — Only patch/minor applied automatically; major CVEs flagged for manual review
2. **CVE scanning** — GitHub Actions OWASP (`Dependency-Check_Action@main`), not Maven plugin; `-DskipDependencyCheck=true` in all Maven commands
3. **Direct deps only** — BOM-managed dependencies updated separately in the izgw-bom process
4. **izgw-bom version** — Checked and flagged if newer is available; not auto-updated (requires coordination)
5. **Version ranges** — Use `[major.minor,major+1)` syntax in `<dependencyManagement>` overrides
6. **Override cleanup** — Stale overrides removed when BOM catches up
7. **CVE-free goal** — All severities targeted, not just CVSS ≥ 7
8. **Unfixable CVEs** — Noted in PR description; workflow does not fail
9. **Exclusions** — Maintained in `.github/dependency-update-exclusions.txt`
10. **Branch naming** — `automated-maven-updates-YYYYMMDD-HHMMSS`
11. **Ecosystem rollout** — Pattern applied to v2tofhir, izgw-hub, izgw-transform after izgw-core
12. **Merge policy** — Developers review and merge manually; no auto-merge
13. **Notifications** — Email via AWS SES now; Jira integration is a future enhancement

## Risks & Mitigations

| Risk | Mitigation |
|------|-----------|
| Minor version introduces breaking change | Full build + test required before PR creation |
| Too many PRs overwhelming team | Batch multiple updates into single PR per run |
| False-positive CVEs | Existing `dependency-suppression.xml` handles these |
| Build failure | Workflow logs failure, no PR created; sends alert email |
| Transitive dependency conflict | `dependency:tree` diff included in PR description |

## Success Criteria

- Workflow executes successfully on schedule (target: >95%)
- CVE remediation within 1 week of disclosure
- <5% update-related build failures
- <30 minutes per PR review
- Dependencies stay within 30 days of latest patch/minor release

## Status Log

| Date | Status | Notes |
|------|--------|-------|
| 2026-03-20 | Created | Initial change request |
| 2026-03-20 | Requirements Approved | All 13 review questions answered |
| 2026-03-20 | Design Complete | design.md and tasks.md created |
| 2026-03-26 | In Progress | tasks.md populated; Task 1 complete |