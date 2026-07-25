// kanso — a shared Jetpack Compose + Material 3 design system.
// One theme + token set + component library for every app; consumed as a git submodule (a
// :kanso module) or, in future, as a Maven artifact. Per-app brand accent via a seed color;
// dynamic color (Material You) on Android 12+. Apache-2.0.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.roborazzi) apply false
}
