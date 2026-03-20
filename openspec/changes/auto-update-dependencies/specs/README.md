# Specifications and References

**Project:** izgw-core  
**Created:** 2026-03-20

This directory contains technical specifications and reference materials for the automated dependency updates implementation.

## Contents

- [Maven Versions Plugin](#maven-versions-plugin)
- [OWASP Dependency Check](#owasp-dependency-check)
- [Semantic Versioning](#semantic-versioning)
- [GitHub Actions](#github-actions)
- [Email Notification](#email-notification)
- [POM Update Patterns](#pom-update-patterns)
- [Scheduling Across Projects](#scheduling-across-projects)

---

## Maven Versions Plugin

**Documentation:** https://www.mojohaus.org/versions-maven-plugin/  
**GitHub:** https://github.com/mojohaus/versions-maven-plugin  
**Version:** 2.16.2

### Key Goals Used

**versions:display-dependency-updates**
- Shows available dependency updates
- Respects version rules (major/minor/patch)
- **CRITICAL**: Must set `-DallowMinorUpdates=true` to show minor updates
- Output can be parsed for automation

**versions:use-latest-releases**
- Updates dependencies to latest releases
- Respects configuration constraints
- **CRITICAL**: Must set `-DallowMajorUpdates=false` to prevent major version jumps
- Can be filtered by include/exclude patterns

### Configuration Example

```xml
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>versions-maven-plugin</artifactId>
    <version>2.16.2</version>
    <configuration>
        <!-- CRITICAL: Prevent major version updates -->
        <allowMajorUpdates>false</allowMajorUpdates>
        <allowMinorUpdates>true</allowMinorUpdates>
        <allowIncrementalUpdates>true</allowIncrementalUpdates>
        <generateBackupPoms>false</generateBackupPoms>
        <!-- Exclusions loaded from config file at runtime -->
    </configuration>
</plugin>
```

**Command Line Usage:**
```bash
# Display updates with minor versions
mvn versions:display-dependency-updates -DallowMinorUpdates=true

# Apply updates (MUST set allowMajorUpdates=false)
mvn versions:use-latest-releases \
    -DallowMajorUpdates=false \
    -DallowMinorUpdates=true \
    -DallowIncrementalUpdates=true
```

---

## OWASP Dependency Check

**GitHub Actions:** https://github.com/marketplace/actions/dependency-check  
**Documentation:** https://jeremylong.github.io/DependencyCheck/  
**Version:** Updated nightly in GitHub Actions

### Why GitHub Actions Instead of Maven Plugin?

Following the pattern established in **izgw-hub**, we use the GitHub Actions OWASP dependency check instead of the Maven plugin:

**Advantages:**
- ✅ **Updated nightly**: Action always has latest CVE database
- ✅ **Faster execution**: Better caching and parallel processing
- ✅ **Separate from build**: Doesn't slow down Maven build
- ✅ **OSS Index integration**: Additional CVE data from Sonatype
- ✅ **Consistent with izgw-hub**: Same approach across projects

### GitHub Actions Configuration

```yaml
- name: Dependency Check
  env:
    JAVA_HOME: /opt/jdk
  uses: dependency-check/Dependency-Check_Action@main
  continue-on-error: true  # or false for strict mode
  timeout-minutes: 5
  with:
    project: IZ Gateway Core
    path: target/*.jar
    format: 'HTML,JSON'
    out: 'reports'
    args: >
      --ossIndexUsername ${{ secrets.OSS_INDEX_USERNAME }}
      --ossIndexPassword ${{ secrets.OSS_INDEX_PASSWORD }}       
      --failOnCVSS 7
      --suppression ./dependency-suppression.xml
      --disableNuspec    
      --disableNugetconf  
      --disableAssembly
```

### Maven Build Configuration

Skip the Maven plugin during build:

```bash
mvn clean install -DskipDependencyCheck=true
```

### OSS Index Integration

**Register at:** https://ossindex.sonatype.org/user/register  
**Purpose:** Enhanced CVE data beyond NVD  
**Required:** Yes (for GitHub Actions OWASP)

**Secrets Needed:**
- `OSS_INDEX_USERNAME`
- `OSS_INDEX_PASSWORD`

---

## Semantic Versioning

**Specification:** https://semver.org/

### Version Format

**MAJOR.MINOR.PATCH** (e.g., 3.2.1)

- **MAJOR:** Breaking changes (incompatible API changes)
- **MINOR:** New features (backward compatible)
- **PATCH:** Bug fixes (backward compatible)

### Update Policy

| Version Change | Auto-Update | Rationale |
|----------------|-------------|-----------|
| 1.2.3 → 1.2.4 | ✅ Yes | Patch: Bug fixes only |
| 1.2.3 → 1.3.0 | ✅ Yes | Minor: New features, backward compatible |
| 1.2.3 → 2.0.0 | ❌ No | Major: May have breaking changes, **even for CVE fixes** |

### Exceptions

**Never Auto-Update:**
- Spring Boot version (managed by izgw-bom)
- Bouncy Castle FIPS modules (bc-fips, bcpkix-fips, bctls-fips)
- izgw-bom parent version (checked separately, but not updated)
- Java version
- Dependencies managed by izgw-bom (unless critical CVE requires override)

**Always Prioritize (within patch/minor constraints):**
- CVE fixes (CVSS ≥7) that can be resolved with patch/minor updates
- Critical security patches
- Bug fixes affecting functionality

**CVE with Major Version Requirement:**
- Flag in PR for manual review
- Do NOT apply automatically
- Include in PR description with recommendation

---

## GitHub Actions

**Documentation:** https://docs.github.com/en/actions  
**Workflow Syntax:** https://docs.github.com/en/actions/using-workflows/workflow-syntax-for-github-actions

### Actions Used

**actions/checkout@v4**
- Checks out repository code
- Supports submodules and LFS

**actions/setup-java@v4**
- Sets up Java environment
- Version: 21 (for izgw-core)
- Distribution: temurin

**actions/cache@v4**
- Caches Maven dependencies
- Caches NVD CVE database
- Speeds up workflow execution

**GitHub CLI (gh)**
- Built-in to GitHub Actions runners
- Used for PR creation
- Supports labels, assignees, reviewers

### Workflow Triggers

**Schedule (Cron):**
```yaml
on:
  schedule:
    - cron: '0 9 * * 1-5'  # 9:00 AM UTC = 4:00 AM ET
```

**Manual Trigger:**
```yaml
on:
  workflow_dispatch:
    inputs:
      dry_run:
        description: 'Dry run mode'
        required: false
        default: 'false'
        type: boolean
```

### Secrets Management

All secrets stored in repository settings, never hardcoded:
- `MAVEN_GPR_TOKEN` - GitHub Packages authentication
- `EMAIL_USERNAME` - SMTP username
- `EMAIL_PASSWORD` - SMTP password
- `DEV_TEAM_EMAIL` - Notification recipients
- `NVDAPIKEY` - Optional, NVD API key

---

## Email Notification

**Action:** dawidd6/action-send-mail@v3  
**Documentation:** https://github.com/dawidd6/action-send-mail

### Configuration (AWS SES)

```yaml
- name: Send Email Notification
  if: steps.create_pr.outputs.pr_number != ''  # Only if PR created
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
      PR created: https://github.com/org/repo/pull/${{ steps.create_pr.outputs.pr_number }}
      
      Updates: ${{ env.DIRECT_UPDATE_COUNT }} dependencies
      CVEs Resolved: ${{ env.CVE_RESOLVED_COUNT }}
      CVEs Remaining: ${{ env.CVE_REMAINING_COUNT }}
```

**Key Points:**
- ✅ Uses existing MAIL_USERNAME and MAIL_PASSWORD secrets (AWS SES)
- ✅ Email ONLY sent when PR is created (conditional)
- ✅ No email if no updates available
- ✅ Recipients: kboone@ainq.com, weckels@ainq.com, pcahill@ainq.com
- ✅ CC: devops@izgateway.opsgenie.net
- ✅ AWS SES SMTP endpoint: email-smtp.us-east-1.amazonaws.com:465

### Future: Jira Integration

**Planned Enhancement:**
- Replace/supplement email with Jira ticket creation
- Use Jira REST API or incoming email integration
- Auto-link PR to Jira issue
- Track resolution time in Jira

---

## POM Update Patterns

### Pattern 1: Override BOM-Managed Dependency with Version Range

**Scenario:** Dependency version is managed by izgw-bom, but we need a newer version for CVE fix. Use version range to allow automatic minor/patch updates.

**Before:**
```xml
<parent>
    <groupId>gov.cdc.izgw</groupId>
    <artifactId>izgw-bom</artifactId>
    <version>1.1.0-SNAPSHOT</version>
</parent>

<dependencies>
    <dependency>
        <groupId>org.apache.commons</groupId>
        <artifactId>commons-text</artifactId>
        <!-- version inherited from BOM: 1.10.0 -->
    </dependency>
</dependencies>
```

**After (CVE requires 1.12.0):**
```xml
<dependencyManagement>
    <dependencies>
        <!-- Override from izgw-bom for CVE-2024-47554 -->
        <!-- Version range [1.12,2) allows 1.12.x, 1.13.x, etc. but not 2.x -->
        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-text</artifactId>
            <version>[1.12,2)</version>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <dependency>
        <groupId>org.apache.commons</groupId>
        <artifactId>commons-text</artifactId>
        <!-- version now from dependencyManagement: [1.12,2) -->
        <!-- Maven will automatically use latest 1.x >= 1.12 -->
    </dependency>
</dependencies>
```

**Version Range Syntax:**
- `[1.12,2)` = Minimum 1.12 (inclusive), Maximum 2.0 (exclusive)
- `[2.5,3)` = Minimum 2.5 (inclusive), Maximum 3.0 (exclusive)
- Square bracket `[` = inclusive bound
- Parenthesis `)` = exclusive bound

**Benefits:**
- ✅ Automatic minor/patch updates (1.12 → 1.13 → 1.14)
- ✅ Prevents major version jumps (won't update to 2.x)
- ✅ Reduces future maintenance (BOM can catch up)
- ✅ When BOM reaches 1.12+, override can be removed

### Pattern 2: Update Direct Dependency

**Scenario:** Dependency declared with version directly in izgw-core pom.xml.

**Before:**
```xml
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.15.2</version>
</dependency>
```

**After (2.15.4 available):**
```xml
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.15.4</version>
</dependency>
```

### Pattern 3: Property-Based Version

**Scenario:** Dependency uses property for version management.

**Before:**
```xml
<properties>
    <aws.sdk.version>2.20.0</aws.sdk.version>
</properties>

<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>s3</artifactId>
    <version>${aws.sdk.version}</version>
</dependency>
```

**After (2.20.5 available):**
```xml
<properties>
    <aws.sdk.version>2.20.5</aws.sdk.version>
</properties>

<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>s3</artifactId>
    <version>${aws.sdk.version}</version>
</dependency>
```

### Pattern 4: Remove Unnecessary Override

**Scenario:** BOM version has caught up to or exceeded our override version.

**Before:**
```xml
<dependencyManagement>
    <dependencies>
        <!-- Override from izgw-bom for CVE-2023-XXXXX -->
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
            <version>[2.15,3)</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

**Check BOM Version:**
```bash
# If izgw-bom now provides version 2.15.4 or higher
# The override is no longer necessary
```

**After (override removed):**
```xml
<!-- Override removed - BOM version now adequate -->
<!-- dependencyManagement section cleaned up or removed if empty -->
```

**When to Remove:**
- BOM version >= override minimum version
- No CVE requiring override
- Override version range satisfied by BOM

**Benefits:**
- ✅ Simplifies pom.xml
- ✅ Reduces maintenance burden
- ✅ Clearer dependency management
- ✅ Less chance of conflicts

### Pattern 5: Exclude BC-FIPS via Config File

**Configuration:**
```xml
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>versions-maven-plugin</artifactId>
    <configuration>
        <excludes>
            <!-- FIPS modules - manual updates only -->
            <exclude>org.bouncycastle:bc-fips</exclude>
            <exclude>org.bouncycastle:bcpkix-fips</exclude>
            <exclude>org.bouncycastle:bctls-fips</exclude>
        </excludes>
    </configuration>
</plugin>
```

**Current izgw-core BC-FIPS Dependencies:**
```xml
<dependency>
    <groupId>org.bouncycastle</groupId>
    <artifactId>bc-fips</artifactId>
    <!-- Version: 2.0.0 - DO NOT AUTO-UPDATE -->
</dependency>
<dependency>
    <groupId>org.bouncycastle</groupId>
    <artifactId>bcpkix-fips</artifactId>
    <!-- Version: 2.0.7 - DO NOT AUTO-UPDATE -->
</dependency>
<dependency>
    <groupId>org.bouncycastle</groupId>
    <artifactId>bctls-fips</artifactId>
    <!-- Version: 2.0.19 - DO NOT AUTO-UPDATE -->
</dependency>
```

**Rationale:** FIPS certification requires specific versions; updates may invalidate certification.

---

## Scheduling Across Projects

### Dependency Order

**Tier 1 (2:00 AM ET):**
- **izgw-bom** - Parent POM with dependency management

**Tier 2 (4:00-4:30 AM ET):**
- **izgw-core** (4:00 AM) - Core library
- **v2tofhir** (4:30 AM) - V2 to FHIR converter

**Tier 3 (5:00-5:30 AM ET):**
- **izgw-hub** (5:00 AM) - Hub application
- **izgw-transform** (5:30 AM) - Transformation service

### Schedule Rationale

1. **izgw-bom first**: Updates parent POM dependencies
2. **30-minute buffer**: Allows izgw-bom PR to be created and reviewed
3. **Core libraries next**: izgw-core and v2tofhir depend on BOM
4. **Applications last**: Hub and transform depend on core libraries
5. **Within 2-6 AM ET**: As specified by requirements

### Cron Schedule Mapping

| Project | Time (ET) | Time (UTC) | Cron Schedule |
|---------|-----------|------------|---------------|
| izgw-bom | 2:00 AM | 7:00 AM | `0 7 * * 1-5` |
| izgw-core | 4:00 AM | 9:00 AM | `0 9 * * 1-5` |
| v2tofhir | 4:30 AM | 9:30 AM | `30 9 * * 1-5` |
| izgw-hub | 5:00 AM | 10:00 AM | `0 10 * * 1-5` |
| izgw-transform | 5:30 AM | 10:30 AM | `30 10 * * 1-5` |

**Note:** UTC times assume Eastern Standard Time (UTC-5). During Daylight Saving Time (UTC-4), cron would need adjustment OR accept one hour earlier execution.

### Coordination Strategy

**Assumption:** When izgw-core workflow starts, izgw-bom has already been updated (separate process handles it).

**Reality Check:**
- PRs created but not necessarily merged
- Workflows detect updates based on published versions
- If izgw-bom PR not merged, core libraries use current BOM version
- Next day's run will pick up BOM updates after merge

**Manual Coordination:**
1. Review and merge izgw-bom PRs promptly (same day)
2. Allow dependent projects to pick up updates next day
3. Monitor dependency chain for proper propagation

---

## Dependency Scope and BOM Management

### Focus on Direct Dependencies

This automation focuses on **direct (non-BOM managed) dependencies** in izgw-core:

**Why Focus on Direct Dependencies?**
1. **BOM-managed separately**: izgw-bom has its own update process (runs at 2:00 AM ET)
2. **Reduces noise**: Fewer changes per PR, easier to review
3. **Prevents conflicts**: Avoids overriding BOM version management
4. **Project-specific**: Direct dependencies are unique to izgw-core

### Identifying BOM-Managed Dependencies

**Extract from effective POM:**
```bash
mvn help:effective-pom -Doutput=effective-pom.xml
xmlstarlet sel -t -m "//dependencyManagement/dependencies/dependency" \
  -v "concat(groupId,':', artifactId)" -n effective-pom.xml
```

**Common BOM-Managed Dependencies in izgw-core:**
- Spring Framework libraries (spring-*)
- Spring Boot starters (spring-boot-*)
- Jackson libraries (jackson-*)
- Apache Commons libraries (commons-*)
- Logging frameworks (slf4j-*, logback-*)

### Exception: CVE Overrides

If a BOM-managed dependency has a CVE that can be fixed with patch/minor update:
- Add override in `<dependencyManagement>` section
- Document reason in XML comment
- Include in PR for review
- Eventually coordinate with izgw-bom update

**Example:**
```xml
<dependencyManagement>
    <dependencies>
        <!-- Override from izgw-bom for CVE-2024-XXXXX (patch fix available) -->
        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-text</artifactId>
            <version>1.12.0</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### Checking for Latest izgw-bom

**Workflow checks for latest izgw-bom from develop branch:**

```bash
LATEST_BOM_VERSION=$(curl -s \
  https://raw.githubusercontent.com/IZGateway/izgw-bom/develop/pom.xml | \
  grep -A 1 '<artifactId>izgw-bom</artifactId>' | \
  grep '<version>' | \
  sed 's/.*<version>//;s/<\/version>.*//')

CURRENT_BOM_VERSION=$(mvn help:evaluate \
  -Dexpression=project.parent.version -q -DforceStdout)

if [ "$LATEST_BOM_VERSION" != "$CURRENT_BOM_VERSION" ]; then
  echo "::warning::izgw-bom update available: $CURRENT_BOM_VERSION → $LATEST_BOM_VERSION"
  # Flag for manual coordination, but don't update automatically
fi
```

**Rationale for Not Auto-Updating BOM:**
- Requires ecosystem coordination
- Multiple projects depend on same BOM version
- Update timing must be coordinated
- Separate process handles BOM updates

---

## Maven Settings Template

**Generated in Workflow:**

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

**Note:** Uses built-in `GITHUB_TOKEN` secret - sufficient for public GitHub Package repositories. No need for separate MAVEN_GPR_TOKEN.

---

## Glossary

- **BOM (Bill of Materials):** Parent POM that declares dependency versions
- **CVE:** Common Vulnerabilities and Exposures
- **CVSS:** Common Vulnerability Scoring System (0-10 scale, ≥7 is high/critical)
- **FIPS:** Federal Information Processing Standards
- **GPR:** GitHub Package Registry
- **NVD:** National Vulnerability Database
- **OWASP:** Open Web Application Security Project
- **SemVer:** Semantic Versioning
- **Transitive Dependency:** Dependency of a dependency

---

## External Links

### Maven Resources
- Maven Central: https://search.maven.org/
- Maven Documentation: https://maven.apache.org/guides/

### Security Resources
- NVD: https://nvd.nist.gov/
- GitHub Security Advisories: https://github.com/advisories
- OWASP: https://owasp.org/

### IZ Gateway Resources
- izgw-bom repository: https://github.com/IZGateway/izgw-bom
- izgw-core repository: https://github.com/IZGateway/izgw-core
- v2tofhir repository: https://github.com/IZGateway/v2tofhir

---

**Last Updated:** 2026-03-20
