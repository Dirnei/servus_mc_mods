## Why

A list in a closed book is easy to forget. Players want to see what they are working on while they play, and to notice when a teammate finishes something (explore 2026-11-02). This change adds the HUD pin in the vanilla look and toasts for ticks by others (PLAN.md, M16 Notes, `m16-notes-hud`).

## What Changes

- "Pin to HUD" / "Unpin" on every list the player sees; one pinned list per player, kept by the server so it follows the player.
- The HUD pin in the vanilla look: list name, open tasks first, then unreached goals with a small bar and count, up to a configurable number of lines with "+n more". Done tasks drop off.
- A key "Toggle HUD pin" (example default J) hides and shows the pin. Client config: corner (top left, top right, left middle) and line count.
- Toasts: when another player ticks a task on a Team list or a list the player is a guest of or owns, a vanilla toast says who ticked what. Never for Server lists. A client config switch turns toasts off.

## Capabilities

### New Capabilities
- `notes/hud`: the HUD pin, its key and config, and tick toasts.

### Modified Capabilities
None.

## Non-goals

- Several pinned lists, dragging the pin to a free position.
- Toasts for edits other than ticks, chat messages.
- A board block in the world.

## Impact

- `notes/`: a GUI layer, a key mapping, client config, a pin payload and the pin in saved data, toast handling on the client fed by the existing list updates.
- Depends on `m16-notes-goals` (apply after it).
