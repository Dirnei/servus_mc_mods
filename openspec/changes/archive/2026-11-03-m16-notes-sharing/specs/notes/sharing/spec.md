## ADDED Requirements

### Requirement: Scopes
Every list SHALL have one scope: Private, Team or Server. A new list SHALL be Private. The owner SHALL be able to switch the scope at any time in any direction from the Share popup, opened with the project's Share button on the left page; guests SHALL stay through a switch.

#### Scenario: Private to Server
- **WHEN** the owner of a Private list with the guest Anna switches it to Server
- **THEN** every player sees it in the Server tab and Anna keeps her guest right

#### Scenario: Open the Share popup
- **WHEN** the owner presses Share under the name of an open project
- **THEN** the Share popup opens over the right page

### Requirement: Team scope uses FTB Teams
The Team scope SHALL exist only while FTB Teams is installed, and SHALL mean the FTB Teams party the owner is in when switching to Team; the list keeps that team. Servus: Notes SHALL load and work without FTB Teams. A player who is in no party SHALL not be offered the Team scope.

#### Scenario: Without FTB Teams
- **WHEN** a game runs Servus: Notes without FTB Teams
- **THEN** the notebook shows no Team tab and the Share popup offers only Private and Server

#### Scenario: Solo player
- **WHEN** a player with FTB Teams installed is in no party
- **THEN** the Share popup offers no Team button and the notebook shows no Team tab

### Requirement: Guests
The owner SHALL be able to add any player who has played on the server, online or not, as a guest of a list with the right edit or read, change the right and remove the guest. A guest SHALL find the list in the Shared tab.

#### Scenario: Read guest
- **WHEN** Dirnei adds Sepp as a read guest of "Nether highway"
- **THEN** Sepp sees the list under the Shared tab, cannot edit its tasks, and can tick them

### Requirement: Rights
The server SHALL decide what a player may do with a list by these rules:
- The owner sees and edits the list, shares it and switches its scope.
- Members of the list's FTB team see and edit a Team list.
- Every player sees a Server list; ops edit it.
- An edit guest sees and edits; a read guest sees.
- Nobody else sees the list; ops do not see Private or Team lists.
- Every player who sees a list may tick and untick its tasks and claim an open task.
Only the owner shares, switches the scope and deletes the list.

#### Scenario: Op on a Server list
- **WHEN** an op opens a Server list owned by someone else
- **THEN** the op can edit and delete its tasks but cannot share it or delete the list

#### Scenario: Op and a private list
- **WHEN** an op opens the notebook
- **THEN** the private lists of other players do not appear anywhere

#### Scenario: Reader ticks
- **WHEN** a player who may only read a Server list ticks one of its tasks
- **THEN** the task is ticked with that player recorded as the one who did it

### Requirement: Tabs
The notebook SHALL show the tabs Mine (projects the player owns), Shared (named "Shared with me" in its tooltip; projects where the player is a guest), Team (Team projects of the player's party, only with FTB Teams and while in a party) and Server (all Server projects), each with the number of projects in it, as coloured paper tabs (red, green, blue, gold). Clicking a tab SHALL show the projects of that tab, also when a project of it is open.

#### Scenario: Counts
- **WHEN** a player owns three projects and is a guest of none
- **THEN** the tabs read "Mine 3" and "Shared 0"

### Requirement: Read-only banner
A project the player sees but may not edit SHALL show a banner on its task list that says why (read guest, or Server project edited by its owner, ops and edit guests) and SHALL hide "+ New task" and the edit controls; the task page SHALL show the values as text, keeping tick, "I'll do it" and typing counts.

#### Scenario: Banner
- **WHEN** a read guest opens a project
- **THEN** the banner reads that they are a read guest and no "+ New task" button is shown

### Requirement: Team changes
A player who leaves a party SHALL lose access to that team's Team lists unless they own them or are a guest. When a party is disbanded, its Team lists SHALL become Private lists of their owners. When FTB Teams is removed from a game, Team lists SHALL stay saved and be seen by their owners only, and SHALL work as Team lists again once FTB Teams is back.

#### Scenario: Member leaves
- **WHEN** Anna leaves the party of a Team list she does not own and is not a guest of
- **THEN** the list disappears from her notebook

#### Scenario: Party disbanded
- **WHEN** the party of a Team list is disbanded
- **THEN** the list is the Private list of its owner, with its guests

### Requirement: Transfer command
An op (permission level 2) SHALL be able to run `/notes transfer <from> <to>` to make the second player the owner of every list the first player owns.

#### Scenario: Player left the server
- **WHEN** an op runs `/notes transfer Max Anna`
- **THEN** every list Max owned is owned by Anna and the command reports how many lists moved
