## ADDED Requirements

### Requirement: Notebook key
Servus: Notes SHALL add a key binding "Open Notebook" in its own category "Servus: Notes", unbound to any item, with N as an example default. Pressing it in game, with no other screen open, SHALL open the notebook. Pressing it or Escape while the notebook is open and no text field has focus SHALL close it.

#### Scenario: Open anywhere
- **WHEN** a player in any dimension, holding any item or none, presses the notebook key with no screen open
- **THEN** the notebook opens

#### Scenario: Close with the same key
- **WHEN** the notebook is open, no text field has focus and the player presses the notebook key
- **THEN** the notebook closes

#### Scenario: Rebind
- **WHEN** a player opens Options, Controls, Key Binds
- **THEN** "Open Notebook" is listed under "Servus: Notes" and can be rebound

### Requirement: Book look
The notebook SHALL be drawn as an open leather bound book: a brown cover around two paper coloured pages and coloured paper tabs on the top edge of the right page. The game SHALL keep running behind the notebook (it does not pause a single player game).

#### Scenario: Empty notebook
- **WHEN** a player who has no projects opens the notebook
- **THEN** the book shows the "Projects" page with the text "No projects yet" and a short hint on the right page

#### Scenario: Game keeps running
- **WHEN** a player opens the notebook in a single player world
- **THEN** the world keeps ticking behind it

### Requirement: Three levels
The notebook SHALL show three levels and turn the pages between them: the projects of the active tab on the left page headed "Projects"; once a project is open, its tasks on the left page with a "‹ Projects" link back; and the chosen task on the right page.

#### Scenario: Back to the projects
- **WHEN** a player with a project open clicks "‹ Projects"
- **THEN** the pages turn and the left page lists the projects of the active tab again

### Requirement: Book size
The notebook SHALL fill the screen with a small margin, laid out larger in GUI pixels rather than scaled, so the pixel font stays sharp: at most about 576 by 328 GUI pixels and never smaller than 336 by 208. Only a window too small for the smallest book SHALL scale it down. Pages, ruled lines and the number of rows SHALL follow the size.

#### Scenario: Large screen
- **WHEN** a player opens the notebook on a screen of 640 by 360 GUI pixels
- **THEN** the book is about 576 by 328 GUI pixels and shows more task rows than on a small screen

#### Scenario: Tiny window
- **WHEN** the window is smaller than 336 by 208 GUI pixels plus the margin
- **THEN** the book keeps its smallest layout and is drawn scaled down to fit
