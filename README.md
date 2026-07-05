# kanso

**A shared Jetpack Compose + Material 3 design system for the ayanami770 apps.**

One theme, one token set, one component library — consumed as a git submodule so every app
(LMSA, CertWatch, Semicon News, medcal, …) shares the same commercial-grade UX while keeping its
own brand accent.

*kanso* (簡素) — the aesthetic of considered simplicity: nothing decorative, everything deliberate.

---

## What it gives you

- **One theme entry point** — `KansoTheme { … }` wraps `MaterialTheme` with a color scheme, the
  type/shape scales and the kanso spacing + elevation tokens, and edge-to-edge system-bar contrast.
- **Shared system + per-app accent** — every Material 3 color role is derived from a single brand
  **seed** color, so an app gets a whole coherent scheme from one value. On Android 12+ **dynamic
  color** (Material You) can take over from the wallpaper.
- **A component library** — buttons (one emphasis system, five weights), cards + section headers,
  text fields with error states, a scaffold + top app bar, list/status rows, and empty/loading
  states. Every component reads from the tokens, never hard-coded values.
- **Aligned versions** — a single Compose BOM is exposed via `api`, so consuming apps inherit the
  same aligned Compose/Material3/navigation/icon versions from the submodule.

## Modules

| module   | type                | contents                                             |
|----------|---------------------|------------------------------------------------------|
| `:kanso` | `com.android.library` | the design system (`theme/` + `component/`)        |
| `:demo`  | `com.android.application` | a gallery app: every token + component, live brand switch + dark/light |

## Consuming it in an app (git submodule)

```bash
# in the consuming repo
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

Then, in the app:

```kotlin
setContent {
    KansoTheme(brand = KansoBrands.Lms) {   // per-app accent
        AppRoot()
    }
}
```

## Brands

`KansoBrands` ships ready-made seeds for the fleet:

| brand       | seed        | note          |
|-------------|-------------|---------------|
| `Kanso`     | `#006A60`   | calm teal (default) |
| `Lms`       | `#006A60`   | secure teal   |
| `CertWatch` | `#3F5AA6`   | trust blue    |
| `Semicon`   | `#8A4F00`   | amber/silicon |
| `Medcal`    | `#386A20`   | clinical green |

Add your own with `KansoBrand("name", Color(0xFF……))`.

## Tokens

- **Spacing** — a 4dp grid (`Kanso.spacing.xs … xxxl`, plus `screen` / `section`).
- **Elevation** — Material 3 tonal + shadow levels (`Kanso.elevation.level0 … level5`).
- **Color / type / shape** — `Kanso.colors`, `Kanso.typography`, `Kanso.shapes` (the current
  `MaterialTheme` values under the kanso theme).

## Build

```bash
./gradlew :kanso:assembleDebug     # the library
./gradlew :demo:assembleDebug      # the gallery app
```

Toolchain: AGP 8.7.3 · Gradle 8.9 · Kotlin 2.0.21 (+ the Compose compiler plugin) · JDK 17 ·
compileSdk 35 · minSdk 24 (`:kanso` library; the `:demo` app is minSdk 26).

## License

[Apache-2.0](LICENSE) © 2026 ayanami770
