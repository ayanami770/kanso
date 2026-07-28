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

**Accessibility is part of the component, not a later pass.** Four conventions, all of which
existing components already follow — match them:

- *Decorative icons take `contentDescription = null`.* Every leading icon in kanso sits beside a
  visible label that already carries the meaning; naming it makes a screen reader say the same
  thing twice. An icon that is the *only* carrier of meaning — a status glyph, an icon-only
  button — must take a description as a parameter, not invent one.
- *Never signal by colour alone.* A status needs text or a shape as well. `Kanso.extendedColors`
  exists so that "success" has a name, not so that green can mean it by itself.
- *Anything tappable clears 48dp.* Padding does not guarantee it — a one-line `KansoListItem` is
  44.4dp at the "Small" font setting. Use `heightIn(min = 48.dp)`.
- *A row is one stop, not several.* A label and its value, or a headline and its supporting line,
  merge with `Modifier.semantics(mergeDescendants = true) {}`. `Modifier.clickable` already does
  this, so only the non-clickable path needs it.

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

There isn't any. kanso is not published and is not tagged; consumers pin a commit SHA, which is
what a git submodule records anyway. `CHANGELOG.md` heads each section with the SHA to move to,
so **that file is the whole release process** — if your change is not in it, a consuming app has
no way to learn about it.

Which means the one rule that used to be carried by a version number is now carried by you: put
anything that **changes a public signature, removes a parameter, or alters which artifacts arrive
via `api`** under a **Changed** heading, with the migration spelled out. The `api` surface is a
promise, not an implementation detail.

**Visual output is deliberately outside that promise.** Retuning a colour curve or a component's
internal padding changes pixels and does not count as breaking. If you screenshot-test against
kanso, pin an exact SHA.

## Not rules

Formatting, import order and the modifier-parameter position are for a tool to enforce, not a
document. Until ktlint/spotless and compose-lints are wired up, match the file you are editing.
