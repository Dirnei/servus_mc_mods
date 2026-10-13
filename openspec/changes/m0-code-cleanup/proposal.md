# Proposal

## Why

About 100,000 lines of Java have grown over many changes, written by many sessions. About 10,000 of them are comments, many of them reasoning, history or references to specs, design notes and play checks instead of explanations of code. Logic is duplicated across blocks and screens. Several GameTests fail now and then, which makes a red run meaningless. The user asked on 2026-10-13 for a proper review and cleanup, with comments only where code needs explaining, self explaining and reusable code, and the flaky tests analysed and fixed.

## What Changes

This change is a cross cutting maintenance item under PLAN.md milestone **M0 Foundations** (repo health), as `m0-code-cleanup`. No game behaviour, balance number, recipe, id, saved data key or public API changes.

- **Code style rule.** `CLAUDE.md` gains a "Code style" section (committed before this change): Javadoc short and precise, comments inside code only where the code cannot speak for itself, no references or reasoning, no dead code.
- **Comments.** Every comment in `foundation`, `manufacturing` and `storage` (main and test sources) is checked against that rule: reasoning, history, references and comments that repeat the code are removed; Javadoc is shortened to what a caller must know.
- **Refactoring.** Duplicated logic is pulled into shared helpers, long methods and classes are split, unclear names are renamed, dead code is deleted. Behaviour is guarded by the existing JUnit tests and GameTests.
- **Flaky tests.** `shaftBreakDropsRiderCargo`, `FieldGameTests.openPlotsCounted`, `FieldGameTests.hydrationReachesFourBlocks` and the `*sendsonopen` tests under `runGameTestServerAll` are analysed and fixed at their cause.

## Capabilities

### New Capabilities
- none

### Modified Capabilities
- none (no observable behaviour changes; the change declares `skip_specs`)

## Non-goals

- New features, balance changes, look changes.
- Renaming registry ids, saved data keys, config keys, datapack fields or the public Foundation API (capabilities, production API, upgrade API).
- Rewriting the docs site, the Balance lab or the texture scripts.
- Performance work beyond what falls out of simpler code.

## Impact

- Code: all Java under `foundation/src`, `manufacturing/src`, `storage/src` (main and test).
- `CLAUDE.md` (Code style section).
- Risk: refactors may change behaviour the tests do not cover; a final review pass and a full play check by the user follow.
