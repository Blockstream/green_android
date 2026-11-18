# Redesign

The app is moving to the new design system screen by screen. Until the migration ends, the legacy and the redesigned UI live side by side behind one feature flag, and production builds only ever show the legacy UI.

Only the UI is duplicated. Screens keep their ViewModel: the legacy and the redesigned composable take the same `…ViewModelAbstract` and the same state, so business logic, navigation and analytics stay in one place.

## Feature flag

- `LocalDesignV2` (`compose/.../designv2/DesignV2.kt`) holds the effective value. Switch on it through `DesignVersion` and `designVersion` (see Switching) rather than reading it directly.
- It is `true` only when the build is a development flavor or a debug build (`AppInfo.isDevelopmentOrDebug`) and the setting is on. Production release builds always get `false`, whatever is stored.
- The setting lives in `SettingsManager` (`isDesignV2Enabled`, `setDesignV2Enabled`), outside `ApplicationSettings`, so toggling it does not go through the app settings save flow.
- Toggle it in App Settings → Developer → Design V2, or on Android press volume down twice quickly on any screen. Both only work on development and debug builds. The change applies at once: the whole tree recomposes.

## Where code goes

| What | Where | Name |
|---|---|---|
| Primitives and templates without app data (button, link, tab bar) | `designsystem/.../components` | `App…` |
| Redesigned composites that need app models (account card, amount row) | `compose/.../designv2/components` | `App…` |
| Glue between app state and the design system (bottom bar, transitions) | `compose/.../designv2` | `…V2` |
| Redesigned screen | next to the legacy screen, `screens/<feature>/<Name>ScreenV2.kt` | `<Name>ScreenV2` |
| Redesigned part of a legacy screen | next to the screen, `screens/<feature>/<Part>V2.kt` | `<Part>V2` |

The legacy code keeps its names: `Green…` components, `compose/.../components`, `MaterialTheme` and `com.blockstream.compose.theme`. Don't rename or move it; it is deleted at the end.

Build components on demand, when a screen needs them, from the Figma component library. Name the Kotlin API after the Figma component and its variants (`AppButtonVariant.Tertiary` for `variant=tertiary`).

## Switching

Pick the narrowest switch that covers the change. `DesignVersion(v1, v2)` picks between two composables, `designVersion(v1, v2)` between two values (a color, a transition set). Give both versions the same signature, receiver included, so the switch is one call.

**A whole screen** switches in `Router`, where the ViewModel is created once for both:

```kotlin
appComposable<NavigateDestinations.Example> {
    val args = it.toRoute<NavigateDestinations.Example>()
    val viewModel = viewModel { ExampleViewModel(args) }
    DesignVersion(
        v1 = { ExampleScreen(viewModel) },
        v2 = { ExampleScreenV2(viewModel) }
    )
}
```

**Part of a screen** (a state, a section) switches where the legacy screen calls it, with the same arguments:

```kotlin
DesignVersion(
    v1 = { SwapsUnavailable(hasSwaps, onProgress, onResetClick, onLearnMoreClick) },
    v2 = { SwapsUnavailableV2(hasSwaps, onProgress, onResetClick, onLearnMoreClick) }
)
```

**The app chrome** (`BottomNavigationBar` / `BottomNavigationBarV2`, scaffold background) switches in `AppScaffold`. It is global: with the flag on, legacy screens also get the new chrome.

**Screen transitions** switch in `Router` between `NavTransitions` and `NavTransitionsV2`: in V2 new screens slide in over the previous one and bottom tabs crossfade. `appComposable` wraps every screen in `ScreenSurface`, which gives it an opaque canvas background in V2, since screens draw none of their own.

**The top bar** belongs to the screen in V2: `ScreenSurface` draws `TopAppBarV2` above the screen from the screen's own `NavData`, so the bar slides with its screen and needs no animation of its own. `AppScaffold` keeps the global `GreenTopAppBar` for V1 only. ViewModels need no change: `AppBarState`, called by `SetupScreen`, feeds both.

`git grep -nE "DesignVersion|designVersion|LocalDesignV2"` lists every switch, and therefore every migrated screen and part.

## Rules for redesigned code

- Style with `AppTheme.colors`, `AppTheme.typography` and the `App…` components. Don't use `MaterialTheme`, `androidx.compose.material3` widgets, `Green…` components or anything from `com.blockstream.compose.theme`.
- Infrastructure from `compose` is fine: `SetupScreen`, `HandleSideEffect`, `appTestTag`, `Rive`, `GreenPreview`, navigation and resources.
- Go through `SetupScreen` (or call `AppBarState` and `HandleSideEffect` yourself). Without it the ViewModel's side effects and `NavData` are silently dropped.
- `NavData` and `NavAction` say what the top bar shows, not how it looks. Don't put icons, colors or click lambdas in a ViewModel: add a semantic `NavAction` type or `NavData` field, give it an `Event` for the click, and draw it in both `ActionMenu` (legacy) and `TopAppBarV2`.
- When migrating a screen, move the texts its ViewModel still resolves with `getString` (snackbars, dialogs, errors) to `StringHolder`, so the UI resolves them.
- Move UI logic that a legacy screen keeps in its composable (side-effect handlers, dialogs, local state with behavior) into the ViewModel or a shared helper before writing the redesigned screen, so both versions call the same code.
- Keep the legacy E2E test tags (`appTestTag`) on the matching elements.
- Add a `@Preview` for the redesigned composable.
- Strings come from the shared resources. Don't fork a string for the redesign.

## Keeping both versions in sync

Until the migration ends, a fix or feature in a migrated screen goes into both versions. The `redesign-parity` skill (`.agents/skills/redesign-parity`) checks a branch for this, and `branch-review` runs it.

## Ending the migration

When every screen is migrated:

1. Replace every `DesignVersion` and `designVersion` with its `v2`, and delete the legacy screens and components that leaves unused.
2. Drop the `V2` suffix: `<Name>ScreenV2` becomes `<Name>Screen`, `<Part>V2` becomes `<Part>`.
3. Delete `LocalDesignV2`, `DesignVersion`, `designVersion`, the setting and its switches.
4. Delete the `redesign-parity` skill, its step in `branch-review`, and this file.
