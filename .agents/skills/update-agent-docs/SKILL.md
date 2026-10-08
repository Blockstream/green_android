---
name: update-agent-docs
description: Check the agent documentation of this repository (AGENTS.md, the skills and the branch-review codebase map) against the code and fix the claims that went stale. Use when asked to update or refresh the agent docs or the codebase map, or after changes to modules, Gradle builds, CI commands or the code paths the docs name.
---

# Update Agent Docs

Keep `AGENTS.md`, the skills in `.agents/skills/` and `.agents/skills/branch-review/references/codebase-map.md` true to the code on the default branch. Verify each claim and correct the ones that are wrong; do not regenerate or restyle the documents.

## Scope

Check against the default branch, not a feature branch:

- Run `git fetch origin` and `git remote set-head origin --auto`.
- Read sources at `origin/HEAD` with `git show origin/HEAD:<path>`, `git ls-tree -r --name-only origin/HEAD` and `git grep <pattern> origin/HEAD`, so the result does not depend on what is checked out.
- Make the edits on a branch created from `origin/HEAD`.

## What To Check

| Claim in the docs | Source of truth |
|---|---|
| Module list and what each module holds | `settings.gradle.kts` and the module folders |
| Module dependencies | the `project(":…")` entries in each `build.gradle.kts`, and the source set each is declared in |
| Build and test commands | `.gitlab-ci.yml`, and that the tasks exist (`./gradlew <tasks> -m`) |
| Paths, classes and functions named in the docs | they exist at `origin/HEAD` |
| Commit types | `git log --format=%s origin/HEAD` |
| Skill index in `AGENTS.md` | the folders in `.agents/skills/` and their `description` |
| Flows and runtime areas in the codebase map | the code they name still does what the text says |

## Making Changes

- Before editing, list for the user each stale claim, what the source says and the proposed edit.
- Edit only the lines that are wrong or missing, and keep the surrounding wording and style.
- Add a fact only when the code confirms it. When unsure, list it as an open question instead.
- Update the commit and date at the top of `codebase-map.md` to the `origin/HEAD` commit the check ran against.
- Let the user review the diff before committing.

## Public Mirror

These files are published through the repository's public mirror:

- Refer to the default branch as `origin/HEAD`, not by name, and leave out internal hosts and links.
- Follow the wallet-data rule in `AGENTS.md`, and never include keys or tokens.

## Report

End with what was checked, what changed and the evidence for each change, and what could not be verified.
