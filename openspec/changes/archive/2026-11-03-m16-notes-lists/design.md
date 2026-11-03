## Context

See proposal.md. After `m16-notes-project` the `:notes` project has a mod class, the key and an empty `NotebookScreen`. Notes has no Foundation, so saved data, payloads and config follow the plain NeoForge patterns the family already uses (`EnderAnchorData` for `SavedDataType`, `ServusFoundationPayloads` for `RegisterPayloadHandlersEvent`, `ServusFoundationConfig` for `ModConfigSpec`) without sharing code with them.

## Goals / Non-Goals

**Goals:** lists and tasks that a player creates, edits, ticks and assigns in the notebook, stored and validated by the server, with the motion from the mockup.

**Non-Goals:** visibility beyond the owner; see proposal.md, Non-goals.

## Decisions

### Pure model
Package `notes.model`, no Minecraft imports:
- `NoteList(UUID id, String name, UUID owner, String ownerName, List<Task> tasks)`, immutable records with `with...` copies.
- `Task(UUID id, String text, String notes, boolean sub, Optional<Person> assignee, Optional<DoneRecord> done)`. Subtasks are flat in list order with `sub = true`: a subtask belongs to the nearest task above it with `sub = false`. One level is then a property of the data, not a rule to police.
- `Person(UUID id, String name)`; names are stored with the id so no lookup is needed to show them.
- `DoneRecord(Person by, long day, long epochMillis)`.
- `NotesBook`: all lists by id, plus `ownedBy(UUID)`.

### Edits as commands
`model.NoteEdit` is a sealed interface of records: `CreateList(listId, name)`, `RenameList`, `DeleteList`, `AddTask(listId, taskId, text, afterId?)`, `EditText`, `EditNotes`, `DeleteTask`, `MoveTask(up)`, `Indent`, `Outdent`, `Tick(done)`, `SetAssignee(Optional<Person>)`, `Claim`. New lists and tasks carry the id the client chose, so the client can select the list it just made and open its name for editing; the server refuses an id that is already taken. `model.NoteEdits.apply(book, actor, access, limits, now, edit)` (`actor` a `Person`, `now` a `Moment(day, epochMillis)`) returns `EditResult.Applied(newBook, changedListIds)` or `EditResult.Refused(reason)` with `Refusal` reasons `NOT_ALLOWED`, `LIMIT_LISTS`, `LIMIT_TASKS`, `TOO_LONG`, `EMPTY` (blank name or text), `NOT_FOUND` (also for a list the actor cannot see, so a forged edit learns nothing). An edit that changes nothing (indent of the first task, tick of a done task) is applied with no changed lists. Rename, task edits and assignee need `canEdit`; tick and claim need `canTick`; delete list needs `canManage`. A task that is not a subtask moves with its subtasks past the neighbouring block; a subtask swaps with its neighbour and so may cross into the next or previous task. Adding after a task that has subtasks lands behind them. Deleting a non-sub task removes the following sub tasks with it; outdent of a sub task, indent of the first task or of a task under nothing is a no-op.

### Access as an interface
`model.Access` answers `canSee`, `canEdit`, `canTick`, `canManage` (share, scope, delete list) for an actor and a list. Each method takes the actor's id and the list, and `rights(actor, list)` bundles the four answers into a `model.Rights` record. This change ships `OwnerAccess` (all four true for the owner only), returned by `NotesService.access(server)`, the one place sharing swaps in its rules. `m16-notes-sharing` replaces it with the full rules, so `NoteEdits` never changes for rights.

### Server storage
`server.NotesData extends SavedData` with `SavedDataType<>(Identifier.fromNamespaceAndPath("servus_notes", "notes"), NotesData::new, CODEC)` on `server.overworld().getDataStorage().computeIfAbsent(TYPE)`, as in `EnderAnchorData`. Codecs live in `server.NotesCodecs` (DFU `RecordCodecBuilder`, `UUIDUtil.CODEC`), not in the model.

### Sync
- Snapshots carry `model.ListView(NoteList, Rights)`: the list plus the recipient's rights, computed per recipient on the server, so the notebook shows only the buttons a player may use.
- `network.ListsPayload(List<ListView>, Limits)`, play to client: the full set a player sees, sent on `PlayerEvent.PlayerLoggedInEvent`.
- `network.ListPayload(ListView)` and `network.ListGonePayload(UUID)`, play to client: after an applied edit the server sends the changed list to every online player who can see it and `ListGonePayload` to every other online player (the client ignores ids it does not have).
- `network.EditPayload(NoteEdit)`, play to server, with a `StreamCodec` that dispatches on a byte tag per record. The handler runs on the server thread (NeoForge 26.1 payload handlers run on the main thread by default, as all family handlers rely on) and calls `server.NotesService.apply(player, edit)`.
- `network.RefusedPayload(String reasonKey)`, play to client: shown for three seconds in the notebook footer, on the leather below the pages.
Registration and the login sync live in `network.NotesPayloads` (`@EventBusSubscriber`); the network codecs of the model in `network.NotesStreamCodecs`. The client copy is `client.ClientNotes` (lists, limits, selected list, last refusal, listeners with `listChanged(before, after)` and `allReplaced()`), cleared on `ClientPlayerNetworkEvent.LoggingOut`.
Lists are small (a few kB), so whole-list snapshots keep the client simple.

### Time and day
`DoneRecord.day` is `level.getOverworldClockTime() / 24000` of the overworld (26.1 replaced `getDayTime` with world clocks; verified in `minecraft-patched-26.1.2.116-sources.jar`, `Level#getOverworldClockTime`). `epochMillis` is `System.currentTimeMillis()` on the server; the client renders "(just now)", "(5 min ago)", "(2 h ago)", "(yesterday)", "(3 days ago)" through pure `model.Ago.between(nowMillis, thenMillis)`, which returns a unit and an amount so the words come from lang keys.

### Notebook pages
- Contents page: entries with name, dotted leader and open count, a line "Private · owner" (scope words come from sharing), "+ New list" at the bottom, scrolls with the wheel.
- Right page: title (click to rename in an `EditBox`), a Delete button with a confirm step, task rows, the add row (`EditBox`, Enter adds, focus stays).
- A task row: box, text (click to edit inline), meta lines (done record, notes in italic), assignee "@name" or "claim" at the right; on hover small icons up, down, indent or outdent, notes, delete. Notes edit in a `MultiLineEditBox` popup on the page. Clicking "@name" clears the assignee for editors; choosing another player comes with the known players of `m16-notes-sharing`.
- The screen is split into `ContentsPage`, `ListPage`, `NotebookMotion`, `PaperButton` and `PageHits` (click areas collected while a frame is drawn). The text fields are drawn inside the book pose too, and the screen hands them mouse events turned into book coordinates.
- Motion: on an applied tick from this client (the pop starts when the snapshot shows the task done), the check mark scales 0 to 1.5 to 1 over about 0.3 s, six sparks fly out and the strike runs across the text; switching lists draws the right page with a horizontal scale from 0.8 to 1 over 0.2 s. Times are in milliseconds rather than frames so the motion does not depend on the frame rate. Both are client only.

### Config
`config.NotesServerConfig` (`ModConfig.Type.SERVER`): `maxListsPerPlayer` 50, `maxTasksPerList` 200, `maxNameLength` 40, `maxTaskLength` 120, `maxNotesLength` 500. The client trims input to the lengths it gets in `ListsPayload` (the limits ride along), the server enforces them.

## Pure logic vs Minecraft

Pure, JUnit: `NoteEdits`, `OwnerAccess`, `Ago`, codec round trip (`NotesCodecs` with `JsonOps`; the `:notes` test source set gets the main compile and runtime classpath for the Minecraft codec classes). GameTest: an edit through `NotesService` is saved in `NotesData`, and an edit by a non-owner is refused. Manual: the notebook pages and motion.

## Risks / Trade-offs

- [Whole-list snapshots on every keystroke-free edit could add up on busy servers] -> edits are discrete actions (Enter, click), not keystrokes; a list is a few kB.
- [Flat subtasks with a flag could be reordered into an orphan sub task at the top] -> `MoveTask` of the first task never makes it a sub, and `NoteEdits` normalises a leading sub task to a normal task after every move.

## Migration Plan

None. Nothing is released.

### Decided during the play check
- Writing and editing tasks moved off the ruled rows onto a task page: the left page lists the open project's tasks one line each (box, text cut with "...", marks for goals "▣n", place "◉" and "@name"), the right page shows the clicked task. The old inline add row, inline text edit, notes popup and the hover icon strip are gone.
- The task page: tick box and an `EditBox` for the text (Enter or focus loss saves, Escape takes the saved text back, an emptied field keeps the old text); Notes as an always visible `MultiLineEditBox` (focus loss or Escape saves); Who with "I'll do it" (`Claim` for an open unassigned task, else an editor's `SetAssignee` to themselves) and "Assign ▾", a dropdown of "nobody" and `ClientNotes.knownPlayers()` sending `SetAssignee`; a footer with ↑, ↓, "Make subtask" / "Make task" and "Delete task" with the same "Sure?" step as the project's Delete. Fields follow the server's value only while they are not focused and the value changed since they last took it, so a sent edit does not flicker back.
- "+ New task" opens an empty task page with the text field focused; Enter sends `AddTask` at the end of the project (client-chosen id) and clears the field for the next one. Escape, or a click elsewhere with an empty field, ends it without a task; a click elsewhere with text adds it.
- Project buttons Rename, Share, Pin and Delete sit under the meta line on the left page and wrap onto a second line when the page is narrow; clicking the name renames too. The meta line reads "by owner · n of m done".
- Clicking a tab, also the active one, goes back to that tab's projects.
- Second play check: the task page no longer has a tick box or a text field. Ticking happens only in the task list; the task's name is edited in its line (click the text of the open task, Enter or click elsewhere saves, Escape reverts, empty keeps the old text). "+ New task" opens a line to write in at the end of the list (`TaskListPage` drafting): Enter sends `AddTask`, opens the new task on the right page once it arrives and leaves an empty line for the next; Escape or an empty click elsewhere closes it. The right page shows the name as a bold heading.
- Task page order: Who, Items, Place, then Notes at the bottom above "Delete task", filling the height that is left (the `MultiLineEditBox` gets its height each frame). Items show as many goal rows as fit while Notes keep 30 px; the rest scroll.
- Field placeholders are faded ink without shadow (`TaskPage.hint`), the notes placeholder included.
- Drag and drop replaces ↑, ↓ and "Make subtask" / "Make task". A press on a line becomes a drag after 4 px; a click without a drag acts on release (open, or edit the open task's text). The ghost line follows the mouse, the dragged lines fade, a red line shows where the task lands, computed with the same pure `NoteEdits.placed` the server uses. Moving right by half an indent (5 px) or more drops as a subtask; where that is refused (at the top) it drops as a task. The list scrolls every 120 ms while the mouse is within 4 px of its top or bottom edge. Escape or a drop outside the list cancels.
- `NoteEdit.PlaceTask(listId, taskId, Optional<UUID> afterId, boolean sub)` (edit right, stream tag 23) replaces `MoveTask`, `Indent` and `Outdent` (tags 7 to 9 stay unused). Pure rules in `NoteEdits.placeRefusal` and `NoteEdits.placed`: the task, with its subtasks unless it is one, is taken out and put back; a subtask goes right after the anchor, a task behind the anchor's whole block; a task with subtasks stays a task. Refused: unknown task or anchor `NOT_FOUND`, an anchor inside the moved block or a subtask at the top `NOT_ALLOWED`. JUnit in `NoteEditsTest`.
- Third play check: "Delete task" leaves the footer for a 9 px bin sprite (`bin`, red `bin_armed` while armed, drawn by `tools/notes-textures/gen.py`) at the top right beside the heading, editors only; the tooltip reads "Delete task", then "Click again to delete". Notes now reach the page bottom. The `MultiLineEditBox` character counter (grey with shadow, drawn under the box) is switched off with `setShowDecorations(false)`; the page draws "n/500" in faded ink at the right of the Notes label instead.
