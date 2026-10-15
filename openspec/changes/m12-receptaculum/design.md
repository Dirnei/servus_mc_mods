# Design

## Context

See proposal.md for the why. Since `m12-hidden-hollow` a Gnome Hollow keeps a small internal store (`HollowStore`) that only a player under the sight empties; `m12-gnome-gifts` gave it `offerGift(ItemStack)` (TAKEN, DISLIKED, NO_GNOME, the caller shrinks the stack) and `boostTicksLeft()`. Before `m12-hidden-hollow` each Hollow exposed a production provider with one recipe: id the vein id, no inputs, the vein's yield as output, the vein's yield ticks as time (commit `fa3d935d`, `GnomeHollowBlockEntity.production()`); the Receptaculum takes that shape over. Foundation's machine base (`MachineBlock`, `MachineBlockEntity`, `MachineMenu`, `MachineScreen`) gives facing, lit state, side configuration with the front locked, slot role rules for automation (input slots insert, output slots extract), output push, carrot and drops; the Salamander Forge (`m12-salamander-forge`) is the latest example. Elemental Sight on the client is `ElementalSight.active(Minecraft.getInstance().player)`; the Hollow's reveal is drawn by `RevealRenderers`, but the homunculus belongs to one block, so it gets a block entity renderer that draws only under the sight.

## Goals / Non-Goals

**Goals:**
- Receptaculum block with screen, slots, item and production capabilities, always lit.
- Sealed Flask item placing the Vas only on a Receptaculum; homunculus growth, Elixir upkeep, harvesting, gifts.
- Hidden look under the sight: homunculus and chains; whispers; freeing with a Nether Star.
- Pure logic for reach, upkeep and scheduling with JUnit.

**Non-Goals:**
- The salamander drawn in the Receptaculum's fire under the sight (plan line "Under Sight the salamander is seen in its fire"): left for a later look pass.
- Glass shards of the broken seal.
- A Jade view of the homunculus' growth.
- Any hint system for the Nether Star (that is `m12-hidden-hints`' field, and the star stays unhinted).

## Decisions

### D1 Classes
New package `homunculus`:
- `ReceptaculumBlock extends MachineBlock` (always lit default state, light 12, fire particles), `ReceptaculumBlockEntity extends MachineBlockEntity` (31 slots: 0..26 yield = output slots, 27..29 gift = input slots, 30 Elixir = input slot), `ReceptaculumMenu extends MachineMenu` (player inventory at y 122).
- `VasBlock extends BaseEntityBlock` with `BooleanProperty FREED`, `canSurvive` = Receptaculum below, `updateShape` returns air when it cannot survive (the torch pattern), `useItemOn` for the Nether Star. `VasBlockEntity` holds the homunculus' state and drives it from its server ticker.
- Pure (no Minecraft imports): `HomunculusReach`, `HomunculusLife`, `ElixirUpkeep`, `HomunculusSettings`, `WhisperClock`.
- `HomunculusData extends SimpleJsonResourceReloadListener` (like `ElixirDoseData`).
- Client: `ReceptaculumScreen extends MachineScreen`, `VasRenderer extends BufferedBlockEntityRenderer<VasBlockEntity>`.

### D2 Who ticks
The Vas ticks; the Receptaculum below only stores. Each tick the Vas finds the Receptaculum below by `level.getBlockEntity(below)`; without it nothing happens (and `updateShape` breaks the Vas anyway). Upkeep: a not freed homunculus asks `ElixirUpkeep.feed(elixirTicks, receptaculum::takeElixir)`, which counts down the bottle in use and takes a new bottle from slot 30 when it is empty. `HomunculusLife.tick(settings, fed)` returns `NONE`, `GREW_UP` or `WORK`: not grown, fed ticks add growth until `grow_ticks`; grown, fed (or freed) ticks count the work timer to `work_interval`. The bottle's remaining ticks are saved on the Vas, so a broken Vas loses the rest of its bottle.

### D3 Reach and the scan
`HomunculusReach.offsets()` lists the 24 (dx, dz) pairs with |dx|, |dz| at most 2 except (0, 0). On `WORK`, and only then, the Vas looks up the block entities at the Receptaculum's level at those offsets (24 lookups every work interval, never every tick) and keeps the offsets of the Gnome Hollows found. The list is synced to clients when it changes, for the chains, and the production capability reads the same lookup. No block placement listener is needed: the scan runs once per interval, which is the reaction time for new Hollows.

### D4 Harvest
For each Hollow in reach, in offset order: `GnomeHollowBlockEntity.takeYield(max)` takes up to `max` items from the store as one stack of the vein's yield, where `max` is how many the yield slots take divided by the multiplier (1, or `freed_multiplier` once freed). The Receptaculum inserts `taken * multiplier` items into its yield slots. `HollowStore.take(int)` is the new pure method (JUnit). Leftovers stay in the Hollow because only what fits is taken.

### D5 Gifts
On `WORK`, for every Hollow in reach whose `boostTicksLeft()` is below `gift_below_ticks`, the Receptaculum tries its gift slots in order: `offerGift(stack)`; on `TAKEN` it shrinks that slot by one and stops for this Hollow, on `DISLIKED` it tries the next slot, on `NO_GNOME` it skips the Hollow. The gift slots accept any item but the Elixir: `GnomeGiftData` is server data and is not on the client, so the menu could not check it.

### D6 Data
`data/servus_alchemy/servus_alchemy/homunculus/homunculus.json`:
```json
{ "grow_ticks": 48000, "work_interval": 100, "elixir_ticks": 12000, "gift_below_ticks": 1200, "freed_multiplier": 2 }
```
All fields optional with these defaults (`HomunculusSettings.DEFAULT`). The loader keeps the file `servus_alchemy:homunculus`, else the first file by id, else the defaults.

### D7 Freeing
`VasBlock.useItemOn` with a Nether Star: on a grown, not freed homunculus the server shrinks the stack (not in creative), sets `FREED` true, plays the glass break sound and sends the secret lines (`message.servus_alchemy.homunculus.secret.1` to `.4`) with `sendSystemMessage`. Otherwise it returns `TRY_WITH_EMPTY_HAND` so nothing happens. `FREED` is a block state, so the model without the seal shows for everyone. A freed homunculus skips the upkeep and counts as fed.

### D8 Whispers
`WhisperClock` (pure) counts down a delay drawn between 1200 and 3600 ticks. When it fires on a grown, not freed homunculus, the server sends one random line of `message.servus_alchemy.homunculus.whisper.1` to `.5` as an overlay message (`sendOverlayMessage`, used by the Athanor) to every player within 8 blocks that has Elemental Sight (`ElementalSight.active`, checked on the server).

### D9 Sync and client state
The Vas' update tag carries the growth share (0 to 1 as a float), grown, and the reach offsets as an int array; `FREED` is in the block state. The server sends a block update when the homunculus grows a visible step (every 1/16 of its growth), grows up, or the reach list changes.

### D10 Renderer
`VasRenderer` draws nothing unless the local player has Elemental Sight. With it:
- Homunculus, boxes in pixels relative to its body centre (from `receptaculum.html`): body (-2,-1.6,-1.5)..(2,1.6,1.5), head (-2,1.65,-1.4)..(2,5.25,2.2), arms (±2.05..±3.25, -1.5..0.9, 0.3..1.5), eyes 0.6 px squares on the head's front at z 2.2..2.3. Pale skin is `block/white_concrete` tinted `0xFFE8D2C4`, eyes `block/black_concrete`. Inside the flask the body centre is at (8, 4.3, 8) px, scaled by `0.35 + 0.65 * growth`, slowly swaying; freed it sits at (8, 18.4, 8) on the rim at full size. Boxes do not overlap each other (gaps of 0.05 px), so no faces share a plane.
- Chains (grown, not freed): from the neck at (0.5, 0.78, 0.5) blocks, moved 2.2 px toward the target, to the top centre of each Hollow in reach at (dx + 0.5, 0.03, dz + 0.5) relative to the Vas; a link every 1.5 px along the curve, each a box 1.8 px long with a 0.9 x 0.5 px cross section along the curve, every second one turned 90 degrees about it; the curve sags by `0.1 * horizontal length` blocks times `sin(pi t)`. Neighbouring links overlap by 0.3 px at their ends, where their end faces lie 0.3 px apart, never on one plane; turned links have different cross sections (sides at 0.45 and 0.25 px from the axis), so no side faces share a plane. Texture: the family steel `servus_foundation:block/electric_pump_steel`, tinted. Boxes are drawn half a block over and the pose moved back, because the block box drawer takes its texture coordinates from the unit cube.
- Render type: `Sheets.cutoutBlockSheet()`; the glass and liquid of the block model are translucent and draw over it.
- Render bounding box: the Vas' block inflated by 3 horizontally and 1 down.

### D11 Models (pixels; front north at z 0)
Receptaculum (rotated by facing, one model for lit and unlit):
- base, `block/bricks`, (0,0,0)-(16,1,16)
- back wall (0,1,14)-(16,5,16), side walls (0,1,0)-(4,5,14) and (12,1,0)-(16,5,14), lintel (4,4.2,0)-(12,5,14), all `block/bricks`
- slab, `block/polished_deepslate`, (0,5,0)-(16,6,16)
- embers `block/magma` light 15 (4,1,2)-(12,1.6,14)
- flames, new `receptaculum_flame` texture, light 15: (5,1.6,3)-(6.5,3.6,4.5), (7.25,1.6,5)-(8.75,4,6.5), (9.5,1.6,3)-(11,3.2,4.5)
- grate bars, steel: (5.75,1,0.5)-(6.25,4.2,1), (7.75,1,0.5)-(8.25,4.2,1), (9.75,1,0.5)-(10.25,4.2,1)
- body `block/deepslate_bricks` (0.5,6,0.5)-(15.5,14,15.5)
- bands, steel: (3.1,6,0.3)-(3.9,13.9,15.7) and (12.1,6,0.3)-(12.9,13.9,15.7)
- lid `block/deepslate_tiles` (0.3,14,0.3)-(15.7,15.2,15.7)
- hasp, brass: (6.8,11.5,0)-(9.2,14.6,0.5)
- collar, brass: (2,15.2,2)-(14,16,14)

Overlap check: base/walls/slab/body/lid/collar stack on opposite faces only (y 1, 5, 6, 14, 15.2). Walls and lintel meet at x 4 and x 12 with opposite faces; the lintel's front at z 0 and the walls' fronts at z 0 touch only along edges. Embers: bottom on the base top (opposite), sides on the walls (opposite), back on the back wall (opposite); top at 1.6 is free. Flames stand on the embers (opposite faces at 1.6) and touch nothing else. Grate bars: bottom on the base (opposite), top on the lintel bottom at 4.2 (opposite); front at z 0.5 and back at z 1 lie on no other face. Bands: front at 0.3 and back at 15.7 differ from the body's 0.5 and 15.5; their bottom at y 6 meets the slab top (opposite); top at 13.9 is inside the body only where the band crosses the body, not on a face. Hasp: its back at 0.5 lies on the body's front with opposite faces; its front at 0 differs from the bands' 0.3 and the lid's 0.3; its top at 14.6 is in front of the lid only between z 0 and 0.3. Lid front 0.3 vs body front 0.5: different planes. Collar sides at 2 and 14 touch nothing.

Vas (one model per `FREED`):
- glass, new translucent `vas_glass`: foot (4,0,4)-(12,1,12), ring (2.5,0.95,2.5)-(13.5,2.95,13.5), belly (1,2.9,1)-(15,8.9,15), shoulder (2.5,8.85,2.5)-(13.5,10.85,13.5), neck (6,10.8,6)-(10,16.8,10)
- murky liquid, new translucent `vas_liquid`: (2.8,1.05,2.8)-(13.2,3,13.2) and (1.3,2.95,1.3)-(14.7,7.9,14.7)
- not freed only: seal, new `vas_seal` (red wax), (5.4,16.4,5.4)-(10.6,18,10.6); stamp, brass, (6.8,18,6.8)-(9.2,18.3,9.2)

Overlap check: neighbouring glass parts overlap by 0.05 px in height, so the upper part's bottom and the lower part's top are 0.05 px apart, never on one plane; their sides differ (4/12, 2.5/13.5, 1/15, 2.5/13.5, 6/10). Liquid: lower part's top 3 and upper part's bottom 2.95 differ; the glass ring's top 2.95 and the upper liquid's bottom 2.95 face opposite ways; the belly's bottom 2.9 is on no liquid face; liquid sides 2.8/13.2 and 1.3/14.7 differ from every glass side. Seal over the neck: the neck's top 16.8 is inside the seal, its sides 6/10 differ from the seal's 5.4/10.6. Stamp bottom on the seal top at 18: opposite faces. Translucent glass inside translucent glass is accepted (inset, not coplanar).

Shapes: the Receptaculum is a full block; the Vas' collision shape is the belly box (1,0,1)-(15,11,15) plus the neck (6,11,6)-(10,17,10).

### D12 Textures
Made by a Python script in the job directory: `block/receptaculum_brass` (brass with noise), `block/receptaculum_flame` (orange to yellow), `block/vas_glass` (pale green glass, alpha about 80 inside, 140 on the rim pixels), `block/vas_liquid` (murky olive, alpha about 170), `block/vas_seal` (red wax), `item/sealed_flask` (flask with liquid and a red seal) and `gui/receptaculum` (176 x 204 panel, slot frames). Quads take their chunk layer from the sprite's transparency, so the glass and liquid need no render type in the model.

### D13 Recipes
- Athanor `sealed_flask`: Crying Obsidian, 250 Salt, 0 Sulfur, 500 Mercury, tier 3 (Rubedo), 200 ticks, in `AlchemyRecipeTable.athanor()`. It is the first Rubedo craft, so the existing Basel and Salzburg task `rubedo_craft` counts it; no new task.
- Crafting `receptaculum`: `DCD / DID / BBB` with D Deepslate Bricks, C Chest, I Ignis Perpetuus, B Bricks.

### D14 Screen
`ReceptaculumScreen` (176 x 204): gift slots at (8,20), (26,20), (44,20) under the label "Gifts" (y 6 is the title, so the label sits right of the slots at x 64, y 25), Elixir slot at (152,20) with the label "Elixir" left of it, yield 3 x 9 at x 8, y 44..80, the inventory label at y 110, the player inventory at y 122 and hotbar at y 180. No flame, arrow or energy bar; the side panel stays.

### D15 Journal
`WorkshopCategory` adds `receptaculum` ("The Receptaculum") and `vas_hermeticum` ("The Vas Hermeticum") under "The Red Crown", condition the Wandering years research node like the red crown. Pages: spotlight, the Athanor craft text from the recipe table, the crafting page, and short placeholder texts in Paracelsus' voice praising his "faithful little helper". Historical claim to check: Paracelsus' recipe for the homunculus in *De natura rerum* (1537), sealed in a vessel and kept warm (in horse dung) for forty days, then fed on blood ("arcanum of human blood"). The page says "forty days of warmth" and swaps the blood for the Elixir.

## Risks / Trade-offs

- [Gift slots take any item, so a pipe could clog them with ore] → The Auto sides insert only where the item is valid; ore is valid in a gift slot, so a player who pipes ore into the Receptaculum should set that side to Off. Documented in the Workshop.
- [A broken Vas loses its homunculus] → Intended: the flask is cheap compared to the Nether Star, and it keeps players from carrying a grown homunculus around. Listed as an assumption.
- [24 block entity lookups per work interval per Vas] → Cheap, and never per tick.
- [Translucent glass and the cutout homunculus] → The homunculus is drawn in the block entity pass before the translucent chunk layer, so it shows through the murky liquid tinted, like a body in a jar.

## Assumptions to confirm with the user

- Sealed Flask: Athanor craft from **Crying Obsidian** (the item) with 250 Salt and 500 Mercury at the **Rubedo** stage; no Glass Bottle or Bone Meal (the Athanor takes one item; the Glass Bottle would clash with the Bottle o' Enchanting craft).
- Receptaculum recipe: `DCD / DID / BBB` (Deepslate Bricks, Chest, Ignis Perpetuus, Bricks) at a crafting table.
- Slots: 27 yield, 3 gift, 1 Elixir. The gift slots take any item but the Elixir.
- Grow time 48000 ticks (two in-game days), work interval 100 ticks, one Elixir per 12000 ticks (10 minutes) of growing or working, gift when the boost has less than 1200 ticks left, freed multiplier 2 (two items delivered per item taken; the Hollows do not dig faster).
- Each work cycle takes the whole store of every Hollow in reach, as far as the yield slots have room.
- Whispers every 1 to 3 minutes to players with the sight within 8 blocks; five placeholder lines; freed homunculi do not whisper.
- The secret: four placeholder chat lines.
- Breaking the Vas, or the Receptaculum under it, drops the Sealed Flask and loses the homunculus (also a freed one).
- A Nether Star does nothing on a homunculus that is still growing.
- The Receptaculum lights at level 12 and keeps Foundation's default sides (all Auto, front closed), so it pushes its yield into neighbours.
- No new journal task: the Sealed Flask fulfils the existing Rubedo task "rubedo_craft".
- Not built here: the salamander in the Receptaculum's fire under the sight, glass shards of the broken seal.
