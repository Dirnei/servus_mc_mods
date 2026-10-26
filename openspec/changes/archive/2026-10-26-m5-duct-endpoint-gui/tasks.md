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
- [x] 4.3 `integration/jei/DuctFilterGhostHandler` still targets the moved filter slots (uses `filterSlotArea`). Check in game with JEI. `gradlew build` green.

## 5. Data Potato

- [x] 5.1 `logistics/duct/DuctReport`: Control and paused state per item, power and fluid side line, powered or unpowered on the centre report; lang keys in `LogisticsDataParts`. Extend `DataPotatoGameTests` with a fluid side While unpowered under a powered duct reporting paused. `gradlew :foundation:runData`, `gradlew build` and GameTests green.

## 6. Rework after the first play check

- [x] 6.1 Pure `logistics/duct/DuctMode` (six flag combinations, `of`, toggles, `forLine`, `bySaved` with the older `SideMode` names) and `logistics/duct/DuctRedstone` (`IGNORE`, `POWERED`, `UNPOWERED`, `met`, `active(on, powered)`, `next`, `byId`, `legacyOff`), replacing `DuctControl`. JUnit `DuctModeTest` (flag round trip, Pull needs In, toggles, only none disconnects, line rules, saved and older names) and `DuctRedstoneTest` (met table, active with On/Off, cycle, ids, older Control ids); `EndpointTallyTest` on the flags. `gradlew :foundation:test` green.
- [x] 6.2 `logistics/duct/UtilityDuctBlockEntity`: modes as `DuctMode`, `redstone` and `switchedOn` per line per side with getters and setters, `isActive` = on and setting met, save under `"Redstone"` and `"SwitchedOff"`, load older `"Modes"` names and `"Controls"` (Off as switched off with Ignore). Networks (`LineNetwork`, `ItemChannelNetwork` pulls only with Pull, `FluidChannelNetwork`, `EnergyChannelNetwork`, `EndpointTally`) and `DuctItemHandler`, `DuctFluidHandler` read the flags. `gradlew build` green.
- [x] 6.3 `logistics/menu/UtilityDuctMenu`: `BUTTON_TOGGLE_IN` (10), `BUTTON_CYCLE_REDSTONE` (50), `BUTTON_TOGGLE_OUT` (60), `BUTTON_TOGGLE_PULL` (70), `BUTTON_TOGGLE_ON` (80, the data line's on and off), each plus the line; `FILTER_Y` 73, `INVENTORY_Y` 122. `gradlew build` green.
- [x] 6.4 Screen: `logistics/client/DuctSideTabs` (six fixed tab places on top of the page, greyed duct tabs, no tab for empty sides), `logistics/client/DuctIconButton` (icon toggles with tooltips, trash can icon), `UtilityDuctScreen` layout per design D6 with the 176 by 204 page and its original texture, tooltips drawn also for the greyed Pull button. `gradlew build` green.
- [x] 6.5 Lang in `logistics/datagen/LogisticsDataParts` (flag names and explanations per line, redstone setting names and tooltip, On/Off and its tooltips, trash can hint, flag names for the potato, "switched off"); `DuctReport` names flags, redstone setting and switched off or paused. `gradlew :foundation:runData` and `gradlew build` green.
- [x] 6.6 GameTests in `logistics/gametest/DuctControlGameTests`: switched off side keeps its arm, flags and redstone setting and moves again when on; While powered follows a lever; switched off fluid side refuses pushed fluid; In only accepts pushed items and does not pull; In, Out and Pull never routes back; lines independent; settings and In, Out and Pull survive reload; an older save with `input`, `output`, `disabled` and Controls Off and While powered loads with the mapped flags, On/Off and redstone; the menu buttons enable an unconnected side again, Pull switches In on with it, On/Off keeps redstone and flags. `DataPotatoGameTests` checks the new names and "switched off". Duct GameTests in Foundation, Shuttles and Alchemy use `DuctMode`. `gradlew :foundation:runGameTestServer` and `gradlew :shuttles:runGameTestServer` green.

## 7. Plan, play check and wrap up

- [x] 7.1 Add to PLAN.md "Open: Foundation": the duct overflow work after this change (`m5-duct-fluid-filter`, `m5-duct-keep-amounts`), a possible Overflow Valve block, and warehouse excess on stock rules (a rule with only a maximum ships the surplus to a station). Verify the lines are there.
- [x] 7.2 Play check from main: carrot on centre, on a duct arm and on an endpoint arm opens the right side; side tab icons for chest, furnace, tank, a modded block and a disguised Gnome Hollow; greyed duct tabs not clickable and no tab for empty sides; a side with neither In nor Out can be connected again; switching sides keeps the line tab; In, Out and Pull toggles with their tooltips, Pull switches In on with it, In only does not pull; the redstone button shows gunpowder, lit torch, unlit torch; On/Off pauses with the arm and redstone setting kept; lever and comparator control a side; the trash can sits apart and its tooltip warns; the potato reports flags, redstone setting, switched off and paused.
- [x] 7.3 Full gradle build, tests green, game launches with all three mods.

## Workflow follow-up

- Archive with `/opsx:archive`, add player lines under `## Unreleased` in `foundation/CHANGELOG.md` (Added: duct side tabs, In, Out and Pull, redstone setting and On/Off per line and side, a trash can for removing a line; Fixed: unconnected duct sides can be connected again).
