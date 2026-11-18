---
name: redesign-parity
description: Keep the legacy and the redesigned version of migrated screens in sync during the design system migration. Use when a branch fixes or changes a screen, component or ViewModel that has a redesigned counterpart, when asked whether a change also needs the redesign, and from branch-review. Temporary, deleted when the migration ends.
---

# Redesign Parity

During the migration a screen can have a legacy and a redesigned (V2) version behind `LocalDesignV2`; `docs/redesign.md` has the conventions. A UI change made to one of them usually belongs in the other. Find the changes that touch a migrated screen and either apply them to the counterpart or say why they don't apply.

## Find the pairs

List every switch on the branch being checked:

```sh
git grep -nE "DesignVersion|designVersion|LocalDesignV2" <ref> -- compose/src
```

Each hit is a pair:

- in `Router.kt`: `<Name>Screen` and `<Name>ScreenV2`, and `NavTransitions` and `NavTransitionsV2`;
- inside a screen: `<Part>` and `<Part>V2`, in the same feature folder;
- in `AppScaffold.kt`: `BottomNavigationBar` and `BottomNavigationBarV2`, and `GreenTopAppBar`, whose V2 counterpart `TopAppBarV2` is drawn per screen by `ScreenSurface`.

## Check the diff

For the changes under review (`git diff <target>...<source>`, or the working tree):

1. List the changed files that belong to a pair, on either side, including the components a pair uses.
2. For each change, decide which kind it is:
   - **Shared**: ViewModel, use case, `NavData`, strings, navigation. Both versions get it already; check that the redesigned composable reads any new state or event.
   - **Behavior in the UI layer**: a side-effect handler, a dialog, a condition on what is shown, a click handler, a test tag. It must be in both versions.
   - **Legacy styling only**: spacing, colors, Material widgets. It does not carry over; the redesign follows Figma.
3. For a behavior change that is only on one side, apply the equivalent change to the other, written in that side's style: `AppTheme` and `App…` components for the redesign, the existing legacy components for the legacy side.
4. If the counterpart can't take the change (the redesign lacks a component, the design differs), don't force it. Report it.

Also flag a redesigned file that breaks the rules in `docs/redesign.md`, such as importing `androidx.compose.material3`, `MaterialTheme`, `Green…` components or `com.blockstream.compose.theme`.

## Report

List each pair the diff touches with one of:

- `in sync`: the change is in both, or is shared;
- `applied`: what you changed on the other side;
- `not applicable`: legacy styling only, and why;
- `open`: what is missing and why it was not applied.

When called from `branch-review`, put this list in the review notes.
