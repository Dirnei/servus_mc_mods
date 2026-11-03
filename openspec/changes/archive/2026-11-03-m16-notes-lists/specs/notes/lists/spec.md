## ADDED Requirements

### Requirement: Lists
A player SHALL be able to create a list, called a project in the notebook, from the "Mine" tab with "+ New project", rename it and delete it. A new project SHALL belong to the player who made it (its owner) and SHALL be private: only the owner sees it until it is shared. Deleting a project SHALL ask for a confirmation.

#### Scenario: New project
- **WHEN** a player presses "+ New project" in the "Mine" tab
- **THEN** a project named "New project" owned by that player is made and opened, and its name is ready to edit

#### Scenario: Delete with confirmation
- **WHEN** the owner presses Delete on a project, then "Sure?"
- **THEN** the project and all its tasks are gone for everyone and the notebook shows the projects again

### Requirement: Projects page
The projects page SHALL list the projects of the current tab, each with its name, a dotted leader and the number of open tasks, and a small line below with its scope, owner and a "pinned" mark. Clicking an entry SHALL open the project.

#### Scenario: Open count
- **WHEN** a project has five tasks of which two are ticked
- **THEN** its entry shows 3

### Requirement: Task list
With a project open, the left page SHALL show the "‹ Projects" link, the project's name, a line with its scope seal, "n of m done" and its guests, the project's buttons (Rename, Share, Pin, Delete, each as the player's rights allow), the tasks on ruled lines and, for players who may edit, "+ New task" at the bottom.

#### Scenario: Header
- **WHEN** the owner opens a private project with six tasks of which two are ticked
- **THEN** the left page shows "‹ Projects", the name, "Private", "2 of 6 done" and the buttons Rename, Share, Pin and Delete

### Requirement: Task rows
Each task SHALL take one ruled line: a tick box, the text cut with "..." when long, and small marks for the number of item goals, a place (its name and coordinates as tooltip) and "@assignee". Subtasks SHALL be indented. A click on the box SHALL tick, a click on the line SHALL open the task on the right page, and an editor's click on the text of the open task SHALL turn it into a text field: Enter or a click elsewhere saves, Escape takes the saved text back, an emptied field keeps the old text.

#### Scenario: Open a task
- **WHEN** a player clicks the text of "Build the portal room"
- **THEN** the right page turns and shows that task

#### Scenario: Rename a task in place
- **WHEN** an editor clicks the text of the open task, types "Build the portal hall" and presses Enter
- **THEN** the line shows "Build the portal hall" for everyone who sees the project

### Requirement: Drag to move
An editor SHALL move a task by dragging its line; a drag starts only after the mouse moved a few pixels, so a click still opens or ticks. A copy of the line SHALL follow the mouse and a line SHALL show where the task lands. The height of the drop SHALL choose the place, and a drop about one indent to the right SHALL make the task a subtask of the task above. A task with subtasks SHALL move with them and stay a task. Escape or a drop outside the list SHALL cancel.

#### Scenario: Make a subtask by dragging
- **WHEN** an editor drags "Light it up" under "Build the portal room" and drops it one indent to the right
- **THEN** "Light it up" is a subtask of "Build the portal room"

#### Scenario: Parent moves with its subtasks
- **WHEN** an editor drags a task with two subtasks below the last task
- **THEN** the task and both subtasks are at the end, the subtasks still under it

#### Scenario: Task dropped among subtasks
- **WHEN** an editor drops a task without indent below the first of two subtasks
- **THEN** it lands behind their parent's whole block as a task

#### Scenario: No subtask at the top
- **WHEN** an editor drops a task one indent to the right above the first task
- **THEN** it becomes the first task, not a subtask

#### Scenario: Scroll while dragging
- **WHEN** an editor holds a dragged line at the bottom edge of a long list
- **THEN** the list scrolls on until the line moves away from the edge

### Requirement: Tasks
On a project the player may edit, "+ New task" SHALL open a line to write in at the end of the task list; Enter adds the task, opens it on the right page once it exists and leaves an empty line for the next one. Escape or a click elsewhere without text SHALL close the line without a task. The task page SHALL show the task's name as a heading, notes in a multi-line field down to the bottom of the page (saved when it loses focus, its character count on the Notes label line), and a bin at the top right: the first click turns it red, the second deletes the task. Tasks SHALL nest one level only.

#### Scenario: Write a quick list
- **WHEN** a player presses "+ New task", types "Portal at spawn", presses Enter, types "Gold farm" and presses Enter
- **THEN** both tasks appear at the end of the project, unticked, and an empty line waits for the next one

#### Scenario: Discard an empty new task
- **WHEN** a player presses "+ New task" and then Escape or clicks elsewhere without typing
- **THEN** no task is added

#### Scenario: One level only
- **WHEN** a player drags a task one indent to the right below a subtask
- **THEN** the task becomes a subtask of the same parent, never a third level

#### Scenario: Delete a parent
- **WHEN** a player deletes a task that has subtasks
- **THEN** its subtasks are deleted with it

### Requirement: Ticking
Ticking SHALL happen in the task list only. Ticking a task SHALL record who ticked it, the in-game day and the real time, and the task page SHALL show "done by <player>, day <n> (<time ago>)"; the task list SHALL strike its text through. Unticking SHALL remove the record; no history is kept. The tick animation SHALL pop the check mark and throw a few sparks.

#### Scenario: Tick
- **WHEN** Anna ticks "Portal at spawn" on day 409
- **THEN** the task shows as done with "done by Anna, day 409 (just now)" for everyone who sees the project

#### Scenario: Untick
- **WHEN** a ticked task is unticked
- **THEN** it is open again and shows no done record

### Requirement: Assignee
A task SHALL have at most one assignee, shown as "@<player>" in the task list and in the "Who" section of the task page, or "nobody yet". A player who may tick SHALL be able to take an open task without an assignee with "I'll do it", and the project's editors SHALL be able to take any task the same way and to pick any known player or "nobody" from "Assign".

#### Scenario: Claim
- **WHEN** Sepp presses "I'll do it" on an open task without an assignee
- **THEN** the task shows "@Sepp" for everyone who sees the project

#### Scenario: Assign another player
- **WHEN** an editor opens "Assign" and picks Anna
- **THEN** the task shows "@Anna"

### Requirement: Saved and synced by the server
Lists SHALL be stored by the server in the world save and survive restarts. A player SHALL receive the lists they can see when they join and an update whenever one of them changes, so two players looking at the same list see the same state within a moment. The server SHALL check every edit against the player's rights and the limits and ignore edits that fail.

#### Scenario: Survives a restart
- **WHEN** the server restarts
- **THEN** every list, task, tick record and assignee is as it was

#### Scenario: Two viewers
- **WHEN** two players have the same list open and one ticks a task
- **THEN** the other sees the tick without reopening the notebook

#### Scenario: Forged edit
- **WHEN** a client sends an edit for a list its player may not edit
- **THEN** the server ignores it and nothing changes

### Requirement: Limits
The server config SHALL cap the number of lists a player owns, the number of tasks in a list and the length of names, task texts and notes (for example 50 lists, 200 tasks, 120 characters for a task, 500 for notes). The notebook SHALL tell the player when a cap is reached instead of failing silently.

#### Scenario: List cap
- **WHEN** a player who owns as many lists as the cap allows presses "+ New project"
- **THEN** no project is made and the notebook shows that the limit of projects is reached
