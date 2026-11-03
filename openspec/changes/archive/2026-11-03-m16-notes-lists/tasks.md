# Tasks

## 1. Pure model

- [x] 1.1 `notes/model`: `NoteList`, `Task`, `Person`, `DoneRecord`, `NotesBook`, `Access`, `Rights`, `ListView`, `OwnerAccess`, `Limits`, `Moment`, `Ago`; JUnit `AgoTest`; verify `gradlew :notes:test`
- [x] 1.2 `notes/model`: `NoteEdit` (sealed records), `Refusal`, `EditResult` and `NoteEdits.apply`; JUnit `NoteEditsTest` (create and cap, rename, delete, add and cap, text too long, edit text and notes, delete parent takes subs, move up and down with leading sub normalised, indent one level only, outdent, tick record with day and time, untick clears, claim only open and unassigned, set and clear assignee, refused for a non-owner); verify `gradlew :notes:test`

## 2. Server

- [x] 2.1 `notes/server`: `NotesCodecs`, `NotesData` (`SavedDataType`); JUnit `NotesCodecsTest` round trip with `JsonOps`; verify `gradlew :notes:test`
- [x] 2.2 `notes/config/NotesServerConfig` registered from the mod class; `notes/server/NotesService` (apply, save, broadcast); verify `gradlew build`
- [x] 2.3 `notes/network`: `ListsPayload`, `ListPayload`, `ListGonePayload`, `EditPayload` (byte tag dispatch), `RefusedPayload`, `NotesStreamCodecs`, `NotesPayloads` registration, login sync; verify `gradlew build`

## 3. Client

- [x] 3.1 `notes/client/ClientNotes` (cache, selected list, listeners); contents page in `NotebookScreen` (entries, leader, count, "+ New list", scroll); verify `gradlew build`
- [x] 3.2 Right page: title rename, delete with confirm, task rows (tick, inline edit, meta line, notes popup, hover buttons, assignee and claim), add row, footer messages; lang keys in `NotesLanguageProvider`, `runData`; verify `gradlew build`
- [x] 3.3 Motion: tick pop and sparks, page flip; sprites for check mark, sparks and the row icons added to `tools/notes-textures/gen.py`; verify `gradlew build`

## 4. GameTests

- [x] 4.1 `notes/gametest`: an edit through `NotesService` lands in `NotesData` and survives a codec round trip; an edit from a player who is not the owner is refused; verify `gradlew :notes:runGameTestServer` and `gradlew build test`

## 5. Final

- [x] 5.1 Full gradle build, tests green, `:notes:runClient` and `clientAll` launch; make, edit, tick, indent and delete tasks in the client and see them after a relog
- [x] 5.2 Play check by the user
