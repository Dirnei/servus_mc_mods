## Context

See proposal.md. After `m16-notes-goals` the client holds every visible list in `ClientNotes`, updated by whole-list snapshots (`ListPayload`) and removals (`ListGonePayload`); each task carries its done record with the player who ticked it, and goals carry count and target. The family draws HUD elements through `RegisterGuiLayersEvent` (`foundation/client/PlacementAssistHud`).

## Goals / Non-Goals

**Goals:** one pinned list per player in the vanilla HUD look, a toggle key and client config, and toasts for ticks by others.

**Non-Goals:** see proposal.md, Non-goals.

## Decisions

### Pin kept by the server
`NotesData` gains `pins` (player UUID to list id). `network.PinPayload(Optional<UUID>)` play to server sets or clears it (refused when the player cannot see the list); `network.PinnedPayload(Optional<UUID>)` play to client on login and on every change. When the server sends `ListGonePayload` to a player whose pin is that list, it clears the pin and sends `PinnedPayload(empty)`. Kept on the server so the pin follows the player across devices, like the lists.

### HUD layer
`client.HudPin` registers on `RegisterGuiLayersEvent` with `registerAbove(VanillaGuiLayers.CROSSHAIR, id, HudPin::render)` (as `PlacementAssistHud`). It draws nothing while `options.hideGui`, a screen is open, the pin is hidden or no list is pinned. Lines come from pure `model.PinLines.of(list, maxLines)`: the name, open tasks in order (subtasks indented by two spaces, "[ ] text"), then unreached goals ("<item>" plus bar plus "count/target"), and "+n more" when cut. The look is vanilla: a `0x60000000` box behind, the game font with shadow, the name in `0xFFD76A`, goal lines in light grey, a 50 by 5 pixel bar. Item names resolve on the client; the pure class takes them through a function.

### Toggle key and client config
`NotesKeys` gains "Toggle HUD pin" (`KeyConflictContext.IN_GAME`, example default J) in the "Servus: Notes" category; the hidden state is client memory, reset to shown on joining. `config.NotesClientConfig` (`ModConfig.Type.CLIENT`): `hudCorner` (`TOP_LEFT`, `TOP_RIGHT`, `LEFT_MIDDLE`), `hudLines` (2 to 8, default 4), `tickToasts` (default true).

### Tick toasts from snapshots
No new payload: when `ClientNotes` replaces a list from a `ListPayload` (not the login `ListsPayload`), pure `model.NewTicks.between(old, new, self)` returns tasks that were open and are now done by someone other than `self`. If the list's scope is not Server and `tickToasts` is on, each becomes `SystemToast.add(minecraft.getToastManager(), TOAST_ID, title, message)` with title the list name and message "<player> ticked <task>". `SystemToast.add` and `SystemToastId` are verified in `minecraft-patched-26.1.2.116-sources.jar`. Since the client only receives lists it may see, "owner, guest or team member" follows from the snapshot and the scope check.

### Notebook
The right page gets "Pin to HUD" / "Unpin" next to Share, and the contents entry of the pinned list a small "pinned" mark.

### Settles from PLAN.md
The explore decided the vanilla HUD, the key, and toasts that skip Server lists. This change decides that the pin lives on the server and that toasts come from diffing snapshots.

### Decided during apply
- The notebook button reads "Pin" / "Unpin" with the tooltip "Pin to HUD" / "Take it off the HUD": "Pin to HUD" next to Share and Delete left the list name almost no room on the right page. Every list a player sees gets the button, read only lists too.
- Goals show only for open tasks; a ticked task drops off with its goals.
- `NotesService.pin(server, player, listId)` works by player id, so GameTests and offline players use the same path. Pins are checked after every stored edit for every player, online or not (`dropLostPins`), and again in `sendAll` (login and party refresh), instead of only next to `ListGonePayload`.
- Pins are saved as `Pins`, a map from player id (string) to list id (`NotesCodecs.PINS`).
- More than three new ticks in one snapshot: two tick toasts and a third "and n more ticked". Toasts use `SystemToast.multiline` so long task texts wrap; every toast shares one `SystemToastId`, and `ToastManager.addToast` queues them.
- The toast title is the list name, the message "<player> ticked <task>".
- Text in the HUD pin is cut at 180 px with "...".

## Pure logic vs Minecraft

Pure, JUnit: `PinLinesTest` (order, indent, done dropped, reached goals dropped, cut and "+n more"), `NewTicksTest` (new tick by other, own tick ignored, untick ignored, already done ignored). GameTest: pin is refused for an invisible list and cleared when access is lost. Manual: HUD corners, lines, key, toasts with a second player.

## Risks / Trade-offs

- [Several ticks in one snapshot flood toasts] -> at most three toasts per snapshot, the rest summed as "and n more".
- [Pin drawn over other mods' HUD elements in the chosen corner] -> the corner is configurable.

## Migration Plan

None. Nothing is released.

### Decided during the play check
- `PinLines.GoalLine` carries the viewer's carried count and the HUD bar fills green for the count and yellow for the carried items above it (`model.GoalFill`).
- The notebook's Pin / Unpin button is one of the project's buttons on the left page, with the tooltip "Pin this project to the HUD" / "Take this project off the HUD". The HUD itself is unchanged.
