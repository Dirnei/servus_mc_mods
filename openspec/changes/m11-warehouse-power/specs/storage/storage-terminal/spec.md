## ADDED Requirements

### Requirement: Terminal shows warehouse power
While a warehouse the terminal reaches is in the no power state, the terminal SHALL show a line in the alert colour saying the warehouse has no power and requests wait. The stock SHALL stay shown and requests SHALL still be accepted; since every item leaves the warehouse by shuttle, an open request that waits for a shuttle or a Crafting Station SHALL show the state waiting for power until the warehouse has power again. A transfer from another site whose wake ended for lack of power SHALL show waiting for power and stay cancellable.

#### Scenario: Request without power
- **WHEN** the warehouse is in the no power state and the player requests iron that sits only in a rack
- **THEN** the request is listed as waiting for power and the iron arrives after FE comes back

#### Scenario: Stock still shown
- **WHEN** the warehouse is in the no power state and the player opens the terminal
- **THEN** the grid shows the stock and the no power line is shown
