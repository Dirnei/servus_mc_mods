# Tasks

## 1. Foundation link role

- [x] 1.1 Add `DataLink` and `ServusFoundationCapabilities.DATA_LINK`, `DataRole.LINK`, the link check in `DataMembership.isMember` and the fourth cache in `DataNetwork`'s probe (`foundation/src/main/java/.../foundation/{production,data}`). Verify with `gradlew build` and `gradlew test`.

## 2. Pure choice

- [x] 2.1 Add `storage/access/AccessPointChoice` (no Minecraft imports) and `AccessPointChoiceTest` with `lowestTripWins`, `tieKeepsFirst`, `unreachableLoses`, `allUnreachablePicksFirst`, `emptyHasNone`. Verify with `gradlew :storage:test`.

## 3. Access Point block

- [x] 3.1 Add `storage/access/AccessPointBlock`, `AccessPointBlockEntity` (intake on all sides, outgoing buffer as `DeliveryTarget`, both dropped when broken, carrot dismantles), registries (block, block entity, item, creative tab), capabilities (item handler, `DATA_LINK`) in `ServusStorageEvents`. Verify with `gradlew build`.
- [x] 3.2 Datagen: blockstate via the station helper, lang, loot, pickaxe tag, recipe; teal textures from the Inbound Station textures with a helper script in the session helpers. Run `:storage:runData` and `:manufacturing:runData`, check the files, `gradlew build`.

## 4. Shipping

- [x] 4.1 Shipments on `AccessPointBlockEntity` (FIFO, one running delivery, NBT), `WarehouseNetwork.ship` with the choice and planning, `ServusStorageShipping.ship`, and `/servus_warehouse ship`. Verify with `gradlew build`.
- [x] 4.2 GameTests in `storage/.../gametest/AccessPointGameTests.java`: `accessPointIsLinkMember`, `accessPointJoinsWarehouse`, `packetsArePutAway`, `shipFromCrate`, `shipFromRackByShuttle`, `nearerAccessPointWins`, `shipRefusedWithoutAccessPoint`, `brokenAccessPointDropsBuffers`. Verify with `:storage:runGameTestServer` all green, plus `:foundation:` and `:manufacturing:` GameTests.

## 5. Docs and integration

- [x] 5.1 Add an Access Point section to `docs/src/content/docs/mods/storage.md` and the link role to `foundation.md`; run `npx vitest run` in `docs/`. Full `gradlew build` and `gradlew test` green, all three GameTest suites green, `runClientAll` reaches the title screen.
