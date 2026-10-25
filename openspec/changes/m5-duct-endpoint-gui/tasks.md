# Tasks

## 1. Pure logic

- [x] 1.1 Add pure `foundation/logistics/duct/DuctControl` (`ALWAYS`, `POWERED`, `UNPOWERED`, `OFF`, `active(powered)`, `next()`, `id()`, `byId(int)` with unknown ids as `ALWAYS`). JUnit `DuctControlTest`: activity table for both powered states, cycle order with wrap, id round trip, unknown id. `gradlew :foundation:test` green.
- [x] 1.2 Add pure `foundation/logistics/duct/DuctSides.startSide(boolean[] selectable, int clicked)` (clicked side when selectable, else first selectable in direction order, else -1). JUnit `DuctSidesTest`: clicked selectable, clicked not selectable falls to first, centre click (-1), none selectable. `gradlew :foundation:test` green.

## 2. Block entity and redstone

- [x] 2.1 `logistics/duct/UtilityDuctBlockEntity`: `controls[line][side]` with `control`, `setControl` (marks dirty, syncs), `isActive(line, side)`, save under `"Controls"` (non-default only) and load with Always as default, include in the update tag. `powered` flag set from `level.hasNeighborSignal` in `onLoad` and `neighbourChanged`, saved and synced. `gradlew build` green.
- [x] 2.2 Networks honour Control: `network/ItemChannelNetwork` (`pull` skips inactive sources, `capacity` 0 for inactive targets), `network/FluidChannelNetwork` (`Endpoint` gets duct and side, `tickChannel` and `fill` skip inactive endpoints), `network/EnergyChannelNetwork` (skip inactive ends), `duct/DuctItemHandler` and `duct/DuctFluidHandler` (accept nothing on an inactive side). `gradlew build` green.
- [x] 2.3 GameTests in `logistics/gametest/ServusLogisticsGameTests`: item pull side with Control Off moves nothing and keeps its arm; While powered moves only while a lever beside the duct is on; fluid Control Off refuses fluid pushed into the duct; item While unpowered and power Always on one machine side under a powered duct gives energy and no items; Controls survive save and load. GameTests green.

## 3. Opening and the menu

- [x] 3.1 `logistics/duct/UtilityDuctBlock.onWrench`: open from the centre and any arm with the start side from `DuctSides.startSide` over the six selectable flags (neighbour not air and not a duct); nothing opens without a selectable side; sneak and panel handling unchanged. `gradlew build` green.
- [x] 3.2 `logistics/menu/UtilityDuctMenu`: mutable `side`, `BUTTON_SELECT_SIDE` (40 to 45) checked against the selectable rule on the server, refresh priority and `sendCableChannels(true)` on change; `BUTTON_CYCLE_CONTROL + line.ordinal()` (50 to 52) cycling Control for item, power and fluid. GameTest: open the menu for a duct whose west side toward a chest is disabled from the centre, select west, cycle the item mode back to in and out, and the arm returns. `gradlew build` and GameTests green.

## 4. Screen

- [x] 4.1 `logistics/client/UtilityDuctScreen`: side picker row with clone item icons (`getCloneItemStack` with player, fallback `asItem`, empty frame for air, grey overlay for ducts), highlight, tooltip with side and block name, click sends `BUTTON_SELECT_SIDE`; neighbour name under the picker; page content moved down per design D6; mode and Control as two half width buttons; tabs below the picker; `UtilityDuctMenu.FILTER_Y` and `INVENTORY_Y` moved accordingly. `gradlew build` green.
- [x] 4.2 Taller `foundation/src/main/resources/assets/servus_foundation/textures/gui/utility_duct.png` (238 px page) and lang keys in `logistics/datagen/LogisticsDataParts` (Control button, the four Control names reusing the Chimney wording plus Off, Control tooltip, side names, picker tooltip). `gradlew :foundation:runData` and `gradlew build` green.
- [ ] 4.3 `integration/jei/DuctFilterGhostHandler` still targets the moved filter slots (uses `filterSlotArea`). Check in game with JEI. `gradlew build` green.

## 5. Data Potato

- [x] 5.1 `logistics/duct/DuctReport`: Control and paused state per item, power and fluid side line, powered or unpowered on the centre report; lang keys in `LogisticsDataParts`. Extend `DataPotatoGameTests` with a fluid side While unpowered under a powered duct reporting paused. `gradlew :foundation:runData`, `gradlew build` and GameTests green.

## 6. Plan, play check and wrap up

- [x] 6.1 Add to PLAN.md "Open: Foundation": the duct overflow work after this change (`m5-duct-fluid-filter`, `m5-duct-keep-amounts`), a possible Overflow Valve block, and warehouse excess on stock rules (a rule with only a maximum ships the surplus to a station). Verify the lines are there.
- [ ] 6.2 Play check from main: carrot on centre, on a duct arm and on an endpoint arm opens the right side; picker icons for chest, furnace, tank, a modded block and a disguised Gnome Hollow; greyed duct and empty sides not clickable; a disabled side can be re-enabled; switching sides keeps the tab; Control Off pauses with the arm kept; lever and comparator control a side; the potato reports Control and paused.
- [ ] 6.3 Full gradle build, tests green, game launches with all three mods.

## Workflow follow-up

- Archive with `/opsx:archive`, add player lines under `## Unreleased` in `foundation/CHANGELOG.md` (Added: duct side picker, Control with redstone per line and side; Fixed: disabled duct sides can be enabled again).
