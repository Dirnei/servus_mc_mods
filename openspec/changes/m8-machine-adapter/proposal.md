# Proposal

## Why

PLAN.md M8 item 9, `m8-machine-adapter`: machines of other mods and vanilla do not speak the Foundation production API, so the Crafting CPU cannot use them. A Machine Adapter placed against such a machine makes it a producer on the data line: the CPU sees its recipes, ships its ingredients, and collects its results, so a plain vanilla furnace smelts only what was ordered. Upkeep (fuel) is kept stocked separately from recipes, as decided in PLAN.md 2.4.

## What Changes

- New Foundation block **Machine Adapter**, placed against a machine (it faces the block it was placed on). It is a data member with the producer role.
- **Profiles** in datapack JSON (`data/<ns>/machine_profiles/*.json`) name the machine blocks, the recipe type they run and what each side of the machine is for: Ingredients, Upkeep, Output or Off, relative to the machine's facing. Foundation ships profiles for the vanilla furnace (smelting), smoker (smoking) and blast furnace (blasting). Without a matching profile the adapter offers nothing.
- The adapter reports the profile's recipes as production recipes (one per ingredient item, with the cooking time), reports the recipe the machine is cooking as active, forwards ingredients into the machine's ingredient side, and offers the machine's output side for extraction, so packets can deliver and collect through it.
- **Upkeep**: the adapter keeps the machine's upkeep side stocked with an upkeep item at a level (default from the profile, coal and 16 for the vanilla profiles), set in a small GUI with a ghost slot and a level. Whenever the slot is below the level it asks a supplier on its data line for the difference, always, independent of orders. Recipes never contain fuel.
- New Foundation capability `servus_foundation:supplier`: a block that can send items to a data member on request. Storage exposes it on the Warehouse Controller and the Access Point, backed by the existing shipping (Access Point, shuttles, packets).
- The Production Terminal's Machines tab names an adapter after its machine ("Furnace (adapter)") and shows its upkeep ("Coal 12 of 16").

## Capabilities

### New Capabilities
- `foundation/machine-adapter`: the Machine Adapter block, profiles, side roles, recipes and forwarding, upkeep.

### Modified Capabilities
- `foundation/production-api`: adds the supplier capability.
- `storage/access-point`: the warehouse answers supplier requests through its controller and Access Points.
- `storage/production-terminal`: Machines tab rows for adapters.

## Non-goals

- Hand made adapter patterns for machines without a profile (later, after play testing).
- Changing an adapter's side roles with the carrot; roles come from the profile only.
- Native Manufacturing machines reporting upkeep: `m8-native-machines`.
- Fluid or energy upkeep.

## Impact

- Foundation: new package `foundation.adapter` (block, block entity, item handler, profiles, reload listener, menu, screen), supplier capability in `foundation.production`, datagen (blockstate, model, loot, tag, recipe, lang), textures, default profiles, GameTests.
- Storage: supplier capability registration on controller and Access Point, Machines tab rows (`ProductionView`, `ProductionRows`, screen), GameTest for a CPU order through an adapter.
- Docs: Foundation page section; regenerated game data.
