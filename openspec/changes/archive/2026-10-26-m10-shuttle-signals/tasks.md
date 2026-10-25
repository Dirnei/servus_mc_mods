# Tasks

## 1. Standing reason on the server

- [x] 1.1 Add pure `shuttle/StandReason` (kinds, leader id, shaft bottom, floor) and `StandReason.chain(start, leaderOf, limit)` returning the walked ids and whether they closed a cycle. JUnit `StandReasonTest`: chain of three ending at a non-follower, chain cut at the limit, two shuttles in a cycle, a shuttle with no leader. `gradlew :shuttles:test` green.
- [x] 1.2 Record the gate reason: `Shuttle.gate` set in `ShuttleFleet.pay` (power) and `ShuttleLifts.mayEnterRide`, `startRide`, `mayEnterCross` (lift car or deck with shaft and floor), cleared when the gate passes. `gradlew build` green.
- [x] 1.3 Work out each shuttle's `StandReason` in `ShuttleFleet` after `traffic()` in the priority from design D1, and sync when its kind changes (`signalShown` beside `followShown`). GameTests in `ShuttleTrafficGameTests` or `ElevatorGameTests`: a shuttle beside a shaft whose car is busy elsewhere has `LIFT` with the right floor; a shuttle behind one that stands for a stop has `FOLLOWING` with that leader; a driving one has `DRIVING`. `gradlew build` and the GameTests green.

## 2. Signal to clients and tail light logic

- [x] 2.1 Add `signal` to `ShuttleView` and its `STREAM_CODEC` (11 parts), filled in `ShuttleFleet.views`. `gradlew build` green.
- [x] 2.2 Add pure `shuttle/TailLight.of(kind, braking, gameTime)` per design D3. JUnit `TailLightTest`: driving dim, driving and braking bright, following bright, lift, deck, power and chunk blink with a 20 tick period, stop, field work, door, handover and riding off. `gradlew :shuttles:test` green.
- [x] 2.3 Add braking detection to `client/ShuttleGlide` (speed at `elapsed` lower than at `elapsed - 1`) using `ShuttleDrive.glide`; extend `ShuttleDriveTest` with a glide that slows before the path end and one that holds speed. `gradlew :shuttles:test` green.

## 3. Lamps and number plate

- [x] 3.1 Add pure `shuttle/SignalLayout`: lamp and plate boxes in model pixels per tier and tool (Mk1 crate, Mk2, Mk3, Lumber hold, Harvester, Spreader, top Speed step). JUnit `SignalLayoutTest`: every box inside the rail block, lamps 0.05 px off the body faces, no lamp or plate overlapping a tool box from `ShuttleLook`. `gradlew :shuttles:test` green.
- [x] 3.2 Draw headlights and tail lights in `client/ShuttleGarageRenderer` from `SignalLayout` and `TailLight`, lit faces at `LightCoordsUtil.FULL_BRIGHT`. List every overlapping box pair (lamps, body, hold, tools, spoiler) and confirm no shared planes. `gradlew build` green.
- [x] 3.3 Draw the number plate on both long sides with `RenderSubmit.text` (`#` plus number, width from the text, smaller scale for three digits), text only within 16 blocks of the camera, no plate without a number. `gradlew build` green.
- [x] 3.4 Play check from main: front and back readable on Mk1, Mk2, Mk3, Lumber, Harvester and Spreader shuttles at night; tail lights dim while driving, bright when braking before a bay and behind a loading shuttle, amber blinking beside a shaft with a busy car and with the controller out of power, off at a stop and while riding; plates read `#n` from both sides and vanish beyond 16 blocks; no flicker on lamps or plates from several angles while driving; slot items show no lamps or plates.

## 4. Data Potato report

- [x] 4.1 Add `shuttle/ShuttleReport.at(fleet, pos)` and the `message.servus_shuttles.potato.shuttle_*` lang keys in `datagen/ServusShuttlesLanguageProvider` (label, driving, each reason, floor, chain step, cycle, no shuttle). Run `gradlew :shuttles:runData` and `gradlew build` green.
- [x] 4.2 `rail/RailBlock` and `rail/ElevatorBlock` implement `PotatoInspectable` through the node's `network()`; `ElevatorBlock.useItemOn` lets the Data Potato pass. GameTests: potato report on an empty rail says no shuttle; on a rail with a shuttle waiting for the lift car names it and the floor; on a rail with a shuttle behind another names the leader. GameTests green.
- [x] 4.3 Play check: potato on rails, a farm rail and a shaft segment with a riding shuttle prints the expected lines; a staged queue behind a shuttle waiting for the car prints the whole chain.

## 5. Plan and wrap up

- [x] 5.1 Add the open point to PLAN.md "Open: Shuttles": a shuttle crossing a shaft on its deck keeps the car from folding that deck, and when the shuttle it waits behind waits for that car nothing moves. Verify the line is there.
- [x] 5.2 Full gradle build, tests green, game launches with all three mods.

## Workflow follow-up

- Archive with `/opsx:archive`, add a player line under `## Unreleased` in `shuttles/CHANGELOG.md` (Added: shuttle lights, number plates and Data Potato report on rails).
