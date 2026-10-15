# Tasks

## 1. Drip rule

- [x] 1.1 Pure `hollow/PrincipleDrip.java` (design D3) with JUnit `PrincipleDripTest`: `dripsOnlyWhatTheStepTakes`, `limitedByPerDrip`, `limitedByHeldAndRoom`, `nothingWithoutCost`. Verify: `gradlew :alchemy:test` green.

## 2. The block

- [x] 2.1 `hollow/PrincipleDropperBlock.java`, `hollow/PrincipleDropperBlockEntity.java` (tanks, ticker, drip onto the Hollow below, block event particles, bucket by hand, save and load), `GnomeHollowBlockEntity.receiveDrip` (D1 to D4, D6); registrations in `ServusAlchemyBlocks`, `ServusAlchemyItems` (creative tab), `ServusAlchemyBlockEntities`, fluid capability in `ServusAlchemyEvents`; defaults in `AlchemyMachineDefaults` and `AlchemyMachineDefaultsTest`, data `machine/principle_dropper.json` (D5). Verify: `gradlew build` green.
- [x] 2.2 Datagen: model and block state in `AlchemyModels` (D7, overlap check listed in design), loot `dropSelf` in `ServusAlchemyBlockLoot`, lang name in `ServusAlchemyLanguageProvider`, block tags (pickaxe) if the other copper parts have them; `:alchemy:runData`, commit generated files. Verify: `gradlew build` green.
- [x] 2.3 GameTests in new `DropperGameTests` (registered in `ServusAlchemy`): `dropperFedByCapabilityRipensHollow` (Salt and Mercury filled through the capability, an iron Hollow below grows steps), `dropperTakesOnlyPrinciplesAndNeverGives` (water refused, drain gives nothing), `nothingWithoutAHollowBelow` (stone below, tanks unchanged), `doesNotPushIntoATank` (Fluid Tank below stays empty), `ripeOrEmptyHollowGetsNothing`, `onlyWhatTheVeinTakes` (Sulfur stays), `hollowStillHasNoCapability` (with a Dropper on top). Verify: `:alchemy:runGameTestServer` green.

## 3. Craft, journal and docs

- [x] 3.1 Athanor craft `principle_dropper` in `AlchemyRecipeTable` (D8); `:alchemy:runData`. GameTest `dropperNeedsCitrinitas` in `DropperGameTests` (an Albedo Athanor does not make it, a Citrinitas one does). Verify: `:alchemy:runGameTestServer` green.
- [x] 3.2 Workshop entry `principle_dropper` in `WorkshopCategory` (D9); `:alchemy:runData`. Docs: `docs/src/content/docs/mods/alchemy.md` (machines and Gnomes sections), `reference/datapack/machines.md` (row and fields). Verify: `gradlew build` green, no em dash in the diff.

## 4. Verification

- [ ] 4.1 Full `gradlew build test :alchemy:runGameTestServer` green.
- [ ] 4.2 User play check in game:
  - The Dropper looks like a copper and glass drip vessel; no flicker from any angle.
  - A Fluid Pipe on a Utility Duct connects to the Dropper and fills it; Jade shows its three tanks.
  - On a seeded, unripe Hollow it drips (coloured drops) and the vein ripens; on a ripe Hollow or on stone it stops.
  - Without the sight Jade on the Dropper shows nothing about the Hollow, and the Hollow still shows as Stone.
  - The Principle Dropper is made on a yellow Athanor from a Principle Vat; the Workshop entry opens after Schwaz.
