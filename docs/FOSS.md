# FOSS dependency policy

Free and open source software requirements for **Screen Wakelock Detector**.

**License:** Apache-2.0 (project)  
**Distribution:** F-Droid only

---

## Requirements

| Requirement | Enforcement |
|-------------|-------------|
| **OSI-approved licenses** | All Gradle dependencies must use OSI-approved licenses |
| **No proprietary SDKs** | No Firebase, Play Services, Crashlytics, proprietary analytics/ads |
| **No closed blobs** | No prebuilt proprietary `.aar` without source |
| **Public source** | GitLab public repo; tags match releases |
| **No network in default flavor** | No `INTERNET` permission — CI grep in validate stage |
| **Reproducible builds** | From v1.0.0 — pinned toolchain |

---

## Approved dependencies (examples)

| Dependency | License | Purpose |
|------------|---------|---------|
| Kotlin stdlib | Apache-2.0 | Language |
| AndroidX / Compose / Room | Apache-2.0 | UI, persistence |
| Hilt | Apache-2.0 | DI |
| libsu | Apache-2.0 | In-app root shell |
| JUnit / MockK | OSS test libs | Testing |

Pin versions in Gradle version catalog. Document new deps in [`AGENT_MEMORY.md`](AGENT_MEMORY.md) decisions log.

---

## Forbidden

- Firebase / Google Analytics / Crashlytics
- Google Play Services (unless unavoidable — **none expected**)
- Proprietary ad SDKs
- Non-free Maven artifacts
- Oracle JDK in CI (use Temurin/OpenJDK 17)

---

## Adding a dependency (agent checklist)

1. Confirm license is OSI-approved (SPDX identifier in POM or manual check)
2. Add to version catalog with comment if license non-obvious
3. Run `./gradlew :app:dependencies` and license-check CI job locally
4. Update AGENT_MEMORY if decision is non-trivial
5. Never add dependency with `INTERNET`-requiring SDK for analytics

---

## License audit process

### Local

```bash
./gradlew :app:licenseDebugReport
# or project-specific license plugin when configured
```

### CI jobs

| Job | Stage | Checks |
|-----|-------|--------|
| `license-check` | validate | Reports non-allowed licenses |
| `foss-audit` | validate | Dependency policy script / allowlist |
| `no-internet-manifest` | validate | Release manifest lacks INTERNET |

Fail pipeline on violation.

---

## Source headers

Where applicable, Kotlin source files may include:

```kotlin
// SPDX-License-Identifier: Apache-2.0
```

Not required on every file — project LICENSE applies to whole repo.

---

## Third-party notices

If required by dependency licenses, aggregate notices in `app/src/main/assets/open_source_licenses.html` (M5) and link from Settings → About.

---

## Security overlap

FOSS policy complements [`SECURITY.md`](../SECURITY.md):

- Reproducible builds + Dependabot reduce supply-chain risk
- No network permission reduces exfil surface
- libsu root usage constrained by command allowlist

---

## Exceptions

Any exception to this policy requires:

1. Documented rationale in AGENT_MEMORY decisions log
2. Explicit `[HUMAN]` approval
3. F-Droid anti-feature declaration if applicable

Default: **no exceptions**.
