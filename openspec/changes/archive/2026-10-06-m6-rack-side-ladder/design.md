# Design

## Context

See proposal.md for motivation. The requirement is in `specs/storage/heavy-duty-rack/spec.md`.

`HeavyRackBlock` is a full cube with no shape override. Vanilla climbing lives in `LivingEntity.onClimbable()`, which NeoForge 21.1.255 routes through `CommonHooks.isLivingOnLadder`. With the default server config that hook only asks the block the entity stands in (`getInBlockState()` at `blockPosition()`) via `IBlockExtension.isLadder`, which defaults to the `minecraft:climbable` tag. A player can never stand inside a full cube, so neither the tag nor an `isLadder` override on the rack is ever consulted. The NeoForge option `fullBoundingBoxLadders` scans the entity's whole bounding box instead, but it is a server side config that defaults to false and we cannot rely on it.

Vanilla ladder motion, verified in `LivingEntity.handleRelativeFrictionAndCalculateMovement` and `handleOnClimbable` of the 21.1.255 sources: before the move, vertical speed is clamped to at least -0.15 and set to 0 for a sneaking player; after the move, `horizontalCollision || jumping` on a climbable sets vertical speed to 0.2; fall distance is reset; gravity and the 0.98 air drag then apply, so a steady climb moves 0.1176 blocks per tick.

## Goals / Non-Goals

**Goals:**
- Climbing that feels like a vanilla ladder on the exposed sides of racks, for players, on client and server.
- No per tick block scanning. A player tick does a handful of block lookups only while the player is airborne or pushing against something.
- Pure, unit tested geometry and motion rules.

**Non-Goals:**
- Using the climbable tag or `isLadder`. See Context.
- Mobs. `PlayerTickEvent` is player only by construction.
- Jump input while climbing. `LivingEntity.jumping` is not readable from common code; pushing into the side is the climb input.

## Decisions

### A player tick handler instead of the ladder hook
`RackClimbing` subscribes to `net.neoforged.neoforge.event.tick.PlayerTickEvent.Post` on the game bus (`@EventBusSubscriber(modid = ServusStorageIds.MOD_ID)`, like `ServusStorageEvents`). It fires at the tail of `Player.tick()` on both logical sides, so position and `horizontalCollision` of this tick are final and the delta movement it sets drives the next tick's move, exactly where vanilla writes the 0.2 climb speed. On the client it acts only for `player.isLocalPlayer()`, since remote players are interpolated. It skips spectators and flying players (`getAbilities().flying`). Early exit when `onGround() && !horizontalCollision`, which covers almost every tick of a player walking around. Alternative considered: a mixin into `onClimbable`. Rejected: the project has no mixins and the event does the job.

### Contact detection on the bounding box
A rack side is touched when a vertical face of the player's bounding box lies on a block boundary within a small tolerance (collision leaves the box exactly on the boundary) and a `HeavyRackBlock` stands in a cell directly beyond that face. For each flush face, the cells beyond it span the box's extent on the other horizontal axis (one or two columns) and its height (two or three rows), which is at most a dozen `getBlockState` calls and usually none. The touched face of the rack is a side when it is perpendicular to the rack's `FACING`: `facing.getStepX() * dx + facing.getStepZ() * dz == 0` for the step `(dx, dz)` from the player into the cell. Front and back fail that test. Alternative considered: ray casting from the player's look direction. Rejected: climbing depends on contact, not on where the player looks.

### Motion mirrors vanilla ordering
Since the handler runs after gravity and drag were applied for the next tick, it writes the post gravity values directly:
- pushing (`horizontalCollision` while touching a side): vertical speed becomes `(0.2 - gravity) * 0.98` with `gravity = player.getGravity()`, which is the vanilla steady climb of 0.1176 per tick at default gravity;
- not pushing: vertical speed is clamped to at least -0.15, giving the vanilla slide; a sneaking player gets 0 and holds;
- in both cases `resetFallDistance()`.

Horizontal speed is left alone. On the server the position comes from movement packets, so the motion write is harmless there and the fall reset is what matters: `ServerGamePacketListenerImpl` accumulates `fallDistance` from packets and would otherwise apply damage on landing. The server's floating kick is not triggered because `noBlocksAround` sees the rack beside the player, and the moved wrongly check passes because the client's positions stay collision valid.

### Climb feel is code, not datapack
The 0.2, -0.15 and 0.98 values are vanilla ladder parity, not balance numbers. They are named constants in `RackClimb`. If a different feel is ever wanted, `rack.json` can grow a `climb` section then.

### Pure logic in `RackClimb`
`storage/.../rack/RackClimb.java`, no Minecraft imports:
- `record Box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ)`.
- `record Contact(int cellX, int cellY, int cellZ, int dx, int dz)`: a cell beyond a flush face and the step from the box into it.
- `static List<Contact> contacts(Box box, double tolerance)`: flush faces and the cells beyond them.
- `static boolean isSide(int facingDx, int facingDz, int dx, int dz)`: the touched face is perpendicular to the facing.
- `static double verticalMotion(double current, boolean pushing, boolean sneaking, double gravity)`.

`RackClimbing` converts `AABB` to `Box`, `Direction` to steps and applies the result to the player.

## Pure versus Minecraft logic

- Pure: `RackClimb` (contacts, isSide, verticalMotion). JUnit `RackClimbTest`.
- Minecraft: `RackClimbing` (event, block lookups, player state). GameTest `rackSideClimbsAndFrontDoesNot`: a mock player from `helper.makeMockPlayer(GameType.SURVIVAL)` is placed flush against the west side of a north facing rack with `fallDistance = 4` and falling speed, `RackClimbing.tick(player)` is called directly, fall distance must be 0 and vertical speed -0.15; with `horizontalCollision = true` the speed must be `(0.2 - gravity) * 0.98`; against the front with `fallDistance = 4` nothing changes. The feel is checked in game.

## Risks / Trade-offs

- [Corner between a wall and a rack side: colliding with the wall while touching the side also climbs] -> Same as a vanilla ladder in a corner. Accepted.
- [Leaving the top of a stack: once the feet clear the top the collision ends and climbing stops] -> The remaining upward speed and the forward input carry the player onto the top, as with a ladder. Verified in game; if it sticks, extend the contact check by the step height.
- [Mock player and `getGravity()`] -> The attribute exists on every `LivingEntity`, so the mock player returns the default 0.08.
- [Another mod's full bounding box ladder config] -> Unrelated; the rack never uses the ladder hook.
