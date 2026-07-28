# Changelog

Notable changes to kanso, newest first. Format follows [Keep a Changelog][keepachangelog].

kanso is consumed as a git submodule, so "upgrading" means moving the submodule pointer. This
file is what tells you what you are moving onto.

**Releases are not tagged.** A submodule records a commit SHA, so that is what this file gives
you: each section below is headed with the SHA to move to. Tags `v0.1.0`–`v0.1.3` exist on the
remote and are left in place as history; nothing after them is tagged, and the version numbers
on older sections are labels for those sections rather than refs you can fetch.

```bash
git -C third_party/kanso fetch origin main
git -C third_party/kanso checkout <sha-from-this-file>
git add third_party/kanso && git commit -m "chore: move kanso to <sha>"
```

**What counts as a breaking change**, since there is no version number left to carry the signal:
a changed public signature, a removed parameter, or a change to which artifacts arrive via `api`.
Each is called out under a **Changed** heading with the migration spelled out, because the `api`
surface is a promise, not an implementation detail. **Visual output is deliberately not covered**
— retuning a colour curve or a component's internal padding changes pixels without appearing
here as breaking, so pin an exact SHA if you screenshot-test against kanso.

kanso is not published to a Maven repository, and that is deliberate — see
[docs/foundation-roadmap.md](docs/foundation-roadmap.md) § 1.5.

## Unreleased

### Added

- **`Kanso.sizing`** — icon sizes named by role (`icon`, `iconSmall`, `iconBadge`, `iconLarge`),
  settable via `KansoTheme(sizing = …)`. The defaults are exactly the literals they replaced, so
  nothing renders differently; every screenshot golden is byte-identical.

  The trigger was duplication, not a new component: 24dp was written into three files that have
  to agree (a list row's leading icon, a setting row's, and the skeleton that stands in for
  them), 48dp into three more, and `KansoDivider`'s 40dp inset was the hand-computed sum of two
  of them. That last one now derives itself — `Kanso.sizing.icon + Kanso.spacing.lg` — because
  written as a number it was correct today and silently wrong the moment the icon size moved, in
  a way no test would catch: the divider would still render, just aligned to nothing.

  **The 48dp minimum touch target is deliberately not a token.** It is an accessibility floor
  that comes from the platform, not a brand decision, and a theme that could set it to 32dp
  would be introducing a defect no test in this repo would notice. It is an internal constant
  the three call sites share.

- **API documentation**, generated from the KDoc by Dokka and published to GitHub Pages on every
  push to `main`: <https://ayanami770.github.io/kanso/>. Every symbol links to the line it is
  declared on, so the rationale in the comments is one click from the code it describes.

  `./gradlew :kanso:checkDokkaLinks` builds it and fails on any link that would render as plain
  text. That check exists because `failOnWarning` has a blind spot: it catches a reference Dokka
  cannot resolve, but not one that resolves to a symbol with no published page — anything
  `internal`, and a data class's constructor parameters. Those are emitted silently. Five were
  already in the tree.

- Formatting is enforced. `./gradlew spotlessApply` fixes it; `spotlessCheck` runs first in CI,
  ahead of the tests. Three ktlint rules are off — composable functions are PascalCase, icon
  properties are not screaming-snake constants, and a file may hold more than one top-level
  declaration — each with its reason next to it in `build.gradle.kts`. The licence header is
  enforced rather than merely conventional, so a new file cannot ship without one. Lines wrap at
  100, which is where the code already wrapped: the 14 lines that exceeded it were rewrapped
  rather than the ceiling raised.

### Changed

- **Releases are no longer tagged.** The four `v0.1.x` tags stay on the remote as history; from
  here on a consumer moves their submodule to a commit SHA, and every section in this file is
  headed with the SHA to move to. Nothing about how breaking changes are communicated has
  changed — see the policy at the top of this file — only where the identifier comes from.
- The Compose test rule moved to `androidx.compose.ui.test.junit4.v2.createComposeRule`, whose
  `StandardTestDispatcher` queues coroutines rather than running them eagerly. Test-only; no
  effect on anything a consumer compiles against. All 54 tests and all 12 goldens pass unchanged
  under it, so the eager dispatcher was not propping any of them up.

## 2026-07-28 — `caec27d`

### Changed

- **`compileSdk` 35 → 36 on both modules — consuming apps must follow.** `androidx.core:core-ktx`
  1.18.0 and `activity-compose` 1.13.0 declare a minimum `compileSdk` in their AAR metadata, and
  that requirement reaches any app with them on its classpath — including transitively, since
  kanso takes core-ktx as `implementation`. An app on `compileSdk 35` will fail its build with
  "requires libraries and applications that depend on it to compile against version 36 or later".

  *Migration:* `compileSdk = 36` in the app. `targetSdk` and `minSdk` are separate decisions and
  need not move — compileSdk only allows newer APIs to be referenced.
- `core-ktx` 1.13.1 → 1.18.0, `activity-compose` 1.9.3 → 1.13.0. **Not** core-ktx 1.19.0, which
  Dependabot proposed: it requires `compileSdk 37`, and no Android SDK platform 37 exists yet.
- CI actions: `actions/checkout` v4 → v7, `actions/setup-java` v4 → v5,
  `actions/upload-artifact` v4 → v7, `gradle/actions` v4 → v6.

### Added

The remaining component proposals from the extension review, landed as one batch. Every one of
them is additive — no existing signature changed — and each exists because it is the *wrong*
version of itself that apps keep building.

- **Status vocabulary.** `KansoStatus` (`Success` / `Warning` / `Error` / `Info` / `Neutral`),
  `KansoStatusBadge` and `KansoInfoBanner`, all resolved against `Kanso.extendedColors`. The
  badge's `text` is required rather than optional: a badge that says only "green" is unreadable
  to anyone who cannot separate it from the amber one, and to a screen reader regardless.
  `KansoDivider` joins them with an `inset` that lines up with a row's text.
- **Settings rows** — `KansoSwitchRow`, `KansoCheckboxRow`, `KansoRadioRow`. The row carries
  `toggleable`/`selectable` with a `Role`, and the control is passed `onCheckedChange = null` so
  it is not a second focus target. This is the fix for `KansoListItem(trailing = { Switch(…) })`,
  which yields two accessibility stops that read as unrelated and a row whose tap does nothing.
- **Overlays** — `KansoAlertDialog` (with `destructive = true` putting the confirm button in the
  error role) and `KansoBottomSheet`.
- **`KansoSelectField`** — a value picked from a list rather than typed, generic in the option
  type so `onSelect` returns your enum instead of an index into a list of strings.
- **`KansoErrorState`** — the failure counterpart to `KansoEmptyState`, with retry present by
  construction rather than by each screen remembering to pass an `action`.
- **`KansoSkeleton`** — the shape of a list drawn as shimmering bars while it loads. Deliberately
  one component and not a `Modifier.kansoSkeleton()`; the whole block is announced as a single
  "loading content" node.
- **`KansoRefreshBox`** — pull-to-refresh with the indicator in the brand colour rather than
  Material's theme-blind grey.
- **`KansoScaffold(largeTopBar = true)`** and **`KansoBackButton`** — the tall collapsing header
  for a screen that starts a hierarchy, and a back arrow that mirrors in RTL and carries a
  translatable content description.
- **`Kanso.motion`** — duration and easing tokens named by intent (`quick`, `standard`,
  `deliberate`, `shimmer`), settable via `KansoTheme(motion = …)`. Added with `KansoSkeleton`,
  its first call site; a token with no call site is a guess.

  This brings `androidx.compose.animation:animation-core` onto the `api` configuration, because
  `Easing` appears in `KansoMotion`'s public signature. It was already on the runtime classpath
  transitively via material3, so no consumer gains a new artifact.

- `KansoScaffold` shows the app's version after the title, on the same line, as part of the
  standard top app bar — "CertWatch v5.33.11". The value is a variable, not a literal: the new
  `kansoAppVersionLabel()` reads the installed package's `versionName` and prefixes it with `v`
  (never doubling an existing one), so the header always states the build actually running and
  there is nothing for a release checklist to forget. Pass `version` to show a different string
  verbatim (a build variant, a git hash) or an empty string to omit it; `titleContent` still
  replaces the whole title line, version included.

## 0.2.0 — 2026-07-25 — `02b34d9`

The release that took kanso from "a private design system for four apps" to something a stranger
could adopt. Several changes are breaking; all of them are cheap to migrate and were landed
together on purpose, while there is no published artifact to protect.

### Fixed

- **The colour engine was emitting sub-AA UI.** Tone was mapped onto HSL lightness, which is not
  perceptual, so a fixed tone landed at very different real luminance depending on hue. Across
  the full hue circle, 157/360 seeds failed WCAG AA for `onPrimary` on `primary` and 159/360 for
  `primary` as text on `surface` — including kanso's own default teal, at 3.60:1. Every Filled
  button label in two shipping apps was below the threshold.

  Tone is now CIE L\*, with colours built in CIELCh(ab) and gamut-mapped by reducing chroma only,
  so L\* — and therefore contrast — survives. The guarantee is structural rather than tuned:
  `onPrimary` on `primary` now measures 6.42–6.46 at *every* hue.
- An achromatic seed (grey, black, white) no longer produces a maroon scheme.
- Seed chroma is preserved instead of discarded, so a muted navy and a vivid blue differ again.
- `KansoButton`'s loading spinner took `colorScheme.primary` instead of the button's content
  colour, and the button grew ~26dp the instant work started. The label now stays laid out at
  `alpha 0` with the spinner over it, so width is stable, and a `stateDescription` announces the
  loading state that "disabled" alone did not convey.
- `KansoTextField` errors announced a generic "invalid input" and never the reason.
- `KansoSectionHeader` truncated to "Encrypted tra…" at large font scales; it now wraps.
- `KansoListItem` and `KansoStatusRow` read as two disconnected screen-reader stops; they are now
  one merged node. A clickable row is held to the 48dp touch target.
- The demo rendered its Control PIN in cleartext — `KeyboardType.NumberPassword` picks the
  keyboard but Compose still draws the glyphs.

### Added

- `KansoPasswordField` — masked, with a reveal toggle whose content description follows its state.
- `KansoButtonStyle.Destructive`, resolved against the error role inside the component.
- `KansoCard`: `onClick` (routed to Material 3's clickable overload, so the ripple is clipped to
  the corners), `contentPadding`, `titleColor`, `titleTrailing`.
- `KansoScaffold`: `bottomBar`, `centeredTitle`, `titleContent`, `containerColor`,
  `contentWindowInsets`.
- `KansoListItem`: a `leading` slot for an avatar or checkbox.
- `KansoContentContainer` and a `contentMaxWidth` token — caps line length on tablets and resized
  windows, where body text was running to ~160 characters a line.
- Extended semantic colours: `success` / `warning` / `info` with `on-`, `-Container` and
  `on-Container` roles, via `Kanso.extendedColors`.
- `KansoTheme` takes `colorScheme`, `extendedColors`, `typography`, `shapes`, `spacing` and
  `elevation`; `kansoTypography(family)` maps a font family over the M3 scale without rescaling
  it. `Kanso.brand` reads back the brand in force.
- `KansoTextField`: `enabled`, `readOnly`, `placeholder`, `leadingIcon`, `trailingIcon`,
  `minLines`, `imeAction`, `keyboardActions`, `visualTransformation`.
- `KansoEmptyState` / `KansoLoadingState`: opt-in `scrollState`.
- Tests where there were none: 33 of them — a WCAG sweep over all 360 hues in both schemes,
  typography, component behaviour, and 8 screenshot goldens.
- CI on every push and pull request: tests, goldens, build, minified demo build, lint.
- `CONTRIBUTING.md` and a pull-request template.

### Changed

- **Breaking — `api` dependencies.** Five of ten were never referenced by the library:
  `material-icons-extended`, `navigation-compose`, `activity-compose`,
  `material3-window-size-class` and `ui-tooling-preview` are gone; `core-ktx` dropped to
  `implementation`. `runtime`, `ui-text` and `ui-unit` were added — they were always in the
  public API and were arriving only transitively.

  *Migration:* an app relying on kanso to supply `activity-compose`,
  `material-icons-extended` or `navigation-compose` declares it itself. One line each; no version
  needed, the BOM still comes through.
- **Breaking — `KansoBrands` removed.** It held the seeds of four private apps in the public API
  of a library meant to be general.

  *Migration:* `KansoBrands.Lms` → `KansoBrand("LMSA", Color(0xFF006A60))`, defined in your app.
  `KansoDefaultBrand` stays.
- **Breaking — explicit API mode.** Every exported symbol now carries a deliberate visibility and
  return type. No symbol that was public became non-public, so this should be source-compatible.
- Toolchain: AGP 8.7.3 → 9.3.1, Kotlin 2.0.21 → 2.4.10, Gradle 8.9 → 9.6.1, compose-bom
  2026.06.00 → 2026.06.01. AGP 9 has built-in Kotlin support, so the
  `org.jetbrains.kotlin.android` plugin is gone.
- Lint is re-enabled, reversing 0.1.1. `checkReleaseBuilds = false` was a workaround for an AGP
  8.7 lint crash, never a policy; the toolchain upgrade resolved it.
- `dynamicColor` still defaults to `true` and still wins over the seed. Flipping it was
  considered and declined — the user's system-wide colour preference outranks the app's brand.
  It is now documented rather than surprising; pass `dynamicColor = false` if your brand must
  hold.

## 0.1.3 — 2026-07-01 — `1656fd6` (tagged `v0.1.3`)

### Changed

- `minSdk` 26 → 24 on `:kanso`, so the design system never constrains a consumer. Compose
  Material 3 supports 21+, and `dynamicColor` was already guarded at runtime.

## 0.1.2 — 2026-07-01 — `6ccba05` (tagged `v0.1.2`)

### Fixed

- `KansoStatusRow` starved its label when the value was long. Both sides are weighted, so a long
  value wraps within its own half instead of squeezing the label to one character per line.

## 0.1.1 — 2026-07-01 — `edf7602` (tagged `v0.1.1`)

### Changed

- Release-blocking lint disabled — AGP 8.7's bundled lint crashed analysing Compose sources under
  Kotlin 2.0.x. A workaround, not a policy; reversed in 0.2.0.

## 0.1.0 — 2026-07-01 — `76d93ea` (tagged `v0.1.0`)

Initial release: `KansoTheme`, the spacing and elevation tokens, and nine components, consumed as
a git submodule by four apps.

Compare any two sections with the SHAs in their headings:
`https://github.com/ayanami770/kanso/compare/<older>...<newer>`. The `v0.2.0` link that used to
sit here pointed at a tag that was never pushed.

[keepachangelog]: https://keepachangelog.com/en/1.1.0/
