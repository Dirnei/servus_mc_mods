# Tasks

## 1. Reveal rules

- [x] 1.1 Add `foundation/.../item/Reveal.java` with `configuring`, `analysing`, `seesThroughPanels` (design D1); remove `GogglesItem.reveals`; switch `client/SideMarkerRenderer.configuring`, `data/DuctLoadSync`, `logistics/client/DuctOverlayRenderer` and `logistics/client/PanelReveal`. The potato check is added with the item in 2.1, so for now `analysing` is goggles only. Verify: `gradlew build` green.

## 2. The item

- [x] 2.1 Add `foundation/.../item/DataPotatoItem.java`, register `data_potato` in `registry/ServusFoundationItems.java` and the Servus creative tab, include it in `Reveal.analysing` and `seesThroughPanels`. Datagen: `basicItem` in `ServusFoundationItemModelProvider`, the recipe in `ServusFoundationRecipeProvider` (design D6), lang name and tooltip in `ServusFoundationLanguageProvider`; run `gradlew :foundation:runData`. Verify: `gradlew build` green.
- [x] 2.2 Add `tools/potato-texture/gen.py` and generate `foundation/src/main/resources/assets/servus_foundation/textures/item/data_potato.png` (design D6). Verify: a second run changes no file, `gradlew build` green, the client log shows no missing texture.
- [x] 2.3 GameTest `revealRules` in `foundation/.../logistics/gametest/ServusLogisticsGameTests.java` (carrot: configuring only; potato: analysing only; goggles: both; pickaxe: neither; panels for all three). Verify: GameTests pass in `gradlew :foundation:runGameTestServer`.

## 3. Duct report

- [x] 3.1 Add pure `foundation/.../logistics/network/EndpointTally.java` (design D4) and `EndpointTallyTest` in `foundation/src/test/java/.../logistics/network/` (`inAndOutCountsBoth`, `pullInCountsIn`, `pushOutCountsOut`, `disabledIgnored`, `otherChannelIgnored`). Verify: `gradlew :foundation:test` green.
- [x] 3.2 `LineNetwork.blockSides()` (channel and mode of member duct sides that touch a block, shared with `channelUses()`); add `foundation/.../logistics/duct/DuctReport.java` (design D3) and call it from `DataPotatoItem.useOn`; `UtilityDuctBlock.useItemOn` passes the potato. Lang keys for the report lines. GameTests `potatoReportsItemArm`, `potatoReportsDataLine`, `potatoChangesNothing` in `logistics/gametest/DataPotatoGameTests.java` (report lists read directly; the full click collects chat through `PlayerClicks`). Verify: `gradlew build` green, GameTests pass.

## 4. Tank report

- [x] 4.1 Tank report in `logistics/tank/TankReport.java`, called from `DataPotatoItem`, with `FluidTankBlock.describe` plus the box size (design D5); `FluidTankBlock.useItemOn` already passes the potato. Lang key for the size line. GameTest `potatoReportsTank` (3 by 2 by 3 tank with water, and an empty single tank). Verify: `gradlew build` green, GameTests pass.

## 5. Plan and integration

- [x] 5.1 Play check notes in game: potato shows load colours and see-through panels but no side markers; carrot shows side markers and panels but no load; goggles show all; potato on an arm, a centre and a tank prints the reports. Verify: the user's play check from main.
- [x] 5.2 Full `gradlew build`, `gradlew test` green, the GameTests pass, and the client launches with all three mods.
