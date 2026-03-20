## Why

The izgw-core library is a foundational component used by IZ Gateway Hub and Transformation services, with numerous dependencies managed through Maven. Currently, dependency updates must be performed manually, which creates several critical issues:

1. **Security vulnerabilities accumulate**: CVEs in dependencies (both direct and transitive) may remain unpatched for extended periods, exposing the IZ Gateway ecosystem to known security risks
2. **Manual maintenance burden**: Developers must track updates across 60+ dependencies declared in pom.xml, plus transitive dependencies
3. **Delayed bug fixes**: Important bug fixes in patch/minor versions are not applied promptly
4. **Integration risk**: Infrequent batch updates can introduce multiple breaking changes simultaneously
5. **BOM synchronization challenges**: izgw-core imports versions from izgw-bom (1.1.0-SNAPSHOT), but may need to override specific versions for security or stability

Maven dependency management for izgw-core has unique characteristics:
- **Core library**: Changes affect all downstream projects (izgw-hub, transformation services)
- **BOM parent**: Inherits dependency versions from izgw-bom (parent POM)
- **Transitive complexity**: 60+ direct dependencies pull in hundreds of transitive dependencies
- **FIPS compliance**: Uses Bouncy Castle FIPS modules (bc-fips, bcpkix-fips, bctls-fips)
- **High security requirements**: Government healthcare data processing requires rapid CVE response
- **Test coverage**: Changes must not break existing tests (JaCoCo, Surefire)

## What Changes

Implement an automated dependency update CI/CD process for izgw-core that:

### 1. GitHub Actions Workflow
- **Schedule**: Nightly at 3 AM UTC (Monday-Friday) to avoid conflict with other jobs
- **Manual trigger**: Support workflow_dispatch for on-demand execution
- **Target branch**: Run on `develop` branch for testing before release

### 2. Dependency Update Strategy
- **Detect updates**: Use `versions-maven-plugin` to identify available versions
- **Patch updates**: Auto-update patch versions (e.g., 3.2.1 → 3.2.2)
- **Minor updates**: Auto-update minor versions (e.g., 3.2.1 → 3.3.0)
- **Major versions**: EXCLUDE from automation (e.g., 3.2.1 → 4.0.0) - **even for CVE fixes**
- **CVE prioritization**: Prioritize dependencies with known vulnerabilities (**all severities** - goal is CVE-free)
- **CVE without fix**: Note in PR, don't fail workflow
- **BOM-managed dependencies**: Focus on dependencies NOT managed by izgw-bom (BOM updates handled separately)
- **izgw-bom version**: Check for and use latest izgw-bom version from develop branch

### 3. Minimal POM Changes Strategy
- **Use `<dependencyManagement>` section**: Override specific versions from izgw-bom without modifying declared dependencies
- **Version range syntax**: Use `[major.minor,major+1)` to allow automatic minor/patch updates while pinning major version
- **Remove unnecessary overrides**: Clean up overrides when BOM version catches up
- **Preserve structure**: Maintain existing dependency declarations and organization
- **Property-based versions**: Use Maven properties where patterns exist
- **Document overrides**: Add comments explaining why versions are overridden

Example minimal change with version range:
```xml
<dependencyManagement>
    <dependencies>
        <!-- Override from izgw-bom for CVE-2024-XXXXX - allows automatic minor/patch updates -->
        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-text</artifactId>
            <version>[1.12,2)</version>  <!-- Fixed major 1, minimum 1.12, allows 1.x updates -->
        </dependency>
    </dependencies>
</dependencyManagement>
```

**Version Range Benefits:**
- ✅ Automatic pickup of newer minor/patch versions (e.g., 1.12 → 1.13 → 1.14)
- ✅ Prevents major version jumps (stays within 1.x)
- ✅ Reduces need for frequent override updates
- ✅ Simplifies maintenance

### 4. CVE Remediation
- **GitHub Actions OWASP scan**: Use `dependency-check/Dependency-Check_Action@main` (faster, updated nightly)
- **Disable Maven plugin**: Set `-DskipDependencyCheck=true` in Maven build
- **Scan before updates**: Identify current vulnerabilities in built JAR (all severities - goal is CVE-free)
- **Update prioritization**: Update CVE-affected dependencies first (within patch/minor constraints)
- **Scan after updates**: Verify vulnerabilities are resolved
- **CVE without fix**: If CVE has no patch/minor fix available, note in PR but don't fail workflow
- **Suppression management**: Respect existing dependency-suppression.xml for false positives
- **Report generation**: Include CVE details in PR description
- **CVE constraint**: Only apply patch/minor updates even for CVE fixes; flag major version CVEs for manual review

### 5. Build Validation
- **Clean build**: `mvn clean install -DskipDependencyCheck=true` must succeed
- **All tests pass**: Surefire tests, including JaCoCo coverage
- **Dependency tree analysis**: Verify no dependency conflicts introduced
- **GitHub Actions OWASP check**: Use separate action for CVE scanning (not Maven plugin)
- **Artifact generation**: Produce both main and test jars

### 6. Automated PR Creation
- **Branch naming**: `automated-maven-updates-YYYYMMDD-HHMMSS`
- **Target**: Pull request to `develop` branch
- **PR description**: Include:
  - Dependency update summary table
  - CVE information and remediation details
  - Build and test results
  - Dependency tree comparison
  - Links to release notes
- **Labels**: Auto-apply `dependencies`, `security` labels
- **Reviewers**: Auto-assign core team members

### 7. Authentication Configuration
- **GitHub Packages**: Configure Maven authentication for izgw-bom and izgw-core repositories
- **NVD API**: Use NVDAPIKEY secret for faster CVE database updates (optional)
- **Token management**: Use MAVEN_GPR_TOKEN for GitHub Package Registry

## Capabilities

### New Capabilities
- `automated-maven-dependency-updates`: Nightly automated detection and update of Maven dependencies (patch/minor only)
- `cve-auto-remediation`: Automatic identification and resolution of dependency vulnerabilities
- `dependency-update-pr-automation`: Automated PR creation with comprehensive update context
- `bom-override-management`: Intelligent override of izgw-bom versions with minimal pom.xml changes

### Modified Capabilities
- None - This is a new CI/CD process that doesn't modify existing code

## Impact

**Affected Code:**
- `pom.xml` - Will receive dependency version updates via automated PRs
  - May add/update `<dependencyManagement>` section for BOM overrides
  - Existing `<dependencies>` section preserved unchanged
- No Java code changes

**New Files:**
- `.github/workflows/maven-dependency-updates.yml` - New GitHub Actions workflow
- Documentation updates explaining the automation process

**Dependencies:**
- **Maven plugins** (added to pom.xml build section):
  - `versions-maven-plugin` (org.codehaus.mojo:versions-maven-plugin:2.16.2)
  - ~~dependency-check-maven~~ - Disabled via `-DskipDependencyCheck=true` (GitHub Action used instead)
- **GitHub Actions**:
  - `dependency-check/Dependency-Check_Action@main` - CVE scanning (faster, updated nightly)
  - `dawidd6/action-send-mail@v3` - Email notifications via AWS SES
- **GitHub secrets** (all already configured ✅):
  - `GITHUB_TOKEN` - Built-in secret for GitHub Packages and PR creation
  - `OSS_INDEX_USERNAME` - Already configured ✅
  - `OSS_INDEX_PASSWORD` - Already configured ✅
  - `MAIL_USERNAME` - Already configured ✅ (AWS SES)
  - `MAIL_PASSWORD` - Already configured ✅ (AWS SES)

**Affected Data:**
- None - This is a build/CI process only

**Testing Impact:**
- **Workflow testing**: Requires testing with sample dependency updates before enabling schedule
- **PR validation**: Ensure generated PRs contain correct information and links
- **Failure scenarios**: Test behavior when updates break builds or introduce CVE issues
- **BOM override testing**: Verify minimal pom.xml changes work correctly
- **Downstream impact**: Test that izgw-core updates don't break dependent projects

**CI/CD Impact:**
- **New workflow**: Runs nightly (Mon-Fri), ~15-20 minutes per run
- **GitHub Actions minutes**: ~5-7 hours per month
- **PR notifications**: Team receives automated PR notifications
- **Review required**: All PRs require manual review and approval before merge
- **Merge target**: PRs target `develop` branch, then follow normal release process to main

**Security Impact:**
- ✅ **Faster CVE response**: Automated detection and remediation within 24 hours
- ✅ **Audit trail**: All dependency changes tracked via git history and PRs
- ✅ **Controlled updates**: Major versions still require manual review
- ✅ **Validation**: All updates tested before PR creation
- ⚠️ **Token security**: Requires secure management of MAVEN_GPR_TOKEN
- ⚠️ **Supply chain**: Automated updates require trust in Maven Central and GitHub Packages

**Downstream Impact:**
- **izgw-hub**: May need to adopt updated izgw-core versions
- **Transformation services**: May need to adopt updated izgw-core versions
- **Version coordination**: Consider synchronized updates across ecosystem

**Cost Considerations:**
- **GitHub Actions minutes**: ~5-7 hours/month (included in most plans)
- **Storage**: Minimal (workflow logs, ~100MB/month)
- **Maintenance time savings**: Estimated 2-4 hours/month of manual dependency tracking

## Non-Goals

This change does NOT include:
- ❌ Automatic major version updates (too risky, require manual review) - **even for CVE fixes**
- ❌ Updates to dependencies managed by izgw-bom (handled separately in BOM update process)
- ❌ Updates to Spring Boot version (managed by izgw-bom)
- ❌ Updates to Java version (21)
- ❌ Updates to izgw-bom parent version (checked separately for latest from develop)
- ❌ Updates to Bouncy Castle FIPS modules (require certification validation)
- ❌ Automatic merging of PRs (requires human review)
- ❌ Rollback automation (handled via git revert if needed)
- ❌ Automatic updates to plugin versions that require configuration changes
- ❌ Automatic propagation to downstream projects (separate process)
- ❌ Using Maven dependency-check-maven plugin (using faster GitHub Actions OWASP instead)
