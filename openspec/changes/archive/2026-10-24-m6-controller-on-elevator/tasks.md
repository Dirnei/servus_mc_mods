# Tasks

## 1. Link rule

- [x] 1.1 In `shuttles/src/main/java/.../network/WarehouseConnectionRule.java` let `STOCK` accept `ELEVATOR` and `ELEVATOR` accept `STOCK` on horizontal sides only, and update the class Javadoc. In `shuttles/src/test/java/.../network/WarehouseConnectionRuleTest.java` replace `controllerNeverLinksStraightToAnElevator` with `controllerLinksToAnElevatorSideways` (both directions true on the four horizontal sides) and `controllerNeverLinksToAnElevatorVertically` (both directions false up and down). Run `gradlew :shuttles:test`.

## 2. In game

- [x] 2.1 In `shuttles/src/main/java/.../gametest/ElevatorGameTests.java` add `controllerBesideShaftJoins`: a controller touching only the side of the bottom segment and an Inbound Station on a rail that meets the top segment; the station's online lamp lights. Add `controllerOnShaftTopStaysApart`: with a controller on the top segment instead, the station stays dark. Run `gradlew :shuttles:runGameTestServer`.

## 3. Integration

- [x] 3.1 Full `gradlew build`, `gradlew test` and `gradlew :shuttles:runGameTestServer` green; launch the game with all mods and check that a controller placed against a shaft lights the warehouse and the segment looks unchanged.

## Workflow follow-up

- Archive with a Fixed line in `shuttles/CHANGELOG.md` under Unreleased.
