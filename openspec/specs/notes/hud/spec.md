# notes/hud Specification

## Purpose
TBD - created by archiving change m16-notes-hud. Update Purpose after archive.

## Requirements

### Requirement: Pin a list
Every project a player sees SHALL offer "Pin" (tooltip "Pin this project to the HUD") among its buttons, and the pinned project "Unpin". A player SHALL have at most one pinned list; pinning another replaces it. The pin SHALL be kept by the server and survive relogging. A pinned list the player can no longer see SHALL be unpinned.

#### Scenario: Pin follows the player
- **WHEN** a player pins "Nether highway", leaves and joins again
- **THEN** "Nether highway" is still shown on their HUD

#### Scenario: Lost access
- **WHEN** the owner removes a guest whose pinned list it was
- **THEN** the pin disappears from that guest's HUD

### Requirement: HUD pin look
The pinned list SHALL be drawn in the vanilla style (the game font with shadow on a translucent dark box): the list name, then the open tasks in list order with subtasks indented, then the goals whose target is not reached with a small bar and "count/target"; the bar fills green up to the shared count and yellow for what the player carries on top of it. It SHALL show at most the configured number of lines (for example 4) and end with "+n more" when lines are left out. Ticked tasks and reached goals SHALL not be shown. The pin SHALL hide while the HUD is hidden (F1) and while a screen is open.

#### Scenario: Line limit
- **WHEN** a pinned list has six open tasks and the line count is 4
- **THEN** the HUD shows the name, four tasks and "+2 more"

#### Scenario: Tick drops a line
- **WHEN** any player ticks an open task of the pinned list
- **THEN** that task leaves the HUD pin

### Requirement: Toggle key and placement
A key binding "Toggle HUD pin" in the "Servus: Notes" category SHALL hide and show the pin. The client config SHALL choose the corner (top left, top right, left middle) and the line count.

#### Scenario: Hide
- **WHEN** a player with a pinned list presses the toggle key
- **THEN** the pin is hidden until the key is pressed again

### Requirement: Tick toasts
When another player ticks a task on a list the player owns, is a guest of, or sees as a member of its team, the player SHALL get a toast naming the list, the player and the task. Server lists SHALL never cause toasts, and the player's own ticks SHALL not. A client config switch SHALL turn tick toasts off.

#### Scenario: Teammate finishes
- **WHEN** Anna ticks "Portal at spawn" on the Team list "Nether highway"
- **THEN** Dirnei, a member of that team, gets a toast "Nether highway: Anna ticked Portal at spawn"

#### Scenario: Server list
- **WHEN** someone ticks a task on a Server list
- **THEN** no player gets a toast
