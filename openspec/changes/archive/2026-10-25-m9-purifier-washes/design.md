# Design

## Context

- `PurificationRecipe(input, water, salt, Optional residue, time)` already allows a Salt of 0 and no residue in its codec. `PurifierBlockEntity.canRun` checks `capacity - amount >= salt`, which holds for 0, and `finish` fills an empty `FluidStack` for 0, which the tank ignores. So the machine needs no rule change for washes.
- The residue slot takes any item (`isItemValid` is true for slot 1) and `residueFits` checks same item, same components and the stack size. The Crock's port gives slot 1 whatever it holds.
- What assumes Ash and Caput Mortuum: `PurifierRenderer` draws the floor with `light_gray_concrete_powder` and the heap with `brown_concrete_powder`; the Jade provider sends only counts and labels them "Ash: %s" and "Caput Mortuum: %s"; `PurificationRecipeCategory` shows the input, the water and the Salt but no residue; `AlchemyRecipeTable.Purify` requires a residue and the book line always prints Salt and residue.
- `CalcinerBlockEntity.saltPerAsh` takes the first purification recipe whose input accepts an Ash stack; the new recipes take no Ash, so it keeps 400 / 4 = 100.
- `Journal.record` ignores amounts of 0, so a wash's `purify` event with 0 Salt counts nothing already.
- Existing item textures (`salt_crystal`, `sulfur`, `quicksilver`) are 16 x 16 shapes with three tones, a dark outline tone, a body tone and a highlight; `caput_mortuum` and `ash` are heaps with more noise.

## Goals / Non-Goals

**Goals:**
- Any item can be washed and left behind, and the build shows what it holds.
- The table in `AlchemyRecipeTable` stays the single source for recipes and the book.

**Non-Goals:**
- Changing how a cycle runs, the tanks, the ports or forming.
- Uses for the new items (next change).

## Decisions

**Items.** `KANDIS`, `BRIMSTONE` and `CINNABAR` are `QuoteItem`s like Sal Ammoniac, each with one line in Paracelsus' voice that hints it may seed something ("Sugar that remembers it was Salt."), registered after Quicksilver and placed after it in the creative tab. No crafting recipe, no tag.

**Textures.** A generator `tools/purifier-textures/gen.py` writes `kandis.png`, `brimstone.png` and `cinnabar.png` in the three tone style of the principle items, so the shapes stay editable:
- Kandis: three clear cubic sugar crystals stacked, pale amber with near white highlights and an amber outline tone.
- Brimstone: a rough lump of yellow rock, faceted, darker ochre in the cracks, so it reads as stone next to the smooth Sulfur powder.
- Cinnabar: red crystals growing out of a grey rock base, vermilion with a dark red outline.
`--preview out.png` writes an 8x preview for checking.

**Recipe table.** `AlchemyRecipeTable.Purify` gets a nullable residue (`Optional` in the recipe) and stays one row per recipe. `purification()` lists Ash, the three seed recipes and the four Salt recipes. A new `record Wash(String kind, String label, String residueLabel, List<Purify> rows)` and `washes()` build one `Purify` row per colour with a loop over `DyeColor` and the item ids `<colour>_concrete_powder` to `<colour>_concrete`, `<colour>_wool` to `white_wool` (white skipped), `<colour>_carpet` to `white_carpet` (white skipped) and `<colour>_terracotta` to `terracotta`. Items are looked up in `BuiltInRegistries.ITEM` by id. `allPurification()` is both lists flat; the recipe provider writes it as `purification/<name>` with names like `wash_red_concrete_powder`.

Example numbers, all datapack data:

| Recipe | In | Water | Salt | Residue | Ticks |
|---|---|---|---|---|---|
| Ash | 4 Ash | 400 | 400 | 1 Caput Mortuum | 160 |
| Sugar | 8 Sugar | 500 | 100 | 1 Kandis | 160 |
| Gunpowder | 4 Gunpowder | 400 | 200 | 1 Brimstone | 160 |
| Redstone | 8 Redstone Dust | 400 | 50 | 1 Cinnabar | 160 |
| Dried Kelp | 4 Dried Kelp | 250 | 200 | none | 80 |
| Rotten Flesh | 4 Rotten Flesh | 250 | 100 | 1 Leather | 120 |
| Soul Sand | 1 Soul Sand | 250 | 25 | 1 Sand | 80 |
| Magma Cream | 1 Magma Cream | 250 | 25 | 1 Slimeball | 80 |
| Washes | 8 items | 250 | 0 | 8 washed items | 40 |

**Book lines.** A pure `alchemy/PurificationText` (no Minecraft imports, JUnit `PurificationTextTest`) formats `line(count, label, water, salt, residueCount, residueLabel)` as "8 Sugar, 500 mB water: 100 Salt, 1 Kandis", leaving out the Salt when it is 0 and the residue when there is none ("4 Dried Kelp, 250 mB water: 200 Salt"), and `kindLine(count, label, water, residueLabel)` as "8 Concrete Powder of any colour, 250 mB water: its Concrete". The Purifier entry's "What It Washes" list prints every `purification()` row with `line` and every `washes()` kind with `kindLine`. The Leaching text page mentions that the Vat takes more than Ash.

**JEI.** `PurificationRecipeCategory` keeps input at x 1 and water at x 21, the arrow at x 46; outputs from x 80: the Salt slot only when Salt is above 0, then the residue slot. The category is 120 wide, so two outputs fit (80 and 100).

**Renderer and sync.** `PurifierBlockEntity.getUpdateTag` sends the input and residue items' registry ids next to the counts (`ShownItem`, `ShownResidue` as strings), and `syncBrine` also resends when the item kind changes. `PurifierRenderer` resolves a sprite per item: for a `BlockItem` the particle sprite of its block's default state (`Minecraft.getModelManager().getBlockStateModelSet().get(state).particleMaterial()`, the same call the facade panels use), otherwise the old ash or residue sprite. The boxes and their positions do not change, so no new overlapping faces come in.

**Jade.** The Purifier provider sends the input and residue item ids; the tooltip uses one key `jade.servus_alchemy.purifier.item` "%2$s: %1$s" with the item's name, for both lines, and shows a line only while the slot holds something. The two old keys go.

**Verification.** GameTests in `PurifierGameTests`: `purifierGrowsKandisFromSugar` (8 Sugar and 500 mB water give 100 mB Salt and one Kandis) and `purifierWashesConcreteWithoutSalt` (8 Red Concrete Powder and 250 mB water give 8 Red Concrete, the Salt tank stays empty, the team's Villach `purify` count stays 0), and `purifierRefusesPlainWool`. The Ash test stays and guards the Ash Pan rule together with the Calciner tests.

## Risks / Trade-offs

- [Concrete in bulk is cheap with a Purifier] → it costs water and time and the Crock holds one stack; vanilla already turns powder into concrete with any water.
- [A modded item that is a `BlockItem` with a strange model] → the particle sprite is what breaking particles use, so it always exists.
- [Kandis and the others have no use until the next change] → their quotes hint at it, and the next change follows directly.
