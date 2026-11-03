# Tasks

## 1. Pure model

- [x] 1.1 `notes/model`: `Goal`, `Spot`, `Places.label`, `Task` fields, goal and spot edits in `NoteEdits`, `maxGoalsPerTask` in `Limits`; JUnit `NoteEditsTest` (add, item, target, remove goal by editors only; count by reader allowed and stranger refused; clamp; lastBy; goal cap; spot set and clear; add task with spot), `PlacesTest`; `NotesCodecs` with goals and spots, extend `NotesCodecsTest`; verify `gradlew :notes:test`

## 2. Server

- [x] 2.1 `notes/config/NotesServerConfig` `maxGoalsPerTask`; `EditPayload` tags for the new edits; verify `gradlew build`

## 3. Client

- [x] 3.1 Goal rows (icon, name, bar, count click to type, reached mark, you carry, last count by), unknown item display, `client/Carried`; verify `gradlew build`
- [x] 3.2 Item picker panel (search, `@mod`, grid, scroll) and "+ item"; verify `gradlew build`
- [x] 3.3 Spot: "+ here", chip, set here and clear on hover; `client/Beams`, `client/BeamRenderer` on `SubmitCustomGeometryEvent`; lang keys, `runData`; verify `gradlew build`

## 4. JEI

- [x] 4.1 `notes/build.gradle` (JEI API `compileOnly`, JEI `localRuntime`), mods.toml `jei` optional; `notes/integration/jei/NotesJeiPlugin` with the ghost handler for "+ item" and goal rows; verify `gradlew build` and `:notes:runClient` with JEI

## 5. GameTests

- [x] 5.1 GameTest: a read guest's count edit is applied and records them as last; verify `gradlew :notes:runGameTestServer` and `gradlew build test`

## 6. Final

- [x] 6.1 Full gradle build, tests green, `clientAll` and `:notes:runClient` launch; add goals by picker and JEI, type counts, see you carry, turn a beam on and walk to it
  - Done during apply: `gradlew build test` green, 9 Notes GameTests pass, `:notes:runClient` reaches the title screen with JEI and the `servus_notes:jei` plugin loaded. Open: `clientAll` launch and the in-game steps (picker, JEI drag, counts, you carry, beam), which need someone at the keyboard.
- [x] 6.2 Play check by the user
