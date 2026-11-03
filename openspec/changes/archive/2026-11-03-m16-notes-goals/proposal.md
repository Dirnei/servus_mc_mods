## Why

A plain text list is something players already have outside the game. What makes an in-game list worth it is that tasks understand the game: how many blocks a project still needs and where the work is (explore 2026-11-02). This change adds item goals and coordinates with a beam (PLAN.md, M16 Notes, `m16-notes-goals`).

## What Changes

- **Item goals** on a task: an item, a target and a shared count typed by hand, with a bar, the target reached mark, and who changed the count last. Every viewer also sees "you carry: n", counted from their own inventory on their own client.
- Anyone who may tick a task may type its counts (decided); editors add and remove goals and change item and target.
- An item picker with a search field to choose a goal's item, and, when JEI is installed, dragging an item from JEI onto the add button or a goal.
- **Coordinates** on a task: "+ here" stores the player's position and dimension; the chip shows them and clicking it turns a beam on at that spot, visible in that dimension, until clicked again.

## Capabilities

### New Capabilities
- `notes/task-goals`: item goals with shared counts and "you carry", the item picker and JEI drag, coordinates and beams.

### Modified Capabilities
None. `notes/lists` and `notes/sharing` are not archived yet; this change adds parts to a task and follows the tick rule of `notes/sharing`.

## Non-goals

- Counting from chests, a Project Crate, or the Shuttles warehouse.
- Adding the ingredients of a JEI recipe in one go.
- Waypoints in Xaero's or JourneyMap.
- HUD lines for goals (`m16-notes-hud`).

## Impact

- `notes/`: model (goal and position, pure, JUnit), edit commands and payloads, the notebook goal rows and picker, a beam renderer, an optional JEI plugin.
- `notes/build.gradle`: JEI API `compileOnly`, JEI in the Notes dev client run; `neoforge.mods.toml`: `jei` optional.
- Depends on `m16-notes-sharing` (apply after it).
