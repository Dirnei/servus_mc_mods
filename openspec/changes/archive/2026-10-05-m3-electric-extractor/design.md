# Design

## Context

Burner extractor, powered machine base, assembling recipes with a `tech` field and research all exist. See proposal.md.

## Decisions

### D1: Shared deposit lookup
`DepositExtraction.yieldBelow(level, pos)` returns the deposit yield under a position; both extractors use it.

### D2: ElectricExtractorBlockEntity
Extends `PoweredMachineBlockEntity` with one output slot (index 0). `canWork` is deposit present and output accepts the yield; `finishWork` inserts the yield. Defaults `MachineStats.powered(40, 20, 8000)`, stats file `electric_extractor.json`.

### D3: GUI
Layout `ELECTRIC` on a generated `electric_extractor.png`: output slot at 116/35, progress arrow, energy bar, no flame. Menu `ElectricExtractorMenu` with the output slot only.

### D4: Recipe and gating
`AssemblingRecipeBuilder.tech(ResourceLocation)` sets the optional field. Recipe `assembling/electric_extractor`: 1 Burner Extractor, 4 Iron Gear, 6 Copper Wire, 200 ticks, tech `servus_manufacturing:electric_extractor`. The tech's `unlocks` entry is updated to this id. No hand crafting recipe exists, so the tech is the only way in.

## Pure Java vs Minecraft

All Minecraft. Verified over RCON: electric extractor on a deposit next to a generator yields raw iron at the expected rate; without a deposit the energy buffer stays full. Gating is covered by the existing add refusal path and the unit tested research rules.

## Risks / Trade-offs

- [Burner extractor as ingredient consumes a placed machine's worth] -> Intended: upgrading replaces the burner tier, a Factorio pattern.
