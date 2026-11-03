## ADDED Requirements

### Requirement: Item goals
A task SHALL be able to hold item goals. A goal SHALL name one item, a target amount and a count, and SHALL show on the task page the item icon, its name, a bar of count against target, the count and "/ target", and a remove button × for editors. When the count reaches the target the goal SHALL show as reached. Reaching all targets SHALL NOT tick the task by itself; only a claimed task ticks on its own (see Auto tick of claimed item tasks).

#### Scenario: Goal row
- **WHEN** a task has the goal Blackstone with count 1840 and target 4000
- **THEN** the row shows the Blackstone icon, "Blackstone", a bar a little under half full, 1840 and "/ 4000"

#### Scenario: Target reached
- **WHEN** the count of a goal is raised to its target
- **THEN** the goal shows as reached and the task stays unticked

### Requirement: Shared count typed by hand
Every player who may tick a task SHALL be able to set the count of its goals by typing a number into the count field, which is always shown as a field; the number SHALL be saved on Enter or when the field loses focus. The server SHALL record who changed the count last; the notebook does not show it. Players who may edit the project SHALL add and remove goals, change their item (by clicking its icon) and type their target the same way.

#### Scenario: Reader updates a count
- **WHEN** a read guest sets the count of Packed Ice from 210 to 300
- **THEN** everyone who sees the project sees 300

### Requirement: Add what I carry
A goal that is not reached SHALL show, directly left of its bar, a button "+ n" to a player who may set its count and carries some of its item, n being what they carry but at most what is still missing. Pressing it SHALL add n to the shared count and leave the inventory as it is. The button SHALL then stay hidden for that goal until the number of that item the player carries changes or the notebook is opened again.

#### Scenario: Add the carried stone
- **WHEN** Anna carries 800 Blackstone and presses "+ 800" beside the goal Blackstone 1840 / 4000
- **THEN** the goal reads 2640 / 4000, the button is gone, and Anna still carries her 800 Blackstone

#### Scenario: Capped at the target
- **WHEN** Sepp carries 100 Stone beside the goal Stone 40 / 64
- **THEN** the button reads "+ 24" and pressing it makes the goal 64 / 64

#### Scenario: Back after picking up more
- **WHEN** Anna pressed "+ 800" and then picks up 64 more Blackstone
- **THEN** the button shows again

### Requirement: You carry
Hovering a goal's bar SHALL show "You carry n", with n the number of that item in the viewing player's own inventory (also 0), counted on their client and never sent to the server. The goal bar SHALL fill green up to the shared count and then yellow for the carried items on top of it, together never past the target.

#### Scenario: Different viewers
- **WHEN** Dirnei carries 512 Blackstone and Anna carries 64 and both hover the bar of the same goal
- **THEN** Dirnei reads "You carry 512" and Anna reads "You carry 64"

### Requirement: Choosing the item
"+ Add item" in the Items section of the task page, and clicking a goal's name, SHALL open an item picker over the right page with a search field over all items. JEI's item list SHALL show beside the notebook only while the picker is open, with the book moved left to make room, and dragging an item from JEI onto the picker SHALL pick it. Servus: Notes SHALL work without JEI.

#### Scenario: Picker
- **WHEN** an editor presses "+ Add item", types "ice" and picks Packed Ice
- **THEN** the task gets a goal for Packed Ice with count 0 and a target ready to type

#### Scenario: JEI drag
- **WHEN** JEI is installed and an editor opens the picker and drags Glass from the JEI list onto it
- **THEN** the task gets a goal for Glass

#### Scenario: JEI hidden otherwise
- **WHEN** JEI is installed and the notebook is open without the item picker
- **THEN** JEI shows no item list beside the notebook

### Requirement: Missing items
A goal whose item no longer exists (a mod was removed) SHALL keep its saved item id and show it as an unknown item, and SHALL come back when the item exists again.

#### Scenario: Mod removed
- **WHEN** a goal names an item of a mod that is no longer installed
- **THEN** the goal shows the item id as an unknown item and keeps its count and target

### Requirement: Coordinates
A task SHALL be able to hold one spot: a block position, a dimension and an optional name of at most 32 characters. The Place section of the task page SHALL show the spot as a chip, for example "Portal room · X 120 Y 64 Z -30 Nether", or "nowhere yet"; editors SHALL name it in a field ("Name this place"), set it to where they stand with "Set to here", which keeps the name, and remove it with "Clear". The task list SHALL mark a task that has a spot, with the chip text as tooltip.

#### Scenario: Task here
- **WHEN** a player in the Nether at -120 64 340 opens "Clear tunnel" and presses "Set to here"
- **THEN** the task shows the chip "X -120 Y 64 Z 340 Nether"

#### Scenario: Name a place
- **WHEN** an editor types "Portal room" into the place's name field and presses Enter
- **THEN** everyone who sees the project reads "Portal room · X 120 Y 64 Z -30 Nether"

#### Scenario: Name too long or missing
- **WHEN** a client sends a place name of 33 characters, or a spot saved before places had names is read
- **THEN** the name is refused as too long, and the old spot reads without a name

### Requirement: Beam
"Beam on" next to a spot SHALL turn a beam on at that spot for the clicking player only, shown while they are in that dimension, with the chip and the button lit; "Beam off" SHALL turn it off. Beams SHALL last until switched off or the player leaves the game.

#### Scenario: Find the spot
- **WHEN** a player presses "Beam on" on a task in their dimension and closes the notebook
- **THEN** a beam stands at that spot, visible from afar, until the player presses "Beam off"

### Requirement: Auto tick of claimed item tasks
An open task with item goals SHALL be ticked for the player who claims it as soon as that player's inventory holds every missing item (target minus count, summed per item). The assignee claims a task; a task without an assignee on a Private list is claimed by the list's owner. The tick SHALL be recorded as done by that player. A task ticked this way SHALL NOT be ticked again automatically in the same session, so unticking it by hand sticks.

#### Scenario: Owner gathers the items for a private task
- **GIVEN** Dirnei's Private list has an unassigned task with the goal Stone 40 / 64
- **WHEN** Dirnei carries 24 Stone
- **THEN** the task is ticked, done by Dirnei

#### Scenario: Unclaimed shared task stays open
- **GIVEN** a Server list task without assignee with the goal Stone 0 / 64
- **WHEN** Anna carries 64 Stone
- **THEN** the task stays open
