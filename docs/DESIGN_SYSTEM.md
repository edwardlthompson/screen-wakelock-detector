# Design system — Material Design 3

Single source of truth for UI agents working on **Screen Wakelock Detector**.

**References:**

- [Material Design 3](https://m3.material.io/)
- [Material 3 in Compose](https://developer.android.com/develop/ui/compose/designsystems/material3)
- [Compose M3 component catalog](https://developer.android.com/develop/ui/compose/components)

---

## Policy

| Rule | Detail |
|------|--------|
| **Design system** | Material 3 (Material You); M3 Expressive opt-in for hero surfaces only |
| **Compose BOM** | Pin stable BOM in version catalog |
| **material3** | `androidx.compose.material3:material3` **1.4.0+** stable |
| **Forbidden** | `androidx.compose.material` (M2) — CI grep fails on import |
| **Theming** | `MaterialTheme` with M3 `ColorScheme`, `Typography`, `Shapes` |
| **Dynamic color** | API 31+ from wallpaper; static light/dark on API 29–30 |
| **Edge-to-edge** | Enabled; correct `WindowInsets` / `Scaffold` padding |
| **Accessibility** | 48dp min touch targets; content descriptions; TalkBack-tested flows |

---

## Theme architecture

```
app/src/main/java/.../ui/theme/
  Color.kt      # Static light/dark + semantic tokens
  Theme.kt      # MaterialTheme / expressive wrapper + dynamic color
  Type.kt       # M3 Typography scale (emphasis roles)
  Shape.kt      # extraLarge → extraSmall
app/src/main/java/.../ui/components/
  WakeEventCard.kt, ConfidenceIndicator.kt, PermissionChip.kt, ...
app/src/main/java/.../ui/navigation/
  NavHost + adaptive navigation suite
```

---

## Color roles

| Role | Usage in app |
|------|----------------|
| `primary` | Primary CTAs, selected nav, Grant buttons |
| `secondary` | Secondary actions, filter chips |
| `tertiary` | Accent badges, root-enhanced indicator |
| `error` / `errorContainer` | Nighttime wake tint, low confidence warnings |
| `surface` / `surfaceContainer` | Cards, sheets, list backgrounds |
| `onSurface` / `onSurfaceVariant` | Primary / supporting text |
| `outline` | Outlined cards, dividers |

Dynamic color on API 31+ maps wallpaper hues to these roles automatically.

---

## Typography

Use M3 `Typography` scale:

- **displayLarge / displayMedium** — onboarding hero headlines (expressive opt-in)
- **headlineSmall** — screen titles, last-wake app name
- **titleMedium** — list row primary (app + channel — largest on row)
- **bodyMedium** — descriptions, rationale bullets
- **labelMedium** — chips, timestamps (secondary to app name)

---

## Shapes

| Token | Usage |
|-------|-------|
| `extraLarge` | Onboarding hero cards |
| `large` | Elevated cards, last-wake hero |
| `medium` | Standard list cards |
| `small` | Chips, buttons |
| `extraSmall` | Inline badges |

---

## Motion

1. **Stable default:** M3 standard motion scheme for navigation and sheets.
2. **Expressive opt-in:** `@OptIn(ExperimentalMaterial3ExpressiveApi::class)` for onboarding hero, last-wake card, empty states — only when using material3 1.5.0-alpha on non-blocking branch.
3. **Reduce motion:** Respect system setting; disable spring slide-in for new wake rows when enabled.
4. **Shared axis:** List row → detail transition for continuity.

---

## Screen component checklist

| Screen | Primary M3 components |
|--------|-------------------------|
| **Home / history** | `Scaffold`, `TopAppBar`, `LazyColumn`, `ElevatedCard`/`OutlinedCard`, `FilterChip` |
| **Wake detail** | `LargeTopAppBar`, `ListItem`, `LinearProgressIndicator`, `FilledTonalButton` |
| **Last wake action** | Expressive-emphasis `Card`, `ExtendedFloatingActionButton` |
| **Onboarding** | `HorizontalPager`, `Button`/`TextButton`, rationale cards, `LinearProgressIndicator`, `AssistChip` |
| **Settings → Permissions** | `Switch` rows, `ListItem` + `SupportingText`, `Banner` |
| **Settings → Root** | Disabled rows: `LocalContentColor` alpha + `SupportingText` |
| **Search/filter** | `SearchBar`, `DatePickerDialog`, `DropdownMenu` |
| **Insights** | `NavigationBar` tab, `ElevatedCard` tiles, Canvas bar chart, `AssistChip` |
| **Quick-fix sheet** | `ModalBottomSheet`, `ListItem` actions |
| **Alerts** | System notifications — copy in [`NOTIFICATIONS.md`](NOTIFICATIONS.md) |

---

## Navigation

Bottom navigation (phone):

- **Home** — last wake hero + permission chips
- **History** — searchable log with date headers
- **Insights** — aggregated stats (M5+)
- **Settings** — Permissions center top-level

Large screens: `material3-adaptive-navigation-suite` (NavigationRail + list-detail where appropriate).

---

## Interaction patterns

| Pattern | M3 implementation |
|---------|-------------------|
| Sticky last-wake card | `ElevatedCard` at top of Home |
| Quick fix | `ModalBottomSheet` over current destination |
| Swipe actions | Swipe-to-reveal + overflow menu duplicate |
| Permission chips | `AssistChip` → deep link Settings row |
| Unknown wake education | `Banner` with `TextButton` |
| Mute undo | `Snackbar` with action |

---

## Empty / loading / error

| State | Treatment |
|-------|-------------|
| Empty history | Illustration + “Monitoring active — lock your phone…” + permission checklist |
| Empty insights | “Need at least 3 wakes” + link to History |
| Unknown attribution | Educational card + permission/root links |
| Loading insights | M3 shimmer via `placeholder` modifier |

---

## Gate GD checklist

See [`GATES.md`](GATES.md). Agents must verify:

- No M2 imports
- Dynamic + static themes on reference devices
- Edge-to-edge insets
- TalkBack labels on primary flows (M5)

---

## Store assets

Screenshot placeholders: `fastlane/metadata/android/en-US/images/`

Capture light, dark, and dynamic color variants per [`ONBOARDING.md`](ONBOARDING.md) order.

Record BOM and material3 version pins in [`AGENT_MEMORY.md`](AGENT_MEMORY.md) when scaffold lands.
