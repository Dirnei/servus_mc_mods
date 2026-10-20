## Why

Milestone M14 (Servus: Delicacies) and M15 (Servus: Brewery) both need to grind: wheat to flour for the bakery, malt to grist for the brewhouse. The explore session of 2026-10-20 decided that the grinder is a Foundation block, the **Mill**, driven by a **Hand Crank** on day one and an **Electric Motor** later, so every Servus mod can add milling recipes and drive blocks without depending on Delicacies. This change is the `m14-mill` item of the M14 change list (Foundation part).

## What Changes

- New recipe type `servus_foundation:milling`: a counted input ground into one result for an amount of work, all datapack data. Foundation ships one: 1 Gravel into 1 Sand. Synced to clients; a JEI category with the Mill as catalyst.
- New block **Mill**: a Foundation machine in the family steel look (heavy iron base, steel housing, an iron grinding wheel with a brass hub on top that turns while it mills, a steel hopper with a brass rim, a steel chute on the front). Input and output slot, a screen with arrow and side panel, items through `Capabilities.Item` (in from the top and sides, out through the bottom and the chute front), output push. It has no power of its own: it grinds only while something drives it.
- New Foundation drive link: a block capability `servus_foundation:drive` through which a crank or motor hands work to the block it touches. The Mill takes drive on its back, left and right. No shaft network: one driver drives the one block it touches.
- New block **Hand Crank**: placed on a side of a block that takes drive; holding right click turns it continuously, each full turn gives a little work, spread over the turn, and costs a little food exhaustion. Let go, the arm coasts to a stop like a flywheel and gives no more work.
- Turning parts (crank arm, Mill wheel, motor coupling and fan) move smoothly between ticks, spin up quickly and coast to a stop in about a second; the server sends only start and stop. It drops off when the driven block goes.
- New block **Electric Motor**: built on the Electric Pump's motor look (steel body, copper windings, fan grille). FE in through the back (`Capabilities.Energy`), the shaft on the front drives the block it faces. It draws FE only while the driven block has work; the shaft and the fan turn and sparks jump while it works. One upgrade slot for the Foundation Speed Upgrade: more work per tick and proportionally more FE.
- Balance numbers in datapack JSON: work per crank turn, exhaustion per turn and motor work per tick in `data/servus_foundation/drive/<block>.json`; motor FE per tick and buffer in `data/servus_foundation/machine/electric_motor.json`; work per milling in the recipe.
- Crafting recipes for the three blocks from vanilla and Foundation materials; datagen for block states, models, loot, recipes, lang and tags; textures from a new generator script `tools/mill-textures/gen.py`.
- Docs: the Mill, crank and motor on the Foundation page; the milling recipe and the drive files in the datapack reference.

## Capabilities

### New Capabilities
- `foundation/mill`: the Mill block, its slots, sides, screen and look, the `servus_foundation:milling` recipe type with the shipped gravel recipe, and the JEI category.
- `foundation/drive`: the drive link between drivers and driven blocks, the Hand Crank and the Electric Motor with its Speed Upgrade, their balance data and their look.

### Modified Capabilities

None. The motor uses the existing upgrade slot rules of `foundation/upgrades` and the powered machine rules of `foundation/machines` without changing them.

## Non-goals

- A shaft or gear network that carries drive over several blocks, or one driver driving several blocks.
- Other drivers (water wheel, windmill, steam engine) and other driven blocks; they can come later through the same capability.
- Wheat to flour and malt to grist: Delicacies (`m14-bakery`) and Brewery add those recipes.
- The data line production capability for the Mill (it needs a driver to run, so the Crafting CPU cannot rely on it yet).
- The Heating Element (`m14-heating-element`).

## Impact

- New package `foundation/.../drive/`: the capability interface and pure logic (work progress, motor rate, crank turns), stats and their reload listener, Hand Crank and Electric Motor blocks and block entities, the motor menu.
- New package `foundation/.../machine/mill/`: `MillingRecipe`, `MillBlock`, `MillBlockEntity`, `MillMenu`, parts in pixels.
- Registries: `MachineContent` (blocks, items, block entities, menus, recipe type and serializer), `ServusFoundationCapabilities.DRIVE`, capability registration in `MachineEvents`, machine defaults in `FoundationMachines`, reload listener for drive stats.
- Client: `MillRenderer`, `HandCrankRenderer`, `ElectricMotorRenderer`, screens in `MachineClient`, standalone models.
- Datagen: models and block states, loot, pickaxe tag, lang, crafting and milling recipes.
- JEI: `MillingRecipeCategory` in the Foundation plugin.
- Assets: block textures and the two GUI textures from `tools/mill-textures/gen.py`.
- Tests: JUnit for the drive logic and the stats codec; GameTests in a new `MillGameTests`.
- Docs: `docs/src/content/docs/mods/foundation.md`, `reference/datapack/recipes.md`, `reference/datapack/machines.md`.
