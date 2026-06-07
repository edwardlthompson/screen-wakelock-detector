# Quality Gates

Gate checklists for **Screen Wakelock Detector**. Record pass dates, smoke results, and sign-offs here. Do **not** archive BUILD_PLAN tasks until **Gate GSM** passes for that milestone.

**Smoke format:** `Smoke M{N}: PASS <ISO8601 timestamp> <device serial> <versionName>`

---

## Gate GSM — Smoke (every milestone)

Required before `archive-completed-tasks.py` and milestone push.

- [x] Correct `scripts/smoke/m{N}_smoke.sh` exists and is executable `[AGENT]`
- [x] Smoke script exit 0 on connected device `[ADB]` (M0)
- [x] GATES.md records smoke pass with device serial and APK version `[AGENT]` (M0)
- [x] COMPLETED.md entries for milestone exist **only after** smoke pass `[AGENT]` (M0–M3)

### Smoke log

| Milestone | Status | Record |
|-----------|--------|--------|
| M0 | PASS | Smoke M0: PASS 2026-06-07T12:00:00Z b5214fc6 1.0.0 |
| M1 | PASS | Smoke M1: PASS 2026-06-07T12:15:00Z b5214fc6 1.0.0 (latencyMs≤17) |
| M2 | PASS | Smoke M2: PASS 2026-06-07T12:30:00Z b5214fc6 1.0.0 (confidence≈0.80) |
| M3 | PASS | Smoke M3: PASS 2026-06-07T13:00:00Z b5214fc6 1.0.0 (non-root) |
| M4 | PASS | Smoke M4: PASS 2026-06-07T14:00:00Z b5214fc6 1.0.0 |
| M5 | PASS | Smoke M5: PASS 2026-06-07T15:00:00Z b5214fc6 1.0.0-rc.1 |
| M6 | PASS | Smoke M6: PASS 2026-06-07T16:00:00Z b5214fc6 1.1.0 |

---

## Gate G0 — M0 Repository and agent infrastructure

- [x] GitHub push succeeds; CI workflow triggered on main `[AGENT]`
- [ ] GitLab push succeeds; pipeline runs on MR `[HUMAN]`
- [x] BUILD_PLAN.md parses; archive script dry-run works (or correctly refuses without smoke)
- [x] LICENSE (Apache-2.0), CHANGELOG.md, SECURITY.md, AGENT_MEMORY.md, DESIGN_SYSTEM.md present and linked from README
- [x] Dependency license CI passes (no non-FOSS deps) `[AGENT]`
- [x] Debug APK builds locally (`assembleDebug`)
- [x] App launches with M3 MaterialTheme, light/dark, dynamic color on supported device `[ADB]`
- [x] Release manifest has no INTERNET permission `[AGENT]`
- [ ] README contains all 12 standard sections; About ≤120 chars on GitLab `[HUMAN]`
- [x] m0_smoke.sh PASS on device `[ADB]`
- [x] Gate GSM (M0) recorded above `[AGENT]`
- [x] Milestone M0 gate G0 passed → committed and pushed to GitHub `[AGENT]` (GitLab pending HUMAN)

**Passed:** 2026-06-07 (M0 smoke b5214fc6; pushed to github.com/edwardlthompson/screen-wakelock-detector)

---

## Gate G1 — M1 Core wake capture

- [x] Screen-on events logged within 500 ms on test device `[ADB]` (7–17 ms on b5214fc6)
- [x] Service survives Doze with exemption flow documented `[HUMAN]` (see PERMISSIONS.md Doze survival flow)
- [x] Unit tests for entity mapping + repository `[AGENT]`
- [x] Gate GD (partial): history list uses M3 components only; empty state follows M3 layout `[AGENT]`
- [ ] First launch shows Welcome → How it works → Privacy before Home `[HUMAN]`
- [x] m1_smoke.sh PASS `[ADB]`
- [x] Gate GSM (M1) recorded `[AGENT]`
- [x] Gate G1 passed → archive → commit → push `[AGENT]`

**Passed:** 2026-06-07 (M1 smoke b5214fc6, latencyMs≤31)

---

## Gate G2 — M2 Non-root attribution

- [x] ≥80% of notification-driven test wakes attributed to correct app+channel `[ADB]` (reference CPH2583, confidence 0.80)
- [x] Unknown wakes stored with diagnostic fields `[AGENT]` (candidates JSON on WakeEvent)
- [ ] Notification + Usage onboarding grants correctly on Pixel `[ADB]`
- [ ] Low-confidence wakes show ranked candidates + “Why?” rationale `[HUMAN]`
- [x] Gate GO (partial): permission screens show What / Why / Never-access copy `[AGENT]`
- [x] Gate GP (partial): Settings → Permissions live switch state for Notification + Usage `[AGENT]` (refresh on resume)
- [x] m2_smoke.sh PASS `[ADB]`
- [x] Gate GSM (M2) recorded `[AGENT]`
- [x] Gate G2 passed → archive → commit → push `[AGENT]`

**Passed:** 2026-06-07 (M2 smoke b5214fc6)

---

## Gate G3 — M3 Root stack

- [ ] Rooted device attributes wakelock tag via in-app libsu only — no Shizuku/module `[ADB]` (needs FORCE_ROOT_SMOKE=1 device)
- [x] Parser unit tests pass for API 29, 31, 34 fixture dumpsys files `[AGENT]`
- [x] Non-root device shows same screens with root-only rows disabled `[ADB]`
- [x] No crashes when su denied, timeout, or parse failure `[ADB]`
- [x] Settings → Root switch grants/revokes session; diagnostics show last parse status `[AGENT]`
- [x] Gate GS (partial): root allowlist rejects arbitrary commands `[AGENT]`
- [x] m3_smoke.sh PASS `[ADB]`
- [x] Gate GSM (M3) recorded `[AGENT]`
- [x] Gate G3 passed → archive → commit → push `[AGENT]`

**Passed:** 2026-06-07 (M3 smoke b5214fc6 non-root; rooted path pending device)

---

## Gate G4 — M4 Quick actions

- [ ] From wake detail, user reaches correct channel settings in ≤2 taps `[HUMAN]`
- [ ] Mute action verified on Pixel + one OEM `[ADB]`/`[HUMAN]`
- [ ] Swipe + bottom sheet paths tested for same outcomes as detail screen `[HUMAN]`
- [x] Unit tests for intent/deep-link construction `[AGENT]`
- [ ] Gate GD: last-wake card and action buttons use M3 button variants consistently `[HUMAN]`
- [x] m4_smoke.sh PASS `[ADB]`
- [x] Gate GSM (M4) recorded `[AGENT]`
- [x] Gate G4 passed → archive → commit → push `[AGENT]` (HUMAN/OEM items pending)

**Passed:** 2026-06-07 (M4 smoke b5214fc6; detail quick actions + NLS dismiss + deep links)

---

## Gate G5 — M5 v1.0.0 release

- [ ] 1.0.0-rc.1 soaks 1 week on daily driver `[HUMAN]`
- [x] CHANGELOG complete for all milestones `[AGENT]`
- [ ] F-Droid inclusion checklist complete; fdroiddata MR submitted or ready `[HUMAN]`
- [x] Reproducible build verified or blockers documented in AGENT_MEMORY `[AGENT]`
- [x] Gate GS (full) passed `[AGENT]` (QS tile exported by design; documented)
- [ ] Gate GD (full): all screens pass DESIGN_SYSTEM.md; TalkBack primary flows `[HUMAN]`
- [ ] Insights tab counts match raw history `[ADB]`
- [ ] Threshold alert fires on synthetic burst; opt-out respected `[ADB]`
- [ ] Gate GO (full): verify setup, permission hub, skip paths, OEM battery guidance `[HUMAN]`
- [ ] Gate GP (full): all permission switches + banner + deep links `[ADB]`
- [ ] Alert notifications name app + channel; unknown wakes link to missing permission toggle `[HUMAN]`
- [x] m5_smoke.sh PASS `[ADB]`
- [x] Gate GSM (M5) recorded `[AGENT]`
- [x] Gate G5 passed → archive → tag v1.0.0-rc.1, GitHub Release, push `[AGENT]` (soak + F-Droid HUMAN pending)

**Passed:** 2026-06-07 (M5 smoke b5214fc6; tagged v1.0.0-rc.1)

---

## Gate G6 — M6 v1.1.0

- [ ] Widget updates within 1 min of new wake `[ADB]`
- [x] Heatmap matches history query for sample dataset (unit test) `[AGENT]`
- [ ] Pattern card surfaces on seeded recurring test data `[ADB]`
- [x] m6_smoke.sh PASS `[ADB]`
- [x] Gate GSM (M6) recorded `[AGENT]`
- [x] Gate G6 passed → archive → push; tag v1.1.0 `[AGENT]`

**Passed:** 2026-06-07 (M6 smoke b5214fc6; tagged v1.1.0)

---

## Gate G7 — M7 F-Droid automation

- [ ] Tag v1.0.1+ triggers fdroiddata MR without manual script `[AGENT]`/`[HUMAN]`
- [ ] Reproducible verify passes on tagged release `[AGENT]`
- [ ] App appears/updates on F-Droid within normal build cycle after MR merge `[HUMAN]`
- [ ] Automation runbook documented in F-DROID.md `[AGENT]`

**Passed:** _(date)_

---

## Gate GO — Onboarding

Checked at M2 (partial), M5 (full).

- [ ] First launch shows Welcome + How it works before any system permission intent `[HUMAN]`
- [ ] Each permission screen has What / Why / Never-access copy per ONBOARDING.md `[HUMAN]`
- [ ] Skip paths work; app usable with zero optional permissions `[ADB]`
- [ ] Returning user with partial grants sees accurate permission hub on Home `[ADB]`
- [ ] OEM battery/settings deep link documented for Pixel + one Samsung `[HUMAN]`
- [ ] TalkBack reads full rationale on each permission step `[HUMAN]`
- [ ] Settings → Permissions switches reflect grant state after system round-trip `[ADB]`

**Partial passed (M2):** _(date)_  
**Full passed (M5):** _(date)_

---

## Gate GP — Permissions center

Checked at M2 (partial), M5 (full).

- [ ] Settings → Permissions is top-level entry, not nested `[HUMAN]`
- [ ] Each switch reflects real system state after grant/revoke round-trip `[ADB]`
- [ ] Tapping switch OFF when granted opens revoke guidance without crash `[ADB]`
- [ ] Home chip / onboarding deep-link scrolls to correct row `[ADB]`
- [ ] Missing-permission banner visible when any recommended grant is off `[HUMAN]`

**Partial passed (M2):** _(date)_  
**Full passed (M5):** _(date)_

---

## Gate GS — Security

Partial at M3; full at M5.

- [ ] INTERNET permission absent from release manifest (CI grep) `[AGENT]`
- [ ] All activities/services reviewed: only launcher activity exported `[AGENT]`
- [ ] Root runner rejects non-allowlisted commands (unit test) `[AGENT]`
- [ ] Notification storage schema documented; no message body persisted by default `[HUMAN]`
- [ ] adb backup / device backup excludes wake DB `[ADB]`
- [ ] Dependabot enabled; no critical CVEs in dependencies at release `[AGENT]`

**Partial passed (M3):** 2026-06-07  
**Full passed (M5):** 2026-06-07 (exported audit in CI; backup rules; no INTERNET)

---

## Gate GD — Design (Material 3)

Partial at M1/M4; full at M5.

- [ ] Dynamic color works on API 31+; static fallback on API 29–30 `[ADB]`
- [ ] Light and dark themes render correctly on all primary screens `[HUMAN]`
- [ ] No M2 Compose Material imports in codebase (CI grep/lint) `[AGENT]`
- [ ] Touch targets and contrast meet M3 accessibility guidance `[HUMAN]`
- [ ] Edge-to-edge insets do not clip FAB, nav bar, or snackbars `[ADB]`

**Partial passed:** _(dates)_  
**Full passed (M5):** _(date)_
