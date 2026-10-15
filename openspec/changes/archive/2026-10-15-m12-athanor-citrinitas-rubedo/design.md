# Design

## Context

See proposal.md for the why. The Athanor tier system is already generic: `AthanorTierData` loads `servus_alchemy/athanor_tier/*.json` (codec allows tier 1 to 3), pure `AthanorTiers.tierOf` requires every lower tier, `AthanorBlock.TIER` goes 0 to 3, and the block state provider already makes the formed and unformed models of all four tiers from `athanor_front_<n>[_lit]` textures, whose flames are already black, white, yellow and red. Only `albedo.json` ships, and `AthanorTierData.SHIPPED` lists the files the multiblock provider turns into `athanor_<stage>` schematics. The journal bridge: a chapter JSON is counted by `JournalLedger`; finishing it awards the advancement `servus_alchemy:journal/<chapter>` (one per entry of `JournalAdvancements.CHAPTERS`); `JournalResearch` turns an advancement into a Modonomicon research node, and entries carry `condition().researchNodeUnlocked(node)`. Today only the Villach node exists. Coagulation of Clay Ball and Bone Meal already ships in `AlchemyRecipeTable.coagulation()`.

Earlier changes applied but not archived: `m12-hidden-hollow` (seeding by hand under the sight, `GnomeHollowBlockEntity.mayGrow` checks the vein's chapter) and `m12-gnome-gifts`. This change reads both as the base.

## Goals / Non-Goals

**Goals:**
- Citrinitas and Rubedo tiers as data, with schematics, Jade names and GameTests.
- Gold, redstone and diamond veins by tag.
- Two chapters with tasks, advancements, research nodes, entries and placeholder story.

**Non-Goals:**
- Any Citrinitas or Rubedo Athanor craft (Ignis Perpetuus comes with `m12-salamander`, the Sealed Flask with `m12-receptaculum`).
- New textures or models: the yellow and red flames exist.
- New journal event kinds.

## Decisions

### D1 The Citrinitas ring stands on the salt ring
Citrinitas is twelve `minecraft:amethyst_block` at the twelve Albedo offsets one block up (`dy` 0). These cells are corners of the 5 by 5 square, never next to the Athanor, never a tank (tanks stand on the diamond tips) and never a duct (ducts are one block higher). The ring rises as a low wall around the hearth corners and leaves the front tip open for the player.
- Alternative: Amethyst Clusters on the salt blocks. Rejected: clusters need Silk Touch and a budding block, and the plan names the Shard as the material; four shards make a block.
- Alternative: an Albedo Athanor craft that makes Amethyst Shards. Rejected for now: geodes are common in the Overworld and the plan's Citrinitas trip is the Nether. Recorded as an assumption.

### D2 Rubedo is four Crying Obsidian pillars
Rubedo is `minecraft:crying_obsidian` at `(±2, 1, ±2)` and `(±2, 2, ±2)`: eight blocks, two high on the corner Amethyst Blocks. The duct level (`dy` 1) holds ducts only on the axes, so the corners are free. Eight is more than a Ruined Portal usually holds, so bartering or Bastion chests are the normal way. No recipe of ours makes Crying Obsidian.
- Alternative: twelve on the duct level. Rejected: needs about 70 gold in bartering; eight keeps the trip without grinding.

### D3 Shipping the tiers
`citrinitas.json` (tier 2, `#F2C531`) and `rubedo.json` (tier 3, `#B3261E`) under `data/servus_alchemy/servus_alchemy/athanor_tier/`; `AthanorTierData.SHIPPED` becomes `albedo`, `citrinitas`, `rubedo`, so `ServusAlchemyMultiblockProvider.athanors()` writes `athanor_citrinitas` and `athanor_rubedo` with every lower tier's blocks. `AthanorLayout.schematic` already pads any height; the new symbols come out as `C` (salt), `E` (amethyst), `F` (crying obsidian).

### D4 Jade shows the stage name
The Jade Athanor line passes `Component.translatableWithFallback("gui.servus_alchemy.athanor.stage." + name, name)` instead of the raw file name; the four stage keys already exist for JEI. A datapack tier with an unknown name falls back to its file name.

### D5 Noble veins
Shipped JSON (example numbers):

| vein | samples | yield | steps | step_ticks | step_cost | yield_ticks |
|---|---|---|---|---|---|---|
| gold | `c:ores/gold`, `c:raw_materials/gold` | Raw Gold | 30 | 100 | 150 Salt, 50 Mercury | 300 |
| redstone | `c:ores/redstone`, `c:dusts/redstone` | Redstone | 30 | 100 | 100 Salt, 100 Mercury | 200 |
| diamond | `c:ores/diamond`, `c:gems/diamond` | Diamond | 40 | 100 | 200 Salt, 100 Mercury, 50 Sulfur | 1200 |

All three carry `"chapter": "servus_alchemy:schwaz"`: like iron, which needs the chapter before the one that asks to bind it, they take root as soon as Wandering years opens. The store counts one yield item per yield, so redstone gives one dust per yield and a faster yield time instead of a stack.

### D6 Chapters
`wandering_years.json` (parent Schwaz, order 2):
- `citrinitas_craft`: `athanor`, `tier:citrinitas`, 1
- `gold`: `bind`, `minecraft:raw_gold`, 1
- `gnomes`: `bind`, no filter, 4

`basel_salzburg.json` (parent Wandering years, order 3):
- `rubedo_craft`: `athanor`, `tier:rubedo`, 1
- `diamond`: `bind`, `minecraft:diamond`, 1
- `crafts`: `athanor`, no filter, 16

`JournalAdvancements.CHAPTERS` gets `wandering_years` (icon Raw Gold) and `basel_salzburg` (icon Crying Obsidian). `JournalResearch` gets nodes `journal/schwaz` (opens Wandering years and The Yellow Ring) and `journal/wandering_years` (opens Basel and Salzburg and The Red Crown), each declared from `onAdvancementEarned` of the chapter's advancement, like Villach. Basel and Salzburg needs no node until a later chapter follows it. `OpusCategory` adds entries `w` and `b` after `s`, parents and conditions on the previous chapter's node. Lang keys `journal.servus_alchemy.<chapter>` and `.finished` in `ServusAlchemyLanguageProvider`.

### D7 Workshop entries
`WorkshopCategory` adds, under the Salt Crystal Block entry column:
- `amethyst_ring` "The Yellow Ring" (icon Amethyst Block, condition Schwaz node): spotlight, how to build, multiblock `athanor_citrinitas`, a page on the noble veins (gold, redstone, diamond by ore or sample, Schwaz finished).
- `crying_obsidian` "The Red Crown" (icon Crying Obsidian, condition Wandering years node): spotlight, where Crying Obsidian is found, multiblock `athanor_rubedo`.
The Athanor entry's "Its Fire" page names the yellow and red flames.

### D8 Placeholder story
Story pages go through Modonomicon datagen into the lang file. Each starts with `[storyteller]` and is a few sentences of a ranting, confused diary. Historical claims used (to check by the storyteller, not in game text as fact):
- Paracelsus wandered across Europe for years after his studies, about 1517 to 1524, also as an army surgeon (to check).
- He became town physician of Basel and lecturer at its university in 1527, lectured in German instead of Latin and burned a book of the old medicine (Avicenna's Canon, or Galen) in the St John's Day fire (to check).
- He left Basel in 1528 after quarrels with the town and the faculty (to check).
- He died in Salzburg in 1541 (to check).

## Risks / Trade-offs

- [Gold can be grown before the Amethyst ring stands] → Same as iron before the salt ring; the chapter asks for both. A stricter gate would need a tier gate on veins, recorded as an assumption.
- [Redstone also coagulates in Nigredo] → The existing recipe stays; the vein is the bulk source. Recorded as an assumption.
- [Placeholder text ships in game] → The `[storyteller]` marker makes it obvious; the storyteller replaces the lang strings in datagen.

## Assumptions to confirm with the user

- Citrinitas ring: twelve Amethyst Blocks on the twelve Salt Crystal Blocks (48 shards); shards only from vanilla geodes, no Athanor craft for them.
- Rubedo: eight Crying Obsidian as four two block pillars on the corner Amethyst Blocks; only vanilla sources.
- Flame colours stay the existing yellow and red front textures; tier colours `#F2C531` and `#B3261E` in the data.
- Gold, redstone and diamond take root once Schwaz is finished (Wandering years open), not after the Citrinitas ring stands.
- Vein numbers as in D5; redstone yields one dust per yield (200 ticks), diamond one per 1,200 ticks.
- Samples: raw gold or any gold ore; redstone dust or any redstone ore; a diamond or any diamond ore.
- Wandering years tasks: Citrinitas craft, bind a gold gnome, bind four gnomes. Basel and Salzburg tasks: Rubedo craft, bind a diamond gnome, sixteen Athanor crafts.
- The Nigredo goods Clay Ball and Bone Meal keep their shipped recipes; the existing Nigredo Redstone and Glowstone coagulations stay.
- Story pages are placeholders marked `[storyteller]`; historical claims in D8 are to check.
