# Tasks

## 1. Pure walk and direction logic (lib)

- [x] 1.1 Add `lib/src/main/java/io/github/dirnei/mc/servus/lib/placement/RunWalk.java` (no Minecraft imports): `walk(start, step, isRun, isFree, maxReach)` returning the target or empty, as in design.md. Add `RunWalkTest` covering: run of one, extend from the middle, run ends at a blocked spot, target exactly at reach, target one past reach, vertical step, and `isRun` never asked about the start. Verify with `gradlew :lib:test`.
- [x] 1.2 Add `lib/.../placement/QuadrantDirection.java` (no Minecraft imports): `nearestEdge(x, z)` with deterministic ties. Add `QuadrantDirectionTest` covering the four edges, the centre and the diagonals (ties). Verify with `gradlew :lib:test`.

## 2. Lib config and item hook

- [x] 2.1 Add the `placement` section with `placementAssistReach` (int, 1 to 64, default 16) to `lib/.../config/ServusLibConfig.java`, plus a `placementAssistReach()` getter. Verify with `gradlew :lib:build` and by checking that `servus_lib-common.toml` gets the commented option on a dev server start.
- [x] 2.2 Add `lib/.../placement/PlacementAssist.java` (`isStart`, `isRun`, `direction`), `AssistedItem.java`, and `PlacementAssists.java` (`target(...)` and `place(context, assist, fallback)` using `BlockPlaceContext.at(context, target.relative(dir.getOpposite()), dir)`, FAIL without a target, fallback without sneak or start). Verify with `gradlew :lib:build`.
- [x] 2.3 Add `lib/.../placement/AssistedBlockItem.java` (a `BlockItem` taking a `PlacementAssist` that overrides `place` and implements `AssistedItem`). Verify with `gradlew :lib:build`.

## 3. Client preview (lib)

- [x] 3.1 Add `lib/.../client/PlacementAssistRenderer.java`: a `RenderLevelStageEvent` subscriber at `AFTER_TRANSLUCENT_BLOCKS`, client only. While the player is sneaking with an `AssistedItem` in the main hand and looking at a block, it draws the translucent ghost of the placement state at the target and an arrow of quads in the walk direction, and draws nothing without a target. Verify with `gradlew :lib:build`. The visual check happens in 6.2.

## 4. Warehouse Rail (storage)

- [x] 4.1 Add `storage/.../rail/RailAssist.java` (run = `RailBlock`, side face hit uses the face, top or bottom hit uses `QuadrantDirection` on the local hit point) and register `ServusStorageItems.RAIL` as an `AssistedBlockItem` with it. Verify with `gradlew :storage:build`.
- [x] 4.2 Add storage GameTests to `ServusStorageGameTests.java` that use a mock sneaking player with a rail in hand and `stack.useOn`: quick straight on a floor (rail lands past the 4th), side face hit, walk passes a junction, run ends at stone (nothing placed, stack count unchanged), bridge between two racks (middle placed, all three stay), overhang collapses, and a non-sneak click places against the face. Verify with `gradlew :storage:runGameTestServer` and with all storage GameTests green.

## 5. Utility Duct (logistics)

- [x] 5.1 Add `logistics/.../duct/DuctAssist.java` (start = any `UtilityDuctBlock`, run = duct whose block entity `has(line)`, direction = clicked face). Make `LineItem` implement `AssistedItem` and override `place` through `PlacementAssists.place`, keeping the non-sneak add-line branch in `useOn`. Verify with `gradlew :logistics:build`.
- [x] 5.2 Add logistics GameTests to `ServusLogisticsGameTests.java`: sneak extends a run of six from the second duct, sneak extends a column upwards, sneak on a duct lacking the line still places above it when free, a run blocked by a duct of another line places nothing, and a non-sneak click on a duct lacking the line still adds the line. Verify with the logistics GameTest server run and with all logistics GameTests green.

## 6. Plan and integration

- [x] 6.1 Update `PLAN.md`: add "Sneak placement assist for rails and ducts" to M6 before the pick rate stats screen, and add the placement assist to the Lib API sketch. Verify that `openspec validate m6-placement-assist` passes and that `gradlew build test` is green across all projects.
- [ ] 6.2 Play check in the dev client, launched by the user from the main checkout. Check that the ghost and arrow appear only while sneaking with a rail or line item, that they follow the top face quadrant on rails and the clicked face on ducts, that they disappear when blocked or out of reach, and that rails bridge a rack gap by sneak clicking. If the translucent ghost renders opaque, switch to the outline fallback from design.md and check again.
