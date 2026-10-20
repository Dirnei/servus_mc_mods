## Why

The Tiled Stove and the Brewhouse burn wood or coal only, so a player with a power grid still has to feed every kitchen and brewhouse by hand or by pipe. PLAN.md, Open: M14 Delicacies, `m14-heating-element`: the **Heating Element** upgrade lets a firebox take FE through a small iron power port instead of burning fuel, with coils over the door and a brass thermometer that climbs while it preheats; FE is the shared interface, no heat API; the Alchemy Calciner never takes it. Both fireboxes exist now (`m14-tiled-stove`, `m15-brewhouse`) and keep the `HeatSource` seam this change fills.

## What Changes

- **Heating Element** (Foundation item): crafted from copper ingots, redstone and an iron ingot, stacks to 16, with a tooltip. Its own kind: it is not a speed upgrade and not a Capacity Upgrade, so upgrade slots and capacity slots refuse it. Sprite `heating_element` from the mockup.
- **Installing** (Tiled Stove and Brewhouse): a right click on any formed tile or brick with the element installs it (one per firebox); a sneaking empty hand click on a formed tile or brick takes it out; it drops when the stove or Brewhouse is broken or unformed. No GUI.
- **Electric heat**: with the element the firebox burns no fuel and refuses new fuel (fuel already in the slot waits for the wood fire). It takes FE (`Capabilities.Energy`) into a small buffer through its power port. While a cooking spot asks for heat and the buffer holds enough FE, it draws FE every tick and its temperature rises; spots cook only once the temperature reached a threshold; without power or demand it cools slowly. FE per tick, buffer size, heat up time, cool down time and threshold come from datapack JSON per firebox (`data/servus_delicacies/heating/tiled_stove.json`, `data/servus_brewery/heating/brewhouse.json`).
- **Power port**: a small flat iron port on the right end face (seen from the front) of the stove and of the Brewhouse's kiln side, shown only with the element; FE goes in only there.
- **Thermometer**: a round brass gauge on the front, shown only with the element (stove: above the door; Brewhouse: between the two doors). Its needle shows the temperature from cold (left) to hot (right) and moves smoothly; the server tells clients only on coarse steps and when heating turns into cooling, and the client carries the needle on between them.
- **Coils over the door**: with the element the door slits show heating coils, warm orange while hot and dull while cold; no flames or crackle, a softer glow.
- **Wood fire unchanged** without the element: no gauge, no port, no FE.
- Pure logic with JUnit tests: the preheat curve (`ElectricHeat`), the switch between wood fire and element (`FireboxHeat`), the gauge sync rule and the needle reading.
- GameTests: a stove with the element and FE cooks without fuel; no FE means it cools and stops; the element drops on unform and comes out by hand; the port is the only FE face; a Brewhouse with the element brews; the speed and capacity slots refuse the element.
- Textures from a new `tools/heating-textures/gen.py` (element item, coil doors cold and hot for both fireboxes, gauge dial, needle, port); datagen for models, block states, recipe, lang and item model.

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- `foundation/upgrades`: ADDED requirement for the Heating Element item, its own kind of upgrade that speed and capacity slots refuse.
- `delicacies/tiled-stove` (delta of the unarchived `m14-tiled-stove`, so ADDED requirements only): installing the element, electric heat with the preheat curve, the power port, the thermometer and coil look, and the wood fire without the element.
- `brewery/brewhouse` (delta of the unarchived `m15-brewhouse`, ADDED requirements only): the same Heating Element on the Brewhouse firebox, its port, gauge and doors.

## Non-goals

- The Alchemy Calciner or any other Alchemy block taking the element; Alchemy is not touched.
- A heat API between mods, heat from other sources than fuel and FE, or a firebox GUI.
- Better heating elements, several elements per firebox, or the element speeding up cooking.
- Showing the temperature in JEI or on the goggles.
- Keeping the temperature or the stored FE when the element is taken out.

## Impact

- Foundation: `upgrade/HeatingElementItem`, registration and creative tab entry, recipe, lang, item model, texture, an upgrade GameTest.
- Delicacies: new pure classes in `firebox` (`ElectricHeat`, `EnergyDraw`, `HeatingStats`, `FireboxHeat`, `GaugeSync`, `GaugeNeedle`) with JUnit tests; Minecraft side `firebox/FireboxHeating`, `firebox/HeatingStatsManager`, `firebox/ElementUse`; `StoveTileBlock` gains `HEATER` and `PORT` states, `StoveTileBlockEntity` swaps its `FuelFire` field for `FireboxHeating` and syncs the gauge; energy capability registration; reload listener; client `GaugeRenderer` and the needle standalone model; models, lang, `heating/tiled_stove.json`; `HeatingElementGameTests`.
- Brewery: `BrewhouseBricksBlock` gains `HEATER`, `BrewhouseBlockEntity` uses `FireboxHeating`, `BrewhouseModel` gains the gauge and port parts, energy capability, models, lang, `heating/brewhouse.json`, a gauge renderer registration, Brewhouse GameTests.
- New `tools/heating-textures/gen.py`. No Alchemy or Shuttles changes; no new dependencies.
