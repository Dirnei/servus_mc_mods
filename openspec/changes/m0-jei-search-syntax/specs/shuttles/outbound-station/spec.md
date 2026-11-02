# Spec Delta

## MODIFIED Requirements

### Requirement: Picker search
A search box SHALL filter both lists of the picker with Foundation's search syntax as the player types. A plain word SHALL match the item name or the station label. A `#` term SHALL match the station label as well as the tooltip, so a station number still finds its offers. The prefixes SHALL match the item by mod, tag, tooltip, creative tab and id.

#### Scenario: Search
- **WHEN** the picker lists rotten flesh, wheat seeds and kelp at Smoker Line (#1) and the player types "smoker"
- **THEN** only kelp is listed

#### Scenario: Search by station number
- **WHEN** the picker lists rotten flesh at Kitchen (#2) and kelp at Smoker Line (#1) and the player types `#1`
- **THEN** only kelp is listed

#### Scenario: Search by tag
- **WHEN** the picker lists rotten flesh, wheat seeds and kelp and the player types `$seeds`
- **THEN** only wheat seeds are listed
