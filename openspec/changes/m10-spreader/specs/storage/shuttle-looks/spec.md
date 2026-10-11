## MODIFIED Requirements

### Requirement: A shuttle is drawn from its parts
A shuttle SHALL be drawn as a body with a hold, drive parts, a Handling part and a tool, chosen from the shuttle item:
- The body and hold SHALL follow the tier: Mk1 an open wooden crate, Mk2 a closed iron hold, Mk3 a closed diamond hold.
- The drive SHALL follow the Speed count in look steps: no Speed shows plain wheels, and more Speed shows a bigger drive (for example 1 to 2, 3 to 5 and 6 or more). Each step SHALL look different from the others.
- The Handling part SHALL follow the Handling count in look steps: no Handling shows nothing, a few show a small loading arm, many show the arm with a roller bed (for example 1 to 3 and 4 or more).
- A fitted tool SHALL be drawn on the front of the shuttle and MAY reach out to the sides past the body; the Harvester shows a reel across the front, the Spreader a hopper box with a spreader bar across the front.
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
- **THEN** a hopper box with a spreader bar is drawn on its front, the bar reaching out past both sides of the body

#### Scenario: Count inside a look step
- **WHEN** a shuttle goes from four to five Speed upgrades
- **THEN** its drive looks the same as before
