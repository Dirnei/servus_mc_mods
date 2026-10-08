# Tasks

## 1. Pure logic

- [x] 1.1 Add `storage/cpu/CpuShape` (candidate cube origins for a block, lowest first; part index of a position) and `storage/cpu/CpuChoice` (first CPU with a free slot, else fewest open, earliest on a tie). JUnit `CpuShapeTest` (`twentySevenOriginsLowestFirst`, `partIndexRoundTrip`) and `CpuChoiceTest` (`firstFreeSlotWins`, `allFullPicksFewestOpen`, `tieGoesToEarliest`, `emptyListChoosesNothing`). Verify with `gradlew :storage:test`.

## 2. CPU block

- [x] 2.1 Add `CpuBlock` and `CpuBlockEntity` (FORMED, PART, carrot forming, unform on removal, data_link capability while formed, summary click), register block, item, block entity, `cpu.json` with `order_slots` in `StorageStatsManager`, datagen blockstate, models, loot, pickaxe tag, recipe, lang and two textures. Verify with `gradlew build` and `:storage:runData`.

## 3. Orders on the CPU

- [x] 3.1 Rework `OrderBook` (new Host, `Producer`, slots, WAITING, pending shipments, collection hook, busy check), `OrderStep` (one producer position, pending feeds, collecting in flight), `WorkOrder` (WAITING). Add `CpuOrders` and the CPU host in `CpuBlockEntity` (producers from the data line, stock, feed by ship, collect by `DataPackets.move` to an Access Point). Verify with `gradlew build`.
- [x] 3.2 Move order creation to CPUs: `TerminalRequests` (create, state, cancel), `WarehouseNetwork` stock rules (`no_cpu` state, lang) and `order()`, commands `orders`, `order`, `producers` on the data line. Remove the order book, producers, board rows and Inbound credit from `WarehouseNetwork` and `producer()` from the Outbound Station. Verify with `gradlew build`.
- [x] 3.3 Remove the Order Board (block, block entity, item, menu, screen, client cache, payloads, datagen, lang, textures). Verify with `gradlew build`, `:storage:runData` and a grep for `OrderBoard` returning nothing in `storage/src`.

## 4. Tests

- [ ] 4.1 Add `CraftingCpuGameTests`: `cpuFormsAndUnforms`, `cpuIncompleteCubeDoesNotForm`, `cpuOrderCraftsShortfallOverTheLine`, `cpuSlotsQueueThirdOrder`, `twoCpusShareOrders`, `cpuUnformCancelsOrders`, `stockRuleOrdersThroughCpu`, `stockRuleWithoutCpu`, `noCpuLeavesShortfallMissing`. Migrate `orderWithoutProducerIsRefused`, `twoLevelOrderRunsLeavesFirst`, `cancelReleasesReservation` and the stock rule tests to the CPU setup; delete `outboundReportsProducer`, `outboundFacingChestIsNoProducer`, `orderCraftsMissingItems` (replaced) and the `orderBoard*` tests. Verify with `gradlew test` and `:storage:runGameTestServer`, `:foundation:runGameTestServer`, `:manufacturing:runGameTestServer` green.

## 5. Docs and plan

- [ ] 5.1 Update `docs/src/content/docs/mods/storage.md` (Crafting CPU section, order board gone, outbound station wording), PLAN.md (settle the CPU choice in section 5, M8 item text), regenerate `docs/src/data/gamedata.json` with `:manufacturing:runData`. Verify with `npx vitest run` and `npm run build` in `docs/`.

## 6. Integration

- [ ] 6.1 Full `gradlew build` and `gradlew test`, all three GameTest runs green, `runClientAll` reaches the title screen without asset warnings for the CPU.
