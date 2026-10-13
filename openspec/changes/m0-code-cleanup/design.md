# Design

## Context

`main` after `m9-anchor-range` (2026-10-13). Java line counts: Foundation 32k (largest packages `logistics` 8.5k, `machine` 8.3k, `client` 3k, `datagen` 2.6k, `data` 2.4k), Manufacturing 14.5k (`alchemy` 6.7k, `datagen` 3k, `gametest` 1.8k), Storage 56k (`gametest` 14.5k, `client` 6.5k, `shuttle` 5k, `menu` 3.8k, `field` 3.4k, `terminal` 2.7k, `cpu` 2.3k). About 10,200 comment lines. Manufacturing and Storage depend on Foundation and never on each other.

## Goals / Non-Goals

**Goals:** code that follows the CLAUDE.md Code style section; less duplication; flaky tests fixed at the cause; the same behaviour.

**Non-Goals:** see proposal.

## Decisions

### D1. What a comment may say

Kept: Javadoc on classes and public or protected members, one to three lines, saying what it does and what a caller must know (units, ranges, side effects, thread). Inside code: only a non-obvious formula, an API quirk, or a constraint the code cannot express. Removed: reasoning and alternatives, history ("used to", "now", "since"), references to specs, design decisions (D1, D2), tasks, PLAN.md, play checks, changes or memory notes, comments that restate the code or the method name, commented out code, section banners that add nothing.

### D2. Refactoring rules

- Behaviour stays identical: same ids, saved data keys and codecs, config keys, datapack fields, network payload layouts, menu data slot order, GameTest names (kept so history stays comparable).
- Public Foundation API types used across mods (capabilities, `production`, `upgrade`, `network`, `graph`) keep their signatures; internals may change.
- Duplicates within a module become a helper in that module; duplicates across Manufacturing and Storage move to Foundation only when the helper is generic.
- Split methods longer than about 60 lines and classes over about 800 lines where a clear seam exists; no splitting for its own sake.
- Every step builds and passes the module's JUnit tests and GameTests before it is committed.

### D3. Work split

Agents work in separate git worktrees on disjoint package sets, so they can run in parallel:

1. Flaky test fixes (Storage GameTests), first, in parallel with step 2, since the Storage cleanup comes later.
2. Foundation, two agents: (a) `logistics`, `transfer`, `network`, `graph`, `flow`, `fluid`, `energy`, `side`, `placement`, `block`, `item`, `save`, `team`, `config`, `command`, `registry`, `craft`, `integration`; (b) `machine`, `client`, `datagen`, `data`, `adapter`, `production`, `wood`, `upgrade`, `gametest`. Cross package helpers inside Foundation are allowed only in the agent's own packages.
3. After Foundation is merged: Manufacturing (one agent) and Storage (four agents: `gametest`; `client`, `menu`, `terminal`, `picker`; `shuttle`, `field`, `rail`, `garage`, `rack`, `zone`, `index`, `station`, `outbound`, `packing`; `cpu`, `network`, `order`, `production`, `pattern`, `controller`, `crafting`, `stats`, `command`, `registry`, `integration`, `power`, `site`, `datagen`).
4. A review pass over the whole diff (correctness and leftovers), then full verification.

Merges are rebases onto `main` in that order; conflicts are resolved keeping both sides' intent.

### D4. Flaky tests

Root causes and fixes are recorded below under "Decisions made during apply" from the analysis. A flaky fix is proven by running the affected GameTests repeatedly (at least 10 runs) without a failure.

### D5. Verification

Pure logic keeps its JUnit tests (none removed unless the code under test is deleted). After the merges: `gradlew build`, `gradlew test`, the three `runGameTestServer` tasks, `runGameTestServerAll`, docs tests, and a client launch. The user play checks the main flows afterwards, since refactors can change untested behaviour.

PLAN.md open decisions settled: none.

## Risks / Trade-offs

- [Silent behaviour change] A refactor changes behaviour no test covers. → Small commits per package, the review pass, the user play check; any regression is reverted per commit.
- [Merge conflicts] Parallel agents touch shared files (registries, datagen providers, `ServusStorageGameTests`). → Disjoint package sets; shared files edited only by their package's agent.
- [Lost knowledge] A removed comment carried a real reason. → Reasons that constrain the code (an API quirk) stay as short comments by D1.
