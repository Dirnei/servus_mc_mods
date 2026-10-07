# Proposal

## Why

Play test (2026-10-07): a Burner Extractor with one side set to Output carried its orange frame all the time, which reads as noise on a block that is simply configured. The player wants the side markers only while configuring.

## What Changes

- Side markers render only while the viewing player holds the Configuration Carrot or wears the goggles. Without them no face shows a frame or glyph, configured or not.

## Capabilities

### Modified Capabilities
- `lib/side-config`: the "Side modes shown on the block" requirement.

## Non-goals

- Changing the panel, colours or glyphs.

## Impact

- `lib`: `SideMarkerRenderer.render` returns early unless configuring.
