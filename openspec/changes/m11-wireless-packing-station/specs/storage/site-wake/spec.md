## ADDED Requirements

### Requirement: Record knows the wireless stations
A site's record SHALL also keep the teams that have a Wireless Packing Station in the site's warehouse, updated while the site is online when such a station joins or leaves, and kept while it sleeps.

#### Scenario: Station placed
- **WHEN** a Wireless Packing Station of a team joins an online site by rail
- **THEN** the site's record lists that team as having a Wireless Packing Station there
