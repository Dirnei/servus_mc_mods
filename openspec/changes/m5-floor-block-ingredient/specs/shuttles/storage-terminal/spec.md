# Spec Delta

## MODIFIED Requirements

### Requirement: JEI fills the order grid
With JEI installed, the transfer button on a vanilla crafting recipe while the Storage Terminal is open SHALL open the order grid and set its ghost cells to the stacks JEI shows for the recipe, the same way it fills the Production Terminal's recipe editor. This SHALL also work for crafting recipes that exist only in JEI, such as the per-block facade examples. For each cell it SHALL choose, among the stacks the warehouse can give out that match a stack JEI shows in that cell, the one the warehouse has the most of, with that stack's components (for example a Rail with its wood). A shown stack with components of its own (for example a Facade of oak planks) SHALL match only stacks with those components. Only when the warehouse has none of them SHALL the cell take the first stack JEI shows. A transfer that opens the order grid SHALL start it as Any when Any is possible; a transfer into the open order grid SHALL keep its mode. It SHALL take nothing from the player inventory or the warehouse. While no formed Crafting CPU and Crafting Station are on the terminal's data line the transfer button SHALL be refused with that reason. JEI's recipe view in front of the terminal SHALL NOT make the terminal forget what its line has.

#### Scenario: Transfer sticks
- **WHEN** the player presses JEI's transfer button on the stick recipe while the warehouse holds birch planks only
- **THEN** the grid shows a birch plank above a birch plank, the result shows four sticks, the mode shows Any and the inventory is unchanged

#### Scenario: Stocked item with its wood
- **WHEN** the warehouse holds 40 oak Rails and 10 spruce Rails and the player presses the transfer button on the Elevator recipe
- **THEN** the centre cell shows an oak Rail with its wood, the result shows oak Elevators and the amount panel takes the Rail from stock

#### Scenario: Nothing stocked
- **WHEN** the warehouse holds no planks and the player presses the transfer button on the stick recipe
- **THEN** each plank cell shows the first plank the recipe accepts

#### Scenario: Recipe only JEI knows
- **WHEN** the warehouse holds stone Facades and oak Facades and the player presses the transfer button on the "Hollow Facade (Bricks)" example recipe
- **THEN** the ring shows eight brick Facades, not stone or oak ones, and the result shows eight "Hollow Facade (Bricks)"

#### Scenario: No CPU
- **WHEN** the terminal's data line has no formed Crafting CPU and the player looks at the stick recipe in JEI
- **THEN** the transfer button is refused with a tooltip saying a Crafting CPU and a Crafting Station are needed

#### Scenario: Recipe view opened from the terminal
- **WHEN** a formed CPU and a Crafting Station are on the line, the player opens JEI's recipe view for sticks from the terminal and presses the transfer button
- **THEN** the button is not refused and the order grid opens with the recipe
