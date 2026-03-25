# Tasks: Automated Maven Dependency Updates

**Project:** izgw-core  
**Change Request:** auto-update-dependencies  
**Created:** 2026-03-20  
**Last Updated:** 2026-03-25  
**Overall Status:** In Progress  
**Total Estimated Effort:** 18.5 hours

---

## Task 1 — Verify Secrets and Pre-conditions
**Estimated:** 0.25 hours  
**Status:** ✅ COMPLETED

All required GitHub Actions secrets are confirmed to exist. Zero configuration needed.

| Secret | Status |
|--------|--------|
| `GITHUB_TOKEN` | ✅ Built-in |
| `OSS_INDEX_USERNAME` | ✅ Exists |
| `OSS_INDEX_PASSWORD` | ✅ Exists |
| `MAIL_USERNAME` | ✅ Exists (AWS SES) |
| `MAIL_PASSWORD` | ✅ Exists (AWS SES) |

**Also completed:** `.github/dependency-update-exclusions.txt` created and pre-populated with BC-FIPS exclusions (bc-fips, bcpkix-fips, bctls-fips).

**Acceptance Criteria:**
- [x] All secrets confirmed accessible
- [x] `dependency-update-exclusions.txt` exists with BC-FIPS modules excluded

---

## Task 2 — Add `versions-maven-plugin` to `pom.xml`
**Estimated:** 1 hour  
**Status:** ❌ NOT STARTED

Add the `versions-maven-plugin` to the `<build><plugins>` section of `pom.xml` with the required configuration to enforce `allowMajorUpdates=false`.

**Changes Required:**
- Add plugin to `pom.xml` `<build><plugins>`:
  ```xml
  <plugin>
      <groupId>org.codehaus.mojo</groupId>
      <artifactId>versions-maven-plugin</artifactId>
      <version>2.16.2</version>
      <configuration>
          <allowMajorUpdates>false</allowMajorUpdates>
          <allowMinorUpdates>true</allowMinorUpdates>
          <allowIncrementalUpdates>true</allowIncrementalUpdates>
          <generateBackupPoms>false</generateBackupPoms>
          <rulesUri>file:///${project.basedir}/.github/versions-maven-plugin-rules.xml</rulesUri>
      </configuration>
  </plugin>
  ```
- Create `.github/versions-maven-plugin-rules.xml` with exclusion rules for BC-FIPS and other pinned dependencies

**Acceptance Criteria:**
- [ ] `versions-maven-plugin` 2.16.2 declared in `pom.xml` build plugins
- [ ] `allowMajorUpdates=false` set in plugin configuration ⚠️ CRITICAL
- [ ] `allowMinorUpdates=true` set
- [ ] `generateBackupPoms=false` set
- [ ] `.github/versions-maven-plugin-rules.xml` created with BC-FIPS exclusions
- [ ] `mvn versions:display-dependency-updates` runs successfully locally

---

## Task 3 — Create GitHub Actions Workflow File
**Estimated:** 2 hours  
**Status:** ❌ NOT STARTED

Create `.github/workflows/maven-dependency-updates.yml`.

**Trigger Configuration:**
- Schedule: `cron: '0 9 * * 1-5'` (Mon–Fri 4:00 AM ET / 9:00 AM UTC)
- `workflow_dispatch` with optional `dry_run` boolean input

**Permissions required:** `contents: write`, `pull-requests: write`, `issues: write`

**Jobs/Steps outline (in order):**
1. Checkout `develop` branch
2. Setup Java 21
3. Configure Maven cache
4. Configure Maven authentication (GitHub Packages via `GITHUB_TOKEN`)
5. Run update detection (Task 5 logic)
6. Run CVE scan before updates (Task 6 logic)
7. Apply updates (Task 7 logic)
8. Run build validation (Task 8 logic)
9. Run CVE scan after updates (Task 6 logic)
10. Create PR (Task 9 logic)
11. Send email (Task 10 logic)

**Acceptance Criteria:**
- [ ] Workflow file created at `.github/workflows/maven-dependency-updates.yml`
- [ ] Schedule and `workflow_dispatch` triggers configured
- [ ] Correct permissions declared
- [ ] All required jobs/steps present in correct order
- [ ] `dry_run` input supported (skips PR/email when true)

---

## Task 4 — Configure Maven Authentication in Workflow
**Estimated:** 0.5 hours  
**Status:** ❌ NOT STARTED

Configure `~/.m2/settings.xml` within the workflow to authenticate against GitHub Packages for both `izgw-bom` and `izgw-core` repositories using the built-in `GITHUB_TOKEN`.

**Acceptance Criteria:**
- [ ] `settings.xml` written with `github` and `github-bom` server entries
- [ ] Uses `GITHUB_TOKEN` (no separate token needed)
- [ ] Maven can resolve `izgw-bom` and `izgw-core` packages in CI

---

## Task 5 — Implement Update Detection Step
**Estimated:** 4 hours  
**Status:** ❌ NOT STARTED

Implement the workflow step that detects available dependency updates.

**Logic:**
- Run `mvn versions:display-dependency-updates -DallowMajorUpdates=false -DallowMinorUpdates=true`
- Parse output to identify updatable dependencies
- Read `.github/dependency-update-exclusions.txt` and filter out excluded dependencies
- Check if a newer `izgw-bom` version is available on the `develop` branch; log if so
- Set `has_updates` output to drive whether subsequent steps run
- Detect BOM-managed vs directly-managed dependencies (focus on directly-managed)

**Acceptance Criteria:**
- [ ] Update detection runs and produces parseable output
- [ ] Exclusions file is read and respected
- [ ] `has_updates` output/env var set correctly
- [ ] BOM-managed dependencies are identified and skipped
- [ ] izgw-bom version check performed
- [ ] `allowMajorUpdates=false` enforced in all `mvn versions:*` commands ⚠️ CRITICAL

---

## Task 6 — Implement CVE Scanning Steps
**Estimated:** 2 hours  
**Status:** ❌ NOT STARTED

Add OWASP CVE scanning using the GitHub Action approach (not Maven plugin) both before and after updates.

**Implementation:**
- Use `dependency-check/Dependency-Check_Action@main` (faster, nightly-updated database)
- Pass `OSS_INDEX_USERNAME` and `OSS_INDEX_PASSWORD` secrets
- Disable Maven OWASP plugin in all Maven commands via `-DskipDependencyCheck=true`
- Scan before updates: identify CVE-affected dependencies to prioritize
- Scan after updates: verify CVEs are resolved; detect any new CVEs introduced
- If CVE has no patch/minor fix: note in PR description, do NOT fail the workflow

**Acceptance Criteria:**
- [ ] OWASP GitHub Action used (not Maven plugin)
- [ ] Pre-update scan identifies CVE-affected dependencies
- [ ] Post-update scan verifies remediation
- [ ] Unfixable CVEs noted in PR but don't block workflow
- [ ] All severities scanned (goal: CVE-free, not just CVSS ≥ 7)
- [ ] Suppression file `dependency-suppression.xml` respected

---

## Task 7 — Implement POM Update Step
**Estimated:** 4 hours  
**Status:** ❌ NOT STARTED

Apply the actual version updates to `pom.xml` using minimal, maintainable changes.

**Strategy:**
- Use `mvn versions:use-latest-releases -DallowMajorUpdates=false -DallowMinorUpdates=true`
- Prioritize CVE-affected dependencies first
- Use version range syntax `[major.minor,major+1)` for `<dependencyManagement>` overrides
  (e.g., `[1.12,2)` — minimum 1.12, allows any 1.x, blocks 2.x)
- Add XML comments explaining why each override exists (CVE number, ticket reference)
- Detect and remove overrides where BOM version has caught up

**Acceptance Criteria:**
- [ ] `mvn versions:use-latest-releases` applied with `allowMajorUpdates=false` ⚠️ CRITICAL
- [ ] Version ranges used in `<dependencyManagement>` overrides
- [ ] Stale/unnecessary overrides removed when BOM catches up
- [ ] Comments added explaining each new/updated override
- [ ] No changes to `<dependencies>` section structure
- [ ] BC-FIPS and other excluded dependencies untouched

---

## Task 8 — Implement Build Validation Step
**Estimated:** 2 hours  
**Status:** ❌ NOT STARTED

Validate the build after updates are applied.

**Steps:**
- Run `mvn clean install -DskipDependencyCheck=true`
- All Surefire tests must pass
- JaCoCo coverage report generated
- Dependency tree comparison (before vs after) to detect conflicts
- If build fails: revert changes, log failure details, send alert email

**Acceptance Criteria:**
- [ ] `mvn clean install -DskipDependencyCheck=true` succeeds
- [ ] All tests pass (no Surefire failures)
- [ ] JaCoCo report produced
- [ ] Dependency conflict detection in place
- [ ] Build failure causes workflow to revert and alert (no broken PR created)

---

## Task 9 — Implement PR Creation Step
**Estimated:** 2 hours  
**Status:** ❌ NOT STARTED

Create an automated pull request targeting the `develop` branch.

**PR details:**
- Branch naming: `automated-maven-updates-YYYYMMDD-HHMMSS`
- Target: `develop` branch
- Title: `chore(deps): automated dependency updates YYYY-MM-DD`
- Body includes:
  - Dependency update summary table (dependency | old version | new version | CVE fixed?)
  - CVE information and remediation details
  - Build and test results summary
  - Dependency tree comparison snippet
  - Links to release notes for major updated libraries
- Auto-apply labels: `dependencies`, `security`
- Auto-assign reviewers: core team members

**Acceptance Criteria:**
- [ ] PR created against `develop` branch
- [ ] Branch name follows `automated-maven-updates-YYYYMMDD-HHMMSS` pattern
- [ ] PR body contains update table, CVE summary, and build results
- [ ] `dependencies` and `security` labels applied
- [ ] Reviewers auto-assigned
- [ ] No PR created when `dry_run=true`
- [ ] No PR created when no updates were found

---

## Task 10 — Implement Email Notification Step
**Estimated:** 0.5 hours  
**Status:** ❌ NOT STARTED

Send email notification via AWS SES when a PR is created.

**Configuration:**
- Use `dawidd6/action-send-mail@v3`
- Server: `email-smtp.us-east-1.amazonaws.com`, port 465, secure: true
- Credentials: `MAIL_USERNAME` / `MAIL_PASSWORD`
- To: kboone@ainq.com, weckels@ainq.com, pcahill@ainq.com
- CC: devops@izgateway.opsgenie.net
- **Conditional:** Email sent ONLY when a PR was actually created (not on dry run or no-update runs)

**Acceptance Criteria:**
- [ ] Email sent only when PR is created
- [ ] AWS SES credentials used correctly
- [ ] All required recipients included
- [ ] No email on dry run or when no updates found

---

## Task 11 — Dry Run Testing
**Estimated:** 1 hour  
**Status:** ❌ NOT STARTED

Test the workflow end-to-end using `dry_run=true` before enabling the scheduled trigger.

**Testing steps:**
- Trigger manually with `dry_run=true`
- Verify update detection produces expected output
- Verify CVE scanning runs correctly
- Verify POM changes are computed but not committed
- Verify no PR is created and no email is sent
- Review logs for correctness

**Acceptance Criteria:**
- [ ] Dry run completes without errors
- [ ] No PR created, no email sent in dry run mode
- [ ] Update detection log shows expected dependencies
- [ ] CVE scan produces report
- [ ] All critical validations pass (`allowMajorUpdates=false`, exclusions respected)

---

## Task 12 — Documentation Updates
**Estimated:** 2 hours  
**Status:** ❌ NOT STARTED

Update project documentation to describe the automated dependency update process.

**Files to update/create:**
- `README.md` — add section on automated dependency updates
- `RELEASING.md` — note that dependency PRs are automated and what to review
- `.github/workflows/README.md` — add entry describing the new workflow

**Acceptance Criteria:**
- [ ] `README.md` updated with dependency automation section
- [ ] `RELEASING.md` updated with guidance on reviewing automated PRs
- [ ] Workflow README updated

---

## Summary

| Task | Description | Estimated | Status |
|------|-------------|-----------|--------|
| 1 | Verify Secrets & Pre-conditions | 0.25h | ✅ Completed |
| 2 | Add versions-maven-plugin to pom.xml | 1h | ❌ Not Started |
| 3 | Create GitHub Actions Workflow File | 2h | ❌ Not Started |
| 4 | Configure Maven Authentication | 0.5h | ❌ Not Started |
| 5 | Implement Update Detection Step | 4h | ❌ Not Started |
| 6 | Implement CVE Scanning Steps | 2h | ❌ Not Started |
| 7 | Implement POM Update Step | 4h | ❌ Not Started |
| 8 | Implement Build Validation Step | 2h | ❌ Not Started |
| 9 | Implement PR Creation Step | 2h | ❌ Not Started |
| 10 | Implement Email Notification Step | 0.5h | ❌ Not Started |
| 11 | Dry Run Testing | 1h | ❌ Not Started |
| 12 | Documentation Updates | 2h | ❌ Not Started |

**Completed:** 1 of 12 tasks (0.25h of 18.5h)  
**Remaining:** 11 tasks (~18.25h)
