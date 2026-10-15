# Proposal

## Why

`alchemy_plan.md`, "The stages", decided with the user on 2026-10-15: after Nigredo and Albedo come Citrinitas (smelt: gold, redstone, diamond) and Rubedo (purify: Netherite later). Today the Athanor stops at Albedo, gnomes grow only iron and copper, and the journal ends with Schwaz. This change opens the two upper stages: their Athanor rings, the noble veins, and the journal chapters that lead there, so the later M12 changes (Principle Dropper, salamander, Receptaculum, Prelum) have a stage to hang on.

## What Changes

- Two new shipped Athanor tiers as datapack data:
  - **Citrinitas** (tier 2, yellow flame): twelve Amethyst Blocks standing on the twelve Salt Crystal Blocks of the Albedo ring, one block up on the Athanor's own level.
  - **Rubedo** (tier 3, red flame): eight Crying Obsidian as four pillars, two blocks high, on the four corner Amethyst Blocks. Eight is more than a Ruined Portal usually holds, so the Nether trip (bartering, Bastions) is the normal way.
- Each tier gets its schematic page in the journal Workshop with Visualize, like the Albedo ring; Jade names the stage in words (Citrinitas, Rubedo) instead of the tier's file name.
- New shipped veins for the Gnome Hollow, seeded by tag so other mods' ores grow too: gold (`c:ores/gold`, `c:raw_materials/gold`, yields Raw Gold), redstone (`c:ores/redstone`, `c:dusts/redstone`, yields Redstone Dust) and diamond (`c:ores/diamond`, `c:gems/diamond`, yields Diamond). They take root once the Schwaz chapter is finished, that is once the Wandering years chapter is open. Iron and copper stay as they are.
- Journal: two new chapters after Schwaz, with tasks of the two allowed kinds using existing events:
  - **Wandering years** (Citrinitas): one craft on a Citrinitas Athanor, bind a gnome to a gold vein, bind four gnomes.
  - **Basel and Salzburg** (Rubedo): one craft on a Rubedo Athanor, bind a gnome to a diamond vein, finish sixteen Athanor crafts.
  - Each chapter has an advancement, a research node that unlocks its journal entry and its Workshop entries (the existing advancement to Modonomicon bridge), and short placeholder story pages in Paracelsus' ranting voice, marked `[storyteller]` for the storyteller.
- Workshop entries "The Yellow Ring" (Citrinitas, with the noble veins) and "The Red Crown" (Rubedo).
- Nigredo goods: Clay Ball and Bone Meal from the principles at the Coagulator already ship; this change specifies them and covers them with GameTests.
- Docs: Alchemy page, datapack reference for tiers, veins and journal.

This change builds on the applied but unarchived `m12-hidden-hollow` (seeding by hand under Elemental Sight) and `m12-gnome-gifts`; its deltas are written against the main specs plus those deltas. The user allowed touching the same specs for this run.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `alchemy/athanor`: the shipped tiers now go up to Rubedo (Citrinitas Amethyst ring, Rubedo Crying Obsidian pillars), Jade names the stage.
- `alchemy/gnome-hollow`: shipped noble veins (gold, redstone, diamond) seeded by tag, gated by the Schwaz chapter.
- `alchemy/journal`: chapters Wandering years and Basel and Salzburg with their entries, advancements and unlocks; Citrinitas and Rubedo Workshop entries and schematics; placeholder story text.
- `alchemy/coagulator`: Clay Ball and Bone Meal as the Nigredo goods.

## Impact

- Data: `data/servus_alchemy/servus_alchemy/athanor_tier/citrinitas.json`, `rubedo.json`; `vein/gold.json`, `redstone.json`, `diamond.json`; `journal/wandering_years.json`, `basel_salzburg.json`.
- Code: `athanor/AthanorTierData` (shipped list), `integration/jade/ServusAlchemyJadePlugin` (stage name), datagen `JournalAdvancements`, `book/JournalResearch`, `book/OpusCategory`, `book/JournalEntries`, `book/WorkshopCategory`, `ServusAlchemyLanguageProvider`; generated resources via `runData`.
- Tests: `AthanorTiersTest`, `AthanorLayoutTest`, `JournalLedgerTest`; GameTests in `AlchemyGameTests`, `HollowGameTests`, `CoagulatorGameTests`.
- Docs: `docs/src/content/docs/mods/alchemy.md`, `reference/datapack/athanor.md`, `veins.md`, `journal.md`.
