# Tasks

## 1. Lib item and assets

- [x] 1.1 Rename `lib/.../item/WrenchItem.java` to `ConfigurationCarrotItem.java` with updated Javadoc; register it in `ServusLibItems` as `CONFIGURATION_CARROT` with id `configuration_carrot` and `stacksTo(1)`; switch the icon and entry in `ServusLibCreativeTabs`; update the `instanceof` checks in `GogglesItem` and `ConnectableBlock`; reword the comment in `ServusLibConfig` without changing keys. Verify `gradlew :lib:compileJava` passes.
- [x] 1.2 Create `lib/src/main/resources/assets/servus_lib/textures/item/configuration_carrot.png` (16 by 16, carrot body without greens, grey metal bar with red tip, transparent background) with a throwaway script in the scratchpad, delete `wrench.png`. Verify with a small script that the PNG is 16 by 16 and contains no pixel whose green channel dominates red and blue.
- [x] 1.3 Update `ServusLibItemModelProvider`, `ServusLibRecipeProvider` (shaped column redstone, iron ingot, carrot, unlocked by having a carrot, wrench recipe removed) and `ServusLibLanguageProvider` (item name and config texts). Run `gradlew :lib:runData` and verify `lib/src/generated/resources` contains `models/item/configuration_carrot.json`, `recipe/configuration_carrot.json`, its advancement and the lang entry, and that no `wrench` files remain.

## 2. Content mods

- [x] 2.1 Replace the `WrenchItem` import and `instanceof` check with `ConfigurationCarrotItem` in `MachineBlock` (manufacturing), `UtilityDuctBlock` (logistics), `WarehouseControllerBlock`, `CrateBlock`, `RackBlock` and `CounterTerminalBlock` (storage). Verify `gradlew build` passes and a search for `WrenchItem` over `*/src` finds nothing.

## 3. Docs and specs

- [x] 3.1 Update `PLAN.md`: Lib table row, the two section 2.2 wrench bullets and the M1 "Wrench item" entry name the Configuration Carrot as the wrench role item. Edit the Purpose of `openspec/specs/lib/wrench/spec.md` to say the Configuration Carrot fills the wrench role and other specs use "wrench" for that role. Verify `openspec validate m1-configuration-carrot` passes.

## 4. Verification

- [ ] 4.1 Full `gradlew clean build` green, `gradlew test` green, `gradlew :manufacturing:runGameTestServer` passes, `gradlew runClientAll` launches with all four mods. In game: craft the Configuration Carrot from redstone, iron ingot and carrot, check the icon has no green and shows the grey bar with red tip, check it is the Servus tab icon, rotate a furnace, sneak dismantle a machine, hold it near a faced duct and see the overlay, confirm it cannot be eaten. Commit.
