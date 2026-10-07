# Proposal

## Why

The user wants a medieval look for the warehouse. The Heavy Duty Rack looks like a modern pallet rack: orange steel and a neutral board texture. Its recipe already takes any planks, but forgets which ones. Other mods show the wood a block was crafted from, and a pack like ATM10 has many woods, so the rack should show the wood it was made of, with a dark iron frame that fits wooden shelves.

Milestone: M6 (Storage automation), a follow up to the Heavy Duty Rack. Only the rack for now; the crates follow later.

## What Changes

- The Heavy Duty Rack remembers the planks it was crafted from and shows their texture on its boards (back panel, floor and shelves). Any block in the `minecraft:planks` tag works, including modded woods.
- Recipe: unchanged pattern and cost (iron ingots and three planks). The wood is the planks that appear most often; with three different planks the middle one decides.
- The frame (posts, beams, edges) gets a blackened wrought iron texture instead of the orange steel, for every rack.
- The wood stays with the rack: on the placed block, in the dropped or dismantled item, in the item's look in hand and inventory, in its tooltip, and in the breaking particles.
- A rack without a stored wood (for example from `/give` or placed before this change) shows oak boards. The creative tab and JEI show one rack, the oak one; neighbouring bays of different woods still join.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `storage/heavy-duty-rack`: crafting records the wood; new requirements for the wood look and the frame.

## Non-goals

- Crates, Double and Quad Crates (their textures paint frame and wood into one image and need a split first).
- Choosing the frame material, or a stone frame.
- Separate blocks per wood type.
- Animated or connected planks textures beyond their base sprite.

## Impact

- `storage`: `rack_wood` data component, `HeavyRackBlockEntity` stores and syncs it and exposes it as model data, a custom rack recipe (serializer plus datagen), a client retexturing baked model for the block and an item override for the item, a new `heavy_rack_iron` texture replacing `heavy_rack_steel`, lang, GameTests.
