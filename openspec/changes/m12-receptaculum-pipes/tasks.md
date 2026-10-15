# Tasks

## 1. Fixed fronts

- [ ] 1.1 `foundation/.../machine/MachineBlockEntity.java`: `itemCapability(Direction)` (null on a fixed Off side) and use it in every `Capabilities.Item.BLOCK` registration that used `sideHandler(side)`: `foundation/.../machine/MachineEvents.java` and `alchemy/.../ServusAlchemyEvents.java`. Foundation GameTest `fixedFrontOffersNoItems`. Verify: `gradlew build test`, `:foundation:runGameTestServer` green.

## 2. Gifts by automation

- [ ] 2.1 `MachineBlockEntity`: `acceptsAutomationInput(int, ItemStack)` hook used by the side handler's insert and `isItemValid`; `alchemy/.../homunculus/ReceptaculumBlockEntity.java` overrides it with `GnomeGiftData` for the gift slots. Verify: `gradlew build test` green.
- [ ] 2.2 GameTests in `alchemy/.../gametest/HomunculusGameTests.java`: `pipeTakesTheYieldFromTheSide`, `noPipeArmAtTheFront`, `automationRefusesJunkGifts`, `handTakesAnyGift`. Verify: `:alchemy:runGameTestServer` green apart from the known FTB Chunks Ender Anchor tests.

## 3. Verification

- [ ] 3.1 Full `gradlew build test` green, the game launches with all three mods; user play check: a pipe at the Receptaculum's front shows no connection, a pipe on its side carries the yield away, a pipe delivering cobblestone and cookies puts only the cookies into the gift slots.
