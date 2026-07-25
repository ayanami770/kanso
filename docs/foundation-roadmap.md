# From private fleet system to general-purpose design foundation

A review of what kanso needs in order to be adopted by apps outside the ayanami770 fleet —
including by people who did not write it.

This document is a **proposal**, not a changelog.

> **Status.** Landed: 1.1 (colour engine + contrast tests), 1.3, 1.4, 1.6, 2.5, 2.6, 2.7, and
> 2.1, 2.2, 2.3, 2.4, 2.8, 2.9. Considered and **declined**: 1.2. Items are marked below; their
> prose is left in the original tense on purpose — it records why each change was or was not
> made. Everything unmarked is still open.

---

## Where kanso stands today

kanso is a well-built private design system that is about three problems away from being a
general foundation. The structure is right and should not be redesigned: one seed per brand,
spec-accurate MD3 role→tone mappings, `Kanso.colors` delegating straight to
`MaterialTheme.colorScheme` so every stock M3 and third-party component themes for free, tokens
as `@Immutable` data classes behind `staticCompositionLocalOf`, zero hardcoded `Color(0x…)` in
any component, and correct library packaging.

Three things separate it from general-purpose use.

**1. The color math is wrong in a way that ships inaccessible UI today.** `tone()` maps the MD3
tone scale onto HSL lightness (`Color.kt:34-35`). HSL lightness is not perceptual, so a fixed
tone lands at wildly different real luminance depending on hue — which is precisely the problem
HCT was designed to solve. Swept across all 360 seed hues:

| pairing | light scheme | dark scheme |
|---|---|---|
| `onPrimary` on `primary` (every Filled button) | **157/360 hues fail WCAG AA** (hues 37–193) | 0/360 fail |
| `primary` as text on `surface` (every card title, section header, list icon) | **159/360 hues fail** (hues 36–194) | 0/360 fail |

This is not theoretical. Three of the four shipped brands are sub-AA, including kanso's own
default:

| brand | seed | light `primary`/`onPrimary` | `primary` as text |
|---|---|---|---|
| Kanso / Lms | `#006A60` | **3.60 — fails** | **3.52 — fails** |
| Medcal | `#386A20` | **3.66 — fails** | **3.58 — fails** |
| Semicon | `#8A4F00` | 4.66 — marginal pass | 4.56 — marginal pass |
| CertWatch | `#3F5AA6` | 7.68 — pass | 7.51 — pass |

The entire gap is in the **light** builder; the dark scheme is clean at every hue. That halves
the blast radius. Two related defects fall out of the same function: an achromatic seed (grey,
black, white) has no hue, so `colorToHSV` returns 0 and the scheme comes out **maroon**
(`primary` = `rgb(149, 55, 55)`); and seed chroma is discarded entirely, so a muted navy and a
neon blue produce identical schemes.

**2. There is no distribution identity.** No Maven coordinates, no `version`, no git tags, no CI.
A third party cannot write a dependency line at all. Separately, five of the ten `api`
dependencies are provably unused by the library yet force-fed to every consumer — including a
Google-deprecated `material-icons-extended` (metadata stops at 1.7.8, Feb 2025) and an
opinionated navigation library that dictates an architecture choice to apps using Voyager,
Circuit or plain state hoisting.

**3. `KansoTheme` exposes exactly one knob.** No type family, spacing grid, corner scale or
palette override.

### One correction worth stating plainly

It is tempting to conclude a third party "must fork kanso to re-theme it." **That is false.**
Nesting `MaterialTheme(typography = …, shapes = …, colorScheme = …)` inside `KansoTheme` and
providing `LocalKansoSpacing` / `LocalKansoElevation` (both already public) re-themes every token
from outside with zero library changes. So most of the theming work below is **ergonomics and
discoverability, not capability** — which is why Stage 1 is short.

---

## The one thing to do first

**Replace the HSL tone approximation with tone-accurate, pure-Kotlin color math, and lock it with
a JVM contrast sweep.**

It is the only item on this list that is a correctness bug rather than a taste call or an
ergonomics gap, and it directly falsifies the one promise the product is built on: *any seed in,
coherent accessible scheme out*.

It is also the ordering constraint for nearly everything else:

- Flipping the `dynamicColor` default to `false` **before** this lands would *increase* the
  number of users routed onto the broken seed path.
- The extended semantic colors and any contrast-level work key off the same rewrite.
- Deleting the `android.graphics.Color.colorToHSV` call is what makes the library JVM-testable at
  all — so this is simultaneously the first test in a repo that has none.
- Screenshot goldens recorded before it would all need re-recording immediately after.

---

## Stage 1 — Foundation blockers

*Make the library correct, honest, and obtainable. Nothing here is about features; it is about a
stranger being able to get kanso, trust what it renders, and read documentation that matches the
code.*

### 1.1 Tone-accurate color math + a JVM contrast test — `L`, breaking — ✅ done

Delete `hueOf()`'s `android.graphics.Color.colorToHSV` call (`Color.kt:28-32`) — it is the only
Android framework call in the theme package and the sole reason no JVM test can cover the engine.

Replace the derivation with real HCT via `com.materialkolor:material-color-utilities` (pure
Kotlin, `kotlin-stdlib` + poko only, no Compose graph) as `implementation`, or an in-house per-hue
L\*-solve. **Do not** take `com.materialkolor:material-kolor` — its Android variant drags Compose
1.11-beta and material3 1.5.0-alpha into a build pinned to a single BOM.

Keep the `kansoLightColorScheme(seed)` / `kansoDarkColorScheme(seed)` signatures byte-identical,
and keep the hardcoded error ramp. Fold in the achromatic guard and seed-chroma preservation.
Add `surfaceBright` / `surfaceDim` while in there. Do **not** touch `scrim` — material3's
baseline is already opaque black in both schemes.

Then add `kanso/src/test/` with a WCAG sweep over all hues × both schemes × every `on`-pair, plus
a regression case per shipped brand. **Land the test first and let it fail** — it is the
specification.

> Do **not** set `unitTests.isReturnDefaultValues = true` as a shortcut. The stubbed void
> `colorToHSV` leaves the out-array at zero, so every seed collapses to hue 0 — which happens to
> pass at 7.29:1. The suite would go green against a red scheme.

**As implemented:** the in-house route, so kanso stays dependency-free. MD3 tone is mapped to
CIE L\* and colours are built in CIELCh(ab), then gamut-mapped into sRGB by reducing *chroma
only*. Because L\* is a function of luminance alone and the mapping never moves it, contrast
between two tones is hue-independent **by construction** rather than by tuning: `onPrimary` on
`primary` now measures 6.42–6.46 across all 360 hues, against 3.17–9.79 before. Achromatic seeds
take a neutral palette, and seed chroma is clamped into a 24–56 band instead of normalised away,
so a muted navy and a vivid blue still differ by ~32 points of chroma.

### 1.2 Flip `dynamicColor` to default `false` — `S`, breaking — ❌ not adopted

On every Android 12+ device the wallpaper branch wins before the seed is ever read
(`KansoTheme.kt:35, :39-44`), so `KansoTheme(brand = MyBrand)` — the exact call the README tells
adopters to write — silently discards their brand. A brand-seeded design system whose default
deletes the brand will be judged broken. The demo already opts out
(`demo/…/MainActivity.kt:92`); the only in-repo consumer disagrees with the library default.

One token, plus one README sentence. Reject a `KansoColorSource` tri-state enum and wallpaper
harmonization — speculative architecture for a ~950-line library, and harmonization forces in a
dependency purely for `Blend.harmonize`.

**Decision: not adopted.** The default stays `true` — the user's system-wide colour preference
outranks the app's brand, and flipping it would change the appearance of four shipping apps on
every Android 12+ device. This was a product call, not a defect, so the fix is documentation
rather than behaviour: the surprise is now stated plainly in the `KansoTheme` KDoc and in a
README admonition, with `dynamicColor = false` given as the opt-out for brands that must hold.
Revisit only if a consuming app reports the wallpaper scheme as a real problem.

*Depended on 1.1.*

### 1.3 Fix the `api` dependency surface — `M`, breaking — ✅ done

Grep over `kanso/src` finds **zero** references to `Icons.`, `WindowSizeClass`, `Preview`,
`androidx.navigation` or `androidx.activity`.

- **Drop from `api`:** `material-icons-extended`, `navigation-compose`, `activity-compose`,
  `material3-window-size-class`, `ui-tooling-preview`.
- **Demote to `implementation`:** `core-ktx` (only `WindowCompat`, `KansoTheme.kt:50`).
- **Keep as `api`:** `material3`, `ui`, `ui-graphics`, `foundation` — the last not for
  `KeyboardOptions` (constructed internally, in no signature) but because `PaddingValues` and
  `RowScope` are in `KansoScaffold`'s public signature.
- **Add as `api`** — currently undeclared but genuinely in the public surface: `ui-text`
  (`KeyboardType` on `KansoTextField`) and `runtime` (the public token `ProvidableCompositionLocal`s).
- **Keep** `api(platform(compose-bom))`. For a design system, version alignment is worth more
  than isolation, and `platform` publishes a floor, not a ceiling.

Then fix `demo/build.gradle.kts` to declare what it actually uses. If `:demo` compiles, the split
is right. Getting this wrong in 1.0.0 makes the fix a major-version break.

### 1.4 Add CI — `S` — ✅ done

There is no `.github/`, so nothing proves the library even compiles. For a submodule-consumed
library a broken `main` breaks all four apps simultaneously at their next `git submodule update`.

One `ubuntu-latest` job: checkout, `gradle/actions/wrapper-validation`, JDK 17, `setup-gradle`,
then `:kanso:assembleRelease :demo:assembleDebug` and `:kanso:testDebugUnitTest`. Start there
only — do not add `apiCheck` or `verifyRoborazzi` lines for tasks that do not exist yet, which is
exactly how a workflow ends up red from day one and ignored. Add `.github/dependabot.yml` in the
same PR.

> Nothing in this review established that the build currently passes — it could not be verified
> in a sandbox without a Gradle cache. The first job of this workflow may be discovering that it
> does not.

### 1.5 Publish to Maven Central with a real version, tags and a CHANGELOG — `M`

Add the vanniktech `maven-publish` plugin (current 0.37.x) with `publishToMavenCentral()` and
`signAllPublications()`. Do **not** also add an `android { publishing { singleVariant } }` block —
the plugin configures the variant itself and will conflict. Use `io.github.<user>` for the groupId
if the vanity domain is not owned; the Maven groupId need not match the Android `namespace`, so
every `dev.ayanami.kanso.*` import stays put. Drive publishing off tag push.

Add `CHANGELOG.md` (Keep a Changelog) and a short SemVer policy — including the clause most design
systems omit and most need: **visual output is not covered by SemVer.** Retuning a color curve or
a component's internal padding is a PATCH even though pixels change, so pin an exact version if
you screenshot-test against kanso.

The submodule and the artifact coexist; migrate the fleet one app at a time.

*Depends on 1.3, 1.4.*

### 1.6 Rewrite the README for strangers — `M`, breaking — ✅ done

The README is written for one person who already owns four apps. It also states two things the
code does not do, and documented rules the code violates are worse than no rules, because
contributors copy the code.

- Lead with what kanso is, and a light/dark screenshot strip from the demo. A design system README
  without a screenshot is the single most persuasive missing section.
- Add the Maven coordinate; a "theme it to your brand" section stating plainly that `dynamicColor`
  gates the seed; a compatibility block (minSdk 24, compileSdk 35, Compose BOM, Kotlin) noting
  that `api`-exposed Compose sets the consumer's floor.
- **Fix `README.md:22`** — "every component reads from the tokens, never hard-coded values" is
  false (`Buttons.kt:46-47`, `ListItems.kt:42`, `States.kt:41` hardcode dp). Soften to
  spacing/color/type/shape; the offenders are Material icon-size constants.
- **Fix `README.md:24`** — the Compose BOM does manage icon versions but does not manage
  `androidx.navigation`, which is hand-pinned.
- Move the four fleet seeds out of `KansoBrands` (`KansoTheme.kt:88-95`) into `:demo` as a private
  list; relabel the README table "example brands". Keep `KansoDefaultBrand`. No
  `KansoSampleBrands` object — re-adding the same four constants under a new name is renaming, not
  de-scoping — and no deprecation cycle, since there is no published binary to protect.

*Depends on 1.2.*

---

## Stage 2 — The substance

*A theme that accepts more than one knob, components that can express a login form and a bottom
nav, and the accessibility fixes a design system exists to guarantee once rather than per-app.*

### 2.1 Parameterize `KansoTheme` + a typography builder — `M` — ✅ done

Add defaulted params before `content`: `typography`, `shapes`, `spacing`, `elevation`, and one
`colorScheme: ColorScheme? = null` that short-circuits derivation. Document
`kansoLightColorScheme(seed).copy(primary = …)` in the KDoc as the partial-override path — it
needs no library change at all.

Ship `kansoTypography(bodyFamily, displayFamily = bodyFamily)` mapping a family over the M3
defaults so callers inherit every metric. Enumerate against the *pinned* `Typography`'s real field
set — the BOM in use carries M3-expressive `*Emphasized` styles that a hand-written copy list will
silently leave on the default family — and comment the BOM-bump obligation.

No `KansoDefaults` object (the data classes' own per-field defaults already *are* the named
defaults) and no `KansoThemeConfig`.

Also add `LocalKansoBrand` + `Kanso.brand`. `brand` is consumed and discarded today, so nothing
downstream can read the active brand's name or raw seed for a splash screen, chart series or debug
badge — and `Kanso.colors.primary` is the derived tone, not the seed.

**As implemented:** all of the above. The `*Emphasized` warning turned out to be sharper than
written — material3 1.4's `Typography` has 30 `TextStyle` fields, but the fifteen `*Emphasized`
accessors and the 30-argument `copy` are `internal` to material3, so they can be neither read nor
set from outside it. A brand family therefore cannot reach them at all; this is a material3
limitation, not a gap in the builder, and it is documented on `kansoTypography` rather than
papered over. kanso's own components use no emphasized style, so only a consumer reaching for a
stock M3 Expressive component is affected. `TypographyTest` pins that the builder changes the
family and nothing else.

### 2.2 Widen `KansoTextField`, add `KansoPasswordField` — `M` — ✅ done

The library's own demo renders a "Control PIN" in cleartext (`MainActivity.kt:256-264`) because
there is no `visualTransformation` — `KeyboardType.NumberPassword` selects the numeric-password
keyboard but Compose draws the glyphs itself. Any consumer with a login screen is blocked on day
one, and `enabled` / `readOnly` / `trailingIcon` are all missing while `KansoButton` has `enabled`.

Add `enabled`, `readOnly`, `placeholder`, `leadingIcon`, `trailingIcon`, `visualTransformation`,
`keyboardActions`, `minLines`, and a single `keyboardOptions` (dropping the `keyboardType` scalar —
a default that forward-references a later parameter will not compile). Then ship
`KansoPasswordField` owning the reveal toggle and `PasswordVisualTransformation`. That single
component is what a design system should be selling.

Skip a `fillWidth: Boolean` escape hatch: `Modifier.width(96.dp)` already yields 96dp, because the
outer constraint is fixed before the inner `fillMaxWidth()` resolves.

### 2.3 Add the missing `KansoScaffold` slots, migrate the demo onto it — `M` — ✅ done

`KansoScaffold` has no `bottomBar`, so the gallery app cannot use it and hand-rolls a raw M3
`Scaffold` + `TopAppBar` + `NavigationBar` (`MainActivity.kt:107, :121-160`) — paying its own
`@OptIn` tax for the privilege. If the showcase can't adopt the scaffold, no third-party app with
bottom navigation can.

Four defaulted params: `bottomBar`, `centeredTitle`, `titleContent`, `containerColor`, plus a
`contentWindowInsets` passthrough so a consumer can `.union(WindowInsets.ime)` themselves. Do
**not** build a `KansoTopBarStyle` × `KansoTopBarScroll` matrix — sixteen combinations nobody
asked for. Do **not** hardcode `.imePadding()` on the scaffold root; it compresses the top bar on
every keyboard show and contradicts the insets parameter.

Then migrate the demo shell onto it — that migration *is* the regression test for whether the
scaffold is actually general.

**As implemented:** all four slots plus the insets passthrough, and the demo migrated. The
migration paid off immediately: the demo's `@OptIn(ExperimentalMaterial3Api::class)` and five
imports — `Scaffold`, `TopAppBar`, `TopAppBarDefaults`, `nestedScroll`, `ExperimentalMaterial3Api`
— all became dead. The gallery now declares no experimental opt-in at all, which is the
observable proof that `KansoScaffold` absorbs it rather than leaking it.

`ScaffoldDefaults` turned out to expose only `contentWindowInsets`, not a `containerColor`, so
the `Color.Unspecified` default falls back to `Kanso.colors.background` — Scaffold's own default —
rather than hard-coding a colour a consumer might have re-themed.

### 2.4 Give components the two seams they actually lack — `M` — ✅ done

A destructive red button and a full-bleed card are the two things genuinely inexpressible today.

- `KansoButtonStyle.Destructive` resolving against `colorScheme.error` / `onError` **inside**
  `Buttons.kt` — the design system owning the decision, strictly better than exposing a raw
  `colors` param.
- `contentPadding` and `titleColor` on `KansoCard`.
- Route `onClick` to M3's clickable `Card` overload. A consumer's `Modifier.clickable` today lands
  outside `Card`'s internal `Surface`, so the ripple is not clipped to the 12dp corners and renders
  as a rectangle bleeding past the card edge.
- `leading: @Composable (() -> Unit)?` on `KansoListItem` (with `icon: ImageVector?` delegating) —
  today an avatar or checkbox leading a row is blocked by the `ImageVector?` type.
- `titleTrailing` on `KansoCard` / `KansoSectionHeader` for the "See all" / overflow pattern.

Explicitly do **not** add `colors` / `shape` / `contentPadding` / `interactionSource` to all nine
composables. That converts kanso into a pass-through wrapper and re-creates the divergence it
exists to prevent.

### 2.5 Fix the `KansoButton` loading state — `S` — ✅ done

Three real defects in eleven lines of the most-used component (`Buttons.kt:43-53`):

- The `CircularProgressIndicator` passes no `color`, so it falls back to
  `ProgressIndicatorDefaults.circularColor` (= `colorScheme.primary`) and ignores the
  `LocalContentColor` the Button provides — full-saturation brand primary next to a
  disabled-emphasis label. Pass `color = LocalContentColor.current`.
- The content swap grows the button ~26dp the instant work starts. Replace with a `Box` overlay:
  keep the label Row laid out at `alpha(0f)` and center the spinner over it. (The jump only fires
  in the no-icon case; the demo masks it with `fillMaxWidth()`.)
- TalkBack announces only "disabled". Add `Modifier.semantics { stateDescription = … }`, sourced
  from a real `strings.xml` in the library rather than a hardcoded English Kotlin default — kanso
  has no string resources at all, which is its own small gap.

While in the file, delete the dead `Row(horizontalArrangement = Arrangement.Center)` configuration
at `Buttons.kt:44`, which is given no modifier and does nothing.

### 2.6 Semantics and touch-target pass — `S` — ✅ done

Accessibility here is accidental — inherited from Material 3 underneath — and stops exactly where
the library writes its own layout. There are zero occurrences of `semantics`, `Role`, `heightIn`
or `minimumInteractive` in `kanso/src`.

1. `.semantics(mergeDescendants = true) {}` on `KansoStatusRow` and the **non-clickable**
   `KansoListItem` branch. A card of four status rows is currently eight disconnected TalkBack
   stops, destroying the label/value association that is the component's entire purpose. The
   clickable branch already merges via `Modifier.clickable`; leave it alone.
2. `heightIn(min = 48.dp)` on the clickable `KansoListItem` path — a one-line row is 44.4dp at the
   "Small" font setting and exactly 48.0dp at default, i.e. passing with zero margin. Do **not**
   adopt M3's 56/72dp list heights: that is a visual redesign of every consuming app's lists
   smuggled in under an accessibility heading.
3. `semantics { error(errorText) }` on `KansoTextField`, plus deriving
   `isError = isError || errorText != null`. M3 announces its own generic "Invalid input" string,
   never the real message — and `isError = true, errorText = null` is currently a legal call whose
   only error signal is a red outline. Keep it non-breaking; a `@Deprecated` overload is
   ambiguous-call bait when every parameter has a default.
4. `semantics { heading() }` on `KansoSectionHeader` and `KansoCard` titles. Heading navigation is
   how a screen-reader user skims exactly the long settings screens this library targets.

Also delete the dead `.padding(end = Kanso.spacing.none)` at `ListItems.kt:42` — a no-op only
because the token is 0.dp, and it would shrink the icon if anyone "fixed" it.

**As implemented:** items 1, 2 and 4 as written. Item 3 landed only as the `semantics { error() }`
half — the `isError = isError || errorText != null` derivation was **dropped**, because the demo
(and so, presumably, the apps) passes a constant `errorText` and gates on `isError` alone.
Deriving the flag would have pinned that field permanently in its error state. `isError` stays
the sole gate; the message is what reaches the screen reader.

### 2.7 Stop the state components clipping at large font scale — `S` — ✅ done

`KansoEmptyState` and `KansoLoadingState` are `fillMaxSize()` + `Arrangement.Center` with no
scroll anywhere in the library. The CTA button — the user's only escape from an empty state — is
the first thing lost at 200% font scale or in landscape.

> The obvious fix is wrong. Adding `.verticalScroll()` inside `States.kt` **crashes the library's
> own demo**: `MainActivity.kt:171-175` already wraps these in a scrolling Column, `fillMaxSize`
> does not degrade to wrap-content under an infinite max-height constraint (`FillNode` only pins a
> bounded axis), and `checkScrollableContainerConstraints` throws on a vertically scrollable
> measured with infinite height.

Either make the caller own scrolling and document it — which already works today, as the demo
proves — or add an opt-in `scrollState: ScrollState? = null` and update the demo in the same commit
to stop double-wrapping.

Separately, delete `maxLines = 1, overflow = Ellipsis` from `KansoSectionHeader`
(`Surfaces.kt:66-67`). It is the only truncation in the library, it turns a section label into
"Encrypted tra…" at 200% scale, and removing it is pixel-identical for every existing single-line
call site.

### 2.8 Extended semantic colors: success / warning / info — `M` — ✅ done

Every consuming app needs pass/fail, in-range/out-of-range, valid/expiring **by name**, and today
each invents its own `Color(0xFF2E7D32)` outside the design system — precisely the token drift a
design system exists to prevent.

An `@Immutable KansoExtendedColors` with `success` / `warning` / `info` plus `on*` / `*Container` /
`on*Container`, as **fixed literal ARGB** for light and dark — the same shape and the same
justification as the existing error ramp.

Provide it via one `staticCompositionLocalOf` in `KansoTheme` **after** the scheme is resolved, so
it is present on the dynamic-color path too. Anything computed inside the seed builders vanishes
under Material You — a device-dependent failure that will not show up on the developer's phone.

*Depends on 2.1.*

### 2.9 `@Preview` multipreviews across every component — `S` — ✅ done

The library pays for `ui-tooling-preview` on the classpath and uses none of it. **Every layout
defect in this roadmap** — the 44dp row, the clipped empty state, the truncated header, the
stretched card — is a render-at-a-non-default-configuration bug that is invisible in normal
development and instantly visible in a preview. That is exactly why they are all still present.

One `@KansoPreviews` multipreview combining `@PreviewLightDark`, `@PreviewFontScale` and
`@PreviewScreenSizes`, plus `@Preview(locale = "ar")` for RTL, stamped on every component with
`KansoTheme(dynamicColor = false)`. Mark them `private`. Zero dependency changes; `KansoTheme` is
already preview-safe via its `!view.isInEditMode` guard.

Highest defects-caught-per-hour item in the roadmap.

**As implemented:** four explicit `@Preview` configurations rather than the three stock
multipreviews. `@PreviewScreenSizes` was dropped — kanso has no adaptive behaviour yet (3.5), so
a component rendered at tablet width shows nothing a phone-width one does not, and it would have
tripled the matrix for no signal. `@PreviewFontScale`'s six steps collapsed to the one that
actually breaks layouts, 200%. Result: 10 preview functions × 4 configurations = 40 renders
covering all nine components.

Two corrections to the plan. It is **not** "zero dependency changes": 1.3 had already removed
`ui-tooling-preview` from `api`, correctly, since nothing used it — it comes back here as
`implementation`, which is the right classification for kanso's own tooling. And the previews
needed an icon, but material3 no longer brings `material-icons-core` transitively; rather than
add an icon set to a library that deliberately ships none, `Previews.kt` draws one `ImageVector`
inline.

*Depended on 1.1.*

---

## Stage 3 — Polish and scale

*None of this blocks adoption; all of it is what keeps the library from decaying once other people
depend on it.*

### 3.1 Bring the toolchain current and re-enable lint — `M`, breaking

Lint is off for release builds (`kanso/build.gradle.kts:21-27`) — and for a library, lint is the
consumer-facing quality gate that catches unguarded API-level calls against the advertised minSdk
24. Every consumer inherits those defects and none can lint kanso's compiled code themselves.

Move off AGP 8.7.3 / Kotlin 2.0.21 / Gradle 8.9 (all Oct 2024), migrating `kotlinOptions {}` to
`kotlin { compilerOptions { jvmTarget } }` — **not** via `jvmToolchain(17)`, which fails here since
only JDK 21 is installed and `settings.gradle.kts` has no toolchain resolver. Then replace
`checkReleaseBuilds = false` with `abortOnError = true`.

Two corrections worth recording: the BOM does **not** force a compileSdk bump (resolved artifacts
declare `minCompileSdk=35` / `minAndroidGradlePluginVersion=8.6.0`, which the current pins
satisfy), so this is housekeeping plus a lint prerequisite, not an emergency; and upgrading AGP in
this repo's root build file does **nothing** for the four consuming apps, which supply their own
AGP and never evaluate kanso's root script.

Hold off on `warningsAsErrors` / `checkDependencies` until CI exists and the real noise level is
known. Add Slack's compose-lints separately — the components already put `modifier` first among
optional params, so adoption should be near-clean.

*Depends on 1.4.*

### 3.2 `explicitApi()` and published Dokka docs — `S`

Everything not marked otherwise is public by Kotlin default, so internal helpers can leak into the
contract by accident, and three accessors on `object Kanso` have inferred return types. Roughly 23
top-level declarations plus a handful of members — an hour of work that forces a deliberate
decision on every symbol.

Add Dokka: the vanniktech plugin auto-detects it for the javadoc jar Maven Central requires, so
without it **the jar ships empty**. The KDoc in this repo is genuinely good and carries design
rationale an adopter needs — and it is invisible to anyone consuming a jar.

Explicitly defer binary-compatibility validation. The kotlinx BCV plugin is in maintenance mode,
its `com.android.library` support is not the drop-in it appears to be, and a committed ABI baseline
with nothing to check it against is ceremony. Revisit with the Kotlin Gradle plugin's built-in
`abiValidation` once there is a published artifact and a second release to compare against.

*Depends on 1.5.*

### 3.3 Version catalog and dependency-update automation — `S`

AGP is duplicated across two lines and Kotlin across two more — and in Kotlin 2.x the Kotlin plugin
and the Compose compiler plugin falling out of lockstep is a hard build failure. This has already
drifted once: commit `5008ce9` is literally "docs: correct minSdk in README toolchain line".

`gradle/libs.versions.toml` where `kotlin-android` and `kotlin-compose` share one `kotlin` ref —
that shared ref is the entire point. Leave `compileSdk` / `minSdk` as literals; they are not
dependency coordinates and routing them through `libs.versions.compileSdk.get().toInt()` is uglier
than what it replaces. Do **not** add a publishable `:version-catalog` module — a second POM and a
second version stream to save consumers one line. The catalog organizes versions; Dependabot is
what actually prevents the next 20-month drift, so land them together.

### 3.4 Deepen CI: scoped screenshot goldens and a minified demo — `M`

A design system's regressions are overwhelmingly visual and none are currently detectable — commit
`6ccba05` ("weight both sides so long values wrap") is exactly the class of bug a golden pins
forever.

Roborazzi, not Paparazzi (which cannot run interaction tests) and not AGP's `screenshotTest` (still
alpha behind an experimental flag, and it renders only `@Preview` functions). Roborazzi shares the
Robolectric runtime, so screenshots, behavior tests and the pure contrast tests all live in
`src/test/` and run in one invocation with no emulator.

Scope deliberately: 4–6 goldens covering the text-layout-sensitive components (`KansoStatusRow`,
`KansoListItem`, `KansoSectionHeader`, `KansoEmptyState`) at font scale 1.0 and 2.0 — not the
22-image component × theme matrix, which is unreviewable in a PR diff and churns on every BOM bump.

Set `isMinifyEnabled = true` on `:demo` release so R8 actually runs over kanso's output — create
the referenced `proguard-rules.pro` first (it does not exist and AGP fails on a missing file) and
drop `isShrinkResources`. `consumer-rules.pro` asserts that a pure-Compose library needs no keep
rules, and that assertion has never been executed.

Add a handful of `createComposeRule()` behavior tests for contracts that live in single
expressions: `KansoButton`'s loading-swallows-clicks, `KansoTextField`'s `supportingText`
precedence, and `KansoScaffold` rendering at all — it is the only component nothing in the repo
exercises.

*Depends on 1.4, 2.9.*

### 3.5 Do the adaptive story properly, or not at all — `M`

Every container calls `fillMaxWidth()` with no maximum, so on a 1280dp window body text runs at
~160 characters per line — the most visible "this was built for a phone" symptom an evaluator hits
in the first thirty seconds on a tablet. And `material3-window-size-class` sits in the dependency
block referenced by nothing.

The cheap 80%: a `KansoContentContainer(maxWidth = …)` plus a `contentMaxWidth: Dp = 640.dp` token.
This needs **zero component edits** — because every component writes `modifier.fillMaxWidth()` with
the caller's modifier *first*, an outer `widthIn(max = …)` narrows the incoming constraint and
`fillMaxWidth` resolves to the smaller width. Give it no built-in horizontal padding, or existing
screens double-pad.

If a rail/drawer is ever needed, wrap Material's `NavigationSuiteScaffold` rather than hand-rolling
the width switch. Do **not** make spacing tokens width-derived: nothing in `:kanso` reads
`spacing.screen` or `spacing.section` today, so an adaptive token no component consumes changes
nothing.

Either delete the unused window-size-class dependency in the same release as real adaptive work, or
keep it — but do not leave it ambiguous.

*Depends on 2.3.*

### 3.6 One `CONTRIBUTING.md` — `S`

Nothing tells an outside contributor what a correct kanso component looks like. The one genuinely
useful design rule in the codebase — Filled for the single primary action, Tonal/Outlined for
secondary — is stranded in a KDoc at `Buttons.kt:30-31` where nobody will find it.

About 30 lines: the token rule (no bare `.dp` for gaps or padding — add to `KansoSpacing`),
`modifier` is the first optional parameter and the component never overrides the caller's sizing,
KDoc says *when* to use the component, add it to the demo, add a CHANGELOG entry. Mirror it as
`.github/pull_request_template.md`.

Do **not** write a separate DESIGN-PRINCIPLES.md, CODEOWNERS or issue templates, and do not write a
checklist whose gates depend on infrastructure that does not exist — that is governance theater for
a one-author repo. Prefer ktlint/spotless and compose-lints for anything a machine can check; the
codebase has a style tell no prose document would have caught (fully-qualified
`androidx.compose.foundation.layout.Spacer(...)` written inline in seven places).

---

## Leave these alone

A short "do not touch" list prevents more wasted work than another feature item.

- **The hardcoded error ramp** (`Color.kt:64-65, :89-90`) is correct, not a shortcut. MD3's own
  `DynamicColor` keeps the error palette fixed independent of seed so "danger" reads as danger
  under every brand — and it is the only role pair in the file that meets AA at every hue. It is
  the argument *for* fixing the derived roles, and the same reasoning is why the new
  success/warning/info roles should also be fixed literals.
- **The role→tone numbers** (`Color.kt:49-63, :74-88`) follow the MD3 spec accurately. The mapping
  table is not the bug — only the tone→pixel function underneath it. Do not touch the table during
  the color rewrite.
- **`Kanso.colors` delegating to `MaterialTheme.colorScheme`** (`KansoTheme.kt:80-81`) rather than
  defining a parallel `KansoColors` class. This is why every stock Material 3 component and every
  third-party Compose library themes correctly under `KansoTheme` with no adapter. Many in-house
  design systems get this wrong.
- **`KansoBrand(name, seed)`** and **`KansoButtonStyle`** collapsing five M3 button types into one
  `style` param. Both are the right public contracts and neither needs to change when the math or
  the components underneath do.
- **The single-artifact shape.** Do not split into `:kanso-tokens` / `:kanso-theme` /
  `:kanso-components` — four POMs, four version streams and a BOM to hold them together, for a body
  of code smaller than one AndroidX source file.
- **Packaging hygiene:** minSdk 24 with no targetSdk, the empty `<manifest/>`, the declared
  `consumerProguardFiles` wiring, `RepositoriesMode.FAIL_ON_PROJECT_REPOS` with content-filtered
  plugin repos, and applying the Compose compiler as the Kotlin 2.x plugin. All current best
  practice.
- **`api(platform(compose-bom))`.** For a design system specifically, version alignment is worth
  more than isolation, and `platform` publishes a floor a consumer can move past by declaring a
  newer BOM. Only the README sentence describing it needs correcting.
- **`staticCompositionLocalOf` for the tokens** (`Tokens.kt:34, :47`) with working defaults rather
  than `error("No KansoTheme")`. Correct on both counts — it matches what `MaterialTheme` itself
  uses, and the working default keeps `@Preview` functional.
- **RTL.** Every directional modifier uses start/end and `KansoStatusRow` uses `TextAlign.End`
  rather than `Right`, so mirroring works for free. `KansoStatusRow`'s 50/50 weight split
  (commit `6ccba05`) is a deliberate, documented, correct call.
- **`contentDescription = null` on decorative leading icons** (`Buttons.kt:47`, `ListItems.kt:41`,
  `States.kt:40`). Each is accompanied by a visible label that carries the meaning; labelling them
  would make TalkBack read the concept twice.
- **Two things that look like bugs and are not:**
  `TopAppBarDefaults.enterAlwaysScrollBehavior()` without an outer `remember` — the androidx
  function is `@Composable` and remembers internally, so an outer `remember` is pure noise; and
  `Spacer(Modifier.padding(top = …))` at `Surfaces.kt:51` — it does render the intended 12dp
  (rename to `.height()` for readability only).
- **Predictive back must not be added to `KansoScaffold`**, and **`KansoButton` must not get
  `minimumInteractiveComponentSize()`**. Back belongs to the navigation layer and to the M3
  components that already implement it — a `BackHandler` in the scaffold would fight the host app —
  and M3's `Button` already applies the 48dp interactive minimum internally.
- **Token classes nobody needs yet:** `KansoMotion`, `KansoOpacity`, and most of a proposed
  `KansoSizing`. There is zero animation and zero `alpha` usage in the library, and
  `minTouchTarget` / `listItemMinHeight` would be misdescriptions besides. Ship tokens with the
  component that needs them; if the four live icon-size literals bother you, a four-field
  `KansoSizing` is the whole fix.
- **Icon sizes at 18/24/56 dp.** Material's own guidance is that icons are dp-sized and should not
  scale with font. Changing rendering in four apps at non-default font scales to fix an
  optical-balance complaint nobody has reported is churn — and the Stage 2 multipreviews will show
  it if it ever becomes real.

---

## Method and caveats

Six dimensions (color engine, token extensibility, component API, distribution/build, quality
infrastructure, accessibility/adaptive) were each audited independently and then adversarially
reviewed against the source. Claims the review found factually wrong were dropped; over-scoped
proposals were right-sized. Items rejected in review and deliberately **not** carried into this
document include: a `KansoColorSource` tri-state enum, `KansoMotion`/`KansoOpacity` token classes
with no call sites, a 4×4 top-bar style matrix, binary-compatibility validation for an unpublished
artifact, a multi-module split, and a second governance document.

The contrast figures were reproduced independently against `Color.kt` — full 360-hue sweep, WCAG
2.x relative luminance — and match to the digit.

**Not verified:** that the project currently compiles. No Gradle cache was available in the review
environment, so no build, test or lint run was executed. Line numbers are from the tree at commit
`5008ce9`.
