## MODIFIED Requirements

### Requirement: A shuttle is drawn from its parts
A shuttle SHALL be drawn as a body with a hold, drive parts, a Handling part and a tool, chosen from the shuttle item:
- The body and hold SHALL follow the tier: Mk1 an open wooden crate, Mk2 a closed iron hold, Mk3 a closed diamond hold.
- The drive SHALL follow the Speed count in look steps: no Speed shows plain wheels, and more Speed shows a bigger drive (for example 1 to 2, 3 to 5 and 6 or more). Each step SHALL look different from the others.
- The last Speed step SHALL show a rear spoiler: a full width steel wing over the back of the hold, reaching a little past the back, with dark iron end plates on both tips, carried by two struts near the middle that lean back from the hold into the wing. The spoiler SHALL sit above every Handling part and SHALL stay inside the rail's block.
- The Handling part SHALL follow the Handling count in look steps: no Handling shows nothing, a few show a small loading arm, many show the arm with a roller bed (for example 1 to 3 and 4 or more).
- A fitted tool SHALL be drawn where it works and MAY reach out to the sides past the body: the Harvester shows a reel across the front, the Spreader a hopper box with a spreader bar across the back, behind the Handling part. A shuttle SHALL carry at most one tool.
- The Lumber module SHALL replace the hold with the Lumber hold: walls in the tier's material (oak, iron, diamond), a dark rim (oak on Mk1) and a lid of two doors, no taller than the garage doorway lets through, with nothing of the crane showing while it is folded. A Lumber shuttle SHALL show no Handling part, and its spoiler SHALL stand on its end plates without the two middle struts.
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
- **WHEN** a Mk2 Lumber shuttle with eight Handling upgrades drives along a farm rail
- **THEN** it shows a closed iron hold with a dark rim and two lid doors, no loading arm and nothing of the crane, and no two parts flicker against each other from any angle

#### Scenario: Through the garage doorway
- **WHEN** a Mk3 Lumber shuttle without Speed upgrades drives into its garage
- **THEN** no part of its hold, rim or gold corners reaches above the doorway's opening

#### Scenario: Spoiler on a Lumber shuttle
- **WHEN** a Mk3 Lumber shuttle with six Speed upgrades unfolds at a rail
- **THEN** the wing stands on its end plates without middle struts, and the back wall folds out under it
