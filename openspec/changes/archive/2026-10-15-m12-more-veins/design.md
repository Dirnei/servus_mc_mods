# Design

## Context

See proposal.md for the why. Since `m12-vein-seeds` a vein is one JSON file in `data/<ns>/servus_alchemy/vein/` (yield, colour, steps, step ticks, step cost, yield ticks), read by `VeinData` and synced to clients by `VeinSync`. Its Seed is one `servus_alchemy:seed` item with the `vein` component; the Athanor recipe for it is a row in `AlchemyRecipeTable.athanor()` built by `seed(vein, inputLabel, input, principles, tier)`. The same table feeds the recipe provider, the Journal's seed texts (`seedText(tier)` in `WorkshopCategory`) and the docs game data. Vein names come from `vein.servus_alchemy.<name>` in `ServusAlchemyLanguageProvider`. The creative tab and JEI list one Seed per loaded vein, so new veins appear there without code.

## Goals / Non-Goals

**Goals:**
- Three more veins and Seeds with data only, plus datagen rows and tests.

**Non-Goals:**
- Any change to the Hollow, the gnome or the renderer code.

## Decisions

### Numbers (example values in data)

| Vein | Yield | Colour | Steps × ticks | Cost per step | Yield every | Seed from | Seed principles (Salt, Sulfur, Mercury) | Tier |
|---|---|---|---|---|---|---|---|---|
| `coal` | Coal | `#5c5c66` | 15 × 100 | 60 Salt, 20 Sulfur | 100 ticks | `minecraft:coal` | 150, 150, 0 | 1 Albedo |
| `quartz` | Nether Quartz | `#ece4d8` | 30 × 100 | 150 Salt, 50 Mercury | 200 ticks | `#c:gems/quartz` | 250, 0, 250 | 2 Citrinitas |
| `emerald` | Emerald | `#2fd66a` | 40 × 100 | 200 Salt, 50 Sulfur, 100 Mercury | 1,200 ticks | `#c:gems/emerald` | 500, 500, 500 | 3 Rubedo |

- Coal is the cheapest vein (fewer steps, cheaper steps, a yield every 100 ticks), because Coal is common and burns away. Quartz matches redstone's place at Citrinitas. Emerald mirrors diamond.
- Coal's Seed uses the item `minecraft:coal`, not `#minecraft:coals`: that tag also holds Charcoal, which would make the coal vein a free loop from trees.
- Quartz and Emerald use the common `c:` gem tags, like the other Seeds, so other mods' quartz and emeralds make the same Seed.
- Colour: coal's material must stay visible against the dark rock layer of the sight renderer, so it is a light slate grey with a blue tint rather than black. The play check confirms it reads.

### Where each part goes
- Vein files: `alchemy/src/main/resources/data/servus_alchemy/servus_alchemy/vein/{coal,quartz,emerald}.json`, hand-written like the five existing ones.
- Seed recipes: three `seed(...)` rows in `AlchemyRecipeTable.athanor()`, ordered by tier (coal first among Albedo, quartz after redstone, emerald after diamond). `runData` writes `recipe/athanor/seed_of_{coal,quartz,emerald}.json` and refreshes `docs/src/data/gamedata.json`.
- Names: `vein.servus_alchemy.coal` "Coal", `.quartz` "Quartz", `.emerald` "Emerald".
- Journal: nothing by hand; `seedText(1..3)` lists the new Seeds from the table.

### Docs
`reference/datapack/veins.md` still documents `samples` and `chapter`, removed by `m12-vein-seeds`. Its field table and the shipped-vein table are rewritten for the current format: id, yield, colour, ripening, yield time, and the Seed with its input and Athanor stage, for all eight veins. The Gnomes steps in `mods/alchemy.md` (call the gnome with a Salt Crystal, plant a Seed, swap with another Seed) are rewritten the same way.

### Pure Java and GameTests
- No new pure logic. `VeinFilesTest` (JUnit) gets the three new ids in its list of shipped veins, so their files must parse.
- GameTests in `HollowGameTests`: `seedOfCoalOnAlbedo`, `seedOfQuartzNeedsCitrinitas`, `seedOfEmeraldNeedsRubedo`, following `seedOfGoldNeedsCitrinitas`. `everyShippedSeedPlants` loops over the loaded veins and covers the new ones.
- Manual: the colour of each new vein in the cracks under the sight, and the Seeds' tint.

### APIs used
None new; `Tags.Items.GEMS_QUARTZ` and `Tags.Items.GEMS_EMERALD` exist in NeoForge 26.1.2.116 (`net.neoforged.neoforge.common.Tags`).

## Risks / Trade-offs

- [Coal's colour may read poorly on the dark rock] → light slate grey; adjust in the JSON after the play check without code.
- [A free coal loop] → the Seed needs real Coal plus principles, and the vein costs principles per step; Charcoal does not make the Seed.
