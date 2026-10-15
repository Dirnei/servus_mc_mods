# Design

## Context

See proposal.md for the why. The Salamander Forge (`m12-salamander-forge`) set the pattern for a single block alchemy machine on Foundation's machine base (`MachineBlock`, `MachineBlockEntity`, `MachineMenu`, `MachineScreen` with a `Layout`): facing, lit state, side configuration with the front locked off, slot role rules for automation, output push, carrot configure and dismantle, drops, data driven stats from `data/<ns>/machine/<name>.json`, item capability and production capability registered in `ServusAlchemyEvents`. Alchemy's own recipe types (`calcination`, `purification`, `coagulation`, `athanor`) are records implementing `AlchemyRecipe.CountedInput` with a `MapCodec` and a `StreamCodec`, registered in `ServusAlchemyRecipes`, synced to clients with `ServusRecipes.syncToClients`, written by datagen from `AlchemyRecipeTable` (which the Workshop also reads) and shown in JEI through `AlchemyRecipeCategory`. The journal counts events by name through `Journal.recordItem`; chapters and tasks are data in `data/servus_alchemy/servus_alchemy/journal/`.

## Goals / Non-Goals

**Goals:**
- Caput Mortuum into Netherite Scrap at a slow, data driven pace, without fuel.
- A one-time Dragon's Breath cost that also requires Rubedo.
- The Foundation machine behaviour, the production capability, JEI and the Workshop.
- A screw press look with a plate that moves while pressing.

**Non-Goals:**
- Netherite Ingots (vanilla smithing turns scrap and gold into ingots), any other pressing recipe, a speed upgrade, upkeep, principles per press.
- Any Elemental Sight interaction: the Prelum is not hidden.

## Decisions

### D1 Pressing recipe type
`prelum/PressingRecipe(SizedIngredient input, ItemStackTemplate resultTemplate, int time) implements AlchemyRecipe.CountedInput`, JSON `{"type": "servus_alchemy:pressing", "input": {"ingredient": ..., "count": 16}, "result": {"id": ...}, "time": 600}`, `time` required, so the shipped JSON shows it. `assemble` returns the result. Registered as `PRESSING` in `ServusAlchemyRecipes` and synced to clients in `ServusAlchemy`. Alternative considered: input count and result in the machine stats JSON. Rejected: stats have no item fields, and a recipe type gives JEI and datapacks the familiar shape of the other alchemy machines.

### D2 Datagen table
`AlchemyRecipeTable.pressing()` returns one `Press("netherite_scrap", "Caput Mortuum", Input.of(CAPUT_MORTUUM), 16, NETHERITE_SCRAP, 600)` row; the recipe provider writes `recipe/pressing/netherite_scrap.json` from it and the Workshop reads it for its text.

### D3 Classes
New package `prelum`: `PrelumBlock extends MachineBlock` (block entity type), `PrelumBlockEntity extends MachineBlockEntity implements TeamOwned` (2 slots: 0 input, 1 output; `TeamOwnership` saved like the Athanor) and `PrelumMenu extends MachineMenu` (input at 56,35, output at 116,35). Registered in `ServusAlchemyBlocks` (strength like the Forge, sound stone, `noOcclusion` since the model is not a full cube), `ServusAlchemyItems` (block item and the `prelum_screw` item, creative tab after the Receptaculum and the Sealed Flask), `ServusAlchemyBlockEntities`, `ServusAlchemyMenus`.

### D4 Pressing logic
Each tick the block entity looks up the first pressing recipe matching its input (`ServusRecipes.first`, which checks the count through `CountedInput.matches`). With a recipe and room for its result in the output, `progress` advances; at the recipe's `time` it extracts the recipe's count, inserts the result, records `press` for the team and starts over. Without, progress resets. `tick()` returns whether a recipe runs, which drives `LIT`. The menu's `DATA_WORK_TICKS` reports the current recipe's time, so the arrow fills at the right pace. `acceptsInput` asks every pressing recipe `accepts(stack)` through `ServusRecipes.all`, which reads the synced recipes on the client, so the menu slot and automation agree.

### D5 No fuel, no principles
The Prelum runs on nothing. It is the reward at the end of the Work; the cost is the Caput Mortuum (example 16 per scrap, made 1 per 4 Ash) and the slow press (example 30 s). The machine stats keep `work_ticks` 600 in `prelum.json` as documentation of the default only; the recipe's `time` decides.

### D6 Sides and capabilities
Foundation's default side configuration: every side Auto, the front locked off, like the Forge. `ServusAlchemyEvents` registers `Capabilities.Item.BLOCK` (`sideHandler(side)`) and `ServusFoundationCapabilities.PRODUCTION`. The production provider maps each pressing recipe to `ProductionRecipe(id, [first accepted item x count], [result], time)` and reports the recipe matching the input slot as active. No `UPKEEP` capability.

### D7 Screen
`MachineScreens.PRELUM = new Layout(false, true, false, gui("prelum"))`: arrow, no flame, no energy bar, with the side panel. `textures/gui/prelum.png` is the Forge's GUI with the input slot frame moved from y 17 to y 35 and the flame area cleared, made by a script in the job directory.

### D8 Built once with Dragon's Breath
Options were a crafting recipe with Dragon's Breath, a Rubedo Athanor craft, or consecrating a placed block. Chosen: a Rubedo Athanor craft (tier 3) of a **Prelum Screw** from one Dragon's Breath with 500 Sulfur and 500 Mercury, then a crafting table recipe `_S_ / LPL / DDD` (S Prelum Screw, L Stripped Dark Oak Log, P Heavy Weighted Pressure Plate, D Polished Deepslate). It follows the family's pattern (the Athanor makes the special part, Ignis Perpetuus, the crafting table builds the machine), it puts the End (Dragon's Breath) and Rubedo (the Athanor tier) both in front of the machine, and it needs no new interaction. The screw is a normal item with its own texture.

### D9 Look
Block model `prelum` (static frame), rotated by facing; front faces north (z 0). Boxes in pixels:
- base, polished deepslate, `(0,0,0)-(16,3,16)`;
- press bed, steel, `(3.5,3,3.5)-(12.5,4,12.5)`;
- posts, stripped dark oak log, `(1,3,6)-(4,9,10)` and `(12,3,6)-(15,9,10)`;
- head beam, stripped dark oak log, around a 4 px hole at x/z 6..10: `(1,9,5)-(6,11.5,11)`, `(10,9,5)-(15,11.5,11)`, `(6,9,5)-(10,11.5,6)`, `(6,9,10)-(10,11.5,11)`.

`PrelumRenderer` draws the moving parts with the steel sprite on every face, centred on x/z 8, at plate height `py` (bottom of the plate): plate `(4.5,py,4.5)-(11.5,py+1.5,11.5)`; screw shaft 2.5 px square from `py+1.5` to `py+7.5`, turned with the bar; bar `(-5,0,-0.5)-(5,1,0.5)` around the centre at `py+7.5`, with knobs `(5,0,-0.9)-(6.5,1.4,0.9)` at both ends, turned about y. Idle `py` is 7.05 (plate up); pressing, `py` runs from 7.05 down to 4.05 over 60 ticks and back over 20 ticks, from game time while `LIT`, and the bar turns 360 degrees on the way down and back on the way up. The item model `prelum_item` is the frame plus these parts at `py` 7.05 as static elements.

Overlap check:
- Base and bed: bed bottom on base top at y 3, opposite faces; bed sides x/z 3.5, 12.5 inside the base outline.
- Posts on base top at y 3 (opposite faces); post tops at y 9 under the beam's bottom (opposite faces). Post outer faces x 1 and x 15 meet the beam's outer faces x 1 and x 15 only along the edge y 9.
- Beam pieces meet each other at x 6 and x 10 with opposite faces; their top faces at y 11.5 touch only along edges.
- Plate x/z 4.5..11.5 keeps 0.5 px from the posts' inner faces (x 4 and 12); at its lowest its bottom is at y 4.05, 0.05 px above the bed top; at its highest its top is at 8.55, under the beam (9).
- Shaft, turned, reaches at most 1.77 px from the centre, inside the 2 px hole: never touches the beam. Its bottom face lies on the plate's top face (opposite faces); its top meets the bar's bottom at the same height with opposite faces; both turn together.
- Bar at its lowest has its bottom at y 11.55, 0.05 px above the beam top; its radius with the knobs is 6.5 px, so it never reaches outside the block. Knobs meet the bar ends at x ±5 with opposite faces; their bottoms lie level with the bar's bottom but beside it, not over it; they stick out up and to the sides.
- Item model: the same boxes at rest; the shaft there is not turned.

### D10 Journal
`PrelumBlockEntity` calls `Journal.recordItem(server, team(), "press", result, result.getCount())`. `basel_salzburg.json` gains `{"id": "scrap", "event": "press", "filter": "minecraft:netherite_scrap", "count": 1}`. The Basel and Salzburg task page lists "Press a Netherite Scrap at the Prelum". The "What Follows" page stays.

### D11 Workshop, JEI, docs
`WorkshopCategory` adds `prelum` "The Prelum" (icon the Prelum) right of "The Crying Obsidian" crown entry, parent the red crown entry, condition Wandering years (the Basel and Salzburg node, like the Receptaculum): a story text marked `[storyteller]`, a spotlight of the Prelum Screw with its Athanor craft, the crafting page and a text on using it. JEI: `PressingRecipeCategory` on `AlchemyRecipeCategory` (input with count, arrow and time, result) with the Prelum as catalyst. Docs: a Prelum section on the Alchemy page, the pressing recipe in the datapack recipe reference and a machine row.

## Risks / Trade-offs

- [A free source of Netherite] → It needs the End, a Rubedo Athanor and 16 Caput Mortuum (64 Ash) per scrap at 30 s each; four scraps and four gold make one ingot.
- [The animation does not follow the real progress] → Only `LIT` reaches the client; the plate runs a fixed cycle from game time while pressing. Enough for a slow press; the screen shows the true progress.
- [Pressing recipes with tag ingredients] → The production capability offers the first accepted item only.

## Assumptions to confirm with the user

- Output Netherite Scrap, not ingots; 16 Caput Mortuum per scrap, 600 ticks per press (recipe JSON).
- The Prelum needs no fuel and no principles to run.
- The Dragon's Breath gate: a Rubedo Athanor craft of a Prelum Screw (1 Dragon's Breath, 500 Sulfur, 500 Mercury), then a crafting recipe `_S_ / LPL / DDD` (Stripped Dark Oak Log, Heavy Weighted Pressure Plate, Polished Deepslate). No "consecrating" interaction.
- Look: polished deepslate base, dark oak posts and beam, steel screw, bar and plate; the plate runs a fixed 80 tick cycle while pressing, not the true progress.
- Sides: Foundation's default (every side Auto, front closed).
- Journal: Basel and Salzburg gets a fourth task "press a Netherite Scrap"; the Workshop entry opens with Basel and Salzburg; its first page is a placeholder marked `[storyteller]`.
- Historical claims to check: "prelum" as the Latin word for a wine or oil press, used for the press in the Workshop text.
