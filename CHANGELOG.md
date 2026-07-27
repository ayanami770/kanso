# Changelog

Notable changes to kanso, newest first. Format follows [Keep a Changelog][keepachangelog].

kanso is consumed as a git submodule, so "upgrading" means moving the submodule pointer to a
newer tag. This file is what tells you what you are moving onto.

**Versioning.** [SemVer][semver], with one clause most design systems omit and most need:
**visual output is not covered.** Retuning a colour curve or a component's internal padding is a
PATCH even though pixels change — pin an exact tag if you screenshot-test against kanso. A
changed public signature, a removed parameter, or a change to which artifacts arrive via `api`
is a MAJOR; the `api` surface is a promise, not an implementation detail.

kanso is not published to a Maven repository, and that is deliberate — see
[docs/foundation-roadmap.md](docs/foundation-roadmap.md) § 1.5.

## [Unreleased]

### Added

- `KansoScaffold` shows the app's version after the title, on the same line, as part of the
  standard top app bar. The value is a variable, not a literal: the new `kansoAppVersionName()`
  reads the installed package's `versionName`, so the header always states the build actually
  running and there is nothing for a release checklist to forget. Pass `version` to show a
  different string (a build variant, a git hash) or an empty string to omit it; `titleContent`
  still replaces the whole title line, version included.

## [0.2.0] — 2026-07-25

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

## [0.1.3] — 2026-07-01

### Changed

- `minSdk` 26 → 24 on `:kanso`, so the design system never constrains a consumer. Compose
  Material 3 supports 21+, and `dynamicColor` was already guarded at runtime.

## [0.1.2] — 2026-07-01

### Fixed

- `KansoStatusRow` starved its label when the value was long. Both sides are weighted, so a long
  value wraps within its own half instead of squeezing the label to one character per line.

## [0.1.1] — 2026-07-01

### Changed

- Release-blocking lint disabled — AGP 8.7's bundled lint crashed analysing Compose sources under
  Kotlin 2.0.x. A workaround, not a policy; reversed in 0.2.0.

## [0.1.0] — 2026-07-01

Initial release: `KansoTheme`, the spacing and elevation tokens, and nine components, consumed as
a git submodule by four apps.

[keepachangelog]: https://keepachangelog.com/en/1.1.0/
[semver]: https://semver.org/spec/v2.0.0.html
[Unreleased]: https://github.com/ayanami770/kanso/compare/v0.2.0...HEAD
[0.2.0]: https://github.com/ayanami770/kanso/compare/v0.1.3...v0.2.0
[0.1.3]: https://github.com/ayanami770/kanso/compare/v0.1.2...v0.1.3
[0.1.2]: https://github.com/ayanami770/kanso/compare/v0.1.1...v0.1.2
[0.1.1]: https://github.com/ayanami770/kanso/compare/v0.1.0...v0.1.1
[0.1.0]: https://github.com/ayanami770/kanso/releases/tag/v0.1.0
