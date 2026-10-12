## ADDED Requirements

### Requirement: Record knows the wireless stations
A site's record SHALL also keep the teams that have a powered Wireless Packing Station in the site's warehouse and, apart from them, the teams whose stations there are all unpowered, updated while the site is online when such a station joins, leaves or its powered state changes, and kept while it sleeps.

#### Scenario: Station placed
- **WHEN** a Wireless Packing Station of a team joins an online site by rail
- **THEN** the site's record lists that team as having a Wireless Packing Station there

#### Scenario: Station loses power
- **WHEN** the only Wireless Packing Station of a team at an online site runs out of FE
- **THEN** the site's record lists that team as having only unpowered stations there, and the site stays so while it sleeps

