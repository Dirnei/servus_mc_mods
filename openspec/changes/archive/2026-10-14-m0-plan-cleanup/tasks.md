# Tasks

## 1. PLAN.md

- [x] 1.1 Rewrite `PLAN.md` as described in proposal.md: intro and mod table, vision per mod, architecture principles and Foundation API rules, Done table with one row per milestone M0 to M11, open points per mod, open decisions. Every open item in the old file stays (checked line by line against the old text). No em dashes. Verify: `git diff --stat PLAN.md` and a read through for lost open items.

## 2. CLAUDE.md

- [x] 2.1 In `CLAUDE.md`, say that PLAN.md is mainly the list of open points and is groomed after every archived change, mostly by removing what was built. Verify: read through.

## 3. Integration

- [x] 3.1 `openspec validate m0-plan-cleanup --strict` green; docs tests green (nothing reads PLAN.md, checked once).
- [x] 3.2 The user reads the new PLAN.md and confirms.
