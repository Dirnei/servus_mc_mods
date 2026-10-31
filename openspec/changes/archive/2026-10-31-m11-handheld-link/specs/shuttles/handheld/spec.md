## ADDED Requirements

### Requirement: REQUEST from STOCK
The STOCK screen SHALL offer REQUEST while its basket holds an entry and the handheld is bound. REQUEST SHALL open the locker picker over the screen. ORDER in the picker SHALL place the basket as one order to the chosen Parcel Locker and empty the STOCK basket; the action bar SHALL name the locker. CANCEL SHALL close the picker and keep the basket.

#### Scenario: Order sent
- **WHEN** a bound player with 128 cobblestone and 64 bread in the STOCK basket presses REQUEST and then ORDER
- **THEN** the order goes to the preselected locker, the STOCK basket is empty and the action bar names the locker

#### Scenario: Cancel keeps the basket
- **WHEN** the player presses REQUEST and then CANCEL
- **THEN** no order is placed and the basket is unchanged

### Requirement: Locker picker
The locker picker SHALL list the bound warehouse's Parcel Lockers sorted by distance from the player, nearest first and preselected, each with its label, distance in blocks and free boxes. A locker that cannot take the whole order SHALL be greyed out and not selectable. Without a locker that can take it, ORDER SHALL be inactive and the picker SHALL say why.

#### Scenario: Nearest first
- **WHEN** the warehouse has Parcel Locker #1 at 48 blocks and #2 at 12 blocks from the player
- **THEN** #2 is listed first and selected

#### Scenario: Full locker
- **WHEN** Parcel Locker #2 has no room for the order and #1 has
- **THEN** #2 is greyed out and #1 is selected

#### Scenario: No locker
- **WHEN** the bound warehouse has no Parcel Locker
- **THEN** the picker says the warehouse has no Parcel Locker and ORDER is inactive

### Requirement: Order range
The Handheld HH-1 SHALL send an order only while the player stands within the bound warehouse's site area, widened by a margin from data (example 64 blocks), in the same dimension. Outside, the picker SHALL say the warehouse is out of range and ORDER SHALL be inactive.

#### Scenario: Too far
- **WHEN** a player 300 blocks outside the site presses REQUEST
- **THEN** the picker says the warehouse is out of range and nothing can be sent

### Requirement: Order stock is checked when sent
ORDER SHALL order at most what the warehouse holds unlocked of each entry at that moment; entries the warehouse no longer holds SHALL be named as short in the action bar, and an order with nothing left SHALL NOT be placed.

#### Scenario: Taken meanwhile
- **WHEN** the STOCK basket asks for 64 bread and another request took all but 20 since the basket was built
- **THEN** 20 bread are ordered and the action bar reports 44 short

## MODIFIED Requirements

### Requirement: Warehouse basket and shelf basket
The STOCK basket (warehouse basket) SHALL be kept for requests and emptied by the player or by a sent order. The INFO basket (shelf basket) SHALL belong to the scanned shelf and SHALL be emptied when the handheld leaves that shelf: a scan of another shelf or a switch to another mode. Closing the GUI and logging out SHALL keep both.

#### Scenario: Warehouse basket survives
- **WHEN** a player builds a STOCK basket, closes the GUI, switches to ZONE and back to STOCK and opens the GUI again
- **THEN** the STOCK basket shows the same entries

#### Scenario: Shelf basket kept on close
- **WHEN** a player builds an INFO basket, closes the GUI and opens it again without scanning
- **THEN** the INFO basket shows the same entries

#### Scenario: Leaving the shelf empties it
- **WHEN** a player builds an INFO basket and scans another shelf
- **THEN** the INFO basket is empty and the STOCK basket is unchanged
