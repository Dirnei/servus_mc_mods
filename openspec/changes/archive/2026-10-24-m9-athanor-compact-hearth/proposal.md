# Proposal

## Why

The Athanor build the user designed in the test world ("Port Test 26.1") replaces the wide diamond hearth with a compact brick hearth and moves the stage marks around the tanks, so the build reads as one furnace instead of a field of rings. The formed model no longer fits that build: its copper sockets sit one block up where the old ducts met it, and it is stone brick on a brick hearth. This is the first of three changes for PLAN.md M9 ("Athanor builds", the stage gate of the Great Work); the Citrinitas and Rubedo rituals follow in `m9-athanor-citrinitas-ritual` and `m9-athanor-rubedo-ritual`.

## What Changes

- **Nigredo hearth:** the Athanor stands on a 3 by 3 hearth: Bricks under the Athanor and in front of it, bottom Brick Slabs on the other seven cells, and a Brick Stair as the front step. Three Fluid Tanks stand two blocks out to its left, behind it and to its right, each three high from the hearth level up. One Copper Duct per tank joins the Athanor and the tank on the Athanor's own level.
- **Albedo:** sixteen Salt Crystal Blocks in a 5 by 5 ring one layer under the hearth. The tanks and the edge of the hearth stand on it.
- **Citrinitas (interim):** a Principle Vat on the hearth level beside each tank, on its outer side, with an Amethyst Block on it. `m9-athanor-citrinitas-ritual` replaces the Amethyst Blocks with the ritual.
- **Rubedo (interim):** four Crying Obsidian pillars, two high, on the salt ring beside the back tank and behind the side tanks. `m9-athanor-rubedo-ritual` replaces them with the obelisk ritual.
- Tier blocks are given for an Athanor facing north and turn with the Athanor's front.
- **New formed look:** a brick tower on a brick plinth with steel corner posts and two copper bands, a black iron fire door with a small window where the flame flickers in the stage colour, copper flanges on the Athanor level where the ducts meet it, a stepped brick and copper dome with four vents glowing in the stage colour, and the steel plate on top where the item rests. Each duct is drawn as a straight copper pipe from a flange into the side of its tank.
- The journal's Athanor schematics and the Modonomicon multiblocks follow the new build.
- Existing formed Athanors unform at their next check until the player rebuilds the hearth.

## Non-goals

- The Citrinitas and Rubedo rituals, the grown crystals and the obelisk (the two follow-up changes).
- A Crying Obsidian recipe or changes to Athanor crafts and principles.
- Breaking rules for ritual blocks (decided in the ritual changes: the stage drops and the ritual is cast again).

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `alchemy/athanor`: hearth, tanks and ducts; shipped tiers and how their blocks turn with the front; the layout and schematics; the formed look.
- `alchemy/journal`: the Athanor schematics show the new build and the stage pages.

## Impact

- `alchemy/.../athanor/AthanorLayout.java`, `AthanorTiers.java`, `AthanorTierData.java`, `AthanorBlockEntity.java`, `AthanorBlock.java` (collision), `AthanorLayoutTest.java`, `AthanorTiersTest.java`.
- `alchemy/.../structure/CopperDuctBlock.java` (formed duct shapes), `datagen/AlchemyModels.java` (formed Athanor and duct models, stage textures), `datagen/ServusAlchemyMultiblockProvider.java`, journal datagen and lang.
- `alchemy/src/main/resources/data/servus_alchemy/servus_alchemy/athanor_tier/*.json`.
- Athanor GameTests and their fixture in `gametest/AlchemyGameTests.java`.
- `alchemy/CHANGELOG.md`: a Changed line at archive.
