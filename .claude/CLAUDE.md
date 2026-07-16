# izgw-core — Project Instructions

Shared types, interfaces, and utilities for IZ Gateway projects. Published to GitHub Packages.
Consumed by `izgw-hub`, `izgw-transform`, and other IZ Gateway services.

**Skills:** `java-maven-style`

**Public repo** — follow IZ Gateway Public Repo Policy (in global CLAUDE.md).

---

## Build

```cmd
mvn clean install
mvn clean package
mvn dependency-check:check
mvn clean site
```

---

## Notes

- This is a library — no runnable application
- Version managed via `izgw-bom`
- Changes here affect all consuming services; check impact on `izgw-hub`, `izgw-transform`, etc.
- Test class suffix: `Tests` (Surefire: `**/*Tests.java`)
