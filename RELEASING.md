# Releasing izgw-core

This covers how to release izgw-core using the GitHub Actions workflows.

## Versioning

We use [Semantic Versioning](https://semver.org/):
- **Major** (2.0.0 → 3.0.0): Breaking changes
- **Minor** (2.3.0 → 2.4.0): New features, backwards-compatible
- **Patch** (2.3.0 → 2.3.1): Bug fixes

Development versions end in `-SNAPSHOT` (e.g., `2.4.0-SNAPSHOT`). Releases drop the suffix.

## Branch Strategy

- **develop**: Where active development happens
- **main**: Production-ready code, always reflects the latest release
- **release/X.Y.Z**: Created during standard releases (kept for history)
- **hotfix/X.Y.Z**: Created manually (from the main branch) for emergency patches (kept for history)

---

## Standard Release

Use this for normal releases from `develop`.

### Before You Start

Make sure:
- All the PRs you want in this release are merged to `develop`
- CI is passing on `develop`
- No SNAPSHOT dependencies (the workflow will check this anyway)

### Run the Workflow

1. Go to **Actions** → **Release - Standard**
2. Click **Run workflow**
3. Make sure `develop` is selected
4. Enter the **release version** (e.g., `2.4.0`)
5. Optionally enter the **next SNAPSHOT version**
    - Leave blank to auto-increment minor version (`2.4.0` → `2.5.0-SNAPSHOT`)
    - Or specify something like `3.0.0` for a major bump
6. Click **Run workflow**

### What Happens

1. Validates everything (version format, no SNAPSHOT deps, tag doesn't exist, etc.)
2. Creates a `release/X.Y.Z` branch from `develop`
3. Updates `RELEASE_NOTES.md` with merged PRs
4. Sets the version to the release version
5. Runs tests and OWASP dependency check
6. Deploys to GitHub Packages
7. Merges to `main` and creates a tag
8. Creates a GitHub Release with artifacts
9. Merges release notes back to `develop` and bumps to the next SNAPSHOT version

**After It's Done** Check the [GitHub Release](https://github.com/IZGateway/izgw-core/releases) looks good

---

## Hotfix Release

Use this when there's a critical bug in production that can't wait for a normal release.

### Create the Hotfix Branch

First, manually create a hotfix branch from `main`:

```bash
git checkout main
git pull
git checkout -b hotfix/2.14.1
git push -u origin hotfix/2.14.1
```

### Make Your Fixes

Developers create fix branches and PR them to the hotfix branch (not develop):

```bash
git checkout hotfix/2.14.1
git checkout -b fix/the-critical-bug
# make fixes
git push -u origin fix/the-critical-bug
# create PR targeting hotfix/2.14.1
```

Merge all the fix PRs to the hotfix branch.

### Run the Workflow

1. Go to **Actions** → **Release - Hotfix**
2. Click **Run workflow**
3. Select your `hotfix/X.Y.Z` branch
4. Enter the **release version** (e.g., `2.14.1`)
5. Click **Run workflow**

### What Happens

Same as a standard release, except:
- Uses the existing hotfix branch instead of creating a new one
- Does NOT bump the version on `develop` (it just merges release notes)

### After It's Done

- **Check the job summary for "Action Required"** - The workflow tries to merge your hotfix into `develop`, but if there were conflicts, some files might not have made it. If you see a warning listing files, go verify those changes are actually in `develop` (or cherry-pick them manually).
- Verify the release
- Notify the teams - this is urgent, so make sure they know

---

## If Something Goes Wrong

The workflow automatically tries to clean up on failure:
- Deletes the git tag
- Reverts the merge to main
- Deletes the release branch (for standard releases)
- Removes the GitHub Packages artifact
- Deletes the GitHub Release

Check the job summary for details. If auto-cleanup couldn't handle something, it'll tell you what to clean up manually.

Common issues:
- **SNAPSHOT dependencies**: Update your deps to release versions
- **Tag already exists**: You already released this version, use a different one
- **Tests failed**: Fix them on develop and try again

---

## Key Differences: Standard vs Hotfix

| | Standard | Hotfix |
|---|----------|--------|
| Source branch | `develop` | `hotfix/*` (from main) |
| Creates new branch | Yes (`release/*`) | No (uses existing) |
| Bumps develop version | Yes | No |
| When to use | Planned releases | Emergency patches |
