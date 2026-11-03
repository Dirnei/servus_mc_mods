# Tasks

## 1. Pure rights and edits

- [x] 1.1 `notes/model`: `Scope`, `Right`, `Guest`, `NoteList` fields, `Viewer`, `SharedAccess`; JUnit `SharedAccessTest` (full matrix per scope, with and without FTB Teams); verify `gradlew :notes:test`
- [x] 1.2 `notes/model`: edits `SetScope`, `AddGuest`, `SetGuestRight`, `RemoveGuest` in `NoteEdits`; extend `NoteEditsTest` (manage only by owner, Team refused without party, guests kept through a scope switch, op edits Server tasks but cannot delete the list, reader ticks); `NotesCodecs` defaults, extend `NotesCodecsTest` with an old save; verify `gradlew :notes:test`

## 2. Teams

- [x] 2.1 `notes/build.gradle` (FTB Teams and FTB Library `compileOnly` and `localRuntime`), mods.toml `ftbteams` optional; `notes/team/TeamLookup`, `NoTeams`; `notes/integration/ftbteams/FtbTeamsLookup`, `FtbTeamsListener` (join, leave, delete) installed only when `ftbteams` is loaded; verify `gradlew build` and `:notes:runClient` starts with and without FTB Teams on the classpath
- [x] 2.2 `notes/server/NotesService`: `SharedAccess` with op and party per player, per-recipient snapshots with `canEdit`/`canManage`, `ViewerPayload`, `KnownPlayersPayload`, known players in `NotesData`, disband to Private; verify `gradlew build`

## 3. Client

- [x] 3.1 Tabs Mine, Shared with me, Team, Server with counts and the four tab sprites; read-only banner; edit controls hidden without `canEdit`; verify `gradlew build`
- [x] 3.2 Share popup (scope buttons, hints, guest rows, add guest from known players); lang keys, `runData`; verify `gradlew build`

## 4. Command and GameTests

- [x] 4.1 `notes/command/NotesCommands` (`/notes transfer`, suggestions); verify `gradlew build`
- [x] 4.2 GameTests: Server list visible to a second player, task edit refused for a non-op stranger and allowed for an op, transfer moves every list; verify `gradlew :notes:runGameTestServer` and `gradlew build test`

## 5. Final

- [x] 5.1 Full gradle build, tests green, `clientAll` (with FTB Teams) and `:notes:runClient` launch (with and without FTB Teams)
- [x] 5.1a Share a list as Server and as Team in the client (needs clicks in the notebook; left for the play check)
- [x] 5.2 Play check by the user, ideally with a friend on a server: team list, guest, Server list and ticks by others
