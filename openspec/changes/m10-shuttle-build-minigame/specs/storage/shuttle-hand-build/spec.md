## Purpose

Building a shuttle tier by hand in the Shuttle Workshop: a short minigame on the shuttle that cannot fail and can always be skipped, rewarded with a name plate that shows wherever the shuttle goes.

## ADDED Requirements

### Requirement: Build by hand
While every supply of the build sheet is in the workshop, the GUI SHALL offer Build by hand next to Build now. Build by hand SHALL show a large view of the shuttle and run the sheet's steps one after another, with the step's number, the step count and what to do. When the last step is done the GUI SHALL ask for a name.

#### Scenario: Start a build by hand
- **WHEN** a Mk1 shuttle and every supply of the Mk2 sheet are in the workshop and the player presses Build by hand
- **THEN** the GUI shows the shuttle large with step 1 of the sheet's steps

#### Scenario: Supplies missing
- **WHEN** one supply of the sheet is short
- **THEN** Build by hand is off, like Build now

#### Scenario: Last step done
- **WHEN** the player finishes the last step
- **THEN** the GUI shows a name field and an Engrave button

### Requirement: Build steps
A build step SHALL be one of three kinds: bolts (click glowing spots on the shuttle in order), wires (connect each contact on one side to the contact of the same colour on the other, in the eight duct colours) and weld (drag along a seam until it is welded). A step SHALL NOT fail: a click on the wrong spot or a wrong pair SHALL do nothing. The steps of each sheet SHALL come from the shuttle stats file with a kind and a count per step.

#### Scenario: Wrong bolt
- **WHEN** the player clicks a spot that is not the glowing one
- **THEN** nothing happens and the same spot still glows

#### Scenario: Wrong wire
- **WHEN** the player connects a red contact to a blue one
- **THEN** no wire is drawn and the red contact is still open

#### Scenario: Steps from data
- **WHEN** a datapack gives the Mk3 sheet the steps bolts 6 and weld 2
- **THEN** a build by hand of a Mk3 asks for six bolts and two seams and nothing else

#### Scenario: Short build
- **WHEN** a player plays a build with the shipped steps
- **THEN** it takes well under a minute

### Requirement: The server owns the build
The server SHALL keep which step a build by hand is on and SHALL only accept the step that is next; a report for any other step SHALL be ignored. While a build by hand runs, the shuttle and supply slots SHALL be locked, the install slot SHALL take nothing and the remove buttons and Build now SHALL be off. Cancel SHALL end the build with nothing used and the slots free again.

#### Scenario: Step out of order
- **WHEN** the client reports step 3 while the build is on step 1
- **THEN** the build stays on step 1

#### Scenario: Locked during the build
- **WHEN** a build by hand runs and the player tries to take a supply out
- **THEN** the supply stays in the workshop

#### Scenario: Cancel
- **WHEN** the player presses Cancel on step 2
- **THEN** the shuttle stays at its tier, every supply is still there and the slots are free

### Requirement: Build progress is kept
The workshop SHALL keep a build by hand and its step when the GUI closes and when the world is saved and loaded, and the build SHALL go on from that step when the GUI opens again. Breaking the workshop SHALL drop the shuttle and the supplies and lose the progress. Installing or removing upgrades SHALL NOT reset a build; only finishing or cancelling it SHALL.

#### Scenario: Close and come back
- **WHEN** the player closes the GUI on step 3 and opens it again later
- **THEN** the build goes on at step 3

#### Scenario: Break the workshop mid build
- **WHEN** a player breaks a workshop on step 2 of a build by hand
- **THEN** the shuttle, still at its old tier, and every supply drop as items

### Requirement: Engraving a name plate
Engrave SHALL finish the build like Build now, using exactly the sheet's supplies and keeping the shuttle's counts, tool and anvil name, and SHALL put the typed name on the new shuttle as its name plate. A name SHALL be at most 20 characters, without formatting codes. An empty name SHALL finish the build and keep the shuttle's old plate. Build now SHALL keep the old plate too.

#### Scenario: Engrave
- **WHEN** a player finishes a Mk2 build by hand and engraves "Berta"
- **THEN** the workshop holds a Mk2 shuttle with the plate "Berta", the Mk1's upgrades and tool, and the supplies are used

#### Scenario: Build now keeps the plate
- **WHEN** a Mk2 shuttle with the plate "Berta" is built into a Mk3 with Build now
- **THEN** the Mk3 shuttle still carries the plate "Berta"

#### Scenario: A new name replaces the old
- **WHEN** a Mk2 shuttle with the plate "Berta" is built by hand into a Mk3 and engraved "Kurt"
- **THEN** the Mk3 shuttle carries the plate "Kurt"

#### Scenario: Mk1 has no plate
- **WHEN** a player crafts a Mk1 shuttle
- **THEN** it carries no name plate, and only a tier build can give it one

### Requirement: The plate shows
A shuttle with a name plate SHALL show the name on the side of its hold while it is drawn on the rails, SHALL carry the name in its item name (so tooltips, the garage GUI and every inventory show it), and the Field Head SHALL name the shuttle while that shuttle works its field. An anvil name SHALL NOT show on the shuttle in the world.

#### Scenario: Plate in the world
- **WHEN** a Mk2 shuttle with the plate "Berta" drives along a rail
- **THEN** "Berta" is written on the side of its hold

#### Scenario: Plate in the garage
- **WHEN** the player hovers over a shuttle with the plate "Berta" in a garage slot
- **THEN** the tooltip names it Berta

#### Scenario: Field Head names the worker
- **WHEN** the harvester with the plate "Berta" works a field and a player opens the field's head
- **THEN** the head says that Berta is working the field

#### Scenario: Anvil name only on the item
- **WHEN** a Mk1 shuttle renamed at an anvil drives along a rail
- **THEN** no name is drawn on it
