# Quick Reference: auto-update-dependencies

**TL;DR:** Automated nightly Maven dependency updates (patch/minor only) with CVE prioritization, minimal pom.xml changes, and PR-based workflow.

---

## 🚦 Status: Requirements Approved - Design Complete

**Project:** izgw-core  
**Effort:** 18.5 hours (2.3 days)  
**Created:** 2026-03-20  
**Approved:** 2026-03-20  
**Updated:** 2026-03-20 (All secrets exist ✅, AWS SES, allowMajorUpdates=false critical)

## 📋 What It Does

```
Nightly 3AM → Check Maven updates → Filter (patch/minor, exclude major)
→ Prioritize CVEs → Update pom.xml (minimal changes) → Build + Test
→ CVE scan → Create PR with details → Manual review required
```

## 🎯 Key Features

- ✅ **Nightly schedule** (Mon-Fri 4AM ET / 9AM UTC) or manual trigger
- ✅ **Patch/minor only** - No major version updates (**even for CVE fixes**)
- ✅ **CVE-free goal** - Target all severities (not just high/critical)
- ✅ **CVE priority** - Security fixes first (within patch/minor constraints)
- ✅ **Version ranges** - Use `[1.12,2)` syntax for auto minor/patch pickup
- ✅ **Override cleanup** - Remove unnecessary overrides when BOM catches up
- ✅ **Minimal POM changes** - Use `<dependencyManagement>` overrides
- ✅ **Full validation** - Build, tests, CVE scan before PR
- ✅ **Rich context** - PR includes update table, CVE details, test results
- ✅ **Direct deps focus** - BOM-managed dependencies handled separately
- ✅ **GitHub Actions OWASP** - Fast, nightly-updated CVE scanning (like izgw-hub)
- ✅ **izgw-bom check** - Flags if newer BOM version available
- ✅ **Configurable exclusions** - Easy-to-maintain config file
- ✅ **Branch naming** - `security-updates-YYYY-MM-DD-HH:MM`

## 🔑 Update Rules

| Change | Auto? | Example | Rationale |
|--------|-------|---------|-----------|
| Patch | ✅ Yes | 1.2.3 → 1.2.4 | Bug fixes only |
| Minor | ✅ Yes | 1.2.3 → 1.3.0 | Backward compatible |
| Major | ❌ No | 1.2.3 → 2.0.0 | Breaking changes **even for CVEs** |
| CVE Fix | 🔒 Priority | Patch/minor only | Security critical (flag major for manual) |

## ⚠️ CRITICAL Configuration

**allowMajorUpdates=false** must be set in:
1. Plugin configuration in pom.xml
2. All `mvn versions:*` commands in workflow
3. Verified in testing before enabling schedule

**Failure to set this will allow major version updates and break builds!**

## 🚫 Never Auto-Update

- Spring Boot (managed by izgw-bom)
- izgw-bom parent version
- Bouncy Castle FIPS modules (in exclusions config)
- Java version
- Dependencies in `.github/dependency-update-exclusions.txt`

## 📊 Current State: izgw-core

- **Parent POM:** izgw-bom 1.1.0-SNAPSHOT
- **Version:** 3.0.0-izgw-core-SNAPSHOT
- **Java:** 21
- **Direct Dependencies:** 60+
- **CVE Threshold:** CVSS ≥7 (already configured)

## 🛠️ Minimal POM Strategy

**Version Range Example:**
```xml
<dependencyManagement>
    <dependencies>
        <!-- Override from BOM for CVE - allows auto minor/patch updates -->
        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-text</artifactId>
            <version>[1.12,2)</version>  <!-- Min 1.12, allows 1.x, not 2.x -->
        </dependency>
    </dependencies>
</dependencyManagement>
```

**Benefits:**
- Auto pickup of 1.12 → 1.13 → 1.14
- Prevents major version jumps
- Reduces future updates
- BOM can catch up and override removed

## ❓ Review Questions

**Answered 2026-03-20:**

1. ✅ **Scope:** Pattern will be applied to v2tofhir, izgw-hub, izgw-transform, etc.
2. ✅ **Schedule:** Mon-Fri, 2-6 AM ET, staggered:
   - 2:00 AM: izgw-bom
   - 4:00 AM: izgw-core
   - 4:30 AM: v2tofhir
   - 5:00 AM: izgw-hub
   - 5:30 AM: izgw-transform
3. ✅ **BOM coordination:** Assume izgw-bom updated before job starts
4. ✅ **FIPS:** BC-FIPS modules (bc-fips, bcpkix-fips, bctls-fips) excluded
5. ✅ **Reviewers:** Developers review and approve in project order
6. ✅ **Merge:** Manual merge by developers
7. ✅ **Notification:** Email for now; Jira integration later

## 📁 Files Created

```
izgw-core/
├── .github/
│   └── dependency-update-exclusions.txt  ✅ NEW - BC-FIPS + maintainable list
└── openspec/changes/auto-update-dependencies/
    ├── .openspec.yaml                    ✅ Metadata
    ├── README.md                         ✅ Overview (12 clarifications)
    ├── QUICK_REFERENCE.md                ✅ This file
    ├── proposal.md                       ✅ Rationale (GITHUB_TOKEN, version ranges)
    ├── design.md                         ✅ Architecture (allowMajorUpdates=false)
    ├── tasks.md                          ✅ 12 tasks, 19 hours
    ├── IMPLEMENTATION_CHECKLIST.md       ✅ Checklist (updated)
    ├── FINAL_SUMMARY.md                  ✅ Summary
    └── specs/
        ├── README.md                     ✅ Specs (CRITICAL notes)
        └── workflow-examples.md          ✅ Code examples
```

## 🚀 Ready for Implementation

**All design documents complete and finalized!**

1. ✅ Requirements reviewed and approved
2. ✅ Design document created
3. ✅ Tasks broken down (12 tasks, 19 hours)
4. ✅ Specs and references documented
5. ✅ **allowMajorUpdates=false** emphasized throughout
6. ✅ GITHUB_TOKEN usage (no MAVEN_GPR_TOKEN needed)
7. ✅ OSS Index already configured
8. ⏳ Ready to assign developer

**Secrets Needed:**
- ✅ GITHUB_TOKEN (built-in)
- ✅ OSS_INDEX_USERNAME (exists)
- ✅ OSS_INDEX_PASSWORD (exists)
- ⚠️ EMAIL_USERNAME (needs config)
- ⚠️ EMAIL_PASSWORD (needs config)
- ⚠️ DEV_TEAM_EMAIL (needs config)

## 🔗 Related Documents

- [Full README](./README.md) - Complete overview
- [Proposal](./proposal.md) - Detailed rationale and impact

---

**Change Request Complete!** All documentation ready for implementation. Next: Assign developer to execute the 12 tasks (18 hours estimated).
