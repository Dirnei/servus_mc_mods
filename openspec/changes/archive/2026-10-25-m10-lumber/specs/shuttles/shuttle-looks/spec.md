## MODIFIED Requirements

### Requirement: A shuttle is drawn from its parts
A shuttle SHALL be drawn as a body with a hold, drive parts, a Handling part and a tool, chosen from the shuttle item:
- The body and hold SHALL follow the tier: Mk1 an open wooden crate, Mk2 a closed iron hold, Mk3 a closed diamond hold.
- The drive SHALL follow the Speed count in look steps: no Speed shows plain wheels, and more Speed shows a bigger drive (for example 1 to 2, 3 to 5 and 6 or more). Each step SHALL look different from the others.
- The last Speed step SHALL show a rear spoiler: a full width steel wing over the back of the hold, reaching a little past the back, with dark iron end plates on both tips, carried by two struts near the middle that lean back from the hold into the wing. The spoiler SHALL sit above every Handling part and SHALL stay inside the rail's block.
- The Handling part SHALL follow the Handling count in look steps: no Handling shows nothing, a few show a small loading arm, many show the arm with a roller bed (for example 1 to 3 and 4 or more).
- A fitted tool SHALL be drawn where it works and MAY reach out to the sides past the body: the Harvester shows a reel across the front, the Spreader a hopper box with a spreader bar across the back, behind the Handling part, and the Lumber module a steel turret at the front of the hold with a folded two part steel arm lying along the top and a felling head at its end, a saw disc between two grip claws. A shuttle SHALL carry at most one tool.
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

#### Scenario: Lumber module folded
- **WHEN** a Mk3 Lumber shuttle with eight Speed and eight Handling upgrades drives along a farm rail
- **THEN** the turret, the folded arm and the felling head are drawn in steel on every face, below the spoiler and clear of the loading mast, and no two parts flicker against each other from any angle
