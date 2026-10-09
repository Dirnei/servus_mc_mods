# Proposal

## Why

The Calciner is the first thing a player builds in Manufacturing, and today it is a box with a GUI and a side configuration. Alchemy should feel like building a lab: the Calciner becomes a real calcining furnace with an alembic, built from bricks and parts in the world, shown as a schematic in Paracelsus' Journal and run without a machine GUI. It comes first because it is the start of the mod; the Coagulator and the Athanor follow as their own changes.

PLAN.md milestone: M9 (Manufacturing becomes alchemy). This change adds item 6b, `m9-calciner-multiblock`, and plans 6c `m9-coagulator-multiblock` and 6d `m9-athanor-multiblock` after it.

## What Changes

- The Calciner becomes a multiblock: a one block deep wall, 3 wide and 5 tall, of vanilla Bricks and six custom parts. The Calciner block is the furnace mouth and the controller; the new parts are the Firebox, Ash Pan, Alembic, Receiver and Sulfur Hood.
- The player forms it by right clicking the Calciner with Paracelsus' Journal. A wrong or missing block is named in a message. The Configuration Carrot plays no part.
- Once formed it renders as one big model of a brick furnace with a copper alembic, with fill levels, fire glow and smoke. Breaking any block of it unforms it and gives the bricks back.
- Fixed ports instead of the side configuration: matter goes in at the mouth, fuel at the Firebox, Salt comes out of the Ash Pan, Mercury out of the Receiver and Sulfur out of the Hood. A bucket used on a port fills with that principle, and each port pushes into a touching fluid handler.
- No Calciner GUI: matter and fuel go in by hand, hopper or pipe, sneaking with an empty hand takes the matter back, and Jade shows exact amounts.
- The firebox keeps the upkeep capability, so a data line keeps the Calciner fuelled.
- The player who forms the Calciner becomes its owner for journal events.
- The Journal's Calciner Workshop entry gets a Modonomicon multiblock page with Visualize and the crafting recipes of every part.
- The Calciner GUI, its menu, its Fluids layer and the gauge buckets are removed.

## Capabilities

### New Capabilities
- `manufacturing/calciner-structure`: the Calciner multiblock itself: its layout, parts, forming with the Journal, unforming, the formed model and the ports by position.

### Modified Capabilities
- `manufacturing/calciner`: work, fuel and principle output move from a GUI machine with side configuration to the formed structure; the GUI and gauge bucket requirements are removed, crafting and journal ownership change.
- `manufacturing/journal`: the Workshop entry of the Calciner shows the multiblock schematic with Visualize and the parts' recipes.

## Impact

- Manufacturing: `alchemy/CalcinerBlock`, `CalcinerBlockEntity`, new part and shell blocks, removed `CalcinerMenu` and `client/CalcinerScreen`, capability registration in `ServusManufacturingEvents`, registries, client renderer, datagen (blockstates, models, loot, recipes, lang, Modonomicon multiblock and book page), `AlchemyGameTests`.
- Optional Jade plugin in Manufacturing (Jade is already in the dev runtime).
- PLAN.md M9: machine text, the machine table and the item list.
- Nothing is released, so placed Calciners need no migration.

## Non-goals

- The Coagulator and the Athanor as multiblocks (`m9-coagulator-multiblock`, `m9-athanor-multiblock`).
- An FE Calciner or Salamander upgrade.
- Mirrored builds or more than one size of Calciner.
- Datapack overrides of the Calciner layout: the layout matches the fixed model, so it stays in code.
- Final art: this change ships a first model and textures that can be refined later in Blockbench.
