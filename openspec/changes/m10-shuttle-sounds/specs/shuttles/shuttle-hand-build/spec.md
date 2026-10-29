# Spec Delta

## MODIFIED Requirements

### Requirement: Build by hand
While every supply of the build sheet is in the workshop, the GUI SHALL offer one Build button. Build SHALL start the build by hand: it shows a large view of the shuttle and runs the sheet's steps one after another, with the step's number, the step count and what to do. While the build runs, the Build button SHALL read Skip. When the last step is done the GUI SHALL ask for a name.

#### Scenario: Start a build by hand
- **WHEN** a Mk1 shuttle and every supply of the Mk2 sheet are in the workshop and the player presses Build
- **THEN** the GUI shows the shuttle large with step 1 of the sheet's steps and the button reads Skip

#### Scenario: Supplies missing
- **WHEN** one supply of the sheet is short
- **THEN** Build is off

#### Scenario: Last step done
- **WHEN** the player finishes the last step
- **THEN** the GUI shows a name field and an Engrave button

### Requirement: The server owns the build
The server SHALL keep which step a build by hand is on and SHALL only accept the step that is next; a report for any other step SHALL be ignored. While a build by hand runs, the shuttle and supply slots SHALL be locked, the install slot SHALL take nothing and the remove buttons SHALL be off. Cancel SHALL end the build with nothing used and the slots free again.

#### Scenario: Step out of order
- **WHEN** the client reports step 3 while the build is on step 1
- **THEN** the build stays on step 1

#### Scenario: Locked during the build
- **WHEN** a build by hand runs and the player tries to take a supply out
- **THEN** the supply stays in the workshop

#### Scenario: Cancel
- **WHEN** the player presses Cancel on step 2
- **THEN** the shuttle stays at its tier, every supply is still there and the slots are free

## ADDED Requirements

### Requirement: Engraving the name plate
Engrave SHALL finish the build, using exactly the sheet's supplies and keeping the shuttle's counts, tool, Muffler and anvil name, and SHALL put the typed name on the new shuttle as its name plate. A name SHALL be at most 20 characters, without formatting codes. An empty name SHALL finish the build and keep the shuttle's old plate. Skip SHALL keep the old plate too.

#### Scenario: Engrave
- **WHEN** a player finishes a Mk2 build by hand and engraves "Berta"
- **THEN** the workshop holds a Mk2 shuttle with the plate "Berta", the Mk1's upgrades and tool, and the supplies are used

#### Scenario: Skip keeps the plate
- **WHEN** a Mk2 shuttle with the plate "Berta" is built into a Mk3 and the player presses Skip
- **THEN** the Mk3 shuttle still carries the plate "Berta"

#### Scenario: A new name replaces the old
- **WHEN** a Mk2 shuttle with the plate "Berta" is built by hand into a Mk3 and engraved "Kurt"
- **THEN** the Mk3 shuttle carries the plate "Kurt"

#### Scenario: Mk1 has no plate
- **WHEN** a player crafts a Mk1 shuttle
- **THEN** it carries no name plate, and only a tier build can give it one


### Requirement: Skip finishes the build
Skip SHALL finish a running build by hand at once on any step, including the name prompt: it uses exactly the sheet's supplies, turns the shuttle into the next tier keeping its counts, tool, Muffler, anvil name and old name plate, and ends the build without asking for a name. The server SHALL accept Skip only while a build by hand runs and every supply is still there.

#### Scenario: Skip on the first step
- **WHEN** a player presses Build on a Mk1 shuttle with every Mk2 supply and then presses Skip on step 1
- **THEN** the workshop holds a Mk2 shuttle with the Mk1's counts and tool, the supplies are used and no name field is shown

#### Scenario: Skip at the name prompt
- **WHEN** the last step is done, the name field shows and the player presses Skip
- **THEN** the build finishes and the shuttle keeps its old plate

#### Scenario: No build, no skip
- **WHEN** the client asks to skip while no build by hand runs
- **THEN** nothing changes

## REMOVED Requirements

### Requirement: Engraving a name plate
**Reason**: Build now is gone; Skip takes its place and keeps the old plate the same way.
**Migration**: Replaced by "Engraving the name plate", which names Skip instead of Build now.
