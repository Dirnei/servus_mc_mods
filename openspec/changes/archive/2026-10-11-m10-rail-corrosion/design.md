## Context

`m10-farm-rail-field-head` made a connected run of farm rails a field with at most one serving Field Head (`FieldHeadBlockEntity`, `FieldGraph`, `FieldHeads`), water drained every `water_interval` ticks in `useWater` and hydration through NeoForge `FarmlandWaterManager` tickets. Two sibling changes are applied but not archived: `m10-field-head-look` (head with wood, `FACING`, `RIPE`, water window and crop icon via `FieldHeadRenderer`) and `m10-rail-look` (rails are wood blocks, `RailBlockEntity` has a `WoodSlot`, every farm rail model references one `#head` texture `servus_storage:block/farm_rail_head`). This change builds on both. Its `storage/field-head` delta modifies the main spec requirements "Field Head GUI" and "Field diagnostics", which neither sibling touches (the head look change only adds requirements); the `foundation/wood-material` delta adds a requirement next to the two that `m10-rail-look` modifies. Archive order: the two siblings first, then this change.

PLAN.md's open decision "Art style and texture direction" is not settled here; the stage textures follow vanilla copper tones.

## Goals / Non-Goals

**Goals:**
- One corrosion stage per field, stored on every farm rail's block entity, drawn by a per block texture swap.
- Aging only when the head uses water; honeycomb upkeep; corroded stops watering; axe scrape per field.
- Pure rules with JUnit tests; GameTests for the in world behavior.

**Non-Goals:**
- Warehouse Rail corrosion (steel never corrodes).
- Waxing rails like vanilla copper (the upkeep slot does this job for a whole field).
- Stages on the item, in recipes or in JEI.

## Decisions

### Pure `FieldCorrosion`
`storage/field/FieldCorrosion` (no Minecraft imports): stage constants `COPPER` 0 to `CORRODED` 4 plus `LOOSE` -1 (a placed rail not yet in a field, drawn as copper), `next`, `previous`, `name`, `join(headStage, railStages)` (the head stage when known, else the youngest assigned stage, `LOOSE` when none is assigned), `age(stage, counted, added, perStep, items)` returning the new stage, the water still counted and the items used (steps beyond corroded are dropped and the counter resets at corroded; `perStep <= 0` switches aging off), `progress(counted, perStep)` in percent, and `axeCanScrape(damageable, remaining, rails)`. JUnit `FieldCorrosionTest`.

### Stage on the rail, decided by the head
`RailBlockEntity` gets `int corrosion = LOOSE`, saved as `Corrosion` and sent in the update tag. `setCorrosion` saves, sends the block update and, on the client, `load` requests a model data update and re-renders when the stage changed (as `WoodSlot.load` does). The stage is not an implicit component, so drops, the carrot's dismantle (`Wood.stamp`) and the `WOODEN` loot table carry only the wood: a re-placed rail is `LOOSE`.

The Field Head keeps the field's stage (`stage`, `stageKnown`, `agedWater`, saved). In `serve(members)`, when the head served before (status was `SERVING`, or a saved stage after a load), the field takes the head's stage; otherwise `join` of the rails' stages, copper when none. Every member whose stage differs is set (rare, at most `max_rails` block entities). `unserve` forgets the stage, so a head standing aside (`OTHER_HEAD`) never decides a join: the serving head of the joined field does. A split refreshes the head, which keeps its stage for its part; the other part keeps its rails' stages and, without a head, never ages.

Headless joins: `FarmRailBlock.onPlace` calls `FieldHeads.railPlaced`. When no loaded head's field touches the rail and no head stands beside it, it collects the field with `FieldGraph` (limit `max_rails`) and sets the `join` of the members' assigned stages on every member; a run of loose rails stays loose. This is one walk per placement, never per tick.

### Aging accounting
`useWater` passes every drained interval to `age(mB)`, which runs `FieldCorrosion.age` with the upkeep slot's count, shrinks the slot by the items used and calls `setStage` when the stage moved. `/servus_warehouse field <pos> age <mB>` calls the same method. `setStage` updates the rails and the head's `CORRODED` block state.

`corrosion_water` example 64000 mB (64 buckets): the head uses `water_per_rail` (1) per rail every `water_interval` (100) ticks, so a full field of `max_rails` (128) uses 128 mB per 100 ticks, about 30700 mB per Minecraft day. One step then takes about two days, and corroded is reached after about eight days, roughly two and a half hours of play, for the busiest possible field; a field of 32 rails needs about a month of game days. One honeycomb per 64 buckets is cheap upkeep for a bee farm, and bigger fields cost proportionally more, as with the water itself. `anti_corrosion_level` example 8 keeps a few steps in reserve without hoarding a stack.

### Corroded
`useWater` returns early when the stage is corroded: it drops the tickets and sets `hydrated` false, drains nothing, so aging stops too. The harvester logic is untouched; `FieldWork` sees `hydrated == false` and reports the existing dry reasons for planting, and the GUI adds the corrosion line in warning colour. In world cue: a `CORRODED` boolean block state on the Field Head selects a top model whose overlay `field_head_top_corroded` paints the crop plate's rim rust red. A block state (rather than the renderer) was chosen because it is static, visible from afar and changes only with the stage; it doubles the top models (12 instead of 6), generated by the same datagen loop.

### Upkeep slot and the network
The data line already keeps upkeep slots stocked: Foundation's `servus_foundation:upkeep` capability (`UpkeepHolder`) makes a block an upkeep member of a touching data line, and `DataNetwork.keepUpkeep` asks suppliers (warehouse controller, Packing Station) every 40 ticks and ships items through `PacketIo`, which inserts with the target's `IItemHandler`. So the head registers both: `Capabilities.ItemHandler.BLOCK` with an insert only view of the one slot handler (extract returns nothing), and `ServusFoundationCapabilities.UPKEEP` with the slot's item (or the first item of the tag while empty), `anti_corrosion_level` and the slot's count. No new mechanism; outbound station keep slots were not needed. The slot handler refuses items outside `servus_storage:anti_corrosion`, which `ServusStorageItemTagsProvider` (new, datagen) fills with `minecraft:honeycomb`.

### Menu and screen
Slots: crop 0, range 1, anti-corrosion 2, inventory from 3. Shift click moves anti-corrosion items into slot 2. The GUI grows 22 pixels: a row at y 88 with the slot on the left and the stage line beside it (stage name and progress, or "Rails corroded, field not watered"), tooltip on the empty slot explaining the upkeep. The background is the garage panel texture drawn in two parts so the inventory frames move down. Two new data slots: stage (-1 when not serving) and progress percent.

### Axe scrape
`FarmRailBlock.useItemOn`: when `stack.canPerformAction(ItemAbilities.AXE_SCRAPE)` (checked in `neoforge-21.1.255-sources.jar`, `ItemAbilities.AXE_SCRAPE` is in `DEFAULT_AXE_ACTIONS`). The client passes when its rail shows fresh copper, so the axe falls through to vanilla behavior (nothing on this block). On the server: the field is the serving head's rails and stage (`FieldHeads.servingAt`), or a headless `FieldGraph` walk with the clicked rail's stage; a field over `max_rails` is refused with a message. When `FieldCorrosion.axeCanScrape(stack.isDamageableItem(), maxDamage - damage, rails)` fails, the player gets an action bar message with the rail count and the needed durability. Otherwise the stage drops by one (through the head when served), `stack.hurtAndBreak(rails, player, LivingEntity.getSlotForHand(hand))` (Unbreaking applies inside), `SoundEvents.AXE_SCRAPE` plays once and `LevelEvent.PARTICLES_SCRAPE` (3005, the event vanilla's `AxeItem` uses) fires on every rail of the field. Creative players lose no durability, as vanilla.

### Texture swap: `WoodModel` learns further swaps
Foundation gets `Wood.SWAPS`, a `ModelProperty<Map<ResourceLocation, ResourceLocation>>` (texture to texture). `WoodModel` adds the swap map to its quad cache key and retextures every quad whose sprite name is a key to the block atlas sprite of the value, the same UV move as the board swap. Items never carry the property, so the item shows fresh copper. This extends the one wrapper rather than adding a Storage wrapper because the wrapper order between `ModelEvent.ModifyBakingResult` listeners of two mods is fragile and `WoodModel` asks its base with `ModelData.EMPTY`, which would hide a Storage wrapper's data. Foundation stays unaware of rails and copper. The stage textures live under `textures/block`, which the vanilla block atlas stitches for every namespace, so no atlas source is needed.

Textures (`farm_rail_head_exposed`, `_weathered`, `_oxidized`, `_corroded`, `field_head_top_corroded`) come from a PIL generator in the job tmp dir, mixing vanilla `exposed_copper`, `weathered_copper` and `oxidized_copper` like `gen_rail_heads.py` mixes copper and exposed copper (as in the approved mock: copper with verdigris patches, green, teal); corroded is darkened oxidized copper toward green black with rust red flecks.

## Risks / Trade-offs

- [A field over `max_rails` or in unloaded chunks] its rails keep whatever stage they had; acceptable, such fields are not served anyway.
- [Headless rails with mixed stages after an unloaded join] the next head that serves them sets the youngest assigned stage.
- [Unbreakable axes] `isDamageableItem` false always scrapes.
- [Test speed] GameTests drive aging through `age(mB)` rather than lowering `corrosion_water`, because the stats manager is shared by parallel field tests.
