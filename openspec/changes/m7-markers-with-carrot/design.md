# Design

## Context

`SideMarkerRenderer.render` draws frames on non default faces always and adds glyphs while configuring.

## Goals / Non-Goals

**Goals:**
- Markers only while configuring.

**Non-Goals:**
- See proposal.

## Decisions

### Settles no PLAN.md open decision

### Renderer
`render` returns when `configuring()` is false; otherwise every face gets frame and glyph as before.

### Minecraft side verification
Client only: launch the client, a configured machine shows nothing with empty hands and everything with the carrot.

## Risks / Trade-offs

- [A wrongly configured side is no longer spotted from afar] → the player's choice; the carrot shows it in one click.

## Open Questions

None.
