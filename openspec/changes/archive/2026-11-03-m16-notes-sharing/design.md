## Context

See proposal.md. After `m16-notes-lists`, lists live in `NotesData`, edits go through the pure `NoteEdits` with an `Access` interface (today `OwnerAccess`), and the server pushes whole-list snapshots to whoever may see a list. FTB Teams 26.1.2.4 is already a dev run mod of the family (`ftb_teams_version` in `gradle.properties`).

## Goals / Non-Goals

**Goals:** the scopes, guests and rights matrix decided in the explore, the four tabs and the share popup, FTB Teams as a soft dependency, the team edge cases and the transfer command.

**Non-Goals:** see proposal.md, Non-goals.

## Decisions

### Model additions
`NoteList` gains `Scope scope` (`PRIVATE`, `TEAM`, `SERVER`), `Optional<UUID> team` (set while the scope is Team) and `List<Guest> guests` with `Guest(Person who, Right right)`, `Right` = `EDIT`, `READ`. New edits: `SetScope(listId, scope)`, `AddGuest(listId, person, right)`, `SetGuestRight`, `RemoveGuest`, all `canManage` only. `SetScope(TEAM)` takes the actor's current party from the server and is refused without one. The codec reads lists saved before this change with the defaults Private, no team, no guests.

### Rights in one pure class
`model.SharedAccess` implements `Access` from a `Viewer(UUID id, boolean op, Optional<UUID> party, boolean teamsInstalled)`:
- see: owner, or guest, or Server, or (Team and teamsInstalled and party equals team)
- edit: owner, or edit guest, or (Team and teamsInstalled and party equals team), or (Server and op)
- tick and claim: see
- manage (share, scope, delete list): owner
A Team list while FTB Teams is missing is seen by its owner and guests only. JUnit `SharedAccessTest` walks the whole matrix of the mockup (owner, team member, read guest, edit guest, stranger, op) for each scope, with and without FTB Teams.

### Team lookup with an optional adapter
`team.TeamLookup` (`installed()`, `party(UUID player) -> Optional<UUID>`). `team.NoTeams` is the default. When `ModList.get().isLoaded("ftbteams")`, the mod class installs `integration.ftbteams.FtbTeamsLookup` and registers `integration.ftbteams.FtbTeamsListener` on `NeoForge.EVENT_BUS`; no other class touches FTB types, so the classes never load without FTB Teams. Verified with javap on `ftb-teams-neoforge-26.1.2.4`:
- `FTBTeamsAPI.api().isManagerLoaded()`, `.getManager().getTeamForPlayerID(UUID)` returns `Optional<Team>`; only `Team#isPartyTeam()` counts as a party, `Team#getId()` is the id.
- NeoForge events in `dev.ftb.mods.ftbteams.api.neoforge.FTBTeamsEvent`: `PlayerJoinedPartyTeam`, `PlayerLeftPartyTeam`, `TeamDeleted` with `getEventData()` records.
FTB Teams needs only FTB Library (no Architectury). `notes/build.gradle`: `compileOnly` FTB Teams and FTB Library, `localRuntime` both for the Notes runs; mods.toml: `ftbteams` `optional`, `ordering="AFTER"`.

### Team events
- Joined or left a party: resend the player's visible set (`ListsPayload`) and a `ViewerPayload`.
- `TeamDeleted`: every list with that team becomes Private, team cleared; changed lists are broadcast.
- FTB Teams removed: nothing to do; `SharedAccess` hides Team lists from members while `teamsInstalled` is false, and they work again when it is back.

### Known players
`NotesData` keeps `knownPlayers` (UUID to last name), updated on every login. The share popup offers these names. Vanilla offers no way to list every cached profile (`UserNameToIdResolver` only answers single lookups, verified in the 26.1.2 sources), so players who joined before Notes was installed appear after their next login. `network.KnownPlayersPayload` sends the set on login and when a new player joins.

### What the client needs to know
`network.ViewerPayload(UUID self, Optional<UUID> party, boolean teamsInstalled, boolean inParty)` on login and on party changes. Each list snapshot carries the recipient's `canEdit` and `canManage` (computed per recipient on the server), so banners and buttons never guess. Tabs are sorted on the client: Mine (owner is self), Shared with me (self is a guest), Team (scope Team and team equals party), Server (scope Server).

### Op check
A player is an op for Notes when `Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)` accepts `player.createCommandSourceStack()`, the same predicate the family's commands use (`ServusFoundationCommands`).

### Share popup and tabs
The popup sits on the right page: scope buttons (Team hidden without a party), a hint line per scope, the guest rows with edit/read toggle and remove, a player dropdown of known players not yet on the list with "+ Guest". Tabs are four paper tab sprites (red, green, blue, gold) on the top edge of the right page with a count each; the active one stands up.

### Transfer command
`command.NotesCommands` on `RegisterCommandsEvent`: `/notes transfer <from> <to>` with `.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))`, names resolved through `knownPlayers` with suggestions. A guest entry for the new owner on a moved list is dropped.

### Settles from PLAN.md
The explore of 2026-11-02 decided scopes, rights and FTB Teams. This change decides the open edge cases: a disbanded party's lists become Private (nothing is lost), and players from before the install appear after their next login.

### Decided during apply
- `Right` is nested as `Guest.Right` (`EDIT`, `READ`) so it does not read like `Rights`.
- `Access` gains `default Optional<UUID> party(UUID actor)`; `SharedAccess` answers it from the `Viewer` (empty without FTB Teams), so `SetScope(TEAM)` stays pure. New refusals: `NO_PARTY` (Team without a party) and `LIMIT_GUESTS` (`NoteEdits.MAX_GUESTS` = 64 per list, against forged packets).
- Transfer and disband are pure too: `NoteEdits.transfer(book, from, to)` and `NoteEdits.teamDeleted(book, team)` return `Applied`; `NotesService.transfer` and `teamDeleted` save and broadcast them.
- `SharedAccess(Function<UUID, Viewer>)` caches each player per instance; `NotesService.access(server)` builds one per edit or broadcast from `NotesService.viewer(server, id)`. An offline player (GameTests, commands) counts as op when the ops list holds them (`PlayerList#isOp`).
- `NotesService.apply` resolves an `AddGuest` against the known players: an unknown id is refused `NOT_FOUND`, a known one is stored with its known name.
- Team lookup holder is `team.Teams` (`lookup()`, `install()`); `integration.ftbteams.FtbTeamsListener.install()` is the one entry point, called from the mod class when `ftbteams` is loaded. Joins and leaves are queued with `NotesService.refreshLater(uuid)` and resent on `ServerTickEvent.Post` (mailbox), so the party is read after FTB Teams applied the change.
- `ViewerPayload(boolean teamsInstalled, Optional<UUID> party)`: the client knows its own id, and "in party" is `party.isPresent()`. It is sent right before every `ListsPayload`.
- Tabs: with counts, "Shared with me 0" does not fit next to the other three on the right page, so the tab reads "Shared" and its tooltip "Shared with me"; tabs start at the right page's edge with less padding.
- The scope seal sits at the start of the meta line under the title (the title row already holds Share and Delete); the meta line adds the guest count.
- The read-only banner takes one or two ruled lines at the top of the task rows; the rows move down and use the add row's line instead.
- The player picker is a dropdown that opens above the add row; a new guest starts as read, as in the mockup.
- `-PnotesWithoutFtb` leaves FTB Teams and FTB Library out of the Notes dev runs, to check Notes without them.
- Picking another assignee from the known players is not part of this change (the design does not cover it); clicking "@name" still clears the assignee.



Pure, JUnit: `SharedAccess` (matrix), new `NoteEdits` (scope, guests, refused for non-owners, Team refused without party), codec defaults for old saves. GameTest: a Server list is visible to a second mock player and editable only by an op; `transfer` moves lists. Manual with FTB Teams: party join, leave and disband in the client.

## Risks / Trade-offs

- [FTB Teams changes its API in a later 26.1 build] -> all FTB calls sit in two small classes behind `TeamLookup`; a failure there falls back to `NoTeams` with a log line instead of a crash.
- [Per-recipient snapshots cost a little more than one broadcast] -> lists are small and edits are discrete.

## Migration Plan

None. Nothing is released; older saves from `m16-notes-lists` read with defaults.

### Decided during the play check
- Wording: tabs count projects ("Projects you own", "Team projects of your party", "Projects for the whole server"), refusals and the read-only banner say project.
- Share opens from the project's buttons on the left page; the Share popup covers the whole right page and its rows follow the book's size (the guest rows fill the room left).
- The read-only banner sits on the left page under the project's buttons (up to three lines); read-only players get the task page with values as text, keeping the tick box, "I'll do it" and the count fields.
- Picking another assignee from the known players is part of the task page now ("Assign ▾", see `m16-notes-lists`).
