## Context

See proposal.md. After `m16-notes-sharing` a task has text, notes, an assignee and a done record; edits are pure `NoteEdit` records checked by `SharedAccess`, where "tick" rights belong to everyone who sees the list. The family already drags JEI ingredients into its own screens (`foundation/integration/jei/DuctFilterGhostHandler`).

## Goals / Non-Goals

**Goals:** item goals with a shared typed count and the client side "you carry", picking items with a search or JEI, a spot per task and a beam to find it.

**Non-Goals:** see proposal.md, Non-goals.

## Decisions

### Model additions
- `Goal(UUID id, String itemId, int target, int count, Person lastBy)`; `itemId` is the item's registry id as text, so the pure model needs no item types and a goal survives a removed mod.
- `Spot(int x, int y, int z, String dimension)`.
- `Task` gains `List<Goal> goals` and `Optional<Spot> spot`.
- Edits: `AddGoal(listId, taskId, itemId)`, `SetGoalItem`, `SetGoalTarget`, `RemoveGoal`, `SetSpot(Optional<Spot>)` need `canEdit`; `SetGoalCount` needs `canTick` (decided: anyone who may tick may type counts) and records `lastBy`. `AddTask` gains an optional spot. Counts and targets are clamped to 0 to 1,000,000; a list holds at most `maxGoalsPerTask` (config, for example 16).

### Rendering an item id
The client resolves `itemId` through `BuiltInRegistries.ITEM.getOptional(Identifier.tryParse(itemId))`. A missing item shows a barrier icon and the raw id in grey ("unknown item"). Names use the item's display name.

### You carry
`client.Carried.count(player, item)` sums matching stacks over `player.getInventory()` (main, armour, offhand), recomputed while the notebook or HUD draws, nothing sent to the server.

### Item picker
A panel on the right page: an `EditBox` and a scrolling grid of matching items (display name contains the text, case insensitive; `@mod` filters by namespace like JEI), first 200 matches. Clicking an item fills the pending goal or creates one.

### JEI as a soft dependency
`integration.jei.NotesJeiPlugin` (`@JeiPlugin`, `IModPlugin`) registers an `IGhostIngredientHandler<NotebookScreen>` with targets on "+ item" (add a goal) and on every goal row the player may edit (change its item). JEI only loads the class when JEI is present. `notes/build.gradle`: JEI common and neoforge API `compileOnly` (as Delicacies), the JEI jar `localRuntime` for the Notes runs; mods.toml `jei` optional.

### Spot and beam
"+ here" takes `player.blockPosition()` and `level().dimension().identifier()` as the spot of the next added task; a task row has "set here" and "clear" on hover for editors. The chip text is "X a Y b Z c <Dimension>", with Overworld, Nether and End named and other dimensions shown by their path in title case.
Beams are client state (`client.Beams`, a set of spots), cleared on `ClientPlayerNetworkEvent.LoggingOut`. `client.BeamRenderer` listens to `SubmitCustomGeometryEvent` (game bus) and, for each beam in the current dimension, translates the pose stack to the spot minus the camera and calls `BeaconRenderer.submitBeaconBeam(poseStack, event.getSubmitNodeCollector(), BeaconRenderer.BEAM_LOCATION, 1f, animationTime, 0, 320, 0xFFD76A, 0.2f, 0.25f)`. Verified in `neoforge-26.1.2.116-sources.jar` (`SubmitCustomGeometryEvent#getSubmitNodeCollector`, `getPoseStack`) and `minecraft-patched-26.1.2.116-sources.jar` (`BeaconRenderer#submitBeaconBeam`). The chip of an active beam is drawn highlighted.

### Settles from PLAN.md
The explore decided typed counts plus "you carry". This change decides the clamp, the goal cap per task and that beams are per player client state, not shared.

### Decided during apply
- `AddGoal(listId, taskId, goalId, itemId)` carries a goal id the client picks, like tasks, so the notebook opens the new goal's target for typing once it arrives. Every goal edit names `taskId` and `goalId`. Tags 17 to 22 in `NotesStreamCodecs`: `AddGoal`, `SetGoalItem`, `SetGoalTarget`, `SetGoalCount`, `RemoveGoal`, `SetSpot`; `AddTask` writes its spot after `afterId`.
- A new goal starts with count 0, target 64 (`NoteEdits.NEW_GOAL_TARGET`) and `lastBy` the player who added it. Counts clamp to 0 to 1,000,000; targets to 1 to 1,000,000, so a goal is never reached with nothing needed.
- `Limits` gains `maxGoalsPerTask` (default 16, config 0 to 200) and keeps a five argument constructor; a full task is refused with the new `Refusal.LIMIT_GOALS`. Item ids longer than 256 characters are refused `TOO_LONG`, blank ones `EMPTY`; spots outside 30,000,000 blocks or with a blank or overlong dimension are refused `NOT_ALLOWED`.
- `NotesService` refuses `AddGoal` and `SetGoalItem` with `NOT_FOUND` when the item is not in the server's registry (or is air). Saved goals of removed items stay and show as unknown.
- The search is pure: `model.ItemQuery` (plain words in the name, `@word` in the namespace or the mod's display name), JUnit `ItemQueryTest`.
- Layout: every goal takes two ruled lines, the item line (reached check in the tick box column, 10 px icon, name, 26 px bar, "count / target") and the carry line ("you carry: n · last count by name"). Clicking the count types it (tick right), clicking "/ target" types the target (edit right). Hovering a goal shows change item and remove icons at the end of its carry line for editors. The spot is a chip on its own line under the task text and done record. The task hover strip gains "add item goal", "set spot here" and, with a spot, "clear spot".
- The add row has "+ here" (a toggle, green while the next task gets the spot; the add field hint shows it) and "+ item". "+ item" with text in the add field adds that text as a task and gives it the picked goal; with an empty field it adds the goal to the last task; with neither it is greyed. A JEI drop on "+ item" follows the same rule.
- JEI: `NotesJeiPlugin` also registers a screen handler (`NotebookScreen#bookArea`) so JEI shows its item list beside the notebook, which is a plain `Screen`. Drop places come from `NotebookScreen#itemDropsOnScreen()`, collected per frame like the click areas: "+ item", every goal line for editors, and the picker grid while it is open.
- The beam widens past 96 blocks like a beacon's and runs from the spot up to build height 2048; color `0xFFFFD76A`.

## Pure logic vs Minecraft

Pure, JUnit: goal and spot edits, count clamp, `SetGoalCount` allowed for a reader and refused for a stranger, `lastBy` recorded, goal cap, codec round trip with goals and spots, the dimension label (`model.Places.label`). GameTest: a reader's count edit is applied through `NotesService`. Manual: picker, JEI drag, you carry, beam.

## Risks / Trade-offs

- [A beam drawn through `submitBeaconBeam` depends on vanilla render state that may change] -> one call site in `BeamRenderer`; if it misbehaves, draw the quads with `RenderTypes.beaconBeam` instead.
- [Item search over the whole registry on every keystroke] -> filter a name list built once per opening of the picker.

## Migration Plan

None. Nothing is released; older saves read with no goals and no spot.

### Decided during the play check
- The goal bar shows the shared count in green and the viewer's carried items in yellow on top of it (pure `model.GoalFill`), in the notebook and on the HUD.
- `client.AutoTick` checks once a second and sends `Tick` for tasks from pure `model.AutoTicks.due`: open, with goals, claimed by the player (assignee, or the owner of a Private list when nobody is assigned), every missing item carried. Each task is auto ticked at most once per session. Done on the client because the inventory is counted there already and ticking needs no more rights than seeing the list.
- Goals live on the task page's Items section: "+ Add item" opens the picker; each goal row has the 16 px icon (item tooltip), the name (click to change the item, editors), the two-colour bar, the count and target as always visible fields (count for tick right, target for edit right, plain text otherwise; click puts the caret in, Enter or focus loss saves, Escape drops the typing), × to remove, and below "you carry n · counted by name". The goal list scrolls with the wheel when it does not fit. A just picked goal still opens its target for typing once it arrives.
- The Place section shows the chip or "nowhere yet" and the buttons "Set to here" and "Clear" (edit right) and "Beam on" / "Beam off" (lit while on). "+ here", "+ item" and the add row are gone; a task gets its spot and goals on its task page.
- JEI: `NotesJeiPlugin` returns gui properties only while the item picker is open, otherwise null so JEI hides its overlay; `NotebookScreen#itemDropsOnScreen` gives ghost targets (the picker grid) only then. While picking, `BookLayout` keeps 170 px free on the right and puts the book at the left margin, so JEI's list has room. Dropping onto "+ item" or a goal line is gone.
- Second play check: `Spot` gains `name` (empty by default; the saved `Name` field is optional, the stream codec writes it after the dimension). `NoteEdits.MAX_PLACE_NAME` = 32, longer refused `TOO_LONG`; names are kept on one line and stripped. `Places.chip` puts the name before the coordinates ("Portal room · X 120 Y 64 Z -30 Nether"), `Places.coordinates` is the bare part. Beams compare spots by position (`Spot.position()`), so renaming keeps a beam on.
- The Place section gives editors a name field ("Name this place", Enter or focus loss saves, Escape reverts) above the chip, whose text is then only the coordinates; readers see the full chip. "Set to here" keeps the name. The task list's place mark shows the chip as tooltip. The HUD shows no places, so it is unchanged.
- Third play check: only the goal's icon opens the picker to change the item (editors); its tooltip is the item's own tooltip plus "Click to change item". The name is plain text.
- A "+ n" paper button at the right of the "you carry" line, for players who may type counts, while they carry some of the item and the goal is not reached: it sends `SetGoalCount` with `Goal.countWithCarried(carried)` (count plus the carried items, at most up to the target; JUnit `GoalTest`), so `lastBy` becomes that player. The inventory is not touched.
- Fourth play check: a goal is one line again (18 px): icon, name, the "+ n" button directly left of the bar, bar, count / target, remove. The second line ("you carry n, counted by name") is gone; hovering the bar shows "You carry n", also for 0. `lastBy` stays in the model but the notebook no longer shows it. After "+ n" is pressed, `TaskPage` remembers the goal with the carried amount at that moment and hides the button until the player carries a different amount of the item; the memory lives in the page, so reopening the notebook shows it again.
