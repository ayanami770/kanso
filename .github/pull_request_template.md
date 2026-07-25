<!--
See CONTRIBUTING.md. Delete anything that does not apply — an empty section is worse than a
missing one.
-->

## What changes

## Why

<!-- If this fixes a defect, say what the user saw, not just what the code did. -->

## Checks

- [ ] Reads from tokens — no bare `.dp` gaps, no `Color(0xFF…)` in a component
- [ ] `@KansoPreviews` preview added or updated (light / dark / 200% font / RTL)
- [ ] Added to `:demo` if it is a component
- [ ] `./gradlew :kanso:testDebugUnitTest :kanso:assembleRelease :demo:assembleDebug :kanso:lintRelease`

## Compatibility

<!--
Public signature changed? Parameter removed? An artifact added to or dropped from `api`?
Say so here and say what a consuming app has to do about it.
-->
