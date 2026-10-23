## MODIFIED Requirements

### Requirement: Fertilizer from stock above the reserve
The Spreader SHALL take the field's fertilizer from the head's buffer at the farm rail beside the head, never from racks: the plots that need a dose times `doses_per_plot` (in `field.json`, example 2), but never more than the buffer holds that no other job reserved and never more than its cargo has room for. It SHALL lock that fertilizer in the buffer (OUT) when it plans the fetch and take it at the head. Only supply runs take fertilizer out of the warehouse, and only above the head's reserve. With an empty buffer no run SHALL go out.

#### Scenario: Picks what the plots need
- **WHEN** two plots need a dose, `doses_per_plot` is 2 and the head's buffer holds 20 bone meal
- **THEN** the Spreader locks and takes 4 bone meal from the buffer

#### Scenario: Stock equal to the reserve
- **WHEN** a field's reserve is 10, its buffer is empty and the warehouse holds exactly 10 bone meal
- **THEN** no supply run takes bone meal, no Spreader drives out and the rack keeps its 10 bone meal

#### Scenario: Only the part above the reserve
- **WHEN** four plots need a dose, `doses_per_plot` is 2, the reserve is 5, the buffer is empty and the warehouse holds 8 bone meal
- **THEN** a supply run brings 3 bone meal into the buffer and the Spreader takes those 3

### Requirement: Leftover goes back to a rack
When no plot needs a dose any more, fertilizer the Spreader still carries SHALL go back into the head's buffer up to its level, less the room a supply run reserved; the rest SHALL go into warehouse storage like the harvest (see `shuttles/harvester`): by the put-away rule with travel cost, with IN locks on the room it fills, routed again when a rack does not take it, and parked in the garage with the error state when no rack it can reach has room. Then it SHALL go home.

#### Scenario: Leftover back in the rack
- **WHEN** a Spreader took 3 bone meal for a wheat plot one dose short of ripe
- **THEN** the plot gets one dose and the other 2 bone meal go back into the head's buffer
