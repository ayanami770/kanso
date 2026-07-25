# Contributing to kanso

Short, because most of what keeps a design system coherent is a handful of rules applied every
time. Anything a machine can check is checked in CI rather than written here.

## What a correct kanso component looks like

**Read from tokens, never from literals.** No bare `.dp` for a gap, padding or inset — use
`Kanso.spacing`. No `Color(0xFF…)` in a component — use `Kanso.colors` or
`Kanso.extendedColors`. If you find yourself wanting a value that has no token, add the token;
if it has one call site, it does not need a token yet.

The exceptions already in the codebase are icon sizes and stroke widths (`18.dp`, `24.dp`,
`56.dp`). Those are Material metrics rather than layout rhythm, and they are named `private val`s
at the top of their file rather than inlined.

**`modifier` is the first optional parameter**, and the component never overrides the caller's
sizing. `modifier.fillMaxWidth()` is fine — the caller's constraint still wins, which is what
makes `KansoContentContainer` work without touching any component. `Modifier.fillMaxWidth()`
(dropping the caller's) is not.

**Absorb experimental opt-ins.** `@OptIn(ExperimentalMaterial3Api::class)` goes on the kanso
function, and no experimental type appears in a public signature. A consumer should never
inherit an opt-in from us — `:demo` compiles without a single one, and that is the test.

**Say when to use it, not just what it is.** The KDoc that earns its place is the sentence that
tells a reader which of five things to pick. The emphasis rule is the model:

> Use `KansoButtonStyle.Filled` for the single primary action on a screen, Tonal/Outlined for
> secondary, Text for tertiary.

**Prefer one decision over one more knob.** `KansoButtonStyle.Destructive` resolves the error
role inside the component; exposing a raw `colors` parameter would push that decision back onto
every screen and re-create the divergence kanso exists to prevent. Add a seam when something is
genuinely inexpressible, not to be thorough.

## Before you open a pull request

- **Add a `@KansoPreviews` preview** in `component/Previews.kt`. Four configurations — light,
  dark, 200% font scale, RTL. Every layout defect this library has had was invisible at default
  settings; this is the cheapest way to keep catching them. If your change fixes a bug, write the
  preview as the case that used to break.
- **Add it to `:demo`** if it is a component. The gallery is the only place behaviour is
  exercised end to end, and a component the demo cannot use is a component nobody can use.
- **Pin any contract you can assert on the JVM.** The colour engine and the typography builder
  both have tests because their guarantees are checkable without a device. Reach for that first;
  it is worth more than a screenshot.
- **Run the same four things CI runs:**

  ```bash
  ./gradlew :kanso:testDebugUnitTest :kanso:assembleRelease :demo:assembleDebug :kanso:lintRelease
  ```

## Versioning

SemVer, with one clause most design systems omit and most need: **visual output is not covered.**
Retuning a colour curve or a component's internal padding is a PATCH even though pixels change.
If you screenshot-test against kanso, pin an exact version.

Anything that changes a public signature, removes a parameter, or alters which artifacts arrive
via `api` is a MAJOR — the `api` surface is a promise, not an implementation detail.

## Not rules

Formatting, import order and the modifier-parameter position are for a tool to enforce, not a
document. Until ktlint/spotless and compose-lints are wired up, match the file you are editing.
