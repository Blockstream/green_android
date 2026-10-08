---
name: qa-handoff
description: Write or update the QA handoff notes for a branch of this repository, for manual and automation QA engineers. Use when asked for QA notes, a QA handoff, or what QA should test on a branch.
---

# QA Handoff

Write the QA notes for the branch in `.reviews/<branch>-qa.md`, where `<branch>` is the source branch name used as a path, as in the `branch-review` skill (`chore/remove-room-database` → `.reviews/chore/remove-room-database-qa.md`). The `.reviews/` folder is gitignored: never commit or stage anything in it.

If a review notes file `.reviews/<branch>-review.md` exists, base the QA notes on its current assessment and remaining risks. To understand the change itself, resolve `<source>` and `<target>` as in the `branch-review` skill's First Steps, then read `git diff <target>...<source>`.

## Audience And Content

The QA file is for manual and automation QA engineers, not developers. It includes:

- the high-level branch goal
- whether behavior is expected to change
- what the changes imply functionally
- why the branch is useful
- important things to watch for
- the broad app flows to cover, and what to pay attention to in each

Do not turn it into a detailed step-by-step test plan; QA already has product test documentation. Prefer broad functional coverage and branch-specific risk signals. For example:

- Home, Transact and Asset Details should agree on balances.
- A Liquid transaction moving from unconfirmed to confirmed should update correctly.
- Asset labels and tickers should remain correct for Bitcoin, LBTC, Lightning Bitcoin and Liquid assets.
- Logging out during sync should not crash or leave stale data.
- Jade and watch-only login should still load accounts, balances and transactions correctly.

## Functional Areas

Use the branch goal to select a focused subset of these areas, which are the most likely to catch regressions:

- login, auto-login, restore, logout, and repeated login/logout
- network interruption during login or sync
- Home balance, fiat balance and asset list
- Transact tab responsiveness and transaction history
- transaction details, especially unconfirmed to confirmed transitions
- Send and Receive for Bitcoin, LBTC, Liquid assets, and invoices where supported
- Asset Details and account-specific views
- settings changes such as denomination and pricing currency
- Liquid asset metadata and cache behavior
- Jade login, signing and logout
- watch-only import and login
- app background/foreground during sync

Follow the wallet-data rule in `AGENTS.md` and keep that data out of the notes.
