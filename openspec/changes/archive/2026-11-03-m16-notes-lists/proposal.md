## Why

The notebook from `m16-notes-project` is empty. This change makes it a todo list: a player keeps lists of tasks, ticks them off and sees who did what and when, saved on the server so the lists follow the player to every device (PLAN.md, M16 Notes, `m16-notes-lists`).

## What Changes

- A pure Java model of lists and tasks (no Minecraft types): a list has an id, a name, an owner and ordered tasks; a task has a text, optional notes, an optional assignee, one level of subtasks and a done record (who, in-game day, real time). Edits are validated commands, covered by JUnit.
- Server storage in world saved data, one store for the whole server, and sync: each player receives the lists they can see on login and after every change.
- Notebook contents: the "Mine" tab lists the player's lists with name, open count and dotted leader; "+ New list"; the right page shows the selected list with its tasks, an add row, rename and delete.
- Tasks: add, edit text and notes, delete, move up and down, indent under the task above (one level), tick and untick with the record shown ("done by Anna, day 409 (2 h ago)"), assign a player or "claim".
- Motion from the mockup: a ticked box pops with a few sparks and the text is struck through; switching lists flips the right page in.
- Server config limits: lists per player, tasks per list, text length.

## Capabilities

### New Capabilities
- `notes/lists`: lists and tasks, what a player can do with them, how they are saved, synced and limited.

### Modified Capabilities
None. The notebook spec of `m16-notes-project` stays as it is; this change fills its pages through `notes/lists`.

## Non-goals

- Who else sees a list: scopes, FTB Teams, guests and the rights matrix (`m16-notes-sharing`). In this change every list is private to its owner.
- Item goals, coordinates, beams and JEI (`m16-notes-goals`).
- HUD pin and toasts (`m16-notes-hud`).
- Undo, history of ticks, deadlines, priorities and labels.

## Impact

- New in `notes/`: `model` (pure), `server` (saved data, edit handling), `network` (payloads), `client` (client cache, notebook pages), `config`, JUnit tests and GameTests.
- Depends on `m16-notes-project` (apply after it).
