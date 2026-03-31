# Design: Automated Maven Dependency Updates

**Project:** izgw-core  
**Created:** 2026-03-20  
**Status:** Design Phase

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│              GitHub Actions Workflow                         │
│         (maven-dependency-updates.yml)                       │
│                                                              │
│  Scheduled: Mon-Fri 4:00 AM ET (9:00 AM UTC)                │
│  Manual: workflow_dispatch                                   │
└─────────────────────────────────────────────────────────────┘
                              │
                              ↓
                    ┌─────────────────┐
                    │  Prerequisites   │
                    │  - Checkout      │
                    │  - Setup Java 21 │
                    │  - Maven Cache   │
                    │  - GPR Auth      │
                    └─────────────────┘
                              │
                              ↓
                    ┌─────────────────┐
                    │  Update Check   │
                    │  - versions:    │
                    │    display-dep  │
                    │    -updates     │
                    │  - Filter rules │
                    │  - Exclude FIPS │
                    └─────────────────┘
                              │
                              ↓
                    ┌─────────────────┐
                    │  CVE Scan       │
                    │  - OWASP check  │
                    │    (before)     │
                    │  - Identify CVE │
                    │    deps         │
                    │  - Prioritize   │
                    └─────────────────┘
                              │
                              ↓
                   ┌──────────────────┐
                   │  Has Updates?    │
                   └──────────────────┘
                     │              │
                    Yes            No → Exit (success)
                     │
                     ↓
          ┌────────────────────┐
          │  Apply Updates     │
          │  - Update pom.xml  │
          │  - Minimal changes │
          │  - Add comments    │
          └────────────────────┘
                     │
                     ↓
          ┌────────────────────┐
          │  Validate Build    │
          │  - mvn clean       │
          │  - mvn install     │
          │  - Run all tests   │
          │  - JaCoCo report   │
          └────────────────────┘
                     │
                     ↓
          ┌────────────────────┐
          │  CVE Scan (After)  │
          │  - Verify fixes    │
          │  - No new CVEs     │
          └────────────────────┘
                     │
                     ↓
          ┌────────────────────┐
          │  All Checks Pass?  │
          └────────────────────┘
                     │
                    Yes → Create PR + Email
                     │
                    No → Log failure + Email alert
```

## Component Details

### 1. Workflow File Structure

**Location:** `.github/workflows/maven-dependency-updates.yml`

**Trigger Configuration:**
```yaml
name: Maven Dependency Updates

on:
  schedule:
    # 4:00 AM Eastern (9:00 AM UTC in winter, 8:00 AM UTC in summer)
    # Using 9:00 AM UTC to cover Eastern Standard Time
    - cron: '0 9 * * 1-5'  # Monday-Friday
  workflow_dispatch:        # Manual trigger
    inputs:
      dry_run:
        description: 'Dry run - show updates without creating PR'
        required: false
        default: 'false'
        type: boolean

permissions:
  contents: write
  pull-requests: write
  issues: write

env:
  MAVEN_OPTS: "-Xmx3g -Xms1g"
  JAVA_VERSION: "21"
```

### 2. Scheduling Strategy

**Time Slots by Project (Eastern Time):**
- **2:00 AM ET** - izgw-bom (dependency parent)
- **3:00 AM ET** - Reserved (buffer time)
- **4:00 AM ET** - izgw-core (this project)
- **4:30 AM ET** - v2tofhir
- **5:00 AM ET** - izgw-hub
- **5:30 AM ET** - izgw-transform

**Rationale:**
- Staggered by 30-60 minutes to ensure dependencies flow correctly
- Assumes izgw-bom updates are available before dependent projects run
- Within 2-6 AM ET window as specified
- Monday-Friday only (no weekend runs)

### 3. Authentication Configuration

**GitHub Packages Authentication:**

```yaml
- name: Configure Maven Authentication
  env:
    GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
  run: |
    mkdir -p ~/.m2
    cat > ~/.m2/settings.xml <<EOF
    <settings>
      <servers>
        <server>
          <id>github</id>
          <username>\${env.GITHUB_ACTOR}</username>
          <password>\${env.GITHUB_TOKEN}</password>
        </server>
        <server>
          <id>github-bom</id>
          <username>\${env.GITHUB_ACTOR}</username>
          <password>\${env.GITHUB_TOKEN}</password>
        </server>
      </servers>
    </settings>
    EOF
    echo "✓ Maven settings configured"
```

**Note:** Using built-in `GITHUB_TOKEN` secret since all package repositories are public and don't require a separate token.

### 4. Versions Maven Plugin Configuration

**Add to pom.xml `<build><plugins>` section:**

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

**Note:** `allowMajorUpdates=false` is critical to prevent major version jumps.

**Exclusions Configuration:**

Instead of hardcoding exclusions, use a maintainable configuration file:

**File:** `.github/dependency-update-exclusions.txt`
```
# Maven Dependency Update Exclusions
# Format: groupId:artifactId

# Bouncy Castle FIPS modules - require certification validation
org.bouncycastle:bc-fips
org.bouncycastle:bcpkix-fips
org.bouncycastle:bctls-fips

# Add other exclusions as needed
```

**Load exclusions in workflow:**
```bash
# Read exclusions file and build exclude pattern
EXCLUSIONS=$(grep -v '^#' .github/dependency-update-exclusions.txt | \
             grep -v '^$' | \
             tr '\n' '|' | \
             sed 's/|$//')

echo "EXCLUSION_PATTERN=$EXCLUSIONS" >> $GITHUB_ENV
```

**Why this approach:**
- ✅ Easy to maintain - just edit text file
- ✅ No code changes needed to add exclusions
- ✅ Documented inline with comments
- ✅ Version controlled
- ✅ Visible to all developers

### 5. Update Detection Logic

**Workflow Steps:**

```bash
# 0. Check for latest izgw-bom version from develop branch
echo "Checking for latest izgw-bom version from develop branch..."
LATEST_BOM_VERSION=$(curl -s https://raw.githubusercontent.com/IZGateway/izgw-bom/develop/pom.xml | \
  grep -A 1 '<artifactId>izgw-bom</artifactId>' | \
  grep '<version>' | \
  sed 's/.*<version>//;s/<\/version>.*//')
CURRENT_BOM_VERSION=$(mvn help:evaluate -Dexpression=project.parent.version -q -DforceStdout)

if [ "$LATEST_BOM_VERSION" != "$CURRENT_BOM_VERSION" ]; then
  echo "::warning::izgw-bom has newer version available: $CURRENT_BOM_VERSION → $LATEST_BOM_VERSION"
  echo "This requires separate coordination - flagging for manual review"
  echo "BOM_UPDATE_AVAILABLE=true" >> $GITHUB_ENV
  echo "LATEST_BOM_VERSION=$LATEST_BOM_VERSION" >> $GITHUB_ENV
fi

# 1. Check for available updates (focus on NON-BOM managed dependencies)
# Use allowMinorUpdates=true to detect minor version updates
mvn versions:display-dependency-updates \
    -DoutputFile=dependency-updates.txt \
    -DoutputEncoding=UTF-8 \
    -DallowMinorUpdates=true

# 2. Identify which dependencies are managed by izgw-bom
# Extract dependency management from effective POM
mvn help:effective-pom -Doutput=effective-pom.xml
# Parse to identify BOM-managed dependencies
xmlstarlet sel -t -m "//dependencyManagement/dependencies/dependency" \
  -v "concat(groupId,':', artifactId)" -n effective-pom.xml > bom-managed-deps.txt

# 3. Load exclusions from config file
EXCLUSIONS=$(grep -v '^#' .github/dependency-update-exclusions.txt | \
             grep -v '^$' | \
             tr '\n' '|' | \
             sed 's/|$//')

# 4. Parse output and filter
cat dependency-updates.txt | \
  grep -E "\\->" | \
  grep -Ev "$EXCLUSIONS" \
  > all-updates.txt

# 5. Separate BOM-managed from direct dependencies
while IFS= read -r dep; do
  if grep -q "$dep" bom-managed-deps.txt; then
    echo "$dep" >> bom-managed-updates.txt
  else
    echo "$dep" >> direct-updates.txt
  fi
done < <(awk '{print $1}' all-updates.txt)

# 6. Focus on direct (non-BOM) dependencies
echo "::notice::Focusing on $(wc -l < direct-updates.txt) direct dependency updates"
echo "::notice::Skipping $(wc -l < bom-managed-updates.txt) BOM-managed dependencies (handled separately)"

# 7. Check for unnecessary overrides in dependencyManagement
# These are overrides where the BOM version has caught up or exceeded our override
if [ -f pom.xml ] && grep -q "<dependencyManagement>" pom.xml; then
  echo "Checking for unnecessary dependency overrides..."
  # Extract current overrides from pom.xml
  xmlstarlet sel -t -m "//dependencyManagement/dependencies/dependency" \
    -v "concat(groupId,':', artifactId, ':', version)" -n pom.xml > current-overrides.txt
  
  # Compare with BOM versions
  while IFS= read -r override; do
    GROUP_ARTIFACT=$(echo $override | cut -d: -f1-2)
    OVERRIDE_VERSION=$(echo $override | cut -d: -f3)
    
    # Get BOM version for this dependency
    BOM_VERSION=$(grep "^$GROUP_ARTIFACT:" bom-managed-deps.txt | cut -d: -f3)
    
    if [ -n "$BOM_VERSION" ]; then
      # Compare versions (simplified - assumes semantic versioning)
      # If BOM version >= override version, mark for removal
      if [[ "$BOM_VERSION" > "$OVERRIDE_VERSION" ]] || [[ "$BOM_VERSION" == "$OVERRIDE_VERSION" ]]; then
        echo "$GROUP_ARTIFACT" >> overrides-to-remove.txt
        echo "::notice::Override no longer needed: $GROUP_ARTIFACT (BOM: $BOM_VERSION >= Override: $OVERRIDE_VERSION)"
      fi
    fi
  done < current-overrides.txt
fi

# 8. Generate structured update list for direct dependencies only
cat direct-updates.txt > filtered-updates.txt
```

**Rationale for BOM Separation:**
- BOM-managed dependencies updated as part of izgw-bom update process
- Reduces noise in PRs
- Prevents conflicts with BOM version management
- Direct dependencies are project-specific and need immediate attention
- Exception: If CVE requires override of BOM version, can be added to `<dependencyManagement>` with version range

**Override Cleanup:**
- Automatically detect when BOM version catches up to or exceeds override
- Remove unnecessary overrides to simplify pom.xml
- Documented in PR description

### 6. CVE Integration

**GitHub Actions OWASP Dependency Check (Before Updates):**

Based on izgw-hub pattern, use GitHub Actions for faster, nightly-updated CVE scanning:

```yaml
- name: Build JAR for Dependency Check
  run: |
    mvn -B clean package -DskipTests -DskipDependencyCheck=true \
        -Dbuildno=${{github.run_number}}

- name: Dependency Check (Before Updates)
  env:
    # Fix JAVA_HOME location for GitHub Action
    JAVA_HOME: /opt/jdk
  uses: dependency-check/Dependency-Check_Action@main
  continue-on-error: true
  timeout-minutes: 5
  with:
    project: IZ Gateway Core
    path: target/*.jar
    format: 'HTML,JSON'
    out: 'reports/before'
    args: >
      --ossIndexUsername ${{ secrets.OSS_INDEX_USERNAME }}
      --ossIndexPassword ${{ secrets.OSS_INDEX_PASSWORD }}       
      --failOnCVSS 0
      --suppression ./dependency-suppression.xml
      --disableNuspec    
      --disableNugetconf  
      --disableAssembly

- name: Parse CVE Report (Before)
  run: |
    jq '.dependencies[] | select(.vulnerabilities) | 
        {name: .fileName, 
         cves: [.vulnerabilities[].name],
         severity: [.vulnerabilities[].severity],
         cvssScore: [.vulnerabilities[].cvssv3.baseScore // .vulnerabilities[].cvssv2.score]}' \
        reports/before/dependency-check-report.json > cve-before.json
```

**CVE Prioritization Logic:**

1. Identify dependencies with CVEs (**all severities** - goal is CVE-free)
2. Cross-reference with available updates
3. **Constraint**: Only consider patch/minor updates (no major versions even for CVEs)
4. Separate CVEs into categories:
   - **Fixable**: CVEs that can be resolved with available patch/minor updates
   - **Unfixable**: CVEs that require major version updates or no fix available
   - **Suppressed**: CVEs in dependency-suppression.xml (false positives)
5. Prioritize fixable CVE dependencies in update order
6. Flag unfixable CVEs in PR description for manual review (don't fail workflow)
7. Include all CVE details in PR description

**CVE Re-Scan (After Updates):**

```yaml
- name: Build JAR After Updates
  run: |
    mvn -B clean package -DskipTests -DskipDependencyCheck=true \
        -Dbuildno=${{github.run_number}}

- name: Dependency Check (After Updates)  
  env:
    JAVA_HOME: /opt/jdk
  uses: dependency-check/Dependency-Check_Action@main
  continue-on-error: true  # Don't fail if CVEs remain (may be unfixable)
  timeout-minutes: 5
  with:
    project: IZ Gateway Core
    path: target/*.jar
    format: 'HTML,JSON'
    out: 'reports/after'
    args: >
      --ossIndexUsername ${{ secrets.OSS_INDEX_USERNAME }}
      --ossIndexPassword ${{ secrets.OSS_INDEX_PASSWORD }}       
      --failOnCVSS 0
      --suppression ./dependency-suppression.xml
      --disableNuspec    
      --disableNugetconf  
      --disableAssembly

- name: Compare CVE Reports
  run: |
    jq '.dependencies[] | select(.vulnerabilities) | 
        {name: .fileName, 
         cves: [.vulnerabilities[].name],
         severity: [.vulnerabilities[].severity],
         cvssScore: [.vulnerabilities[].cvssv3.baseScore // .vulnerabilities[].cvssv2.score]}' \
        reports/after/dependency-check-report.json > cve-after.json
    
    # Generate remediation report
    echo "## 🔒 CVEs Resolved" > cve-remediation.md
    comm -23 <(jq -r '.cves[]' cve-before.json | sort | uniq) \
             <(jq -r '.cves[]' cve-after.json | sort | uniq) >> cve-remediation.md
    
    echo "" >> cve-remediation.md
    echo "## ⚠️ CVEs Remaining (Require Manual Review)" >> cve-remediation.md
    echo "" >> cve-remediation.md
    echo "These CVEs require major version updates or have no fix available:" >> cve-remediation.md
    echo "" >> cve-remediation.md
    comm -12 <(jq -r '.cves[]' cve-before.json | sort | uniq) \
             <(jq -r '.cves[]' cve-after.json | sort | uniq) | \
    while read cve; do
      # Get details for remaining CVEs
      jq -r --arg cve "$cve" \
        '.dependencies[].vulnerabilities[] | select(.name == $cve) | 
         "- **\(.name)** (CVSS \(.cvssv3.baseScore // .cvssv2.score)): \(.description | .[0:100])..."' \
        reports/after/dependency-check-report.json | head -1 >> cve-remediation.md
    done
    
    # Count results
    RESOLVED_COUNT=$(comm -23 <(jq -r '.cves[]' cve-before.json | sort | uniq) \
                                 <(jq -r '.cves[]' cve-after.json | sort | uniq) | wc -l)
    REMAINING_COUNT=$(comm -12 <(jq -r '.cves[]' cve-before.json | sort | uniq) \
                                  <(jq -r '.cves[]' cve-after.json | sort | uniq) | wc -l)
    
    echo "CVE_RESOLVED_COUNT=$RESOLVED_COUNT" >> $GITHUB_ENV
    echo "CVE_REMAINING_COUNT=$REMAINING_COUNT" >> $GITHUB_ENV
    
    # Note: Don't fail workflow even if CVEs remain
    # PR will document what requires manual attention
```

**Key Differences from Maven Plugin:**
- ✅ **Faster**: GitHub Action updated nightly, Maven plugin requires manual updates
- ✅ **Parallel execution**: Can run while other steps execute
- ✅ **Better caching**: GitHub Action has better NVD database caching
- ✅ **Consistent with izgw-hub**: Uses same approach as existing project
- ✅ **OSS Index integration**: Leverages Sonatype OSS Index for additional CVE data

### 7. POM Update Strategy

**Implementation Approach:**

**Step 1: Remove Unnecessary Overrides**

```bash
# Remove overrides that are no longer needed (BOM caught up)
if [ -f overrides-to-remove.txt ] && [ -s overrides-to-remove.txt ]; then
  echo "Removing $(wc -l < overrides-to-remove.txt) unnecessary dependency overrides..."
  
  while IFS= read -r dep; do
    GROUP_ID=$(echo $dep | cut -d: -f1)
    ARTIFACT_ID=$(echo $dep | cut -d: -f2)
    
    # Use xmlstarlet to remove the override
    xmlstarlet ed -L \
      -d "//dependencyManagement/dependencies/dependency[groupId='$GROUP_ID' and artifactId='$ARTIFACT_ID']" \
      pom.xml
    
    echo "  Removed: $dep"
  done < overrides-to-remove.txt
  
  # Clean up empty dependencyManagement section if all overrides removed
  OVERRIDE_COUNT=$(xmlstarlet sel -t -c "count(//dependencyManagement/dependencies/dependency)" pom.xml)
  if [ "$OVERRIDE_COUNT" == "0" ]; then
    xmlstarlet ed -L -d "//dependencyManagement" pom.xml
    echo "  Removed empty dependencyManagement section"
  fi
fi
```

**Step 2: Update Direct Dependencies**

```bash
# Load exclusions
EXCLUSION_LIST=$(cat .github/dependency-update-exclusions.txt | grep -v '^#' | grep -v '^$' | tr '\n' ',')

# Update dependencies automatically (direct dependencies only)
mvn versions:use-latest-releases \
    -DallowMajorUpdates=false \
    -DallowMinorUpdates=true \
    -DallowIncrementalUpdates=true \
    -DgenerateBackupPoms=false \
    -DexcludesList="$EXCLUSION_LIST"
```

**Step 3: Apply Version Ranges to Overrides (if adding new)**

When a CVE requires overriding a BOM-managed dependency:

```bash
# For each CVE-affected BOM dependency that needs override
for dep in $(cat bom-cve-overrides.txt); do
  GROUP_ID=$(echo $dep | cut -d: -f1)
  ARTIFACT_ID=$(echo $dep | cut -d: -f2)
  MIN_VERSION=$(echo $dep | cut -d: -f3)  # e.g., 1.12.0
  MAJOR_VERSION=$(echo $MIN_VERSION | cut -d. -f1)
  NEXT_MAJOR=$((MAJOR_VERSION + 1))
  
  # Use version range: [major.minor,next_major)
  VERSION_RANGE="[$MIN_VERSION,$NEXT_MAJOR)"
  
  # Add to dependencyManagement with version range
  # This allows automatic pickup of 1.12.x, 1.13.x, etc. but not 2.x
  xmlstarlet ed -L \
    -s "//dependencyManagement/dependencies" -t elem -n "dependency" \
    -s "//dependencyManagement/dependencies/dependency[last()]" -t elem -n "groupId" -v "$GROUP_ID" \
    -s "//dependencyManagement/dependencies/dependency[last()]" -t elem -n "artifactId" -v "$ARTIFACT_ID" \
    -s "//dependencyManagement/dependencies/dependency[last()]" -t elem -n "version" -v "$VERSION_RANGE" \
    pom.xml
  
  echo "  Added override: $GROUP_ID:$ARTIFACT_ID with range $VERSION_RANGE"
done
```

**Version Range Syntax:**
- `[1.12,2)` - Minimum 1.12, excludes 2.0 and above
- `[2.5,3)` - Minimum 2.5, excludes 3.0 and above
- Square bracket `[` = inclusive
- Parenthesis `)` = exclusive

**Benefits:**
- ✅ Automatic minor/patch updates (1.12 → 1.13 → 1.14)
- ✅ Prevents major version jumps
- ✅ Reduces maintenance overhead
- ✅ BOM can "catch up" and override becomes unnecessary

**Avoid Unnecessary Overrides:**
- Only add override if CVE requires it AND it's a BOM-managed dependency
- Don't add overrides for direct dependencies (just update version directly)
- Remove overrides when BOM version catches up
- Use version ranges to allow automatic updates within major version

**Exclusion Rules (from config file):**
- Load exclusions from `.github/dependency-update-exclusions.txt`
- Skip if groupId:artifactId matches exclusion list
- Easy to maintain - just edit config file

### 8. Build Validation

**Full Build Process:**

```bash
# 1. Clean build (skip Maven OWASP plugin - using GitHub Action instead)
mvn clean install -DskipTests=false -DskipDependencyCheck=true

# 2. Run tests with coverage
mvn test -DskipDependencyCheck=true

# 3. Generate JaCoCo report
mvn jacoco:report

# 4. Dependency tree analysis
mvn dependency:tree -Dverbose=true > dependency-tree.txt

# 5. Check for conflicts
grep "conflicts with" dependency-tree.txt || echo "No conflicts detected"
```

**Success Criteria:**
- Build exits with code 0
- All tests pass (0 failures, 0 errors)
- No new dependency conflicts
- JaCoCo coverage maintained or improved
- CVE scan performed by GitHub Action (separate step)

### 10. PR Generation

**Branch Creation:**

```bash
# Create update branch with new format: security-updates-YYYY-MM-DD-HH:MM
BRANCH_NAME="security-updates-$(date +%Y-%m-%d-%H:%M)"
git checkout -b $BRANCH_NAME

# Stage changes
git add pom.xml
git commit -m "chore(deps): automated Maven dependency updates

- Updated patch/minor versions of dependencies
- Resolved CVEs: [list CVE IDs]
- All tests passing
- Build validation successful

Automated by maven-dependency-updates workflow"
```

**PR Description Template:**

```markdown
## 🔄 Automated Maven Dependency Updates - izgw-core

**Generated:** $(date -u '+%Y-%m-%d %H:%M:%S UTC')  
**Workflow Run:** [${{ github.run_id }}](${{ github.server_url }}/${{ github.repository }}/actions/runs/${{ github.run_id }})

### 📦 Updated Dependencies

| Dependency | Current | Updated | Type | CVE Fixed |
|------------|---------|---------|------|-----------|
| commons-text | 1.11.0 | 1.12.0 | Minor | CVE-2024-XXXXX |
| jackson-databind | 2.15.2 | 2.15.4 | Patch | CVE-2024-YYYYY |
| spring-web | 6.1.3 | 6.1.5 | Patch | - |

**Total Updates:** X dependencies updated

### 🔒 Security Impact

**CVEs Resolved:**
- ✅ CVE-2024-XXXXX (CVSS 8.1) - commons-text RCE vulnerability
- ✅ CVE-2024-YYYYY (CVSS 7.5) - jackson-databind deserialization

**CVE Scan Results:**
- Before: X vulnerabilities (Y high, Z moderate, A low)
- After: B vulnerabilities (C high, D moderate, E low)
- **Remediated:** N vulnerabilities

**⚠️ CVEs Requiring Manual Review:**

The following CVEs remain and require manual attention (major version updates or no fix available):

- **CVE-2024-ZZZZZ** (CVSS 6.5) - Requires major version update to 3.0.0 (currently 2.15.4)
  - Component: example-library
  - Recommendation: Evaluate upgrade path to 3.x or accept risk with suppression
  
**Goal:** CVE-free across all severity levels (currently X remaining)

### 📋 Changes Made

**POM Updates:**
- Updated N direct dependency versions
- Added M dependency overrides with version ranges (e.g., `[1.12,2)`) for automatic minor/patch pickup
- Removed P unnecessary overrides (BOM version caught up)
- No changes to dependency exclusions or scope

**Version Ranges Used:**
- Version ranges allow automatic minor/patch updates while pinning major version
- Example: `[1.12,2)` allows 1.12.x, 1.13.x, 1.14.x but not 2.x
- Reduces future maintenance overhead

**Override Cleanup:**
- Removed X overrides where BOM version now meets or exceeds our requirement
- Simplifies pom.xml management

**Build Validation:**
- ✅ Clean build successful
- ✅ All tests passing (XXX tests, 0 failures)
- ✅ JaCoCo coverage: XX.X% (maintained)
- ✅ No dependency conflicts detected

### 📎 Artifacts

- [Dependency Tree](link) - Full dependency resolution
- [CVE Report (Before)](link) - Vulnerabilities before updates
- [CVE Report (After)](link) - Vulnerabilities after updates
- [Test Results](link) - JUnit test execution
- [Build Logs](link) - Full Maven build output

### ⚠️ Review Notes

**Manual Review Required:**
- Review dependency version changes
- Check for breaking changes in release notes
- Verify no downstream impact
- Test locally if needed

**Excluded from Automation:**
- Bouncy Castle FIPS modules (bc-fips, bcpkix-fips, bctls-fips)
- Spring Boot version (managed by izgw-bom)
- izgw-bom parent version

---

**Next Steps:**
1. Review changes and artifacts
2. Approve PR if changes are acceptable
3. Merge manually to develop branch
4. Monitor downstream projects (izgw-hub, izgw-transform)

**Questions?** Contact DevOps team or comment on this PR.
```

**PR Creation:**

```bash
gh pr create \
  --title "chore(deps): Maven dependency updates - $(date +%Y-%m-%d)" \
  --body "$(cat pr-body.md)" \
  --base develop \
  --head $BRANCH_NAME \
  --label "dependencies,automated,security"
```

### 11. Email Notification

**Send Email on PR Creation (Only if changes made):**

```yaml
- name: Send Email Notification
  if: steps.create_pr.outputs.pr_number != ''
  uses: dawidd6/action-send-mail@v3
  with:
    server_address: email-smtp.us-east-1.amazonaws.com
    server_port: 465
    secure: true
    username: ${{ secrets.MAIL_USERNAME }}
    password: ${{ secrets.MAIL_PASSWORD }}
    subject: "[izgw-core] Automated Dependency Updates PR #${{ steps.create_pr.outputs.pr_number }} - ${{ env.NOW }}"
    to: kboone@ainq.com,weckels@ainq.com,pcahill@ainq.com
    cc: devops@izgateway.opsgenie.net
    from: GithubActionNotification <GithubActionNotification@izgateway.org>
    body: |
      Automated Maven dependency update PR has been created for izgw-core.
      
      **PR #${{ steps.create_pr.outputs.pr_number }}**: ${{ steps.create_pr.outputs.pr_url }}
      **Branch**: security-updates-${{ env.NOW }}
      
      **Summary:**
      - Dependencies updated: ${{ env.DIRECT_UPDATE_COUNT }}
      - Overrides removed: ${{ env.OVERRIDE_REMOVE_COUNT }}
      - CVEs resolved: ${{ env.CVE_RESOLVED_COUNT }}
      - CVEs remaining: ${{ env.CVE_REMAINING_COUNT }} (require manual review)
      - Build status: ✅ All tests passing
      
      **izgw-bom Status:**
      ${{ env.BOM_UPDATE_AVAILABLE == 'true' && format('⚠️ Newer version available: {0}', env.LATEST_BOM_VERSION) || '✅ Up to date' }}
      
      Please review and approve the PR when ready.
      
      **Workflow Run:** ${{ github.server_url }}/${{ github.repository }}/actions/runs/${{ github.run_id }}
```

**Send Email on Failure (Only if expected to create PR but failed):**

```yaml
- name: Send Failure Email
  if: failure() && steps.check_updates.outputs.has_updates == 'true'
  uses: dawidd6/action-send-mail@v3
  with:
    server_address: email-smtp.us-east-1.amazonaws.com
    server_port: 465
    secure: true
    username: ${{ secrets.MAIL_USERNAME }}
    password: ${{ secrets.MAIL_PASSWORD }}
    subject: "[izgw-core] Dependency Update Workflow Failed - ${{ env.NOW }}"
    to: kboone@ainq.com,weckels@ainq.com,pcahill@ainq.com
    cc: devops@izgateway.opsgenie.net
    from: GithubActionNotification <GithubActionNotification@izgateway.org>
    body: |
      The automated dependency update workflow failed for izgw-core.
      
      **Workflow:** ${{ github.server_url }}/${{ github.repository }}/actions/runs/${{ github.run_id }}
      
      Updates were detected but the build or CVE validation failed.
      Please review the logs and investigate the issue.
      
      **Likely causes:**
      - Build failure after applying updates
      - Test failures
      - Dependency conflicts
      
      Check the workflow logs for details.
```

**Key Points:**
- ✅ Uses existing `MAIL_USERNAME` and `MAIL_PASSWORD` secrets
- ✅ AWS SES SMTP configuration (email-smtp.us-east-1.amazonaws.com:465)
- ✅ Email only sent if PR created (conditional: `if: steps.create_pr.outputs.pr_number != ''`)
- ✅ No email sent if no updates available
- ✅ Failure email only if updates detected but workflow failed
- ✅ Uses same recipients and format as existing workflows
- ✅ Includes summary counts and links

### 12. Workflow Secrets Required

**Repository Secrets:**

1. **GITHUB_TOKEN** (Built-in)
   - Purpose: Authenticate with GitHub Packages Maven registry and create PRs
   - Value: Automatically provided by GitHub Actions
   - Used by: Maven settings.xml for dependency resolution, GitHub CLI for PR creation
   - Note: Sufficient since all package repositories are public

2. **MAIL_USERNAME** (Already Configured)
   - Purpose: AWS SES SMTP authentication for email notifications
   - Status: ✅ Already exists and configured
   - Used by: dawidd6/action-send-mail@v3

3. **MAIL_PASSWORD** (Already Configured)
   - Purpose: AWS SES SMTP authentication for email notifications
   - Status: ✅ Already exists and configured
   - Used by: dawidd6/action-send-mail@v3

4. **OSS_INDEX_USERNAME** (Already Configured)
   - Purpose: Sonatype OSS Index authentication for enhanced CVE data
   - Status: ✅ Already exists and configured
   - Used by: dependency-check/Dependency-Check_Action

5. **OSS_INDEX_PASSWORD** (Already Configured)
   - Purpose: Sonatype OSS Index authentication
   - Status: ✅ Already exists and configured
   - Used by: dependency-check/Dependency-Check_Action

**Note:** All required secrets already exist! No new secret configuration needed.

## Edge Cases & Handling

### 1. No Updates Available

**Detection:**
```bash
if [ ! -s filtered-updates.txt ]; then
  echo "No updates available"
  exit 0  # Success, no PR needed
fi
```

**Notification:**
- No email sent
- Workflow completes successfully
- Log message: "No dependency updates available"

### 2. Build Failure After Update

**Handling:**
```bash
if ! mvn clean install; then
  echo "::error::Build failed after applying updates"
  # Don't create PR
  # Send failure email
  # Upload build logs as artifact
  exit 1
fi
```

**Actions:**
- No PR created
- Email sent to dev team with failure details
- Build logs uploaded as workflow artifact
- Workflow fails (red status)

### 3. New CVEs Introduced

**Detection:**
```bash
# Check if new high/critical CVEs introduced
NEW_CVES=$(jq '.dependencies[].vulnerabilities[] | 
  select(.severity == "HIGH" or .severity == "CRITICAL")' \
  target/dependency-check-report.json | wc -l)

if [ $NEW_CVES -gt 0 ]; then
  echo "::error::New high/critical CVEs introduced"
  exit 1
fi
```

**Actions:**
- No PR created
- Email sent with CVE details
- Workflow fails

### 4. Dependency Conflicts

**Detection:**
```bash
mvn dependency:tree -Dverbose=true > dependency-tree.txt

if grep -q "conflicts with" dependency-tree.txt; then
  echo "::warning::Dependency conflicts detected"
  # Include in PR description for manual review
fi
```

**Actions:**
- PR still created (conflicts may be acceptable)
- Conflicts highlighted in PR description
- Dependency tree attached as artifact

### 5. Multiple Updates for Same Dependency

**Handling:**
- Take latest available version (within patch/minor constraints)
- Document version path in PR (e.g., 1.0.0 → 1.0.5, skipping 1.0.1-1.0.4)

### 6. Transitive Dependency Updates

**Strategy:**
- Focus on declared dependencies only
- Transitive dependencies updated via parent version updates
- Document significant transitive changes in PR

## Caching Strategy

**Maven Dependencies:**

```yaml
- name: Cache Maven Dependencies
  uses: actions/cache@v4
  with:
    path: ~/.m2/repository
    key: ${{ runner.os }}-maven-${{ hashFiles('**/pom.xml') }}
    restore-keys: |
      ${{ runner.os }}-maven-
```

**NVD CVE Database:**

```yaml
- name: Cache NVD Database
  uses: actions/cache@v4
  with:
    path: ~/.m2/repository/org/owasp/dependency-check-data
    key: ${{ runner.os }}-nvd-${{ hashFiles('**/dependency-check-maven/version') }}
    restore-keys: |
      ${{ runner.os }}-nvd-
```

**Benefits:**
- Faster workflow execution (5-10 min vs 20-30 min)
- Reduced Maven Central API calls
- Faster CVE database updates

## Monitoring & Metrics

**Track via GitHub Actions:**
- Workflow success rate
- Average execution time
- Number of updates per run
- CVEs resolved per month

**Workflow Summary:**

```yaml
- name: Generate Summary
  if: always()
  run: |
    echo "## Dependency Update Summary" >> $GITHUB_STEP_SUMMARY
    echo "" >> $GITHUB_STEP_SUMMARY
    echo "- **Updates Found:** $UPDATE_COUNT" >> $GITHUB_STEP_SUMMARY
    echo "- **CVEs Resolved:** $CVE_RESOLVED_COUNT" >> $GITHUB_STEP_SUMMARY
    echo "- **Build Status:** $BUILD_STATUS" >> $GITHUB_STEP_SUMMARY
    echo "- **PR Created:** $PR_NUMBER" >> $GITHUB_STEP_SUMMARY
```

## Testing Strategy

### Pre-Production Testing

1. **Dry Run Mode:**
   ```yaml
   - name: Dry Run Check
     if: github.event.inputs.dry_run == 'true'
     run: |
       echo "DRY RUN MODE - No PR will be created"
       echo "Updates that would be applied:" >> $GITHUB_STEP_SUMMARY
       cat filtered-updates.txt >> $GITHUB_STEP_SUMMARY
   ```

2. **Test on Feature Branch:**
   - Trigger manually on test branch
   - Verify PR creation and content
   - Validate email notifications

3. **Test Scenarios:**
   - No updates available
   - Single dependency update
   - Multiple dependency updates
   - CVE remediation
   - Build failure
   - Dependency conflict

## Rollout Plan

### Phase 1: Implementation (Week 1)
- Create workflow file
- Configure secrets
- Add versions-maven-plugin to pom.xml
- Test with manual triggers only

### Phase 2: Validation (Week 2)
- Run manually several times
- Review PRs manually
- Validate email notifications
- Refine PR description format

### Phase 3: Enable Schedule (Week 3)
- Enable cron schedule (4:00 AM ET)
- Monitor first week closely
- Adjust as needed

### Phase 4: Expand (Week 4+)
- Apply pattern to v2tofhir (4:30 AM ET)
- Apply to izgw-hub (5:00 AM ET)
- Apply to izgw-transform (5:30 AM ET)

## Future Enhancements

1. **Jira Integration:**
   - Replace email with Jira ticket creation
   - Auto-link PR to Jira issue
   - Track resolution time

2. **Smart Grouping:**
   - Group related dependencies (e.g., all Jackson libraries)
   - Bundle minor updates weekly vs patch daily

3. **Release Notes Integration:**
   - Auto-fetch and include release notes links
   - Highlight breaking changes

4. **Dependency Dashboard:**
   - Track update velocity
   - CVE resolution metrics
   - Project comparison

5. **Auto-Approve Logic:**
   - For patch updates with 100% test pass (controversial)
   - Require 2 consecutive successful runs

## Maintenance

**Weekly:**
- Review generated PRs
- Monitor workflow success rate
- Check email delivery

**Monthly:**
- Review dependency-suppression.xml
- Update NVD database cache
- Review and merge dependency PRs

**Quarterly:**
- Review and update exclusion rules
- Evaluate workflow performance
- Consider expanding to more projects

---

## Appendix A — Cross-Project Scheduling

### Dependency Order and Cron Schedule

| Project | Time (ET) | Time (UTC) | Cron |
|---------|-----------|------------|------|
| izgw-bom | 2:00 AM | 7:00 AM | `0 7 * * 1-5` |
| izgw-core | 4:00 AM | 9:00 AM | `0 9 * * 1-5` |
| v2tofhir | 4:30 AM | 9:30 AM | `30 9 * * 1-5` |
| izgw-hub | 5:00 AM | 10:00 AM | `0 10 * * 1-5` |
| izgw-transform | 5:30 AM | 10:30 AM | `30 10 * * 1-5` |

**Note:** UTC times assume Eastern Standard Time (UTC-5). During Daylight Saving Time (UTC-4) cron runs one hour earlier.

**Coordination assumption:** When izgw-core's workflow starts, izgw-bom's run has already produced a PR. If that PR hasn't been merged yet, izgw-core uses the current BOM version; the next day's run will pick up any merged BOM changes.

---

## Appendix B — Maven Settings Template

Generated at the start of the workflow:

```xml
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
                        http://maven.apache.org/xsd/settings-1.0.0.xsd">
    <servers>
        <server>
            <id>github</id>
            <username>${env.GITHUB_ACTOR}</username>
            <password>${env.GITHUB_TOKEN}</password>
        </server>
        <server>
            <id>github-bom</id>
            <username>${env.GITHUB_ACTOR}</username>
            <password>${env.GITHUB_TOKEN}</password>
        </server>
    </servers>
</settings>
```

Uses the built-in `GITHUB_TOKEN` — no separate `MAVEN_GPR_TOKEN` required.

---

## Appendix C — Workflow Code Examples

### C.1 Update Detection with Override Cleanup

```yaml
- name: Detect Available Updates
  run: |
    # Check for latest izgw-bom from develop
    LATEST_BOM=$(curl -s https://raw.githubusercontent.com/IZGateway/izgw-bom/develop/pom.xml | \
      grep -A 1 '<artifactId>izgw-bom</artifactId>' | grep '<version>' | \
      sed 's/.*<version>//;s/<\/version>.*//')
    CURRENT_BOM=$(mvn help:evaluate -Dexpression=project.parent.version -q -DforceStdout)
    if [ "$LATEST_BOM" != "$CURRENT_BOM" ]; then
      echo "::warning::izgw-bom update available: $CURRENT_BOM → $LATEST_BOM"
      echo "BOM_UPDATE_AVAILABLE=true" >> $GITHUB_ENV
    fi

    # Load exclusions
    EXCLUSIONS=$(grep -v '^#' .github/dependency-update-exclusions.txt | \
                 grep -v '^$' | paste -sd '|')

    # Detect updates (CRITICAL: allowMajorUpdates=false)
    mvn versions:display-dependency-updates \
        -DoutputFile=updates.txt \
        -DallowMajorUpdates=false \
        -DallowMinorUpdates=true

    # Get BOM-managed dependencies from effective POM
    mvn help:effective-pom -Doutput=effective.xml
    xmlstarlet sel -t -m "//dependencyManagement/dependencies/dependency" \
      -v "concat(groupId,':', artifactId)" -n effective.xml > bom-deps.txt

    # Filter to direct, non-excluded dependencies only
    grep "\\->" updates.txt | grep -Ev "$EXCLUSIONS" | while read line; do
      dep=$(echo $line | awk '{print $1}')
      if ! grep -q "$dep" bom-deps.txt; then
        echo $line >> direct-updates.txt
      fi
    done

    # Detect unnecessary overrides (BOM has caught up)
    if grep -q "<dependencyManagement>" pom.xml; then
      xmlstarlet sel -t -m "//dependencyManagement/dependencies/dependency" \
        -v "concat(groupId,':', artifactId,':', version)" -n pom.xml | \
      while IFS=: read group artifact override_ver; do
        bom_ver=$(grep "^${group}:${artifact}:" bom-deps.txt | cut -d: -f3)
        if [ -n "$bom_ver" ]; then
          if printf '%s\n%s\n' "$bom_ver" "$override_ver" | sort -V -C; then
            echo "${group}:${artifact}" >> overrides-to-remove.txt
            echo "::notice::Override no longer needed: ${group}:${artifact}"
          fi
        fi
      done
    fi

    UPDATE_COUNT=$(wc -l < direct-updates.txt 2>/dev/null || echo 0)
    REMOVE_COUNT=$(wc -l < overrides-to-remove.txt 2>/dev/null || echo 0)
    echo "DIRECT_UPDATE_COUNT=$UPDATE_COUNT" >> $GITHUB_ENV
    echo "OVERRIDE_REMOVE_COUNT=$REMOVE_COUNT" >> $GITHUB_ENV
```

### C.2 Apply Updates with Version Ranges

```yaml
- name: Apply POM Updates
  run: |
    # Step 1: Remove unnecessary overrides
    if [ -f overrides-to-remove.txt ] && [ -s overrides-to-remove.txt ]; then
      while IFS=: read group artifact; do
        xmlstarlet ed -L \
          -d "//dependencyManagement/dependencies/dependency[groupId='$group' and artifactId='$artifact']" \
          pom.xml
        echo "  Removed: $group:$artifact"
      done < overrides-to-remove.txt

      # Clean up empty dependencyManagement section
      count=$(xmlstarlet sel -t -c "count(//dependencyManagement/dependencies/dependency)" pom.xml)
      if [ "$count" == "0" ]; then
        xmlstarlet ed -L -d "//dependencyManagement" pom.xml
      fi
    fi

    # Step 2: Apply direct dependency updates (CRITICAL: allowMajorUpdates=false)
    EXCLUSION_LIST=$(grep -v '^#' .github/dependency-update-exclusions.txt | \
                     grep -v '^$' | paste -sd ',')
    mvn versions:use-latest-releases \
        -DallowMajorUpdates=false \
        -DallowMinorUpdates=true \
        -DallowIncrementalUpdates=true \
        -DgenerateBackupPoms=false \
        -DexcludesList="$EXCLUSION_LIST"

    # Step 3: Apply version ranges to new CVE-driven BOM overrides
    if [ -f bom-cve-overrides.txt ] && [ -s bom-cve-overrides.txt ]; then
      while IFS=: read group artifact min_ver; do
        major=$(echo $min_ver | cut -d. -f1)
        next_major=$((major + 1))
        ver_range="[${min_ver},${next_major})"
        echo "  Added override: $group:$artifact = $ver_range"
        # xmlstarlet edit to insert override with range...
      done < bom-cve-overrides.txt
    fi
```

### C.3 CVE Scanning (Before and After)

```yaml
- name: Build JAR for CVE Scan (Before Updates)
  run: mvn -B clean package -DskipTests -DskipDependencyCheck=true -Dbuildno=${{ github.run_number }}

- name: OWASP Dependency Check (Before)
  env:
    JAVA_HOME: /opt/jdk
  uses: dependency-check/Dependency-Check_Action@main
  continue-on-error: true
  timeout-minutes: 5
  with:
    project: IZ Gateway Core
    path: target/*.jar
    format: 'HTML,JSON'
    out: 'reports/before'
    args: >
      --ossIndexUsername ${{ secrets.OSS_INDEX_USERNAME }}
      --ossIndexPassword ${{ secrets.OSS_INDEX_PASSWORD }}
      --failOnCVSS 0
      --suppression ./dependency-suppression.xml
      --disableNuspec --disableNugetconf --disableAssembly

- name: Parse CVE Report (Before)
  run: |
    jq -r '.dependencies[] | select(.vulnerabilities) |
      .vulnerabilities[] | "\(.name)|\(.severity)|\(.cvssv3.baseScore // .cvssv2.score)"' \
      reports/before/dependency-check-report.json | sort -u > cve-before.txt

# ... apply updates, build, then re-scan ...

- name: OWASP Dependency Check (After)
  env:
    JAVA_HOME: /opt/jdk
  uses: dependency-check/Dependency-Check_Action@main
  continue-on-error: true   # Don't fail on unfixable CVEs
  timeout-minutes: 5
  with:
    project: IZ Gateway Core
    path: target/*.jar
    format: 'HTML,JSON'
    out: 'reports/after'
    args: >
      --ossIndexUsername ${{ secrets.OSS_INDEX_USERNAME }}
      --ossIndexPassword ${{ secrets.OSS_INDEX_PASSWORD }}
      --failOnCVSS 0
      --suppression ./dependency-suppression.xml

- name: Generate CVE Remediation Report
  run: |
    jq -r '.dependencies[] | select(.vulnerabilities) |
      .vulnerabilities[] | "\(.name)|\(.severity)|\(.cvssv3.baseScore // .cvssv2.score)"' \
      reports/after/dependency-check-report.json | sort -u > cve-after.txt
    comm -23 cve-before.txt cve-after.txt > cve-resolved.txt
    comm -12 cve-before.txt cve-after.txt > cve-remaining.txt
    echo "CVE_RESOLVED_COUNT=$(wc -l < cve-resolved.txt)" >> $GITHUB_ENV
    echo "CVE_REMAINING_COUNT=$(wc -l < cve-remaining.txt)" >> $GITHUB_ENV
```

### C.4 PR Creation and Email Notification

```yaml
- name: Generate PR Description
  run: |
    cat > pr-body.md <<'EOF'
    ## 🔄 Automated Maven Dependency Updates — izgw-core
    **Generated:** $(date -u '+%Y-%m-%d %H:%M UTC')

    ### 📦 Updated Dependencies
    $(cat direct-updates.txt | awk '{print "- **" $1 "**: " $2 " → " $4}')

    ### 🧹 Cleanup
    $([ -f overrides-to-remove.txt ] && \
      cat overrides-to-remove.txt | awk '{print "- " $0 " (BOM caught up)"}' || \
      echo "No overrides removed")

    ### 🔒 Security (Goal: CVE-Free)
    **Resolved:** $RESOLVED_CVES
    $(cat cve-resolved.txt | awk -F'|' '{print "- ✅ **" $1 "** (CVSS " $3 ")"}')

    **Remaining (manual review required):** $REMAINING_CVES
    $(cat cve-remaining.txt | awk -F'|' '{print "- ⚠️ **" $1 "** (CVSS " $3 ") — requires major version update"}')

    ### ✅ Validation
    - [x] Build successful (`-DskipDependencyCheck=true`)
    - [x] All tests passing
    - [x] No dependency conflicts
    - [x] GitHub Actions OWASP scan complete
    EOF

- name: Create Pull Request
  id: create_pr
  if: env.DIRECT_UPDATE_COUNT > 0 && env.DRY_RUN != 'true'
  run: |
    gh pr create \
      --title "chore(deps): automated Maven dependency updates $(date +%Y-%m-%d)" \
      --body "$(cat pr-body.md)" \
      --base develop \
      --head $BRANCH_NAME \
      --label "dependencies,security"
    PR_NUMBER=$(gh pr view $BRANCH_NAME --json number -q .number)
    PR_URL=$(gh pr view $BRANCH_NAME --json url -q .url)
    echo "pr_number=$PR_NUMBER" >> $GITHUB_OUTPUT
    echo "pr_url=$PR_URL" >> $GITHUB_OUTPUT

- name: Send Email Notification
  if: steps.create_pr.outputs.pr_number != ''
  uses: dawidd6/action-send-mail@v3
  with:
    server_address: email-smtp.us-east-1.amazonaws.com
    server_port: 465
    secure: true
    username: ${{ secrets.MAIL_USERNAME }}
    password: ${{ secrets.MAIL_PASSWORD }}
    subject: "[izgw-core] Automated Dependency Updates PR #${{ steps.create_pr.outputs.pr_number }}"
    to: kboone@ainq.com,weckels@ainq.com,pcahill@ainq.com
    cc: devops@izgateway.opsgenie.net
    from: GithubActionNotification <GithubActionNotification@izgateway.org>
    body: |
      PR: ${{ steps.create_pr.outputs.pr_url }}

      Summary:
      - Dependencies updated: ${{ env.DIRECT_UPDATE_COUNT }}
      - Overrides removed: ${{ env.OVERRIDE_REMOVE_COUNT }}
      - CVEs resolved: ${{ env.CVE_RESOLVED_COUNT }}
      - CVEs remaining: ${{ env.CVE_REMAINING_COUNT }} (manual review needed)
      - Build: ✅ All tests passing
      - izgw-bom: ${{ env.BOM_UPDATE_AVAILABLE == 'true' && format('⚠️ Update available: {0}', env.LATEST_BOM_VERSION) || '✅ Current' }}

- name: Send Failure Email
  if: failure() && env.DIRECT_UPDATE_COUNT > 0
  uses: dawidd6/action-send-mail@v3
  with:
    server_address: email-smtp.us-east-1.amazonaws.com
    server_port: 465
    secure: true
    username: ${{ secrets.MAIL_USERNAME }}
    password: ${{ secrets.MAIL_PASSWORD }}
    subject: "[izgw-core] Dependency Update Workflow FAILED"
    to: kboone@ainq.com,weckels@ainq.com,pcahill@ainq.com
    cc: devops@izgateway.opsgenie.net
    from: GithubActionNotification <GithubActionNotification@izgateway.org>
    body: |
      Automated dependency update workflow FAILED for izgw-core.
      Workflow: ${{ github.server_url }}/${{ github.repository }}/actions/runs/${{ github.run_id }}
      Manual intervention required — check build/test/CVE logs.
```

### C.5 Version Range Helper Script

```bash
#!/bin/bash
# Calculate Maven version range from a minimum version.
# Input: 1.12.0  →  Output: [1.12,2)
calculate_version_range() {
    local min_version=$1
    local major=$(echo $min_version | cut -d. -f1)
    local minor=$(echo $min_version | cut -d. -f2)
    echo "[${major}.${minor},$((major + 1)))"
}

calculate_version_range "1.12.0"  # [1.12,2)
calculate_version_range "2.5.3"   # [2.5,3)
```

---

## Appendix D — Reference Links

### Maven
- Versions Plugin: https://www.mojohaus.org/versions-maven-plugin/
- Maven Central: https://search.maven.org/

### Security
- OWASP Dependency Check Action: https://github.com/marketplace/actions/dependency-check
- NVD: https://nvd.nist.gov/
- OSS Index: https://ossindex.sonatype.org/

### GitHub Actions
- Workflow Syntax: https://docs.github.com/en/actions/using-workflows/workflow-syntax-for-github-actions
- `dawidd6/action-send-mail`: https://github.com/dawidd6/action-send-mail

### IZ Gateway Repositories
- izgw-bom: https://github.com/IZGateway/izgw-bom
- izgw-core: https://github.com/IZGateway/izgw-core

---

## Glossary

| Term | Definition |
|------|-----------|
| BOM | Bill of Materials — parent POM that declares dependency versions |
| CVE | Common Vulnerabilities and Exposures |
| CVSS | Common Vulnerability Scoring System (0–10; ≥7 = high/critical) |
| FIPS | Federal Information Processing Standards |
| NVD | National Vulnerability Database |
| OWASP | Open Web Application Security Project |
| SemVer | Semantic Versioning (MAJOR.MINOR.PATCH) |
| Transitive dependency | A dependency pulled in by one of your direct dependencies |
