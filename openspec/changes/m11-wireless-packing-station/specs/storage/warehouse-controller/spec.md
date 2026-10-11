## ADDED Requirements

### Requirement: Controller lists the team's sites
The controller GUI SHALL list the other sites of its team in its dimension by name, each with its state: online, sleeping and able to wake, or sleeping and unable to wake, and whether it has a Wireless Packing Station of the team. A controller without a team SHALL list nothing.

#### Scenario: Two outposts
- **WHEN** the base's controller GUI is open and the team has an online outpost and a sleeping one without upgrades
- **THEN** the list shows both by name, the first online, the second sleeping and unable to wake
