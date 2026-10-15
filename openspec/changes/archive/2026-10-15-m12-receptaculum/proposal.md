## Why

`alchemy_plan.md` ("Vas Hermeticum and Receptaculum") makes the homunculus the only automated way to get grown ore out of the hidden Gnome Hollows. Since `m12-hidden-hollow` a Hollow has no screen, no pipes and no production capability: only a player under Elemental Sight empties it. The Receptaculum with the Vas Hermeticum on top closes that gap: the homunculus harvests every Hollow in its reach into a normal inventory that pipes and the Crafting CPU reach, and brings the gnomes their gifts.

## What Changes

- New item **Sealed Flask**, made at a red (Rubedo) Athanor from Crying Obsidian with Salt and Mercury. Placed on top of a Receptaculum it becomes the **Vas Hermeticum** block; it cannot be placed anywhere else and pops off when the Receptaculum below goes.
- New block **Receptaculum**: a Hearth Chest (brick hearth with a grated, always burning fire and flames at the front, an iron banded deepslate brick chest body, a deepslate tile lid and a brass collar on top). Crafted at a crafting table with Ignis Perpetuus. Always lit. A Foundation machine: faces the player, side configuration, carrot, drops its contents. Its screen holds 27 yield slots, 3 gift slots and 1 Elixir slot.
- The homunculus grows in the Vas for a few in-game days (datapack, example 48000 ticks) while the Receptaculum holds Elixir of Sight; the Elixir is used up while it grows and while it works (datapack, example one bottle per 12000 ticks). Without Elixir it pauses.
- A grown homunculus with Elixir takes the stored ore out of every Gnome Hollow in the 5x5 area on the Receptaculum's level (centred on the stack) every work interval (datapack, example 100 ticks) into the yield slots, and gives a gift from the gift slots to every bound gnome whose boost runs low.
- The Receptaculum exposes the item capability (pipes extract the yield and insert gifts and Elixir) and the Foundation production capability listing the yields of the Hollows in reach, replacing what each Hollow offered before `m12-hidden-hollow`.
- Hidden: without Elemental Sight the Vas shows glass and murky liquid only. With the sight the client draws the pale homunculus inside the flask and sagging chains from the Vas neck to each Hollow in reach.
- Whispers: now and then a player with the sight near a grown, bound homunculus reads a whisper in the action bar (placeholder lines for the storyteller about warmth and a star).
- Freeing: a Nether Star used on the Vas of a grown homunculus is consumed, the seal breaks for everyone (block state `freed`), the homunculus no longer needs Elixir and delivers two items for every item it takes; it tells the player its secret in chat (placeholder lines for the storyteller). Under the sight the freed homunculus sits on the rim, without chains. Nothing hints at the star outside the whispers.
- Journal: Workshop entries for the Sealed Flask and Vas, and for the Receptaculum, opening with Basel and Salzburg (Rubedo). The Sealed Flask is the first Rubedo Athanor craft, so it counts for the existing Rubedo task.
- `HollowStore` and the Hollow learn to hand out part of the store, for the homunculus.

This change builds on the applied but unarchived `m12-hidden-hollow` (the hidden Hollow and its store), `m12-gnome-gifts` (`offerGift`, boost ticks), `m12-athanor-citrinitas-rubedo` (the Rubedo tier), `m12-principle-dropper`, `m12-elixir-dose`, `m12-salamander` (Ignis Perpetuus) and `m12-salamander-forge` (the forge's fire particles become a shared helper). It adds new capabilities only and modifies none of their specs.

## Capabilities

### New Capabilities
- `alchemy/receptaculum`: the Receptaculum block, its look, screen, slots, automation sides, item and production capabilities, recipe and Workshop entry.
- `alchemy/homunculus`: the Sealed Flask, the Vas Hermeticum on a Receptaculum, the homunculus' growth, Elixir upkeep, harvesting and gift delivery, its look under Elemental Sight, the whispers, the freeing with a Nether Star, the Athanor craft and the Workshop entry.

### Modified Capabilities
None.

## Impact

- New package `alchemy/.../homunculus/`: Receptaculum block, block entity and menu; Vas block and block entity; pure `HomunculusReach`, `HomunculusLife`, `ElixirUpkeep`, `HomunculusSettings`, `WhisperClock`; data loader `HomunculusData` (`data/<ns>/servus_alchemy/homunculus/<name>.json`).
- `hollow/HollowStore.take(int)` and `GnomeHollowBlockEntity.takeYield(int)`.
- Client: `ReceptaculumScreen`, `VasRenderer` (block entity renderer), registrations.
- Registries, capabilities in `ServusAlchemyEvents`, machine defaults for `receptaculum`.
- Assets: textures (brass, flame, glass, murky liquid, wax seal, Sealed Flask item, GUI), generated block states, models, loot, tags, recipes, lang.
- Journal: two `WorkshopCategory` entries. Docs: Alchemy page.
- Tests: JUnit for the pure logic and the store, GameTests in a new `HomunculusGameTests`.
