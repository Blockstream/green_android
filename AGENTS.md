# Agent Instructions

Blockstream App (`green_android`) is a non-custodial Bitcoin, Liquid and Lightning wallet built on GDK, written in Kotlin Multiplatform with Compose. Android is the shipped app; `desktopApp` is a Compose Desktop target.

## Repository

- Base branch: the remote's default branch, `origin/HEAD`. Run `git fetch origin` and `git remote set-head origin --auto` first so it is current, then branch from it and target it in merge requests.
- Modules (`settings.gradle.kts`): `androidApp`, `compose`, `designsystem`, `data`, `domain`, `gdk`, `hardware`, `jade`, `network`, `utils`, `base-gms` / `gms` / `no-gms` (store variants), `desktopApp`.
- Building needs the GDK native libraries. Gradle fetches them; `BUILD.md` covers fetching and building them by hand.

## Build and test

- Debug build: `./gradlew assembleDevelopmentDebug` (production flavor: `assembleProductionGoogleDebug`).
- Unit tests, as CI runs them: `./gradlew test :compose:testAndroidHostTest -x :androidApp:testProductionGoogleDebugUnitTest`. CI first copies `contrib/blockstream_keys.txt` to the gitignored `app_keys.txt`.
- That command skips the Kotlin Multiplatform modules `data`, `domain`, `jade` and `utils`, which have no `test` task. Run their JVM and Android host tests with `./gradlew :<module>:allTests`.
- Prefer a single module's tasks while iterating; list them with `./gradlew :<module>:tasks --all`.
- Gradle dependency verification is enforced through `gradle/verification-metadata.xml`. After adding or upgrading a dependency, run `./update_dependency_verification.sh` (`-b`, `-t` or `--desktop` also resolve the dependencies of the debug build, the tests or the desktop app) and review the checksums it adds. Never bypass verification with `--dependency-verification=lenient` or `off`.

## Conventions

- Commit subjects follow Conventional Commits: `fix:`, `feat:`, `refactor:`, `docs:`, `build:`, `ci:`, `chore:`, optionally scoped, as in `feat(design-system):`.
- Comment code only where it is not obvious: why a choice was made, a constraint it works around, or a non-obvious behavior. Don't restate what the code does or describe how it changed. A short header stating a new file's purpose is fine.
- The code is public. Don't put links to private resources, such as Figma files, Google Docs or internal trackers, in code, comments or docs; describe what the reader needs instead.
- Wallet data is sensitive. Never put mnemonics or their encrypted forms, BIP39 passphrases, private keys, PINs, xpubs, descriptors, wallet IDs, auth tokens, 2FA secrets or transaction IDs in commits, notes or logs, including when copying from logs a user provides.
- Report security vulnerabilities privately as described in `SECURITY.md`; never describe them in public issues or pull requests.

## UI

- Style new and changed UI with the `designsystem` tokens, `AppTheme.colors` and `AppTheme.typography`, which `GreenTheme` provides. Don't hard-code colors, text styles or fonts, and don't reach for `MaterialTheme` values when a token exists.
- Reuse components before writing new ones. Shared ones live in `compose/src/commonMain/kotlin/com/blockstream/compose/components` (`GreenButton`, `GreenCard`, `GreenTextField`, `GreenTopAppBar`, `GreenBottomSheet` and more); feature-specific ones in `compose/.../screens/<feature>/components`. When one almost fits, extend it rather than adding a near-duplicate.
- Put a new component in `components/` when more than one feature uses it, and in the feature's `screens/<feature>/components` otherwise.

## Skills

Task-specific workflows live in `.agents/skills/`; Claude Code reads them through the `.claude/skills` link.

- `branch-review`: review a branch or merge request and keep its running review notes.
- `qa-handoff`: write the QA handoff notes for a branch.
- `update-agent-docs`: check this file, the skills and the codebase map against the code and fix stale claims; run it after changing modules, Gradle builds or CI commands.
