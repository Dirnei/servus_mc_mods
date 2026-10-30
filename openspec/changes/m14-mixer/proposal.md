# Proposal

## Why

The Mill grinds one item into another, but nothing in Foundation can combine several items, or items and a fluid, into one result. Alchemy wants to make Dirt from Sand, Clay, Humus and Water, and Delicacies wants dough and Obatzda later. The decision of 2026-10-30 is a Foundation machine for that, the **Mixer**, the Mill's sibling: driven the same way, built the same way, open to recipes from every mod. This change is the `m14-mixer` item under "Open: Foundation" in `PLAN.md` (milestone M14, Foundation part, like `m14-mill`).

## What Changes

- New recipe type `servus_foundation:mixing`: one to four counted item ingredients (shapeless), at most one fluid ingredient with an amount in mB, one item result with a count, and the work one mixing takes. All datapack JSON, synced to clients; any mod or datapack may add more.
- Foundation ships a modest set of vanilla mixing recipes, as example values in JSON: Dirt and Water into Mud, Dirt and Gravel into Coarse Dirt, and Sand, Gravel and a dye into Concrete Powder of each of the 16 colours. No two shipped recipes compete for the same contents.
- New block **Mixer**: a Foundation machine in the family steel look (heavy iron base, an open steel tub with a brass rim, a steel bridge carrying a brass paddle hub, two crossed heavy iron paddle blades, a steel chute on the front, a brass fluid port on the back). Four input slots, one output slot, one fluid tank (example 4,000 mB from data), a screen with input grid, fluid gauge, progress arrow, output and side panel.
- It has no power of its own. Like the Mill it advances only by work from a driver through the existing drive link (Hand Crank, Electric Motor) on its back, left or right, keeps its progress when not driven, and wants work only when it can finish a recipe and the output has room.
- Items through `Capabilities.Item` like the Mill (in from the top, back and sides, out through the chute front and the bottom). Fluid through `Capabilities.Fluid` on the back, left and right, with a Fluids side layer of one fill only tank; a bucket used on the Mixer pours in, an empty bucket takes a full bucket back out.
- The paddle turns with the drive's flywheel motion, also when the tub is empty; standing, it keeps its pose. The contents show as a layer inside the tub: the fluid while the tank holds one, a mix texture while it holds only items.
- A JEI category "Mixing" with the items, the fluid, the result and the work, and the Mixer as catalyst.
- The pure shapeless slot matcher moves into Foundation (`production/SlotMatcher`, from Delicacies) with a pure one kind per slot rule beside it, both JUnit tested, so the Mixer, the Pot and the Brew Kettle share them.

## Capabilities

### New Capabilities
- `foundation/mixer`: the Mixer block, its slots, tank, sides, screen and look, the `servus_foundation:mixing` recipe type with the shipped vanilla recipes, and the JEI category.

### Modified Capabilities

None. The Mixer uses the drive link of `foundation/drive`, the machine rules of `foundation/machines` and the Fluids layer of `foundation/side-config` as they are.

## Non-goals

- Fluid results and more than one fluid ingredient. Items in, items out, one fluid in.
- Chance outputs or byproducts.
- Concrete from Concrete Powder and water, and other vanilla conversions beyond Mud, Coarse Dirt and Concrete Powder.
- Alchemy's Dirt recipe (Sand x4, Clay Ball, Humus, 250 mB Water into Dirt x8): Alchemy ships it in `m9-venter-equinus`.
- Delicacies' dough and Obatzda recipes: later Delicacies changes.
- Switching Delicacies' `VesselRecipe` and Brewery's `BrewingRecipe` over to Foundation's `SlotMatcher` and deleting the Delicacies copy: their code belongs to unarchived changes (`m14-tiled-stove`, `m15-brewhouse`), so it follows once they are archived.
- The data line production capability for the Mixer (like the Mill, it needs a driver).
- A shaft network or other drivers.
- Docs pages: the docs site describes released versions only; the Mixer goes in with the next Foundation release.

## Impact

- New package `foundation/.../machine/mixer/`: `MixingRecipe`, `MixingInput`, `MixerParts`, `MixerBlock`, `MixerBlockEntity`, `MixerMenu`, `MixerGameTests`.
- New pure helpers in `foundation/.../production/`: `SlotMatcher` (moved from Delicacies, Delicacies keeps its copy for now) and `KindSlots`; JUnit tests.
- Registries: `MachineContent` (block, item, block entity, menu, recipe type and serializer, creative tab order), capabilities in `MachineEvents` (item, fluid, drive), recipe sync in `ServusFoundation`, GameTest registration.
- Data: `data/servus_foundation/machine/mixer.json` (`fluid_capacity`), generated mixing recipes under `recipe/mixing/`.
- Client: `MixerRenderer` (paddle and contents layer), `MixerScreen`, standalone paddle model, registrations in `MachineClient`.
- Datagen: `MixerModels`, `MixingRecipes`, crafting recipe, loot, pickaxe tag, lang.
- JEI: `MixingRecipeCategory` in `ServusFoundationJeiPlugin`.
- Assets: `block/mixer_mix.png`, `block/mixer_port.png` and `gui/mixer.png` from `tools/mill-textures/gen.py`; reused `heavy_iron`, `electric_pump_steel`, `brass`, `mill_chute`.
- `foundation/CHANGELOG.md` under Unreleased on archive.
