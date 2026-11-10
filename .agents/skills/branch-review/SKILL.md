---
name: branch-review
description: Review a branch or merge request of this repository and keep its running review notes. Use when asked to review a branch, an MR or new commits, to re-check earlier findings, or to assess whether a branch is ready to merge.
---

# Branch Review

Review a branch against its target branch (the merge request's target, or else the remote's default branch, `origin/HEAD`) and record the result in a review notes file that stays useful across review rounds. For the QA handoff notes, use the `qa-handoff` skill.

For orientation in the codebase, read `references/codebase-map.md` next to this file.

## Notes Files

Keep the notes in `.reviews/` at the repository root. The folder is gitignored: never commit or stage anything in it.

- Review notes: `.reviews/<branch>-review.md`
- QA notes: `.reviews/<branch>-qa.md`

`<branch>` is the source branch name used as a path, so `chore/remove-room-database` keeps its notes in `.reviews/chore/remove-room-database-review.md`; create the subfolders as needed. Git does not allow both `a` and `a/b` as branch names, so every branch gets its own path.

The review file is a living document. Keep updating it as the branch tip changes instead of replacing previous notes. Tie each review round to the exact commit being reviewed.

## First Steps

1. Resolve what to review, without switching the user's checkout:
   - `git fetch origin` and `git remote set-head origin --auto`, so the remote refs and `origin/HEAD` are current.
   - Source: the branch or merge request the user names. For a merge request, read its source and target branches from the merge request itself. With neither named, use the current branch (`git branch --show-current`); on a detached HEAD, ask which branch to review.
   - Set `<source>` to `origin/<source branch>`. Use the local branch instead when it has not been pushed, or when the user wants unpushed commits reviewed, and say so in the notes.
   - Set `<target>` to `origin/<target branch>` for a merge request, and to `origin/HEAD` otherwise.
2. Check the tip and what the branch adds on top of its target:
   - `git rev-parse --short <source>`
   - `git log --oneline <target>..<source>`
   - review the changes with `git diff <target>...<source>`. Read files at the reviewed tip with `git show <source>:<path>`.
   - To build or run tests, use a separate worktree (`git worktree add --detach <dir> <source>`) rather than checking the branch out in place.
3. Open the branch notes file if it exists.
4. Compare the current tip with the latest commit already recorded in the notes.
5. If the tip changed, add a new top section for the new commit or commits.
6. Preserve older sections. They show whether follow-up commits actually addressed earlier findings.
7. If the diff touches a screen or component that has a redesigned counterpart (`git grep -nE "DesignVersion|designVersion|LocalDesignV2"`), run the `redesign-parity` skill and add its report to the notes.

## Ask For Missing Context

Before making a final merge or readiness assessment, make sure the user has provided branch context. If it is not already clear, ask for:

- the goal of the branch
- whether functional behavior is expected to change
- important constraints or non-goals
- specific risks already discussed in the MR
- reported repro steps, logs, screenshots or QA observations
- whether the branch is a refactor, a behavior change, a bug fix, or preparation for future changes

Do not assume a large refactor is meant to change behavior. If the stated goal is "no functional change", review primarily for regressions.

## Per-Commit Review Section

For each reviewed tip, add a section like:

```md
## <short-sha> - <commit subject>

### Overall State

...

### Fixed Or Improved

- ...

### Remaining Risks

- ...

### Current Interpretation

...

### Suggested QA

- ...
```

Use concrete code references where they help, but keep the file readable across repeated review rounds.

## Overall Branch State

Keep an `Overall Branch State` section near the top of the review file and update it whenever the assessment changes. It includes:

- the current reviewed tip
- the branch goal
- whether user-facing behavior is expected to change
- the current merge assessment
- the main remaining concerns
- the QA gate or verification suggested before merge
- the latest local QA results reported by the user

Use cautious states such as:

- `not merge-ready`
- `known blocker`
- `focused-QA required before merge`
- `merge candidate with residual risks`
- `merge-ready from code review, pending normal QA`

## Compare Against Previous Notes

When a new commit claims to fix an earlier issue:

- identify the old finding
- read the new diff
- state whether it is fixed, partially fixed or still open, and why
- update the overall branch state if needed

Do not mark an issue fixed only because the code now catches the exception. Check that the state is correct, recoverable and visible to the user.

Back findings with evidence where you can: build the affected modules and run the relevant tests (see `AGENTS.md` for the commands), and record in the notes what you ran and its result. A finding confirmed only by reading the code is recorded as such.

## Logs And Repro Reports

When the user provides logs:

- summarize the fatal path or suspicious sequence
- record exact repro steps separately from the analysis
- mention whether the current tip addresses that path
- if a crash no longer reproduces, record that as QA evidence, not as proof by itself

Treat logs as sensitive: follow the wallet-data rule in `AGENTS.md` and keep that data out of the notes.
