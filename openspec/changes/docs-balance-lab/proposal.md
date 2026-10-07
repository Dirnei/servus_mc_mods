# Proposal

## Why

Every balance idea today means editing JSON, running the game, building a chain and watching it. The M3 "Balance pass" item is still open for exactly that reason. A documentation site with a Balance lab lets us see the production chains, try what-if numbers and draft new items, recipes and machines in the browser, and hand the result back as an implementable proposal. The same site becomes the public home for player and modpack author docs.

PLAN.md item: M3 "Balance pass" (the lab is the tool for it). The site itself is new scope, added to PLAN.md as a cross-cutting "Documentation site" item in this change.

## What Changes

- New `docs/` project at the repo root: Astro with the Starlight theme, its own `package.json`, never touched by Gradle.
- Public docs skeleton: landing page, getting started skeleton, an overview page per mod, and a datapack reference for machine stats, deposits, techs and recipes.
- Balance lab section:
  - Chain view per product: a flow diagram from deposits to the product with machine counts, utilization, FE draw and coal draw for a target rate (default one Red Pack per 5 s, reproducing the PLAN.md reference block).
  - Route toggles where an item has more than one recipe (for example iron ingot from raw iron or from grit).
  - Research view: tech graph with costs, unlocks and time to research for N labs.
  - Edit any number from the game data; edited values are highlighted with deltas against the committed baseline.
  - Draft new items, new recipes of existing recipe types, and concept machines (flagged "needs code").
  - Export and import of a versioned proposal file (changes, new content, impact, notes), with conflict detection against a changed baseline. Autosave in the browser, share links that encode the draft in the URL.
- Manufacturing datagen gains a game data exporter that writes `docs/src/data/gamedata.json` (committed) with the effective numbers the game uses: machine stats merged with code defaults, recipes, deposits, techs and display names.
- CI: `build.yml` fails when `gamedata.json` is stale; a new `docs.yml` tests, builds and deploys the site to GitHub Pages.
- Pure ratio math in TypeScript (`docs/src/lib/ratios.ts`) covered by Vitest.
- PLAN.md: a documentation site entry in the repository layout and milestones.

## Capabilities

### New Capabilities
- `lib/gamedata-export`: the datagen export of effective game data to a versioned JSON file for tools outside the game, and the CI freshness guard. Placed under `lib/` as a cross mod integration surface; Manufacturing is the first mod to export.
- `docs/balance-lab`: the documentation site, its Balance lab (chain view, routes, research view, edits, drafts, proposals) and its deployment.

### Modified Capabilities
None. No in-game behavior changes.

## Non-goals

- Target rate planner (pick "2 Red Packs per second", get the whole build). Deferred until after the first release; it will build on the same ratio module.
- Drafting new techs or textures in the lab.
- Several saved scenarios side by side. One open draft plus import and export.
- Logistics and Storage data in the export and the lab (duct throughput, shuttle rates).
- Writing the full player tutorial. Only the skeleton and the datapack reference now.
- Vanilla crafting recipes and recipes of other mods beyond the vanilla smelting recipes the chains need.

## Impact

- New: `docs/` (Node 22, Astro, Starlight, Preact, Vitest, lz-string), `.github/workflows/docs.yml`.
- Manufacturing: new datagen provider in `manufacturing/.../datagen/`, registered in `ServusManufacturingDataGen`; data run gets a system property for the export path in `build.gradle`.
- `.github/workflows/build.yml`: data run plus a diff check on `gamedata.json`.
- GitHub repo settings: Pages source set to "GitHub Actions" (one manual step by the owner).
- `PLAN.md`: repository layout and a documentation site entry.
