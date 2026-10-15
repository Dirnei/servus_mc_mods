# shuttles/shuttle-looks Specification

## Purpose

How a shuttle looks: its tier, its Speed and Handling upgrades and its tool are visible on the shuttle, on the rails and as an item, so players tell their shuttles apart at a glance.

## Requirements

### Requirement: A shuttle is drawn from its parts
A shuttle SHALL be drawn as a body with a hold, drive parts, a Handling part and a tool, chosen from the shuttle item:
- The body and hold SHALL follow the tier: Mk1 an open wooden crate, Mk2 a closed iron hold, Mk3 a closed diamond hold.
- The drive SHALL follow the Speed count in look steps: no Speed shows plain wheels, and more Speed shows a bigger drive (for example 1 to 2, 3 to 5 and 6 or more). Each step SHALL look different from the others.
- The last Speed step SHALL show a rear spoiler: a full width steel wing over the back of the hold, reaching a little past the back, with dark iron end plates on both tips, carried by two struts near the middle that lean back from the hold into the wing. The spoiler SHALL sit above every Handling part and SHALL stay inside the rail's block.
- The Handling part SHALL follow the Handling count in look steps: no Handling shows nothing, a few show a small loading arm, many show the arm with a roller bed (for example 1 to 3 and 4 or more).
- A fitted tool SHALL be drawn where it works and MAY reach out to the sides past the body: the Harvester shows a reel across the front, the Spreader a hopper box with a spreader bar across the back, behind the Handling part. A shuttle SHALL carry at most one tool.
The front of the shuttle SHALL face the way it drives.

#### Scenario: Two tiers side by side
- **WHEN** a Mk1 and a Mk3 shuttle without upgrades are out at the same time
- **THEN** the Mk1 shows an open wooden crate and the Mk3 a closed diamond hold, both on plain wheels

#### Scenario: Speed changes the drive
- **WHEN** a Mk3 shuttle gets its first Speed upgrade and later its sixth
- **THEN** its drive changes from plain wheels to the first drive step and later to the last one

#### Scenario: Harvester at work
- **WHEN** a harvester drives along a farm rail
- **THEN** a reel is drawn across its front, reaching out past both sides of the body

#### Scenario: Spreader at work
- **WHEN** a Spreader shuttle drives along a farm rail
- **THEN** a hopper box with a spreader bar is drawn at its back, the bar reaching out past both sides of the body, and nothing is drawn on its front

#### Scenario: Count inside a look step
- **WHEN** a shuttle goes from four to five Speed upgrades
- **THEN** its drive looks the same as before

#### Scenario: Full Speed shows the spoiler
- **WHEN** a Mk3 shuttle gets its sixth Speed upgrade
- **THEN** a steel wing with dark iron end plates appears over the back of its hold on two struts leaning back from the hold, and no copper exhaust stacks are drawn

#### Scenario: Spoiler over the crane
- **WHEN** a Mk3 shuttle with eight Speed and eight Handling upgrades drives past a rack
- **THEN** the spoiler is drawn above the crane without touching it, and no part of the spoiler is drawn inside the rack's block

#### Scenario: Spreader behind the crane
- **WHEN** a Mk3 Spreader shuttle with eight Speed and eight Handling upgrades is drawn
- **THEN** the hopper box sits behind the loading mast and below the spoiler, and no two of these parts flicker against each other

### Requirement: The item looks like the shuttle
A shuttle item SHALL show the same composed look as the shuttle on the rails, in inventories, the garage GUI, the Shuttle Workshop and when held or dropped. Installing or removing an upgrade or a tool, or a tier build, SHALL change the item's look at once.

#### Scenario: Installing a tool
- **WHEN** a player installs a Harvester into a Mk1 shuttle in the workshop
- **THEN** the shuttle in the workshop slot shows the reel on its front

### Requirement: Shuttle size
A shuttle SHALL be drawn a little below block scale (example 0.8, so a body of about 10 pixels), bigger than the first carts, with its wheels on the rail top, and SHALL stay inside the block of the rail it stands on, so it does not reach into a rack or a station beside the rail. Only a tool MAY reach past the body to the sides. A shuttle standing on a lift car SHALL fit on the car. Its wheels or tracks SHALL stand on the two rail heads, on a rail and on a lift car, and its wheels SHALL show from the side below the body, so the shuttle does not look like a box hovering over the rail.

#### Scenario: Passing a rack
- **WHEN** a Mk3 shuttle drives along the rail in front of a rack
- **THEN** no part of the shuttle is drawn inside the rack's block

#### Scenario: Wheels on the heads
- **WHEN** a shuttle drives along a straight rail or rides a lift car
- **THEN** its wheels touch the tops of the rail heads and do not sink into them

#### Scenario: Wheels from the side
- **WHEN** a player looks from the side at a shuttle without Speed upgrades standing on a rail
- **THEN** its wheels show between the body and the rail heads, each with a hub

### Requirement: Look steps are pure logic
Which look step a Speed or Handling count shows SHALL be decided by pure logic without Minecraft types, testable with JUnit.

#### Scenario: Step boundaries
- **WHEN** the look step is asked for Speed counts 0, 1, 2, 3, 5, 6 and 8
- **THEN** the answers are steps 0, 1, 1, 2, 2, 3 and 3
