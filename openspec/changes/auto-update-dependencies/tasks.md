# Tasks: Automated Maven Dependency Updates

**Project:** izgw-core  
**Total Estimated Effort:** 18.5 hours (2.3 days)  
**Created:** 2026-03-20  
**Updated:** 2026-03-20 (All secrets already exist, AWS SES email config)

## Task Breakdown

### Task 1: Verify Repository Secrets
**Estimated:** 0.25 hours  
**Priority:** High  
**Dependencies:** None

**Description:**
Verify all required GitHub secrets are already configured and accessible.

**Subtasks:**
1. Verify existing secrets are accessible:
   - GITHUB_TOKEN (built-in ✅)
   - OSS_INDEX_USERNAME (already configured ✅)
   - OSS_INDEX_PASSWORD (already configured ✅)
   - MAIL_USERNAME (already configured ✅)
   - MAIL_PASSWORD (already configured ✅)

**Acceptance Criteria:**
- [ ] All secrets verified as accessible in workflow test run
- [ ] GITHUB_TOKEN works for GitHub Packages authentication
- [ ] OSS Index credentials work with dependency-check action
- [ ] Mail credentials work with action-send-mail

**Files Changed:**
- None (verification only)

**Testing:**
- Create simple test workflow that accesses each secret
- Verify no authentication errors

**Note:** All required secrets already exist! No configuration needed.

---

### Task 2: Add versions-maven-plugin and Exclusions Config
**Estimated:** 1 hour  
**Priority:** High  
**Dependencies:** None

**Description:**
Add versions-maven-plugin with exclusions config file for maintainable dependency exclusions.

**Subtasks:**
1. Create `.github/dependency-update-exclusions.txt` file
2. Add BC-FIPS modules to exclusions list
3. Add plugin to `<build><plugins>` section in pom.xml
4. Configure update rules:
   - **allowMajorUpdates=false** (CRITICAL - prevents major version jumps)
   - allowMinorUpdates=true
   - allowIncrementalUpdates=true
5. Test plugin locally

**Acceptance Criteria:**
- [ ] `.github/dependency-update-exclusions.txt` created with BC-FIPS modules
- [ ] Plugin added to pom.xml
- [ ] **allowMajorUpdates=false** explicitly set in plugin configuration
- [ ] allowMinorUpdates=true set
- [ ] allowIncrementalUpdates=true set
- [ ] No hardcoded exclusions in plugin (uses config file)
- [ ] `mvn versions:display-dependency-updates -DallowMinorUpdates=true` runs successfully
- [ ] BC-FIPS modules not listed in output
- [ ] Major version updates not shown in output
- [ ] Build still succeeds

**Files Changed:**
- `.github/dependency-update-exclusions.txt` (new)
- `pom.xml` (add plugin configuration)

**Testing:**
```bash
mvn versions:display-dependency-updates -DallowMinorUpdates=true
# Verify BC-FIPS modules not listed
# Verify major version updates not shown
# Verify minor/patch updates are shown
```

---

### Task 3: Create Workflow File Structure
**Estimated:** 2 hours  
**Priority:** High  
**Dependencies:** Task 1

**Description:**
Create the GitHub Actions workflow file with basic structure, triggers, and permissions.

**Subtasks:**
1. Create `.github/workflows/maven-dependency-updates.yml`
2. Configure workflow triggers:
   - Cron schedule: '0 9 * * 1-5' (4 AM ET)
   - workflow_dispatch with dry_run input
3. Set up permissions (contents: write, pull-requests: write, issues: write)
4. Define environment variables (MAVEN_OPTS, JAVA_VERSION)
5. Add checkout and Java setup steps
6. Configure Maven cache

**Acceptance Criteria:**
- [ ] Workflow file created
- [ ] Schedule configured for 9:00 AM UTC (4 AM ET)
- [ ] Manual trigger works
- [ ] Permissions properly set
- [ ] Workflow appears in Actions tab
- [ ] Can be triggered manually

**Files Changed:**
- `.github/workflows/maven-dependency-updates.yml` (new)

**Testing:**
- Trigger workflow manually with dry_run=true
- Verify checkout and Java setup work

---

### Task 4: Implement Maven Authentication
**Estimated:** 0.5 hours  
**Priority:** High  
**Dependencies:** Task 1, Task 3

**Description:**
Configure Maven settings.xml generation for GitHub Packages authentication using built-in GITHUB_TOKEN.

**Subtasks:**
1. Add step to generate ~/.m2/settings.xml
2. Configure GitHub Packages server authentication
3. Use built-in GITHUB_TOKEN secret (not MAVEN_GPR_TOKEN)
4. Test dependency resolution from GitHub Packages

**Acceptance Criteria:**
- [ ] settings.xml generated correctly
- [ ] GITHUB_TOKEN properly referenced (built-in secret)
- [ ] Maven can resolve izgw-bom from GitHub Packages
- [ ] Private dependencies resolve successfully (public packages)

**Files Changed:**
- `.github/workflows/maven-dependency-updates.yml` (update)

**Testing:**
```bash
# In workflow
mvn dependency:resolve
# Should successfully resolve all dependencies using GITHUB_TOKEN
```

**Note:** No need for MAVEN_GPR_TOKEN - GITHUB_TOKEN provides access to public GitHub Package repositories.

---

### Task 5: Implement Update Detection
**Estimated:** 4 hours  
**Priority:** High  
**Dependencies:** Task 2, Task 4

**Description:**
Detect updates with allowMinorUpdates=true, check izgw-bom, filter BOM-managed deps, apply exclusions, and identify unnecessary overrides for cleanup.

**Subtasks:**
1. Check for latest izgw-bom version from develop branch
   - Fetch from GitHub raw URL
   - Compare with current version
   - Flag if update available (for manual coordination)
2. Load exclusions from `.github/dependency-update-exclusions.txt`
3. Run `versions:display-dependency-updates -DallowMinorUpdates=true`
4. Extract BOM-managed dependencies from effective POM
5. Parse output to identify available updates
6. Separate BOM-managed from direct dependencies
7. Focus filtering on direct (non-BOM) dependencies
8. Filter out major version updates
9. Check for unnecessary overrides (where BOM version >= override version)
10. Generate removal list for outdated overrides
11. Generate structured update list
12. Handle "no updates" case gracefully
13. Count total updates for reporting

**Acceptance Criteria:**
- [ ] Workflow checks for latest izgw-bom version
- [ ] BOM update flagged if available (not applied automatically)
- [ ] Exclusions loaded from config file and applied
- [ ] versions:display-dependency-updates called with `-DallowMinorUpdates=true`
- [ ] Workflow detects available updates for direct dependencies
- [ ] BOM-managed dependencies identified and separated
- [ ] Only direct dependencies included in update list
- [ ] Major versions filtered out
- [ ] Exclusions from config file respected
- [ ] Unnecessary overrides identified for removal
- [ ] Update list generated in parseable format
- [ ] Workflow exits gracefully when no updates
- [ ] Update count available for summary

**Files Changed:**
- `.github/workflows/maven-dependency-updates.yml` (update)

**Testing:**
- Test override cleanup detection logic
- Verify allowMinorUpdates=true shows minor version updates
- Test with dependencies that have updates available
- Test with no updates available
- Verify exclusions from config file respected
- Verify major versions excluded
- Verify BOM-managed deps separated correctly
- Verify izgw-bom version check works

---

### Task 6: Implement CVE Scanning Integration
**Estimated:** 2 hours  
**Priority:** High  
**Dependencies:** Task 4

**Description:**
Integrate GitHub Actions OWASP dependency-check (following izgw-hub pattern) to identify CVE-affected dependencies before and after updates.

**Subtasks:**
1. Build JAR before updates (with `-DskipDependencyCheck=true`)
2. Run GitHub Actions dependency-check before updates (failOnCVSS=0)
3. Parse CVE report (JSON format)
4. Extract list of CVE-affected dependencies
5. Identify which CVEs can be fixed with patch/minor updates
6. Flag CVEs requiring major version updates for manual review
7. Rebuild JAR after updates
8. Run GitHub Actions dependency-check after updates (failOnCVSS=7)
9. Compare before/after to generate remediation report
10. Fail if new high/critical CVEs introduced
11. Include CVE data formatted for PR

**Acceptance Criteria:**
- [ ] JAR built before CVE scan (skipping Maven OWASP plugin)
- [ ] GitHub Actions OWASP check runs before updates
- [ ] CVE-affected dependencies identified
- [ ] CVE severity and scores extracted
- [ ] Patch/minor fixable CVEs separated from major version CVEs
- [ ] JAR built after updates
- [ ] GitHub Actions OWASP check runs after updates
- [ ] Remediation report generated
- [ ] Workflow fails if new CVEs introduced (CVSS ≥7)
- [ ] CVE data formatted for PR
- [ ] OSS Index credentials used correctly

**Files Changed:**
- `.github/workflows/maven-dependency-updates.yml` (update)

**Artifacts:**
- CVE report (before) - HTML and JSON
- CVE report (after) - HTML and JSON
- CVE remediation summary

**Testing:**
- Test with dependency that has known CVE
- Verify CVE is detected
- Verify remediation tracked
- Verify major version CVEs flagged separately
- Test OSS Index authentication

---

### Task 7: Implement POM Update Logic
**Estimated:** 4 hours  
**Priority:** High  
**Dependencies:** Task 5

**Description:**
Remove unnecessary overrides, update direct dependencies with allowMinorUpdates=true, and apply version ranges for any new BOM overrides needed.

**Subtasks:**
1. Remove unnecessary overrides (where BOM version >= override version)
   - Use xmlstarlet to remove override entries
   - Clean up empty dependencyManagement section if needed
2. Update direct dependencies with `versions:use-latest-releases`
   - Set `-DallowMinorUpdates=true`
   - Apply exclusions from config file
3. For any new BOM overrides needed (CVE fixes):
   - Calculate version range: [major.minor,major+1)
   - Add to dependencyManagement with version range
   - Add XML comments explaining override and range
4. Ensure only patch/minor versions updated (no major versions)
5. Validate pom.xml is well-formed after updates
6. Create git commit with changes
7. Include override cleanup summary in commit message

**Acceptance Criteria:**
- [ ] Unnecessary overrides removed from pom.xml
- [ ] Empty dependencyManagement section cleaned up if all overrides removed
- [ ] Updates applied to direct dependencies with allowMinorUpdates=true
- [ ] Only patch/minor versions updated
- [ ] New BOM overrides (if any) use version range syntax [x.y,x+1)
- [ ] Version ranges documented in comments
- [ ] pom.xml remains valid XML
- [ ] Comments added for clarity
- [ ] Changes committed to git
- [ ] Commit message includes override cleanup count
- [ ] Commit message follows convention

**Files Changed:**
- `.github/workflows/maven-dependency-updates.yml` (update)
- `pom.xml` (updated by workflow - direct deps and cleaned overrides)

**Testing:**
- Test override removal logic
- Test version range calculation and application
- Test with single direct dependency update
- Test with multiple direct dependency updates
- Verify BOM-managed deps not modified
- Verify pom.xml structure preserved
- Verify XML validity
- Verify version ranges work correctly
2. Ensure only patch/minor versions updated (no major versions)
3. Skip BOM-managed dependencies (unless CVE override needed)
4. Add XML comments explaining any overrides
5. Validate pom.xml is well-formed after updates
6. Create git commit with changes
7. Include BOM version info in commit message if update available

**Acceptance Criteria:**
- [ ] Updates applied to direct dependencies in pom.xml
- [ ] Only patch/minor versions updated
- [ ] BOM-managed dependencies unchanged (unless override added)
- [ ] pom.xml remains valid XML
- [ ] Comments added for clarity
- [ ] Changes committed to git
- [ ] Commit message follows convention
- [ ] BOM update flagged in commit if applicable

**Files Changed:**
- `.github/workflows/maven-dependency-updates.yml` (update)
- `pom.xml` (updated by workflow for direct dependencies only)

**Testing:**
- Test with single direct dependency update
- Test with multiple direct dependency updates
- Verify BOM-managed deps not modified
- Verify pom.xml structure preserved
- Verify XML validity

---

### Task 8: Implement Build Validation
**Estimated:** 2 hours  
**Priority:** High  
**Dependencies:** Task 7

**Description:**
Run full Maven build and test suite to validate updates don't break the build. Use `-DskipDependencyCheck=true` since CVE scanning is done via GitHub Actions.

**Subtasks:**
1. Run `mvn clean install -DskipDependencyCheck=true`
2. Execute all tests (unit + integration)
3. Generate JaCoCo coverage report
4. Generate dependency tree
5. Check for dependency conflicts
6. Upload build artifacts (logs, reports)
7. Fail workflow if build fails
8. Note: CVE scanning performed by separate GitHub Action step

**Acceptance Criteria:**
- [ ] Full Maven build executes with `-DskipDependencyCheck=true`
- [ ] All tests run and pass
- [ ] JaCoCo report generated
- [ ] Dependency tree generated
- [ ] Conflicts detected and reported
- [ ] Build failure prevents PR creation
- [ ] Artifacts uploaded for review
- [ ] Maven OWASP plugin skipped (GitHub Action used instead)

**Files Changed:**
- `.github/workflows/maven-dependency-updates.yml` (update)

**Artifacts:**
- Build logs
- Test results (JUnit XML)
- JaCoCo coverage report
- Dependency tree

**Testing:**
- Test with valid updates (should pass)
- Test with breaking update (should fail)
- Verify artifacts are accessible
- Verify `-DskipDependencyCheck=true` is effective

---

### Task 9: Implement PR Creation
**Estimated:** 2 hours  
**Priority:** High  
**Dependencies:** Task 8

**Description:**
Generate and create pull request with comprehensive update information.

**Subtasks:**
1. Create new branch for updates
2. Generate PR description using template
3. Include dependency update table
4. Include CVE remediation details
5. Add build/test results
6. Link to workflow artifacts
7. Create PR using GitHub CLI
8. Apply labels (dependencies, automated, security)
9. Target develop branch

**Acceptance Criteria:**
- [ ] Branch created with timestamp
- [ ] PR description comprehensive and formatted
- [ ] Update table included
- [ ] CVE information included
- [ ] Artifacts linked
- [ ] PR created successfully
- [ ] Labels applied
- [ ] Targets develop branch

**Files Changed:**
- `.github/workflows/maven-dependency-updates.yml` (update)

**Testing:**
- Create test PR
- Verify all information included
- Verify formatting correct
- Verify artifact links work

---

### Task 10: Implement Email Notifications
**Estimated:** 0.5 hours  
**Priority:** Medium  
**Dependencies:** Task 9

**Description:**
Configure email notifications using existing MAIL_USERNAME and MAIL_PASSWORD with AWS SES. Send only when PR is created.

**Subtasks:**
1. Add email notification step after PR creation (conditional)
2. Use AWS SES SMTP configuration (email-smtp.us-east-1.amazonaws.com:465)
3. Include PR number, URL, and summary in email
4. Use existing recipient list (kboone@ainq.com, weckels@ainq.com, pcahill@ainq.com)
5. CC devops@izgateway.opsgenie.net
6. Add failure notification step (only if updates detected but failed)
7. Include failure details and logs link in failure email
8. Test email delivery

**Acceptance Criteria:**
- [ ] Email sent only when PR is created (conditional logic)
- [ ] No email sent when no updates available
- [ ] Uses existing MAIL_USERNAME and MAIL_PASSWORD secrets
- [ ] AWS SES configuration correct (server, port, secure: true)
- [ ] Email includes PR link, update summary, CVE counts
- [ ] Recipient list matches existing pattern
- [ ] Failure email sent only if updates detected but workflow failed
- [ ] Failure email includes logs link
- [ ] Email format is readable and informative

**Files Changed:**
- `.github/workflows/maven-dependency-updates.yml` (update)

**Testing:**
- Trigger workflow and verify PR email sent
- Verify no email when no updates
- Simulate failure and verify failure email
- Check email formatting and links
- Verify AWS SES delivery

**Email Template:**
- From: GithubActionNotification <GithubActionNotification@izgateway.org>
- To: kboone@ainq.com,weckels@ainq.com,pcahill@ainq.com
- CC: devops@izgateway.opsgenie.net
- Subject: [izgw-core] Automated Dependency Updates PR #XXX

---

### Task 11: Add Dry Run Mode
**Estimated:** 1 hour  
**Priority:** Medium  
**Dependencies:** Task 5

**Description:**
Implement dry run mode for testing without creating PRs.

**Subtasks:**
1. Add conditional logic for dry_run input
2. Show what updates would be applied
3. Skip POM modification in dry run
4. Skip PR creation in dry run
5. Output summary to workflow summary

**Acceptance Criteria:**
- [ ] dry_run input works
- [ ] Shows pending updates without applying
- [ ] No PR created in dry run mode
- [ ] Summary visible in workflow output
- [ ] Useful for testing and validation

**Files Changed:**
- `.github/workflows/maven-dependency-updates.yml` (update)

**Testing:**
- Run with dry_run=true
- Verify no changes made
- Verify summary shows pending updates

---

### Task 12: Documentation and Testing
**Estimated:** 2 hours  
**Priority:** Medium  
**Dependencies:** Task 11

**Description:**
Create comprehensive documentation and test all scenarios.

**Subtasks:**
1. Add workflow documentation comment block
2. Update project README with automation info
3. Create troubleshooting guide
4. Test all scenarios:
   - No updates available
   - Single update
   - Multiple updates
   - CVE remediation
   - Build failure
   - Dependency conflict
5. Document common issues and resolutions

**Acceptance Criteria:**
- [ ] Workflow well-documented
- [ ] README updated
- [ ] Troubleshooting guide created
- [ ] All scenarios tested
- [ ] Test results documented

**Files Changed:**
- `.github/workflows/maven-dependency-updates.yml` (add comments)
- `README.md` (add automation section)
- `docs/DEPENDENCY_AUTOMATION.md` (new, optional)

**Testing:**
- Run through all test scenarios
- Document results
- Verify documentation accuracy

---

## Task Dependencies Graph

```
Task 1 (Secrets) ────────┐
   │                      │
   ↓                      ↓
Task 2 (Plugin)     Task 3 (Workflow) ──→ Task 4 (Auth) ──┐
   │                                                        │
   │                                                        │
   └────────────────┬───────────────────────────────────────┘
                    ↓
           Task 5 (Update Detection) ────────────┐
                    │                             │
                    ↓                             │
           Task 6 (CVE Scanning) ────────────────┤
                    │                             │
                    ↓                             │
           Task 7 (POM Updates) ─────────────────┤
                    │                             │
                    ↓                             │
           Task 8 (Build Validation) ────────────┤
                    │                             │
                    ↓                             │
           Task 9 (PR Creation) ──────────────────┤
                    │                             │
                    ↓                             │
           Task 10 (Email) ───────────────────────┤
                    │                             │
                    ↓                             │
           Task 11 (Dry Run) ─────────────────────┘
                    │
                    ↓
           Task 12 (Documentation)
```

## Implementation Order

**Day 1 (8 hours):**
1. Task 1: Verify Secrets (0.25h) - All already exist
2. Task 2: Add Plugin and Exclusions Config (1h)
3. Task 3: Create Workflow Structure (2h)
4. Task 4: Implement Authentication (0.5h) - Use GITHUB_TOKEN
5. Task 5: Implement Update Detection (4h)
6. Task 11: Add Dry Run Mode (0.25h of 1h)

**Day 2 (8 hours):**
1. Task 6: Implement CVE Scanning (2h)
2. Task 7: Implement POM Updates with Override Cleanup (4h)
3. Task 8: Implement Build Validation (2h)

**Day 3 (2.5 hours):**
1. Task 9: Implement PR Creation (2h)
2. Task 10: Implement Email Notifications (0.5h) - Use existing mail secrets
3. Task 11: Complete Dry Run Mode (0.75h remaining)
4. Task 12: Documentation and Testing (2h)

**Total: 18.5 hours (2.3 days)**

## Success Metrics

After implementation, track:
- ✅ Workflow execution success rate (target: >95%)
- ✅ Time from update availability to PR creation (target: <24h)
- ✅ CVE remediation time (target: <1 week)
- ✅ Build break rate from updates (target: <5%)
- ✅ Average PR review time (target: <48h)

## Rollback Plan

If automation causes issues:
1. **Immediate:** Disable cron schedule (keep manual trigger)
2. **Short-term:** Fix identified issues
3. **Testing:** Re-enable with dry run mode
4. **Re-enable:** Gradual rollout (manual → weekly → nightly)

## Post-Implementation Checklist

After all tasks complete:
- [ ] Workflow runs successfully on manual trigger
- [ ] Dry run mode tested
- [ ] All secrets configured and working
- [ ] Email notifications delivered
- [ ] PR format meets requirements
- [ ] Build validation working
- [ ] CVE scanning working
- [ ] BC-FIPS modules excluded
- [ ] Documentation complete
- [ ] Team trained on reviewing PRs

## Next: Apply to Other Projects

**After izgw-core validated (Week 3-4):**

1. **v2tofhir** (4:30 AM ET)
   - Copy workflow
   - Adjust schedule
   - Test separately

2. **izgw-hub** (5:00 AM ET)
   - Copy workflow
   - Adjust schedule
   - Test separately

3. **izgw-transform** (5:30 AM ET)
   - Copy workflow
   - Adjust schedule
   - Test separately

**Coordination:**
- Ensure izgw-core PRs merged before dependent projects run
- Monitor dependency chain updates
- Adjust schedules if conflicts arise
