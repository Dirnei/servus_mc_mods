# Tasks

Apply after `m5-duct-endpoint-gui` and `m5-duct-fluid-filter`.

## 1. Pure logic

- [x] 1.1 Add pure `foundation/logistics/duct/KeepAmounts` (`giveAllowance`, `receiveAllowance`). JUnit `KeepAmountsTest`: 300 keep 64 offer 1,000 gives 236; 40 keep 64 gives 0; 13 up to 16 offer 64 takes 3; no amount passes the whole offer; held above `Integer.MAX_VALUE` does not overflow. `gradlew :foundation:test` green.
- [x] 1.2 `logistics/duct/FluidFilter`: amount per slot, cleared with the slot, `amountFor(id)` first match wins. Extend `FluidFilterTest`. `gradlew :foundation:test` green.

## 2. Block entity and networks

- [x] 2.1 `logistics/duct/UtilityDuctBlockEntity`: `itemAmounts[side][slot]`, cleared when the slot changes item or clears, `amountFor(side, stack)`, `amountFor(side, fluid)`, setters with clamps, save under `"FilterAmounts"` and with the fluid filter, update tag, dismantle. `gradlew build` green.
- [x] 2.2 `logistics/network/ItemChannelNetwork`: `pull` caps by `giveAllowance` over the held count, `capacity` caps by `receiveAllowance` for targets with an amount. `gradlew build` green.
- [x] 2.3 `logistics/network/FluidChannelNetwork`: source offers capped by keep, `Sink.fill` capped by up to from the live amount, pushed in `fill` capped per sink. `gradlew build` green.
- [x] 2.4 GameTests in `logistics/gametest/ServusLogisticsGameTests`: 300 saplings keep 64 into a composter-like target leaves 64; tank 12,000 mB keep 8,000 into a Sink leaves 8,000; furnace up to 16 coal with the rest to a lower priority chest; furnace refilled after coal is taken; keep counted over three stacks; amounts survive save and load. GameTests green.

## 3. Menu and screen

- [x] 3.1 Add `logistics/menu/SetDuctFilterAmountPayload(line, slot, amount)` and `UtilityDuctMenu.setFilterAmount` with the checks from design D5. `gradlew build` green.
- [x] 3.2 `logistics/client/UtilityDuctScreen`: amount overlay on item and fluid slots (mB as k), tooltip "Keep n", "Up to n" or "Keep / up to n" by side mode, right click opens the amount popup, shift right click clears (`client/PopupPanel`, `client/NumberStepper`, steps per design D5, Clear, Save). `gradlew build` green.
- [x] 3.3 Lang keys in `logistics/datagen/LogisticsDataParts` (Keep, Up to, Keep / up to, popup title, Clear, tooltip forms). `gradlew :foundation:runData` and `gradlew build` green.
- [x] 3.4 Follow the endpoint GUI rework: "Keep", "Up to" and "Keep / up to" follow the flags (an item side gives only with Pull, a fluid side with In; a side receives with Out); the keep amount specs and the Duct inspect copy use the flags; keep amount GameTests set `DuctMode`. `gradlew build` and `gradlew :foundation:runGameTestServer` green.
- [x] 3.5 The amount popup and its step buttons fit the largest amounts and steps for items and fluids: `client/NumberStepper` sizes its step buttons to the widest label and its field to the largest value, measured with the font (at least the old 24 and 48 px); `logistics/client/AmountPopup` sizes itself to the stepper, Clear and Save and the title, at least 110 px and at most the GUI width. `gradlew build` and `gradlew test` green.

## 4. Data Potato

- [x] 4.1 `logistics/duct/DuctReport`: number of entries with an amount on item and fluid side lines; lang keys. Extend `DataPotatoGameTests`. `gradlew :foundation:runData`, `gradlew build` and GameTests green.

## 5. Plan, play check and wrap up

- [x] 5.1 PLAN.md "Open: Foundation": remove the duct overflow line, keep the Overflow Valve block and warehouse excess lines. Verify.
- [x] 5.2 Play check from main: chest of saplings keeps 64 and the rest goes to a composter; a pumped tank holds 8,000 mB and the surplus goes to the Sink; a furnace is kept at 16 coal while the rest goes on; overlay and labels change with the side mode; popup set, edit and clear.
- [x] 5.3 Full gradle build, tests green, game launches with all three mods.

## Workflow follow-up

- Archive with `/opsx:archive` after `m5-duct-fluid-filter`, add player lines under `## Unreleased` in `foundation/CHANGELOG.md` (Added: keep and fill up to amounts on duct filter entries, the overflow valve).
