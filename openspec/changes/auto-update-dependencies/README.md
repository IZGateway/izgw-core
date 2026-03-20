# Automated Maven Dependency Updates

**Status:** Requirements Approved - Design Complete  
**Created:** 2026-03-20  
**Updated:** 2026-03-20 (All secrets exist, AWS SES email, allowMajorUpdates=false)  
**Estimated Effort:** 18.5 hours (2.3 days)  
**Project:** izgw-core

## Overview

This change implements an automated Maven dependency update process that runs on a nightly schedule or manual trigger, checking for patch and minor version updates while addressing CVEs and security vulnerabilities with minimal changes to pom.xml.

## Current State

**Dependency management is currently manual:**
- 60+ direct dependencies declared in pom.xml
- Dependencies inherited from izgw-bom (parent POM version 1.1.0-SNAPSHOT)
- Manual CVE monitoring and remediation
- Infrequent batch updates create integration risk
- No automated tracking of available updates

**Current pom.xml characteristics:**
- Parent: `gov.cdc.izgw:izgw-bom:1.1.0-SNAPSHOT`
- Version: `3.0.0-izgw-core-SNAPSHOT`
- Java: 21
- Key dependencies: Spring Boot, HAPI FHIR, Bouncy Castle FIPS, AWS SDK, Security frameworks
- Build plugins: OWASP dependency-check (12.1.1), JaCoCo, Surefire
- Already configured: CVE scanning with CVSS ≥7 fail threshold

## Proposed Changes

1. **Create GitHub Actions Workflow** - Nightly dependency check and update automation
2. **Configure versions-maven-plugin** - Detect available updates (patch/minor only)
3. **Implement CVE Prioritization** - OWASP dependency-check integration
4. **Minimal POM Strategy** - Use `<dependencyManagement>` overrides for BOM-managed deps
5. **Build Validation** - Full Maven build, tests, and CVE scan before PR
6. **Automated PR Creation** - Generate PR with detailed context and validation results
7. **Documentation** - Team guide for reviewing dependency update PRs

## Key Benefits

- **Improved Security**: Faster CVE response (24-48 hours vs weeks/months)
- **Reduced Maintenance**: Automated detection and updating
- **Better Quality**: Regular incremental updates reduce integration risk
- **Audit Trail**: All changes tracked via PRs and git history
- **Controlled Risk**: Only patch/minor versions updated automatically
- **Focused Updates**: Only direct dependencies, BOM-managed separately
- **Faster CVE Scanning**: GitHub Actions OWASP (updated nightly)

## Key Clarifications (2026-03-20)

Based on review of izgw-hub patterns and requirements:

1. **CVE fixes do NOT bypass major version restriction** - Even for security fixes, only patch/minor updates applied
2. **Use GitHub Actions OWASP** - Following izgw-hub pattern for faster, nightly-updated CVE scanning
3. **Skip Maven OWASP plugin** - Set `-DskipDependencyCheck=true` in Maven builds
4. **Focus on direct dependencies** - BOM-managed dependencies updated separately in izgw-bom process
5. **Check for latest izgw-bom** - Flag if update available, but don't apply (requires coordination)
6. **Version ranges for overrides** - Use `[major.minor,major+1)` syntax to allow automatic minor/patch updates
7. **Remove unnecessary overrides** - Clean up overrides when BOM version catches up
8. **allowMinorUpdates=true** - Set in versions:display-dependency-updates to detect minor version updates
9. **Configurable exclusions** - Maintain exclusion list in `.github/dependency-update-exclusions.txt`
10. **CVE-free goal** - Target all CVE severities, not just high/critical
11. **Branch naming** - Use `security-updates-YYYY-MM-DD-HH:MM` format
12. **Unfixable CVEs** - Note in PR but don't fail workflow

## Update Strategy

### What Gets Updated Automatically
- ✅ **Patch versions** (1.2.3 → 1.2.4): Bug fixes only
- ✅ **Minor versions** (1.2.3 → 1.3.0): New features, backward compatible
- 🔒 **CVE fixes**: Prioritized regardless of version type

### What Requires Manual Review
- ❌ **Major versions** (1.2.3 → 2.0.0): Breaking changes
- ❌ **Spring Boot**: Managed by izgw-bom
- ❌ **izgw-bom parent version**: Ecosystem coordination required
- ❌ **Bouncy Castle FIPS**: Certification implications
- ❌ **Java version**: Major upgrade decision

### Minimal POM Changes Approach
- Add `<dependencyManagement>` section to override specific BOM versions
- Preserve all existing `<dependencies>` declarations unchanged
- Use comments to document why overrides are needed
- No duplication of BOM-managed dependency declarations

## Documents

- [Proposal](./proposal.md) - ✅ Complete - Detailed rationale, impact analysis, non-goals
- [Design](./design.md) - ⏳ To be created - Technical architecture and implementation
- [Tasks](./tasks.md) - ⏳ To be created - Implementation work breakdown
- [Specs](./specs/) - ⏳ To be created - Technical specifications and references

## Dependencies

**Maven Plugins:**
- `versions-maven-plugin:2.16.2` (new) - Update detection
- `dependency-check-maven:12.1.1` (existing) - Disabled in workflow via `-DskipDependencyCheck=true`

**GitHub Actions:**
- `actions/checkout@v4` - Repository checkout
- `actions/setup-java@v4` - Java 21 setup
- `actions/cache@v4` - Maven dependency caching
- `dependency-check/Dependency-Check_Action@main` - CVE scanning
- `dawidd6/action-send-mail@v3` - Email notifications (AWS SES)
- GitHub CLI (`gh`) - PR creation

**Secrets** (All Already Configured ✅):
- `GITHUB_TOKEN` - Built-in for GitHub Packages and PR creation
- `OSS_INDEX_USERNAME` - Already configured ✅
- `OSS_INDEX_PASSWORD` - Already configured ✅
- `MAIL_USERNAME` - Already configured ✅ (AWS SES)
- `MAIL_PASSWORD` - Already configured ✅ (AWS SES)

**Note:** All required secrets already exist! Zero new secret configuration needed.

## Related Projects

This automation pattern could be applied to:
- **dmi-converter** - Data modernization converter service
- **cda2fhir** - CDA to FHIR converter library
- **v2tofhir** - V2 to FHIR converter library
- Other Maven projects in the IZ Gateway ecosystem

## Risks & Mitigations

| Risk | Mitigation |
|------|-----------|
| Minor versions introduce breaking changes | Full build+test validation before PR creation; manual review required |
| Too many PRs overwhelm team | Batch multiple updates into single weekly PR if needed |
| False positive CVEs | Use existing dependency-suppression.xml for known false positives |
| Build failures block automation | Workflow logs failure but doesn't create PR; creates issue instead |
| Transitive dependency conflicts | Include dependency:tree analysis in PR |
| Downstream project breaks | izgw-core follows normal release process; downstream projects update on their schedule |

## Success Criteria

After implementation, measure:
- ✅ Workflow executes successfully on schedule (target: >95% success rate)
- ✅ CVE remediation time (target: <1 week from disclosure)
- ✅ Build stability (target: <5% update-related build breaks)
- ✅ Team efficiency (target: <30 minutes per PR review)
- ✅ Dependency freshness (target: <30 days behind latest patch/minor)

## Review Questions

**Before proceeding with implementation, please review:**

1. **Scope**: Should this apply only to izgw-core, or coordinate with other IZ Gateway projects?
   - ✅ **ANSWER**: This pattern will be applied to other projects (v2tofhir, izgw-hub, izgw-transform, etc.)
   
2. **Schedule**: Is nightly (Mon-Fri) appropriate, or prefer weekly?
   - ✅ **ANSWER**: Monday-Friday in 2-6 AM Eastern time frame, staggered by project dependencies:
     - izgw-bom first
     - v2tofhir and izgw-core next
     - izgw-hub and izgw-transform last
   
3. **BOM coordination**: How should we coordinate with izgw-bom updates?
   - ✅ **ANSWER**: Assume when job starts that another process has updated izgw-bom dependencies. Ignore coordination problem for now.
   
4. **FIPS dependencies**: Should Bouncy Castle FIPS modules be excluded from automation?
   - ✅ **ANSWER**: Yes, Bouncy Castle FIPS modules (bc-fips, bcpkix-fips, bctls-fips) should NOT be automated.
   
5. **Reviewer assignment**: Who should be auto-assigned to review dependency PRs?
   - ✅ **ANSWER**: Developers will review and approve changes in appropriate order for projects.
   
6. **Merge policy**: Should PRs auto-merge to develop after approval, or require manual merge?
   - ✅ **ANSWER**: Developers will perform merges manually as appropriate.
   
7. **Notification**: Should we create Slack/email notifications for new dependency PRs?
   - ✅ **ANSWER**: Email notifications for now. Future: Jira ticket integration (separate feature).

## Next Steps

1. ✅ **Create openspec change request** - Complete (this document)
2. ⏳ **Team review** - Review proposal and answer review questions (1-2 days)
3. ⏳ **Create design document** - Technical architecture and workflow design
4. ⏳ **Break down into tasks** - Detailed implementation plan
5. ⏳ **Assign developer** - Resource allocation
6. ⏳ **Implement workflow** - Development work (2-2.5 days)
7. ⏳ **Test with dry run** - Validate with test updates (0.5 day)
8. ⏳ **Enable on schedule** - Production deployment
9. ⏳ **Monitor first month** - Track metrics and adjust as needed

---

**Status Log:**

| Date | Status | Notes |
|------|--------|-------|
| 2026-03-20 | Created | Initial change request - awaiting requirements review |
| 2026-03-20 | Requirements Approved | Team answered 7 review questions; proceeding to design phase |
| TBD | Design Complete | Technical design document created |
| TBD | In Progress | Implementation started |
| TBD | Testing | Workflow validation in progress |
| TBD | Complete | Workflow enabled and monitoring |
