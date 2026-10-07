# Proposal

## Why

Play test (2026-10-07): an extractor sits on its deposit, so its bottom side always touches the deposit block. Letting the player set that side to input, output or anything else is pointless and confusing. The pumps already fix their bottom to Off for the same reason.

Milestone: M3 (Manufacturing Tier 1 completion), a follow up to the extractors.

## What Changes

- The Burner Extractor and the Electric Extractor fix their bottom side to Off, like their front. The side panel shows it greyed, the carrot cannot change it, no marker is drawn on it, and a saved configuration with another mode on the bottom loads as Off.
- The Electric Extractor keeps taking energy on every face, like the other powered machines.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `manufacturing/burner-extractor`: new requirement, the bottom side is closed.
- `manufacturing/electric-extractor`: new requirement, the bottom side is closed for items.

## Non-goals

- Fixing the bottom of other machines (smelter, assembler, crusher), which do not stand on a source.
- Closing the Electric Extractor's energy input on the bottom.

## Impact

- `manufacturing`: `BurnerExtractorBlockEntity` and `ElectricExtractorBlockEntity` lock `RelativeSide.BOTTOM` to Off; GameTest.
