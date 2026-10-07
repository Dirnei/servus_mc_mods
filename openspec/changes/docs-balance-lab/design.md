# Design

## Context

See proposal.md for the motivation. Facts from the code that shape the approach:

- Machine stats are partials. `MachineStats.Partial.over(defaults)` merges a datapack file over per machine code defaults held in `private static final MachineStats DEFAULTS` of each block entity (`BurnerExtractorBlockEntity` 80 ticks burner, `CrusherBlockEntity` powered 60/20/8000, `AssemblerBlockEntity` crafter 20/20/8000/2, `CoalGeneratorBlockEntity` generator 40/16000/4, `ResearchLabBlockEntity` and `ChunkLoaderBlockEntity` powered 1/10/4000, `ElectricExtractorBlockEntity` powered 40/20/8000, `BurnerSmelterBlockEntity` burner 80). Raw JSON alone does not give the effective numbers.
- Cycle time rules (read from the tick code):
  - `BurnerMachineBlockEntity.tick`: progress runs to `workTicks()`; fuel is burned only while there is work; one fuel item lasts `getBurnTime(SMELTING) / fuelDivisor` ticks (coal 1600).
  - `PoweredMachineBlockEntity.tick`: consumes `energyPerTick` only on ticks it works.
  - `BurnerSmelterBlockEntity` runs vanilla `RecipeType.SMELTING` recipes at its own `work_ticks`, ignoring the recipe's `cookingtime`.
  - `CrusherBlockEntity.workTicks()` is the crushing recipe `time` (codec default 60).
  - `AssemblerBlockEntity` uses the assembling recipe `time` (codec default 20) and `stats().workTicks()` for plain crafting recipes.
  - `ResearchLabBlockEntity.workTicks()` is the tech's `ticks_per_pack` (default 100).
  - The coal generator runs only while it has room for energy, so its coal use follows FE demand.
- Datagen: `ServusManufacturingDataGen.gather` registers providers; the data run passes `--existing src/main/resources`, so `ExistingFileHelper` can read the hand written machine, deposit and tech files plus vanilla resources.
- Smelting of raw ores uses vanilla recipes that are not in our repo; they must come from the vanilla data on the classpath.
- No git remote is configured locally. PLAN.md names `github.com/Dirnei/servus_mc_mods`, so the site lives at `https://dirnei.github.io/servus_mc_mods/`.
- `m3-extractor-fuel-reserve` (owned by a parallel session) adds `fuel_reserve` to `MachineStats`. This change does not edit that change's files; the exporter writes every `MachineStats` component, so whichever lands second adds the field to the export.

## Goals / Non-Goals

**Goals:**
- One source of truth for numbers: the game's own codecs and defaults produce the data, the site only reads it.
- The ratio math is a pure, tested module that the later planner (option C) can reuse.
- Public pages are static HTML; only the lab ships JavaScript.

**Non-Goals:**
- Running Java or Gradle in the docs pipeline.
- Sharing code between the Java pure logic and the TypeScript ratio module. They are separate implementations of the same rules, pinned by tests on both sides (the reference block GameTest in Java, the reference block Vitest in TypeScript).

## Decisions

### D1: Astro with Starlight, Preact islands for the lab
Starlight gives sidebar, search (Pagefind), dark mode and Markdown/MDX content. The lab pages are MDX pages mounting Preact components with `client:only="preact"`. Alternative VitePress was set aside: equally good for docs, but the lab is an app inside the docs and Astro islands keep the rest static. Preact over React for bundle size; over Svelte to keep a mainstream JSX model.

### D2: Diagram rendering with dagre layout and hand drawn SVG
Chains and the tech graph are directed acyclic graphs (cycles are cut into loop nodes first). `@dagrejs/dagre` computes the layout; a Preact component draws nodes and edges as SVG with theme tokens from Starlight's CSS variables. Alternatives: Mermaid (no rich interactive nodes, poor click handling), React Flow (heavy, React only). Dagre keeps interactivity in our hands at about 30 KB.

### D3: Exporter is a datagen provider in Manufacturing
`GameDataExportProvider implements DataProvider`, registered last in `ServusManufacturingDataGen.gather` under `includeServer()`. It:
1. Reads `machine/*.json`, `deposit/*.json` and `servus_manufacturing/tech/*.json` through `ExistingFileHelper` resources and parses them with the existing codecs (`MachineStats.Partial.CODEC`, `DepositDefinition` codec, `TechDefinition` codec).
2. Merges machine partials with code defaults. The block entities expose their defaults through a package level `MachineDefaults` table (`Map<ResourceLocation, MachineInfo>` with defaults, kind and recipe types); the block entities read their `DEFAULTS` from that table so there is one place.
3. Reads recipes from the recipe provider output of the same run (`src/generated/resources/data/servus_manufacturing/recipe/**`) with the recipe serializers' codecs, which applies `time` defaults. Running last in the provider list guarantees the files exist; `DataGenerator` runs providers sequentially in registration order (verify in `net.minecraft.data.DataGenerator.run` for 1.21.1 during apply).
4. Collects referenced items, then reads vanilla smelting and blasting recipes from `ExistingFileHelper` (`PackType.SERVER_DATA`, namespace `minecraft`, `recipe/*.json`) whose ingredient or result is referenced, repeating once to close the set.
5. Names: Servus names from `ServusManufacturingLanguageProvider`'s entries (exposed as a static map the provider fills), vanilla names from `assets/minecraft/lang/en_us.json` via `ExistingFileHelper` (`PackType.CLIENT_RESOURCES`). If the vanilla lang is not reachable in the data run, fall back to title casing the path and note it in the task.
6. Writes with `DataProvider.saveStable(cache, json, path)` to the path from system property `servus.gamedata.output`, set in `build.gradle` for the Manufacturing data run only to `<root>/docs/src/data/gamedata.json`. If `CachedOutput` rejects a path outside the pack output root, write with `Files.writeString` after building the same sorted JSON.

Alternative of reading raw JSON in TypeScript was rejected because defaults live in Java and would drift.

### D4: gamedata.json shape (schema 1)
```json
{
  "schema": 1,
  "mod_version": "0.1.0",
  "machines": { "servus_manufacturing:burner_extractor": { "kind": "burner", "recipe_types": ["servus:extraction"], "work_ticks": 80, "energy_per_tick": 0, "energy_capacity": 0, "fuel_divisor": 1, "items_per_tick": 0, "recipe_slots": 1 } },
  "recipes": { "servus_manufacturing:assembling/red_pack": { "type": "servus_manufacturing:assembling", "inputs": [{"item": "servus_manufacturing:iron_gear", "count": 1}], "outputs": [{"item": "servus_manufacturing:red_pack", "count": 1}], "time": 100 } },
  "deposits": { "servus_manufacturing:iron": { "block": "servus_manufacturing:iron_deposit", "yield": {"item": "minecraft:raw_iron", "count": 1} } },
  "techs": { "servus_manufacturing:crusher": { "parents": [], "cost": [], "ticks_per_pack": 100, "unlocks": [] } },
  "fuels": { "minecraft:coal": 1600 },
  "items": { "minecraft:raw_iron": { "name": "Raw Iron" } }
}
```
Extraction from deposits is modeled as the pseudo recipe type `servus:extraction` (input none, output the deposit yield) so the ratio math treats extractors like any other machine. `fuels` holds burn times for items accepted as fuel by burner machines that appear in chains (coal, charcoal). Maps are sorted by key; arrays keep source order. The commit hash is not in the file (it would make the CI diff fail every commit); the site reads it at build time from `GITHUB_SHA` or `git rev-parse --short HEAD`.

### D5: Ratio module
`docs/src/lib/ratios.ts`, no DOM or framework imports. Inputs: game data, overrides, drafts, target `{item, perSecond}`, route choices. Output: a graph of nodes `{machine, recipe, rate, count, utilization, fePerTick, coalPerSecond, draft, conflict}` plus totals. Rules from the Context section:
- cycle ticks: recipe `time` for assembling and crushing, `ticks_per_pack` for the lab, machine `work_ticks` otherwise.
- items per second per machine = output count * 20 / cycle ticks.
- count = ceil(needed rate / per machine rate); utilization = needed / (count * per machine).
- powered draw = `energy_per_tick` * utilization per machine; generators needed = ceil(total FE / generator FE per tick); generator coal per second = total FE / (generator FE per tick * fuel burn ticks / `fuel_divisor`) * 20.
- burner coal per second = count * utilization * 20 / (fuel burn ticks / `fuel_divisor`).
- total coal feeds back into the coal deposit node, solved by one fixed point pass (coal for coal extractors is tiny; iterate until the coal rate changes by less than 1e-9, at most 20 rounds).
- default route per item: the first recipe by id among non draft recipes, with a fixed preference list for known items (burner smelting before grit) so the reference block is the default view.

### D6: Overrides, drafts and the proposal file share one model
A `Draft` holds `overrides` (path to value, for example `recipes.servus_manufacturing:assembling/red_pack.time`), `newItems`, `newRecipes`, `newMachines`, `notes` and `routes`. Applying a draft to game data yields effective data used by the ratio module. The proposal file is the draft plus `baseline` values per override, `impact` and metadata:
```json
{ "servus_proposal": 1, "title": "", "data": { "schema": 1, "mod_version": "", "commit": "" },
  "edits": [{ "path": "", "baseline": 0, "value": 0, "note": "" }],
  "new": { "items": [], "recipes": [], "machines": [{ "id": "", "needs_code": true }] },
  "impact": { "target": {}, "before": {}, "after": {} }, "notes": "" }
```
Validation with `zod` schemas, used for import, share links and autosave restore. Conflict: an edit whose `baseline` differs from the current effective value.

### D7: Persistence and sharing
Autosave to `localStorage` key `servus-lab-draft-v1`, wrapped in try/catch; the lab works without storage. Share links put `lz-string` `compressToEncodedURIComponent(JSON)` in the URL hash `#d=`; links over 8000 characters are refused with a pointer to export. The hash never goes to the server.

### D8: Site location, base path and CI
`docs/` with `astro.config.mjs` `site: 'https://dirnei.github.io'`, `base: '/servus_mc_mods'`. `docs.yml`: on push to `main` with `paths: ['docs/**']` and `workflow_dispatch`; jobs `build` (setup-node 22 with npm cache, `npm ci`, `npm test`, `npm run build`, `actions/upload-pages-artifact` of `docs/dist`) and `deploy` (`actions/deploy-pages`, `pages: write`, `id-token: write`, environment `github-pages`). `build.yml` gets a step after the build: `./gradlew :manufacturing:runData` then `git diff --exit-code -- docs/src/data/gamedata.json` with a message naming the file.

### D9: Pure and tested vs manual
- Java, JUnit: the machine defaults table merge (`MachineDefaults` plus `Partial.over`) and the JSON shape writer, given in memory inputs. No Minecraft bootstrap needed beyond `ResourceLocation`, which the existing tests already use.
- TypeScript, Vitest: ratio rules, the reference block, route switching, drafts, cycles, missing sources, research time, proposal export and import round trip, conflict detection, share link encode and decode, schema rejection.
- Manual: data run output review, site build, the lab in a browser at desktop and phone width, the deployed page.
- No in game behavior changes; the existing reference block GameTest still guards the game side.

### D10: PLAN.md
Add `docs/` to the repository layout, and a "Documentation site and Balance lab" item under M3 next to "Balance pass" with a note that the target rate planner follows after the first release.

## Risks / Trade-offs

- [The TS ratio rules drift from the Java machine code] → The reference block test exists on both sides; the design lists each rule with its source method; a later machine change that alters timing must update `ratios.ts` in the same change (noted in the lab spec's ratio requirement).
- [`gamedata.json` goes stale after a hand edit] → CI diff guard in `build.yml`.
- [Vanilla lang or recipes not reachable from the data run] → Fallbacks in D3: title cased names; if vanilla recipes are unreachable, read them from the Minecraft jar on the runtime classpath via `ClassLoader.getResource`.
- [Writing outside the pack output root] → `Files.writeString` fallback in D3.
- [Share links get long] → 8000 character limit with a clear message and export as the path for big drafts.
- [Pages not enabled or no remote] → One manual step for the owner; the workflow fails clearly until Pages is set to GitHub Actions.

## Migration Plan

Not applicable. Nothing is released (see the project note on unreleased content). Rollback is deleting `docs/`, `docs.yml` and the provider.

## Open Questions

- Visual identity of the site (colors, logo). Starlight defaults until a texture or logo exists.
