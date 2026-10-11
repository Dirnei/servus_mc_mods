## Context

Both rails share `storage/datagen/RailModels`: a 6 by 6 centre plate 1 pixel high (pure `rail/RailCenter` picks straight, curve or cross and its rotation), a 6 pixel wide arm per connected side, and a dock piece per side that touches a storage block, all in flat `rail*` / `farm_rail*` textures and a flat item sprite. `client/ElevatorRenderer` draws the same rectangles with rotated top UVs on decks, the car and the lowest floor. `client/ShuttleGarageRenderer` draws shuttles at 0.8 scale with `FLOOR = 0`, so the wheels (about 2.4..3.6 and 12.4..13.6 pixels) stand on the ground beside the strip. `RailBlockEntity` (a `StorageNodeBlockEntity`) already exists for every rail. The Field Head became a wood block in `m10-field-head-look`; the stations use the same Foundation wood API (`Wood.register`, `WoodContent.WOOD`, `WoodSlot`, `WoodHolder`, `WoodModel`, `WoodRecipes.output`).

## Goals / Non-Goals

**Goals:**
- Wide gauge track (heads 3..5 and 11..13, y 1..2; four sleepers 1..15 wide, 2 long, y 0..1) for every piece, both rails, the elevator rail and the item.
- Sleepers in the recipe's wood, kept through place, drop and dismantle.
- Shuttles on the heads.
- Pure geometry with JUnit tests, one source for datagen and the elevator renderer.

**Non-Goals:**
- Copper corrosion stages (`m10-rail-corrosion` later).
- Any change to connections, docks, spans or networks.
- Wood on the elevator rail (the elevator has no wood; its rail uses oak sleepers).

## Decisions

### Pure `RailLook`: pieces and boxes
`storage/rail/RailLook` (no Minecraft imports) owns the geometry in pixels:
- `Piece` per rail mask, from the sides that carry rail: `SINGLE` (no side: a centre piece z 4..12 with two sleepers), `END` (one side: heads from that edge to z 12, three sleepers), `STRAIGHT` (two opposite sides), `CURVE` (two sides on a corner), `CROSS` (three or four sides). `RailLook.of(north, east, south, west)` returns the piece and its clockwise rotation (0, 90, 180, 270 like a blockstate y rotation). Straight, curve and cross take shape and rotation from `RailCenter`, so the elevator and the outside rail keep agreeing; `END` turns its arm to the connected side (north 0, east 90, south 180, west 270).
- `Box(part, x0, y0, z0, x1, y1, z1)` with `Part` `SLEEPER`, `HEAD` or `DOCK`, and `rotated(rotation)`, which maps (x, z) to (16 - z, x) per quarter turn.
- `boxes(piece)` unrotated (straight along north to south, curve joining north and east, end arm towards north). Heads of straight pieces are one box per head; the cross splits the east west heads at the north south heads so no two head boxes overlap (no coplanar tops).
- The curve is rasterised per pixel around the north east corner (16, 0): a pixel whose centre lies at distance 3..5 or 11..13 from the corner is a head pixel (exactly the straight gauge where the arc meets the north and east edges), a pixel at distance 1..15 whose arc position `t = angle / 90 * 16` has `t mod 4` in 1..3 is a sleeper pixel (radial sleepers spaced like the straight ones). Runs of pixels in one row merge into one box. The pixel sets are symmetric about the diagonal through the corner.
- `dock()`: the buffer beam x 1..15, z 0..1, y 0..3 (`DOCK`). `dockInline()`: heads z 1..4 and one sleeper z 1..3, drawn when the rail runs straight into the dock, so the heads reach the beam.

JUnit `RailLookTest`: piece choice for all 16 masks, rotation round trip, straight gauge and sleeper spacing, curve heads at the north edge (x 3, 4, 11, 12) and the east edge (z 3, 4, 11, 12), curve symmetry, all boxes inside the block and at most 2 high (3 for the dock beam), no overlapping head boxes in the cross.

### Datagen: one model per piece, docks apart
`RailModels.rail(provider, block, name)` writes `<name>_single`, `_end`, `_straight`, `_curve`, `_cross`, `_dock` and `_dock_inline` from `RailLook` boxes. Each box is one element; sleepers use `#wood` (= `Wood.BOARD`, `servus_foundation:block/wood_board`), heads `#head` (`block/rail_head` or `block/farm_rail_head`), dock beams `#dock` (the existing stripe textures). Faces at y 0 get `cullface: down`. UVs stay the element defaults, so a rotated model turns its grain with it.

The multipart keeps today's per mask parts (16 masks, each conditioned on all four sides with the dock flags, so exactly one piece applies) and maps each mask to `RailLook.of`. Mask 0 is split in two parts: the single piece turns east west when a dock lies east or west and none north or south, else north south. The dock beam is a part per side on `DOCK_<side>`; the inline piece is a part per side on `DOCK_<side>` with both perpendicular sides not carrying rail. The old arm and centre models and the textures `rail`, `rail_curve`, `rail_cross`, `farm_rail`, `farm_rail_curve`, `farm_rail_cross` are removed.

The item model is the straight piece with its own display transforms (gui seen from above at an angle, fixed lying flat), so `WoodModel`'s item override shows the stack's wood.

### Rails are wood blocks
- `RailBlockEntity implements WoodHolder` with a `WoodSlot`, forwarding save, load, update tag, update packet, implicit components, `removeComponentsFromTag` and model data, copied from `ShuttleGarageBlockEntity`.
- `ServusStorage` adds `RAIL` and `FARM_RAIL` to the `Wood.register` list; `WoodModels` wraps their multipart block state models and item models.
- `WoodModel` and multipart: NeoForge's `MultiPartBakedModel` implements `IDynamicBakedModel` and `getQuads(state, side, rand, ModelData, RenderType)` picks the parts by state and passes model data through `MultipartModelData.resolve` (checked in `neoforge-21.1.255-sources.jar`). `WoodModel` asks its base with `ModelData.EMPTY` and caches by wood, state, side and render type, so the wrapped multipart works without a Foundation change. Hanging rails are no separate model (the kind only decides support), so nothing else to wrap.
- New `rail/RailItem extends AssistedBlockItem` adds the wood tooltip line of `WoodBlockItem`; placement assist calls `BlockItem.place`, which applies the stack's components to every placed rail of a run.
- Loot: both rails move from `SELF_DROPPING` to `WOODEN` (copies `WoodContent.WOOD` from the block entity).
- Dismantle: `ConnectableBlock.onWrench` in Foundation pops `new ItemStack(this)`; it now uses `Wood.stamp(new ItemStack(this), blockEntity)`, a no-op for blocks without wood (ducts).
- Recipes through `WoodRecipes.output`:

```
I I      C C
IPI      CPC     I iron ingot, C copper ingot, P planks; 8 rails each
I I      C C
```

The Farm Rail no longer needs Warehouse Rails and a composter. Neither pattern collides with the vanilla rail (stick in the middle).

### Rail head texture, one sprite per rail type
`rail_head` is light steel (176,180,190 with 150,154,164 and 120,124,134 shading); `farm_rail_head` is vanilla copper block tones with a few exposed copper pixels mixed in. Every head face of a rail type's models references the one `#head` variable, so `m10-rail-corrosion` can swap it per block the way `WoodModel` swaps the board sprite (or by a block state that selects the head texture), without touching the geometry.

Textures are 16 by 16 PNGs from a Python PIL generator in the job tmp dir.

### Elevator rail
`ElevatorRenderer` replaces its rectangles with `RailLook` boxes: `RailLook.of` for the floor's sides, boxes rotated into the world frame, then clipped to the deck, car and stub areas as before. The top face keeps the turned UV mapping so pieces cut at the deck edge line up. Heads use `servus_storage:block/rail_head`, sleepers `minecraft:block/oak_planks`. Sleepers are clipped to the shaft area between the posts, so no sleeper tip stays behind when a flap folds. Rail boxes stand on the deck plate (y 0..2), as outside.

### Heads one pixel inward (play check)
The first play check showed heads at 2..4 and 12..14 running into the lift car: the car's post sleeves reach to 3 and from 13, so the heads cut into them. The heads move to 3..5 and 11..13 on every piece through `RailLook.HEADS`; the curve band follows (distance 3..5 and 11..13 from the corner, so the arc meets the edges at pixels 3, 4, 11 and 12), the cross splits at the new heads, and datagen and `ElevatorRenderer` pick it up without own numbers. Sleepers stay 1..15, and the elevator still clips them to the shaft area 2..14.

### Shuttles on the heads
`ShuttleGarageRenderer.FLOOR` becomes `2 / 16`. The car's height comes from the lift and the car rail stands on the car the same way, so riding shuttles are lifted too. No other renderer places shuttles or cargo on rails.

### Wheels that show (play check)
The play check showed shuttles as boxes hovering over the rail. The composition was not at fault: `ShuttleLook.speedStep(0)` is 0, so `ShuttleItemModel.compose` always adds `drive_0`; `ServusStorageClient.onRegisterAdditional` registers every part from `ShuttleItemModel.parts` as a standalone model, the generated `models/shuttle/drive_*.json` exist and the client log shows no model errors; `renderStatic` resolves the item overrides. The cause was the drive geometry: the wheels were 1 model pixel wide coal slivers at x 1..2 and 14..15 (0.8 block pixels after scaling), y 0..3, so only 1.6 block pixels of thin black showed under the chassis (model y 2..4, block 3.6..5.2 above the ground), and after the heads moved inward they stood mostly beside the heads.

The drive parts are rebuilt for the 0.8 scale around the block centre, where model x `(t - 8) / 0.8 + 8` lands on block pixel t (heads 3..5 and 11..13 give 1.75..4.25 and 11.75..14.25):
- Wheels: a tread at x 2..4 and 12..14, y 0..2, under the chassis, which is block 3.2..4.8 and 11.2..12.8, on the heads. Outside the chassis a disc at x 1..2 and 14..15 as tall as the wheel (4 for four wheels at z 3 and 9, 3.5 for six wheels at z 2.25, 6.25 and 10.25), rounded by a full height core and a slightly inset full length band, with a 1 pixel iron hub at x 0.5..1 and 15..15.5. The disc rises above the chassis bottom, so the wheel reads as a wheel from the side.
- Tracks: x 1.75..4.25 and 11.75..14.25, y 0..3, z 2..14, exactly the heads, with three iron road wheel hubs per side. The racing exhaust stacks stay at x 1..2 and 14..15 from y 3 on the track tops.
- Every wheel and track bottom is model y 0, which `FLOOR` puts on the head tops (block y 2). The drive spans x 0.5..15.5, so the shuttle stays inside its block; the Handling mast (z 1..2) and the Harvester arms (from y 3) do not meet the drive, and the lift car's post sleeves (to x and z 3 and from 13) stay clear of the discs.

## Risks / Trade-offs

- [Curve made of many small elements] about 60 boxes; baked once, fine for chunk meshing.
- [Coplanar faces where a dock beam meets a crossing's head ends] only when a rail crosses three other rails next to a station; accepted.
- [Different woods do not stack] accepted by the user.
- [Item model shape] the gui angle is settled in the play check.
- [Elevator sleepers in oak] the elevator has no wood of its own; a rail of another wood shows oak sleepers inside the shaft.
