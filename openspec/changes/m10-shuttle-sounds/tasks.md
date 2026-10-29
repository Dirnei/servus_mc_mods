# Tasks

## 1. Drive sound files

- [x] 1.1 Add `tools/shuttle-sounds/gen.py` (numpy, ffmpeg): the B5 drive loop from the explore (two layer rumble with slow random swells, irregular dull clicks with skips, short loose rattle, 10 s mono, seamless by circular filtering), three takes from three seeds as `shuttle_drive_1..3.ogg` and three damped takes (low pass about 600 Hz) as `shuttle_drive_muffled_1..3.ogg` into `shuttles/src/main/resources/assets/servus_shuttles/sounds/`; run it and verify the six files exist and play seamlessly when looped with `ffplay -loop 0`.
  - Verified by decoding: six Vorbis files, 10.000 s mono 44.1 kHz, the step from the last sample to the first lies within the normal sample to sample steps of each take. Listening with `ffplay -loop 0` is part of the play check (10.2).

## 2. Sound events

- [x] 2.1 Register `shuttle.drive` and `shuttle.drive_muffled` in `registry/ServusShuttlesSounds.java`; add both to `datagen/ServusShuttlesSoundsProvider.java` with the three streamed takes each, volume (example 0.25 and 0.08) and `attenuationDistance(10)`, subtitles in `ServusShuttlesLanguageProvider`; run datagen and `gradlew :shuttles:build`, verify `sounds.json` lists both events.

## 3. Pure sound logic

- [x] 3.1 Add `client/ShuttleSoundPick.java` without Minecraft imports: pick the N nearest moving keys to a listener position with stable ties, and `pitch(speed, reference, jitter)` clamped to 0.8 to 1.4, plus `jitter(long keyHash)` in 0.95 to 1.05. Add JUnit `ShuttleSoundPickTest`: nearest four of ten, fewer than N all picked, ties stable, standing (speed 0) never picked, faster gives higher pitch, clamps at both ends, jitter stable per key and within range. Run `gradlew :shuttles:test`.

## 4. Muffler item and shuttle flag

- [x] 4.1 Add the Muffler item in `registry/ServusShuttlesItems.java` with model, texture (drawn by a small generator `tools/shuttle-sounds/muffler_texture.py`, wool pad on a steel clamp; play check: redrawn as the copper upgrade card with a muted speaker glyph), lang and a shaped recipe (example wool around an iron ingot) in the datagen providers; run datagen and `gradlew :shuttles:build`.
- [x] 4.2 Add `muffled` to `shuttle/module/ShuttleModules.java` (codec default false, stream codec, `withMuffled`, included in `isEmpty`), the tooltip line in `ShuttleItem`, `Install.MUFFLER` in `ShuttleUpgrades` with `room` 1 while not muffled; add JUnit for the codec round trip with and without the flag if the codec test runs without a registry, otherwise cover it in the GameTest of 5.2. Run `gradlew :shuttles:build` and `gradlew :shuttles:test`.
  - The codec needs the item registry, so the round trip (save codec and stream codec, with and without the flag, and old data without the field) is the GameTest `modulesCodecKeepsMuffler` in `ShuttleMufflerGameTests`.

## 5. Workshop: Muffler install

- [x] 5.1 `menu/ShuttleWorkshopMenu.java`: install a Muffler through the install slot (one item of a stack), refuse a second, new `BUTTON_REMOVE_MUFFLER` that gives it back, shift click routes Mufflers to the install slot; `client/ShuttleWorkshopScreen.java` shows a Muffled line with its remove button. Run `gradlew :shuttles:build`.
- [x] 5.2 GameTests in `gametest/` (workshop tests): install a Muffler from a stack of three, second Muffler refused, remove gives it back and the shuttle stacks with a new one, Muffler survives a Mk1 to Mk2 build, Muffler stays next to a Harvester. Run `gradlew :shuttles:runGameTestServer`.

## 6. Workshop: Build and Skip

- [x] 6.1 `menu/ShuttleWorkshopMenu.java` and `ShuttleWorkshopBlockEntity.java`: `BUTTON_BUILD` starts the hand build, `BUTTON_SKIP` finishes a running build (any step and the name prompt) with exactly the sheet's supplies, keeping counts, tool, Muffler, anvil name and old plate, and is ignored without a build; remove the Build now path. `client/ShuttleWorkshopScreen.java`: one button at the old Build now place reading Build or Skip, Cancel unchanged; lang keys `build` and `skip` replace `build_now` and `by_hand`. Run `gradlew :shuttles:build`.
- [x] 6.2 GameTests: Skip on step 1 makes a Mk2 with supplies used and no name prompt state, Skip at the name prompt keeps the old plate, Skip without a build changes nothing, Build is off with a supply short. Update existing workshop and hand build GameTests that press Build now. Run `gradlew :shuttles:runGameTestServer`.

## 7. Workshop turntable

- [x] 7.1 Add a yaw to `client/GuiLargeItemRenderState.java` and rotate about Y in `client/GuiLargeItemRenderer.java`; `client/ShuttleWorkshopScreen.java` turns the shuttle 36 degrees per second while idle, follows a mouse drag on the board, and eases to the work angle in about 0.3 s when a hand build runs and holds there. Check other users of `GuiLargeItemRenderState` pass yaw 0. Run `gradlew :shuttles:build`, launch the client and check the turn, the drag, and that bolt spots, wires and seams sit on the shuttle after the swing.
  - Code, build and tests done; the in-game check of the turn, the drag and the swing is part of the user's play check (10.2).

## 8. Garage Muffler slot

- [x] 8.1 `garage/ShuttleGarageBlockEntity.java`: one slot `ItemSlots` that takes only the Muffler, saved and loaded, dropped on break and dismantle; `menu/ShuttleGarageMenu.java` and its screen: the Muffler slot next to the shuttle slots, shift click routes a Muffler there. `shuttle/ShuttleSyncPayload.java` gains `muffled`, set in `ShuttleFleet` where the payload is built, and a garage sync is sent when the slot changes. Run `gradlew :shuttles:build`.
- [x] 8.2 GameTests: only a Muffler fits the slot, the Muffler survives a save and load, breaking an empty garage drops it, the sync payload of a garage with a Muffler says muffled. Run `gradlew :shuttles:runGameTestServer`.

## 9. Client playback

- [x] 9.1 Add `client/ShuttleSounds.java` (client event subscriber, like `HarvesterSounds`): `update(key, pos, speed, muffled)` from `ShuttleGarageRenderer.drawShuttle` (key = garage pos plus shuttle number, else view index; muffled = garage flag or the shuttle's module flag), and a client tick that picks the nearest four moving shuttles through `ShuttleSoundPick`, starts looped `AbstractTickableSoundInstance`s on `shuttle.drive` or `shuttle.drive_muffled` with a short volume fade in, moves them, sets pitch from the view's speed, restarts on a muffled change and stops what is no longer picked or drawn, and stops everything when the level goes. Run `gradlew :shuttles:build`, launch the client and check one shuttle driving, stopping at a rack, riding a lift car, and a parked shuttle silent.
  - Code, build and tests done; the in-game listening check is part of the user's play check (10.2).

  Play check fix 2026-10-29: a muffled Lumber shuttle skips `HarvesterSounds.update` in `ShuttleGarageRenderer`, so its chainsaw, hold and crane stay silent.

## 10. Play check and wrap up

- [x] 10.1 Add Unreleased lines to `shuttles/CHANGELOG.md`: Added drive sound and Muffler (shuttle and garage); Changed workshop shuttle turns, Build now and Build by hand are one Build button with Skip. Run `gradlew build`.
- [ ] 10.2 Full `gradlew build` and `gradlew test` green, the game launches with all mods; play check with the user: loudness and range against a vanilla minecart, ten shuttles with only four sounding, both Muffler places, turntable and drag, Build then Skip, Build through all steps and Engrave. Tune volume numbers in the sounds provider if asked and rebuild.

## Workflow follow-up

- Archive after the user signs off the play check; groom PLAN.md (nothing open from this change unless the play check adds a point, such as a client volume setting).
