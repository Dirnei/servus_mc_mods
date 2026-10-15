# Design

## Context

See proposal.md for the why. Alchemy has no entity yet; the gnomes are drawn by the Hollow's reveal renderer. Elemental Sight is a mob effect, and the client already knows its own player's effects (`ElementalSight.active(Minecraft.getInstance().player)` gates `RevealRenderers`). The Athanor recipe is one counted item plus principles plus a minimum tier, written from `AlchemyRecipeTable.athanor()`, which also feeds the Workshop text and the docs game data. The Citrinitas tier (2) and the `journal/schwaz` research node come from the unarchived `m12-athanor-citrinitas-rubedo`. Data files with one record per id follow `ElixirDoseData` (`SimpleJsonResourceReloadListener`, registered in `AlchemyEvents`).

APIs verified in the Minecraft 26.1.2 and NeoForge 26.1.2.116 sources:
- `DeferredRegister.createEntities(modid).registerEntityType(name, factory, category, builder)`; `EntityType.Builder.sized`, `fireImmune`, `clientTrackingRange`.
- `EntityAttributeCreationEvent.put(type, AttributeSupplier)` and `RegisterSpawnPlacementsEvent.register(type, placementType, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, predicate, Operation.REPLACE)` on the mod bus. `SpawnPlacementType` is an interface with `isSpawnPositionOk(LevelReader, BlockPos, EntityType)` and a default `adjustSpawnPosition`, so a mod can supply its own.
- Creature spawning (`MobCategory.CREATURE`, a persistent category) runs every 400 game ticks; animals never despawn (`Animal.removeWhenFarAway` is false), so Striders from chunk generation keep the Nether's creature cap full (found in the play check: no salamander at all, only Striders). Ambient spawning (`MobCategory.AMBIENT`, bats) runs every tick with its own cap (`ServerChunkCache`: `getGameTime() % 400L == 0L`). Vanilla gives the Strider weight 60, group 1 to 2, in the Nether Wastes, Soul Sand Valley, Basalt Deltas and Crimson Forest; the Warped Forest has no creature spawns (`NetherBiomes`).
- `neoforge:add_spawns` biome modifier: `biomes` plus `spawners` as a list of `{type, weight, minCount, maxCount}` (`WeightedList<SpawnerData>`); the creature category comes from the entity type.
- Entity type registers before items (`BuiltInRegistries` order), so `new Item.Properties().spawnEgg(type)` in an item supplier works; spawn eggs in 26.1 have their own item texture.
- Client: `EntityRenderersEvent.RegisterLayerDefinitions.registerLayerDefinition`, `RegisterRenderers.registerEntityRenderer`; `MobRenderer<T, S extends LivingEntityRenderState, M>` with `extractRenderState`, `getBlockLightLevel` (Blaze returns 15 for full bright) and `EntityRenderer.shouldRender`, which skips model, shadow and name together.
- `TamableAnimal` gives owner, tame flag and sitting pose (synced), `tame(player)`, entity events 7 (hearts) and 6 (smoke), `FollowOwnerGoal` with teleport, `SitWhenOrderedToGoal`. `Animal.removeWhenFarAway` returns false.
- Strider: `canStandOnFluid(lava)`, a ground navigation that accepts lava path types, the float logic and the lava spawn rule.
- Jade 26.1.11: `IWailaClientRegistration.addRayTraceCallback`; returning `null` shows no tooltip.

## Goals / Non-Goals

**Goals:**
- A working, tested salamander pet with data driven numbers and a family style look.
- Sal Ammoniac and Ignis Perpetuus items with the Athanor craft.

**Non-Goals:**
- Salamander Forge and Receptaculum (later changes use Ignis Perpetuus).
- A dynamic light around the salamander: vanilla has no dynamic light; the full bright model must do.
- Breeding, riding, attacking.
- The richer "far, near, here" hints for the salamander (`m12-hidden-hints`); this change only adds the stray flame particle.

## Decisions

### D1 A TamableAnimal that walks on lava
`Salamander extends TamableAnimal`. From the Strider: `canStandOnFluid` for lava, path malus 0 for lava and fire, a navigation that accepts lava and fire path types and lava as a stable destination, the float logic that lifts it out of deep lava, and `getWalkTargetValue` 0 everywhere: no pull toward lava (a wild salamander roams the forest floor, a pet does not drift into lava while following) and none toward light. Natural spawning also asks `PathfinderMob.checkSpawnRules`, which refuses a negative walk target value; the inherited animal value is light minus 0.5, negative almost everywhere in the dark Nether, so without this override no salamander spawns (found in the play check). Entity type: creature category, `fireImmune()`, size 0.6 by 0.5, tracking range 10. Attributes: 10 health, speed 0.2. `isFood` is false and there is no offspring: no breeding. "Swims in lava" is walking on the lava surface like a Strider. Wild ones despawn (`removeWhenFarAway` is `!isTame()`).
- Alternative: a `PathfinderMob` with its own owner code. Rejected: TamableAnimal already brings owner, sitting, following and teleport.

### D2 Goals
1 `SitWhenOrderedToGoal`; 2 `TamableAnimalPanicGoal`; 3 `FollowOwnerGoal(1.0, 10, 2)` (teleports beyond 12 blocks like a wolf); 4 `LieInCampfireGoal` (tame and not ordered to sit: a `MoveToBlockGoal` over 8 blocks to a lit campfire, standing in its block; while there it takes the sitting pose); 5 `RandomStrollGoal`; 6 `LookAtPlayerGoal` and `RandomLookAroundGoal`. No target goals.

### D3 Hidden on the client
`SalamanderRenderer.shouldRender` returns false for a wild salamander when the local player lacks Elemental Sight. That skips model, shadow and name tag. Jade gets a ray trace callback that returns no accessor for a wild salamander without the sight. The client spawns a `FLAME` particle at a wild salamander about once every 40 ticks, seen by all.
Limits: the hitbox is still there, so a player without the sight can still hit or right click a salamander they cannot see, arrows hit it, and debug hitboxes (F3+B) show it.

### D4 Befriending by right click
Right click with a Gold Ingot (one used up per click) is the robust choice: it reuses `mobInteract` like the wolf and works in GameTests with a mock player. Picking up thrown gold like a piglin would need an item pickup goal and wanted item logic. Success: `tame(player)`, stop navigation, event 7 (hearts); the salamander stays standing and follows. Failure: event 6 (smoke). NeoForge's `EventHooks.onAnimalTame` can cancel.

### D5 Feeding and digestion
The owner's right click with a Gold Ingot while `Digestion` is idle uses one ingot and starts it with the data digest time; the salamander plays an eating sound. A non owner's gold or a busy stomach is refused (the click passes). Each server tick counts down; at zero it drops one Sal Ammoniac at its position (`spawnAtLocation`). The remaining ticks save as `digest_ticks`. Pure `Digestion` (no Minecraft imports) holds the countdown.

### D6 Data
`data/<ns>/servus_alchemy/salamander_temper/<entity>.json`, file id equal to the entity type id:
```json
{ "befriend_chance": 0.33, "digest_ticks": 1200 }
```
Pure record `SalamanderTemper(befriendChance, digestTicks)` validates (chance 0 to 1, ticks at least 1) and decides `befriends(roll)`; `SalamanderTemperData` loads it, with `SalamanderTemper.DEFAULT` (same numbers) when no file exists. The same file holds the spawn numbers in a `spawn` object:
```json
{ "befriend_chance": 0.33, "digest_ticks": 1200,
  "spawn": { "home_radius": 4, "crowd_radius": 32, "crowd_max": 3,
             "biome_chance": { "minecraft:warped_forest": 0.5, "minecraft:nether_wastes": 0.2,
                               "minecraft:basalt_deltas": 0.2 } } }
```
Each field has a default in `SalamanderTemper.DEFAULT`; a biome without a `biome_chance` entry passes every try. Spawning weights are data in biome modifiers. One `neoforge:add_spawns` file gives one weight to all its biomes, so there are two files in `data/servus_alchemy/neoforge/biome_modifier/`: `salamander_forest.json` for the Crimson Forest and the Warped Forest (weight 40, group 1 to 2) and `salamander_lava.json` for the Nether Wastes and Basalt Deltas (weight 5, group 1); the old `salamander.json` goes.
- Spawn group: the salamander is an ambient mob (`MobCategory.AMBIENT`), not a creature. In the creature group the Striders, which never despawn, fill the cap and nothing else spawns. No other ambient mob spawns in the Nether, so the weights only rank salamanders against spawns another pack adds; how often they show comes from `biome_chance` (Warped Forest 0.5, Nether Wastes and Basalt Deltas 0.2) and the crowd limit. Wild salamanders still despawn far from players (`removeWhenFarAway` is `!isTame()`); there are no chunk generation spawns in this group.
- Overcrowding is held by the crowd check, not by the ambient cap alone: the cap counts all ambient mobs around a player, while the crowd check counts wild salamanders around the spawn spot.

### D7 Spawn placement
A custom `SpawnPlacementType` (`SalamanderPlacement`) accepts either a lava spot (`SpawnPlacementTypes.IN_LAVA`) or a ground spot (`SpawnPlacementTypes.ON_GROUND`), with `MOTION_BLOCKING_NO_LEAVES`, registered with `Operation.REPLACE`. The spawn rule `Salamander.checkSpawnRules` then decides:
- On lava: the Strider rule (walk up the lava column, the block above must be air), and only in a biome of the tag `servus_alchemy:salamander_lava` (Nether Wastes, Basalt Deltas).
- On the ground: a block of the tag `servus_alchemy:salamander_home` within `home_radius` (a box of at most 9 by 9 by 9 blocks read once per spawn try, never per tick).
- Both: `biome_chance` of the biome, then fewer than `crowd_max` wild salamanders within `crowd_radius` (`getEntitiesOfClass` with a filter on `!isTame()`).
The block tag `salamander_home` holds `#minecraft:crimson_stems`, `#minecraft:warped_stems`, `minecraft:nether_wart_block`, `minecraft:warped_wart_block` and `minecraft:nether_wart`; both tags come from datagen (`ServusAlchemyBlockTagsProvider`, a new biome tags provider).
Pure `SalamanderSpawn` (no Minecraft imports) holds `crowdAllows(wildNearby, max)`, covered by JUnit. The home box is `BlockPos.betweenClosed` over the home radius, which `SalamanderTemper.Spawn` keeps between 0 and 4; a pure box check would only repeat the loop bounds.

### D8 Model
`SalamanderModel` after the salamander of `elementals.html`, in model units (y down, head at negative z, ground at y 24). Texture 64 by 64. All boxes with their faces (Y is the HTML height above ground, Z the HTML forward axis):

| part | size w h d | HTML span x / y / z | parent |
|---|---|---|---|
| body | 6 4 10 | -3..3 / 2..6 / -5..5 | root |
| belly (glow) | 5 1 9 | -2.5..2.5 / 1.5..2.5 / -4.5..4.5 | root |
| head | 5 3 4 | -2.5..2.5 / 2.25..5.25 / 4..8 | pivot (0, 3.75, 5) |
| snout | 3 2 3 | -1.5..1.5 / 2.5..4.5 / 7..10 | head |
| eyes | 1 1 1 | ±(2..3) / 3.75..4.75 / 5.5..6.5 | head |
| tail 1 | 4 3 5 | -2..2 / 2.4..5.4 / -9..-4 | pivot (0, 3.9, -5) |
| tail 2 | 3 2 5 | -1.5..1.5 / 2.65..4.65 / -13..-8 | pivot -4 behind tail 1, 0.25 down |
| tail 3 | 2 2 5 | -1..1 / 2.45..4.45 / -17..-12 | pivot -4 behind tail 2, 0.2 down |
| tail flame | 1 2 1 | -0.5..0.5 / 3.95..5.95 / -17.5..-16.5 | tail 3 |
| legs | 2 3 2 | ±(2.6..4.6) / 0.5..3.5 / ±(2..4) | pivots (±3.6, 3.5, ±3) |
| feet | 3 1 3 | outer side, 0..1, 0.25 forward | leg |
| crest | 1 4 1, seven | -0.5..0.5 / from 5.5 up / centres -4.5 + 1.5 i | pivots at their base |

Coplanar check of every overlapping pair:
- body and belly: belly ends 0.5 inside the body on x and z, its top (2.5) is inside the body and the body bottom (2) inside the belly: no shared plane.
- body and head: head starts 1 inside the body (z 4 vs 5); head top 5.25 and bottom 2.25 vs body 6 and 2, head sides 2.5 vs 3: no shared plane, also with the head turned (its corners stay inside 3).
- head and snout: snout starts 1 inside the head; snout top 4.5 and bottom 2.5 vs head 5.25 and 2.25; sides 1.5 vs 2.5: none.
- head and eyes: eye inner side (2) inside the head, outer side (3) outside; eye top 4.75 vs head top 5.25; z inside: none.
- body and tail 1: tail starts 1 inside the body; tail bottom 2.4 and top 5.4 vs body 2 and 6; sides 2 vs 3: none. Tail 1 and tail 2, tail 2 and tail 3: each starts 1 inside its parent, offsets of 0.25 and 0.2 keep tops and bottoms apart, widths differ: none. Tail 3 and its flame: flame bottom 3.95 is inside tail 3 (top 4.45), flame sticks out 0.5 behind tail 3: none.
- legs and body: the body side (x 3) cuts through the leg (2.6..4.6), leg top 3.5 inside the body: none. Legs and belly: leg inner side 2.6 vs belly side 2.5: none. Legs and feet: leg bottom 0.5 inside the foot (0..1), foot sides 0.25 out from the leg sides: none.
- crest and body: flames start 0.5 inside the body top; 1 wide flames on 1.5 centres leave 0.5 gaps: none between flames. Crest and head: the front flame ends at z 4.5 + 0.5 = 5, the head spans x ±2.5 at y up to 5.5 while the flame spans x ±0.5 from 5.5 up: at the HTML head height (2.5..5.5) the flame bottom would share the plane 5.5 with the head top where both overlap (z 4..5), so the head sits 0.25 lower (top 5.25): none. The same holds for the back flame (z -5..-4) over tail 1, whose top therefore is 5.4, not 5.5. Back legs and tail 1 both touch z -4 but never overlap in x (2.6..4.6 vs ±2).
- The flicker scales flames and the tail flame along y from their base, which stays inside the body or tail: no new shared plane.
Animation: tail segments sway in yaw with a phase lag (like the HTML), the head turns slowly, legs swing with walk speed, crest flames flicker by a hash of age and index. Sitting: legs spread flat (x rotation) and body lowered 2 px.

### D9 Texture and glow
A 64 by 64 texture generated by a script in the job directory: coal red body (`#B8361A` to `#E2471B`), glowing cracks (`#FFB347`) and dark seams (`#7A1E0C`) speckled over every face of body, head, tail and legs, an orange belly, cream eyes (`#FFF4D6`), feet in dark red, flames yellow and orange with a lighter top. The whole model draws full bright (`getBlockLightLevel` 15, like the Blaze), so it glows in the dark Nether. Shadow radius 0.4.

### D10 Items
`SAL_AMMONIAC` and `IGNIS_PERPETUUS` are `QuoteItem`s, a plain item that adds the gray italic line `item.<id>.quote` to its tooltip. Ignis Perpetuus has no fuel data map entry, so no furnace burns it. `SALAMANDER_SPAWN_EGG` is a vanilla `SpawnEggItem` with `spawnEgg(type)`. Textures: Sal Ammoniac as white grey crystal lumps with a yellow tint, Ignis Perpetuus as a glowing ember (dark core, orange and yellow rim, a small flame tip), the egg in coal red with orange spots. All three go in the creative tab after Quicksilver.

### D11 Athanor craft
`AlchemyRecipeTable.athanor()` gains `ignis_perpetuus`: one Sal Ammoniac, 500 mB Sulfur, tier 2, 200 ticks. The recipe provider, the Workshop and the docs game data read it from there.

### D12 Journal
The Workshop gets an entry "The Salamander" (`salamander`, icon Ignis Perpetuus) under The Yellow Ring, unlocked by the `journal/schwaz` node like the other Citrinitas entries: where it lives (first page "Among the Crimson Stems": mostly the Crimson Forest near its trees and Nether Wart, a bit less among the warped trees, and now and then a flame on a lava lake), the sight, gold to befriend, sitting and campfires, Sal Ammoniac, and Ignis Perpetuus with its craft text from the table. Wandering years gets no salamander task: chapter tasks are all required and the plan keeps the salamander optional ("Players who skip the salamander still reach Rubedo").

## Risks / Trade-offs

- [An invisible creature can still be hit or clicked] → Accepted and noted (D3); the rendering is what gives a creature away.
- [The pet walks on lava when following its owner through the Nether] → It is fire immune; lava is fine.
- [A tamed salamander in rain or water] → It is not water sensitive, so it does not die in Overworld rain.
- [The Strider like navigation lets a pet path across lava toward its owner] → Intended.

## Assumptions to confirm with the user

- Befriending is right click with a Gold Ingot (not thrown gold picked up like a Piglin); chance 1 in 3 per ingot (`befriend_chance` 0.33).
- Digest time 60 s (`digest_ticks` 1200), one Gold Ingot gives one Sal Ammoniac; only the owner can feed it.
- Spawns mainly in the Crimson Forest (weight 40, groups 1 to 2) and a bit less in the Warped Forest (same weight, `biome_chance` 0.5), on the ground within 4 blocks of crimson or warped stems, wart blocks or Nether Wart; rarely on lava in the Nether Wastes and Basalt Deltas (weight 5, alone); at most 3 wild ones within 32 blocks. "Netherwarts" means the wart blocks and the Nether Wart plant. Revised after the play check, where lava only at weight 5 practically never spawned.
- Wild salamanders despawn like other wild creatures; befriended ones never.
- No dynamic light: vanilla has none; the model is full bright instead.
- Lying in a campfire is the sitting pose on the campfire block within 8 blocks; it does not take a pose when the owner calls.
- The hint without the sight is a flame particle about every 2 s; the richer hints come with `m12-hidden-hints`.
- Hidden means not drawn and not named in Jade; it can still be hit and clicked without the sight.
- Ignis Perpetuus: one Sal Ammoniac and 500 mB Sulfur on a Citrinitas Athanor, 200 ticks; a building part and not a furnace fuel (the Calciner takes it as its eternal flame, `m12-calciner-eternal-fire`).
- No journal task for the salamander, as it is optional for progression.
- Tooltip texts: Ignis Perpetuus "Do not ask what it is. It burns forever, that is all you need to know."; Sal Ammoniac "The salamander gives back what it eats, only better. Do not tell the goldsmiths." (the historical camel dung source stays out of game text; to check).
- Health 10, speed 0.2, harmless, no breeding.
