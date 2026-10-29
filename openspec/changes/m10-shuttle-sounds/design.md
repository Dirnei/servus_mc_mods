# Design

## Context

See proposal.md for the why. Shuttles are not entities: the server sends each garage's `ShuttleSyncPayload` with one `ShuttleView` per shuttle, and `ShuttleGarageRenderer` (render box `AABB.INFINITE`, renders off screen) draws every shuttle where `ShuttleGlide.at` extrapolates it. The Lumber harvester already plays sounds the same way: `HarvesterSounds.update` is called from the renderer on each draw, and a client tick silences what was not drawn lately. Shuttle modules (`ShuttleModules`: tool, speed, handling) are a data component on the shuttle item, which travels in every `ShuttleView`. The workshop draws the large shuttle through `GuiLargeItemRenderState`/`GuiLargeItemRenderer`, a picture in picture item render.

PLAN.md leaves no open decision for this; the choices below come from the explore of 2026-10-29.

## Goals / Non-Goals

**Goals:**
- A drive sound that is audible near a shuttle and never adds up to noise in a big warehouse.
- One Muffler item for a shuttle and a garage, with no new sync beyond one flag per garage.
- Workshop turntable and the Build/Skip flow without touching the minigame steps.

**Non-Goals:**
- Server side sound playback, stop or lift sounds, a client volume setting.

## Decisions

### Sound files are our own, generated
The drive loop is a procedural recording (two layer rumble with slow random swells, irregular dull clicks with skips, a short loose rattle now and then, 10 s, mono, seamless through circular filtering) made by `tools/shuttle-sounds/gen.py` with numpy and ffmpeg, like the texture generators. It writes three takes of the normal loop and three damped takes (low pass about 600 Hz) to `shuttles/src/main/resources/assets/servus_shuttles/sounds/`. The vanilla minecart cannot be shipped modified (Mojang asset), and a `sounds.json` reference to it can only change pitch and volume, not damp it.
Alternative: vanilla `entity.minecart.riding` at pitch 1.3 and volume 0.25. Rejected after listening: too static and the muffled version cannot sound damped.

### Two sound events, volume and range in sounds.json
`ServusShuttlesSounds` gains `shuttle.drive` and `shuttle.drive_muffled`. `ServusShuttlesSoundsProvider` gives each three weighted takes with `.stream()`, `.volume(...)` (example 0.25 and 0.08) and `.attenuationDistance(10)` (NeoForge `SoundDefinition.Sound`, verified in neoforge-26.1.2.116 sources). The event picks a take at random per play, which keeps shuttles out of step without seeking into the file (Minecraft sound instances always start at the beginning).

### Played client side from the renderer
`ShuttleGarageRenderer.drawShuttle` already computes the world position and the glide speed of each shuttle; it calls a new `ShuttleSounds.update(key, pos, speed, muffled, level)`. The key is the garage position plus the shuttle number (`ShuttleItem.number`), falling back to the view index for an unnumbered shuttle. A client tick (like `HarvesterSounds.onClientTick`) then:
1. collects the shuttles drawn this tick that move (glide speed above zero, not parked);
2. asks the pure `ShuttleSoundPick` which keys sound: the N nearest to the camera (example 4);
3. starts a looped `AbstractTickableSoundInstance` for a newly picked key, moves each playing one to its shuttle, and stops instances whose key is no longer picked, not drawn for a few ticks, or whose muffled state changed (restarted on the other event).
A shuttle riding a lift car moves at the car's speed and sounds like one on a rail.
Alternative: compute positions in a client tick from `ClientShuttles` alone. Rejected: it would repeat the renderer's position and lift height logic.

### Pitch
`ShuttleSoundPick.pitch(speed, baseSpeed, jitter)` (pure): `clamp(sqrt(speed / baseSpeed), 0.8, 1.4)` times a per shuttle jitter of 0.95 to 1.05 taken from a hash of the key, so a shuttle keeps its pitch across syncs. Pitch is set on the instance each tick, so it follows speed changes smoothly (acceleration, corners, braking). The base speed is a fixed sound reference of 0.15 blocks per tick in `ShuttleSounds`, not read from `shuttle.json`: the client has no stats file, only each view's `Drive`, and the reference only tunes how the sound bends, it balances nothing.

### Muffled flag
- Shuttle: `ShuttleModules` gains `boolean muffled` (`Codec.BOOL.optionalFieldOf("muffled", false)`, stream codec `ByteBufCodecs.BOOL`), so old shuttle items load unchanged and `isEmpty` includes it. `ShuttleUpgrades.Install` gains `MUFFLER`; `room` is 1 while not muffled. Tier builds already copy `ShuttleModules` whole.
- Garage: `ShuttleGarageBlockEntity` gets a one slot `ItemSlots` that accepts only the Muffler, saved with the block, dropped on break and dismantle with the other contents. `ShuttleSyncPayload` gains `boolean muffled`, sent with every garage sync and resent when the slot changes.
- Client: muffled = garage flag OR the view's shuttle item module flag.

### Workshop turntable
`GuiLargeItemRenderState` gains a yaw in degrees; `GuiLargeItemRenderer` rotates about Y before submitting the item. `ShuttleWorkshopScreen` keeps the yaw: idle it adds 36 degrees per second from frame time; a mouse drag on the board sets it directly; when a build by hand runs it eases to the work angle (the current fixed view) within about 0.3 s and holds there, so step spots, wires and seams stay where they are drawn now. Picture in picture renders key their cached texture on the state; the yaw is part of the state record, so a changed yaw re-renders.

### One Build button and Skip
`ShuttleWorkshopMenu`: `BUTTON_BUILD_NOW` becomes `BUTTON_SKIP`, accepted only while a hand build runs (`ShuttleWorkshopBlockEntity` finishes the build like the old Build now path, keeping the old plate, and clears the build state). `BUTTON_BUILD_BY_HAND` becomes `BUTTON_BUILD`. The screen shows one button at the old Build now place: Build when no build runs, Skip while one runs (also at the name prompt). The `buildNow` translation key goes, `skip` and `build` come.

### Pure logic and tests
- `ShuttleSoundPick` (no Minecraft imports): nearest N pick with stable ties, pitch from speed and jitter. JUnit `ShuttleSoundPickTest`.
- `ShuttleModules` codec round trip with and without muffled, and `ShuttleUpgrades.room` for the Muffler: JUnit where the class has no registry access, otherwise GameTest.
- GameTests: install and remove a Muffler in the workshop, second Muffler refused, Muffler survives a tier build, garage Muffler slot accepts only the Muffler and survives save, Skip finishes a build with supplies used and plate kept, Skip refused without a build.
- Manual play check: sound level and range, nearest four in a busy warehouse, muffled sound for both places, turntable and drag, Build then Skip.

## Risks / Trade-offs

- [The renderer only runs while the garage's chunk renders] → a shuttle far from its garage beyond render distance is silent; it is also farther than the 10 block range in almost every layout.
- [Sound starts and stops at the loop beginning] → the three takes each start soft; the instance fades in over a few ticks by ramping its volume.
- [Volume numbers chosen from a browser preview] → tuned in the play check; they live in the sounds datagen provider only.

## Migration Plan

None needed: the new `muffled` field defaults to false, the garage slot loads empty for old garages, and removing the Build now button changes no saved data.
