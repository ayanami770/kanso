# Module kanso

A shared Jetpack Compose + Material 3 design system: one theme, one token set and one component
library, with a per-app brand accent derived from a single seed colour.

Everything below is reachable from `KansoTheme`. Wrap your app in it once and read tokens through
the `Kanso` object:

```kotlin
KansoTheme(brand = KansoBrand("my app", Color(0xFF6750A4))) {
    KansoScaffold(title = "Peers") { inner ->
        Column(Modifier.padding(inner)) {
            KansoListItem("dao-node", supporting = "192.168.1.24", onClick = {})
        }
    }
}
```

Two things are worth knowing before reading any further:

**`dynamicColor` defaults to `true` and wins over your brand.** On Android 12+ the scheme comes
from the user's wallpaper and the seed is never read. That is deliberate — a user's system-wide
colour preference outranks an app's — but if brand identity has to hold, pass
`dynamicColor = false`.

**Colour is not a suggestion.** Tone is CIE L\*, built in CIELCh(ab) and gamut-mapped by reducing
chroma only, so contrast survives the mapping. `onPrimary` on `primary` measures 6.42–6.46 at
every hue on the circle, and that is pinned by a test rather than by inspection.

# Package dev.ayanami.kanso.theme

The theme entry point, the colour engine, and the token classes.

`KansoTheme` is the only thing an app calls. `Kanso` is the accessor object every component reads
through — `Kanso.spacing.lg`, `Kanso.colors.primary`, `Kanso.sizing.icon`. Each token class is an
`@Immutable data class` with working defaults, provided over a `staticCompositionLocalOf`, so a
`@Preview` renders without a theme and an app can override one field without restating the rest.

`kansoLightColorScheme` / `kansoDarkColorScheme` turn a seed into a full Material 3 `ColorScheme`.
They are pure Kotlin — no `android.graphics` — which is why the contrast guarantee is checkable
on the JVM.

# Package dev.ayanami.kanso.component

The components. Everything is `Kanso`-prefixed, applies the caller's `modifier` first, and never
overrides the caller's sizing.

The accessibility decisions are the part worth reading rather than skimming, because they are
what a hand-rolled equivalent gets wrong: a settings row carries `toggleable` on the *row* so a
screen reader announces one control and not two; a status badge requires its text because colour
alone is unreadable to a reader who cannot separate green from amber; a list row merges into a
single semantics node so a label and its value are not read apart.
