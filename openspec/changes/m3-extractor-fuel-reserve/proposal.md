# Proposal

## Why

A Burner Extractor on a coal deposit mines coal but pushes every piece into its output slot, so the player has to hand feed it or loop its own output back into the fuel slot with a duct. Today's run (2026-10-07) showed this as the one burner machine that could feed itself and does not. This change implements part of the PLAN.md M3 "Balance pass" item: the coal extractor keeps a small reserve of its own yield as fuel and only pushes the surplus.

## What Changes

- **Fuel reserve on the Burner Extractor.** When the deposit yield is itself a furnace fuel, the extractor fills its fuel slot with that yield up to a reserve target before it sends anything to the output slot. The reserve refills whenever burning drains it below the target. A fuel slot holding a different fuel, or more than the target, is left alone and every yield goes to output.
- **Reserve target in data.** The target is a new optional balance number in the machine datapack file, `fuel_reserve`, example 4 for the Burner Extractor and 0 for every other machine.
- **Work availability.** The extractor counts a yield that the reserve wants as work, so a coal extractor with a full output slot still keeps itself fueled.
- **PLAN.md** section 2 machine table notes that the Burner Extractor keeps a coal reserve when it sits on coal.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `manufacturing/burner-extractor`: "Extracts from the deposit below" gains the reserve routing of the yield; a new requirement "Fuel reserve from own yield" describes when the fuel slot is topped up and when yields go to output.

## Non-goals

- Reserve behavior on the Electric Extractor, Burner Smelter or Coal Generator. Only the Burner Extractor has a yield that can be fuel.
- Pulling fuel from the output slot or from neighbours. The reserve is filled from fresh yields only.
- A separate reserve slot or GUI element. The existing fuel slot holds the reserve.
- Changing the extraction rate or coal burn time.

## Impact

- `manufacturing`: `BurnerExtractorBlockEntity` routes yields; a small pure routing rule in `machine/extractor/` with a JUnit test; `MachineStats` and its partial codec gain `fuel_reserve`.
- Datapack: `data/servus_manufacturing/machine/burner_extractor.json` gains `"fuel_reserve": 4`.
- GameTests: a new extractor test on a coal deposit. The reference block test starts with 64 coal per machine and is unaffected.
- `PLAN.md`: one line in the Tier 1 machine table.
