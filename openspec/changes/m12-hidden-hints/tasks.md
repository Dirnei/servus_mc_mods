# Tasks

## 1. Pure logic

- [x] 1.1 `hint/OldGnomeLedger.java` (D6) and `hint/AncientHollowSite.java` (D7); JUnit `OldGnomeLedgerTest` (`wakesOnlyOnce`, `lostBeforeFoundMayWakeAgain`, `foundNeverWakesAgain`, `lostAtOtherPosIsIgnored`, `foundOnlyFromAwake`) and `AncientHollowSiteTest` (`picksTheTunnelWall`, `startDirectionChangesTheChoice`, `noneWithoutAir`, `skipsTheBrokenBlockItself`, `sideFacesTheAir`). Verify: `gradlew :alchemy:test` green.

## 2. Sound and the simple hints

- [x] 2.1 `hint/HintSounds.java` registered in `ServusAlchemy` (D1); sound definitions provider in datagen with the subtitle; `GnomeHollowBlock.animateTick` (D2), `Salamander` dancing flame (D3), `VasBlock.animateTick` (D4); `:alchemy:runData`. Verify: `gradlew build` green.

## 3. The old gnome

- [x] 3.1 `hint/OldGnomeSettings.java`, `hint/OldGnomeData.java` (registered in `AlchemyEvents`), `data/servus_alchemy/servus_alchemy/old_gnome/old_gnome.json` (D5); `hint/OldGnomeSavedData.java` (D6). Verify: `gradlew build` green.
- [x] 3.2 Trace items in `ServusAlchemyItems` and the creative tab, textures from a script in the job directory, item models, tag `hint_traces` (D10); `hint/AncientHollowBlock.java`, `hint/AncientHollowBlockEntity.java`, `hint/AncientHollowTracker.java`, registrations, block state and model, loot, pickaxe and `wakes_old_gnome` tags (D8); `hint/OldGnome.java`, `hint/HintEvents.java` (D9); `Journal.recordNamed` (D12); Jade callback; lang; `:alchemy:runData`. Verify: `gradlew build` green.
- [x] 3.3 `client/sight/OldGnomeRevealRenderer.java` registered in `ServusAlchemyClient` (D11, overlap check in design). Verify: `gradlew build` green.

## 4. Journal and book

- [x] 4.1 `journal/old_gnome.json`, side chapter advancement in `JournalAdvancements`, Opus entry `old_gnome`, Workshop entry `knockers`, lang (D12, D13); `:alchemy:runData`. Verify: `gradlew build` green.

## 5. Behaviour tests

- [x] 5.1 GameTests in new `HintGameTests` (registered in `ServusAlchemy`): `wakesAfterTheChapter`, `notBeforeTheChapter`, `notAboveTheHeight`, `onlyOncePerTeam`, `lostBeforeFoundWakesAgain`, `foundNeverWakesAgain`, `traceLiesInFrontWhenHeWakes`, `traceDropsNearHim`, `tracePickupCountsFind`, `journalUnderSightFindsHim`, `journalWithoutSightDoesNothing`, `breaksIntoCobbledDeepslate`, `oldGnomeChapterFinishes`, `shippedSettings`. Verify: `:alchemy:runGameTestServer` green.

## 6. Docs and plan

- [x] 6.1 Docs: hints and the old gnome in `docs/src/content/docs/mods/alchemy.md`, `old_gnome.json` in the datapack reference; PLAN.md M12: open points House Gnome companion (`m12-house-gnome`), storyteller texts, play check of all M12 changes. Verify: no em dash in the diff.

## 7. Verification

- [x] 7.1 Full `gradlew build test :alchemy:runGameTestServer` green; client smoke start from main reaches the title screen without errors mentioning servus.
- [ ] 7.2 User play check in game:
  - A Gnome Hollow with a bound gnome knocks softly now and then; the subtitle reads "Something knocks in the rock".
  - A wild salamander in lava sometimes shows a little spiral of flames; a Vas bubbles now and then without the sight.
  - After Schwaz, mining deepslate below Y 0 (set `wake_chance` to 1 in a datapack for the check) turns a tunnel wall into an Ancient Hollow that looks like deepslate, Jade says Deepslate; a trace lies in front of it.
  - Knocking comes from its direction; within 6 blocks dust falls from the one block; breaking deepslate near it sometimes drops a trace; picking up a trace shows "They are near. Drink, and look."
  - Under the sight the old gnome with red cap and white beard stands in front of the face, no flicker; the journal on it shows the placeholder line, and The Old Gnome chapter finishes; then knocking and dust stop.
  - The Workshop entry "Knocking in the Rock" and the Opus entry "The Old Gnome" read well.
