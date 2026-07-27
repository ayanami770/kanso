# kanso

**A Jetpack Compose + Material 3 design system you can theme to your own brand.**

Give it one seed colour and it builds a whole coherent Material 3 scheme — light and dark, every
role, with WCAG AA contrast guaranteed for any hue you hand it. On top of that sits one token set
and a small component library, so a screen you write reads the same as every other screen.

*kanso* (簡素) — the aesthetic of considered simplicity: nothing decorative, everything deliberate.

[![CI](https://github.com/ayanami770/kanso/actions/workflows/ci.yml/badge.svg)](https://github.com/ayanami770/kanso/actions/workflows/ci.yml)

---

## What it gives you

- **One theme entry point** — `KansoTheme { … }` wraps `MaterialTheme` with a colour scheme, the
  type/shape scales and the kanso spacing + elevation tokens, plus edge-to-edge system-bar contrast.
- **A whole scheme from one seed** — every Material 3 colour role is derived from a single brand
  colour. Tone maps to CIE L\*, so contrast between two tones is the same at every hue rather than
  varying with it: `onPrimary` on `primary` measures 6.42–6.46 across all 360 hues. That guarantee
  is [pinned by tests](kanso/src/test/java/dev/ayanami/kanso/theme/ColorContrastTest.kt), not
  asserted here.
- **A component library** — buttons (one emphasis system, five weights), cards + section headers,
  text fields with error states, a scaffold + top app bar, list/status rows, and empty/loading
  states. Components read spacing, colour, type and shape from the tokens. The top app bar shows
  the app's version right after the title by default, read from the installed package
  (`kansoAppVersionName()`) so it always states the build actually running — pass `version` to
  override the string, or an empty string to omit it.
- **Aligned versions, and nothing more** — a single Compose BOM is exposed via `api`, so consuming
  apps inherit the same aligned Compose and Material 3 versions. `api` carries only the artifacts
  that appear in kanso's own public signatures; your activity plumbing, icon set and navigation
  library stay your choice. The BOM is a floor, not a ceiling — declare a newer one and it wins.

## Quick start

```kotlin
val MyBrand = KansoBrand("my app", Color(0xFF6750A4))

setContent {
    KansoTheme(brand = MyBrand) {
        KansoScaffold(title = "Home") { inner ->
            Column(Modifier.padding(inner).padding(Kanso.spacing.screen)) {
                KansoCard(title = "Status") {
                    KansoStatusRow("Connection", "Secure")
                    KansoStatusRow("Last sync", "2 minutes ago")
                }
                KansoButton("Continue", onClick = { /* … */ })
            }
        }
    }
}
```

Any `Color` works as a seed. Achromatic seeds (grey, black, white) produce a neutral scheme rather
than picking an arbitrary hue, and a seed's own saturation is preserved, so a muted navy and a
vivid blue give visibly different schemes.

### Theming it to your brand

`KansoBrand(name, seed)` is the whole per-app surface. There is no ready-made brand list — an app
defines its brand where it defines everything else about itself.

> **`dynamicColor` defaults to `true`, and it wins over your seed.** On Android 12+ the scheme
> comes from the user's wallpaper (Material You) and the seed is never read. Below Android 12 the
> seed is always used. If your brand identity has to hold on every device:
>
> ```kotlin
> KansoTheme(brand = MyBrand, dynamicColor = false) { … }
> ```

Every other axis of the theme is a parameter, so adopting kanso never means forking it:

```kotlin
KansoTheme(
    brand = MyBrand,
    typography = kansoTypography(MyFontFamily),        // the M3 scale in your own face
    shapes = Shapes(medium = RoundedCornerShape(4.dp)),
    spacing = KansoSpacing(screen = 24.dp),
) { … }
```

`kansoTypography(family)` inherits every size, weight, line height and letter spacing from
Material 3 and changes only the family, so a brand font cannot accidentally rescale the type
system. Pass `displayFamily` separately if your display face differs from your text face.

For a scheme that is *nearly* the seeded one, override the roles you care about and pass the
result as `colorScheme` — it takes precedence over both `dynamicColor` and `brand`:

```kotlin
KansoTheme(colorScheme = kansoLightColorScheme(seed).copy(primary = Color(0xFF1B5E20))) { … }
```

`Kanso.brand` reads back the brand in force, for a splash screen or a chart series that needs the
raw seed — `Kanso.colors.primary` is a derived tone, and under dynamic colour it has no
relationship to the brand at all.

## Installing

kanso is consumed as a git submodule. It is **not** published to a Maven repository, and that is
a decision rather than a gap — [roadmap 1.5](docs/foundation-roadmap.md) records why, and what
would change it. Releases are git tags; see [CHANGELOG.md](CHANGELOG.md).

```bash
git submodule add https://github.com/ayanami770/kanso.git third_party/kanso
```

`settings.gradle.kts`:

```kotlin
include(":kanso")
project(":kanso").projectDir = file("third_party/kanso/kanso")
```

`app/build.gradle.kts`:

```kotlin
dependencies {
    implementation(project(":kanso"))
}
```

kanso brings the Compose BOM and the Compose artifacts in its public API. It does **not** bring an
activity integration, an icon set or a navigation library — declare whichever you use:

```kotlin
implementation("androidx.activity:activity-compose:1.9.3")
implementation("androidx.compose.material:material-icons-extended")  // version from the BOM
```

### Compatibility

| | |
|---|---|
| `minSdk` | 24 (`:demo` is 26) |
| `compileSdk` | 35 |
| Compose | BOM 2026.06.01, exposed via `api` — this sets your **floor**; declare a newer BOM to move past it |
| Kotlin | 2.4.10 (AGP 9's built-in Kotlin), with the Compose compiler plugin |
| JDK | 17 |
| AGP / Gradle | 9.3.1 / 9.6.1 |

## Tokens

- **Spacing** — a 4dp grid (`Kanso.spacing.xs … xxxl`, plus `screen` / `section`).
- **Elevation** — Material 3 tonal + shadow levels (`Kanso.elevation.level0 … level5`).
- **Colour / type / shape** — `Kanso.colors`, `Kanso.typography`, `Kanso.shapes`. These delegate
  straight to the current `MaterialTheme` values, which is why every stock Material 3 component
  and every third-party Compose library themes correctly under `KansoTheme` with no adapter.

## Modules

| module   | type                      | contents |
|----------|---------------------------|----------|
| `:kanso` | `com.android.library`     | the design system (`theme/` + `component/`) |
| `:demo`  | `com.android.application` | a gallery app: every token + component, live brand switch, dark/light, dynamic-colour toggle |

## Build

```bash
./gradlew :kanso:testDebugUnitTest   # the colour-contrast and typography contracts
./gradlew :kanso:assembleRelease     # the library
./gradlew :demo:assembleDebug        # the gallery app
./gradlew :kanso:lintRelease         # the consumer-facing lint gate
```

CI runs all four on every push and pull request.

## Contributing

[CONTRIBUTING.md](CONTRIBUTING.md) — what a correct kanso component looks like, in about thirty
lines. The public API is in explicit API mode, so every exported symbol is a deliberate choice.

## Status

kanso started as a private design system for four apps and has been generalised.
[docs/foundation-roadmap.md](docs/foundation-roadmap.md) records the whole exercise: what was
done, what was considered and **declined** and why, and an explicit list of things that were
already right and should be left alone. What remains open is the adaptive story beyond a content
width cap — a navigation rail or list-detail layout.

## License

[Apache-2.0](LICENSE) © 2026 ayanami770
