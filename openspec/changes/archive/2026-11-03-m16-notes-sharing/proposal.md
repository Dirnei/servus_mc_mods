## Why

The point of the notebook is playing together: team projects and server projects need lists that more than one player sees and edits (explore 2026-11-02). This change adds the three scopes, the FTB team as an optional dependency, guests per list and the rights decided in the explore (PLAN.md, M16 Notes, `m16-notes-sharing`).

## What Changes

- Every list has a scope: **Private**, **Team** or **Server**, plus guests, each with **edit** or **read**.
- Team means the FTB Teams party of the list's owner. FTB Teams is an optional dependency: without it the Team scope and the Team tab do not exist.
- Rights (decided): the owner always edits and may switch the scope in any direction; team members edit Team lists; everyone reads Server lists and ops edit them; guests edit or read as set; ops do not see Private or Team lists. Anyone who sees a list may tick and claim tasks.
- Notebook tabs: Mine, Shared with me, Team (only with FTB Teams while the player is in a party), Server, with counts. A "Share" popup for the owner: scope buttons and the guest list with edit/read and remove, adding any player who has been on the server.
- The read-only banner on lists the player may only read.
- Edge cases: leaving a team removes access to its lists (unless owner or guest); a disbanded team's lists fall back to Private for their owners; without FTB Teams installed, Team lists are seen by their owners only and come back when FTB Teams returns.
- Op command `/notes transfer <from> <to>` hands every list of a player to another player.

## Capabilities

### New Capabilities
- `notes/sharing`: scopes, guests, rights, the share popup and tabs, FTB Teams integration and the transfer command.

### Modified Capabilities
None. `notes/lists` (from `m16-notes-lists`, not archived yet) already leaves visibility beyond the owner to sharing.

## Non-goals

- Item goal counts (`m16-notes-goals`; they follow the tick rule defined here).
- Toasts when someone else ticks (`m16-notes-hud`).
- Team mods other than FTB Teams, vanilla scoreboard teams.
- An op command to read private or team lists.
- Respecting FTB Teams ally relations.

## Impact

- `notes/`: model rights (pure, JUnit), a team lookup with an FTB Teams adapter loaded only when FTB Teams is present, payloads for scope and guests, the share popup and tabs, the command.
- `notes/build.gradle`: FTB Teams and FTB Library as `compileOnly` and in the Notes dev runs; `neoforge.mods.toml`: `ftbteams` optional.
- Depends on `m16-notes-lists` (apply after it).
