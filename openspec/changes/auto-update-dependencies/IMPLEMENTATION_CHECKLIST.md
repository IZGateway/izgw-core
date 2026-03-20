# Implementation Checklist

**Project:** izgw-core  
**Status:** Ready for Implementation  
**Estimated:** 18.5 hours (2.3 days)  
**Date:** 2026-03-20  
**Updated:** 2026-03-20 (All secrets exist ✅, allowMajorUpdates=false critical)

## Pre-Implementation

- [x] Requirements approved
- [x] Design complete
- [x] Tasks defined (12 tasks, 18.5 hours)
- [x] All secrets already configured ✅
- [ ] Developer assigned
- [ ] Start date set

## Day 1 Tasks (8 hours)

- [ ] Task 1: Verify Secrets (0.25h) - All exist ✅
- [ ] Task 2: Add Plugin & Exclusions (1h) - **allowMajorUpdates=false** ⚠️
- [ ] Task 3: Create Workflow (2h)
- [ ] Task 4: Maven Auth (0.5h) - GITHUB_TOKEN (built-in)
- [ ] Task 5: Update Detection (4h) - allowMinorUpdates, cleanup
- [ ] Task 11: Dry Run Part 1 (0.25h)

## Day 2 Tasks (8 hours)

- [ ] Task 6: CVE Scanning (2h) - All severities, unfixable OK
- [ ] Task 7: POM Updates (4h) - Version ranges, cleanup
- [ ] Task 8: Build Validation (2h) - Skip Maven OWASP

## Day 3 Tasks (2.5 hours)

- [ ] Task 9: PR Creation (2h) - security-updates-YYYY-MM-DD-HH:MM
- [ ] Task 10: Email (0.5h) - AWS SES, conditional (PR created only)
- [ ] Task 11: Dry Run Part 2 (0.75h)
- [ ] Task 12: Documentation (2h)

## Testing

- [ ] All secrets accessible (GITHUB_TOKEN, OSS Index, MAIL) ✅
- [ ] **allowMajorUpdates=false** prevents major versions ⚠️ CRITICAL
- [ ] allowMinorUpdates=true shows minor updates
- [ ] Version ranges work: [x.y,x+1)
- [ ] Override cleanup removes unnecessary entries
- [ ] Exclusions config maintained easily
- [ ] CVE scanning (all severities)
- [ ] Unfixable CVEs noted (don't fail)
- [ ] Build passes with -DskipDependencyCheck
- [ ] PR branch: security-updates-YYYY-MM-DD-HH:MM
- [ ] Email ONLY sent when PR created ✅
- [ ] Email uses AWS SES (email-smtp.us-east-1.amazonaws.com)

## Critical Validations

- [ ] **allowMajorUpdates=false** in pom.xml plugin config ⚠️ CRITICAL
- [ ] **allowMajorUpdates=false** in ALL mvn commands ⚠️ CRITICAL
- [ ] No major version updates slip through ⚠️
- [ ] Email conditional: only when PR created
- [ ] GITHUB_TOKEN sufficient (no MAVEN_GPR_TOKEN)
- [ ] All secrets accessible (nothing to configure)

**Total: 18.5 hours** 🚀

**Secrets Status:** ✅ All configured, zero setup needed!
