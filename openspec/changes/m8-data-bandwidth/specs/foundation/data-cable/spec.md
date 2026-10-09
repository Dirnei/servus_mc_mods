# Spec Delta

## ADDED Requirements

### Requirement: Data line tiers
The data line SHALL come in tiers, each placed by its own line item with a bandwidth in items per second and a direction mode, simplex or duplex, from datapack JSON (examples: Data Cable 32 simplex, Duplex Data Cable 32 duplex, Fast Data Cable 128 duplex). A duct SHALL carry one tier of the data line. Ducts of different tiers SHALL link into one data network.

#### Scenario: Mixed tiers
- **WHEN** a Data Cable duct touches a Fast Data Cable duct
- **THEN** both belong to the same data network

### Requirement: Bandwidth per duct
Each duct SHALL let at most its tier's bandwidth of items pass per second. A simplex duct SHALL share that bandwidth between both directions; a duplex duct SHALL allow the full bandwidth in each direction at the same time.

#### Scenario: Simplex shared
- **WHEN** two deliveries of 64 items cross the same simplex duct of bandwidth 32 in opposite directions and nothing else limits them
- **THEN** together they move about 32 items per second through it

#### Scenario: Duplex separate
- **WHEN** the same two deliveries cross a duplex duct of bandwidth 32
- **THEN** each moves about 32 items per second

### Requirement: Upgrade a cable in the world
Using a data cable item of a higher tier on a duct whose data line has a lower tier SHALL replace the line in place, keep its sides and connections, consume one item and drop the old tier's item. Using a lower or equal tier on such a duct SHALL not change it and SHALL behave like any line item on a duct that already carries the line. Sneaking SHALL keep the placement assist behaviour.

#### Scenario: Upgrade
- **WHEN** a player uses a Fast Data Cable on a duct carrying a Data Cable data line
- **THEN** the duct carries the Fast Data Cable line and one Data Cable item drops

#### Scenario: No downgrade
- **WHEN** a player uses a Data Cable on a duct carrying a Fast Data Cable line
- **THEN** the duct keeps the Fast Data Cable line

### Requirement: Higher tiers are obtainable
Every tier SHALL have a recipe from vanilla materials (examples: Duplex Data Cable from Data Cables and quartz, Fast Data Cable from Duplex Data Cables, gold and quartz) and a duct SHALL drop the item of its data line's tier when broken.

#### Scenario: Broken fast duct
- **WHEN** a duct carrying a Fast Data Cable line is broken
- **THEN** one Fast Data Cable drops

### Requirement: Load on the goggles
While a player reveals hidden things (goggles worn or Configuration Carrot held), data ducts SHALL show their load with a colour from idle to full, based on the share of their bandwidth used in the last second.

#### Scenario: Busy duct
- **WHEN** a delivery uses the full bandwidth of a duct and the player wears goggles
- **THEN** that duct shows the full load colour while idle ducts show the idle colour

## MODIFIED Requirements

### Requirement: Network summary
Right clicking a duct that carries the data line with an empty hand SHALL show the player a one line summary in the action bar: how many ducts carry the network's data line, how many producers and how many stock members it has, and how many items per second pass this duct out of its bandwidth. Clicking with an item in hand SHALL keep that item's own use.

#### Scenario: Summary
- **WHEN** a player right clicks a duct of a data network of 5 ducts with one Crafting Station and one Warehouse Controller as members
- **THEN** the action bar says 5 cables, 1 producer, 1 stock

#### Scenario: Load in the summary
- **WHEN** 24 items per second pass a Data Cable duct of bandwidth 32 and the player right clicks it with an empty hand
- **THEN** the summary includes 24 of 32 items/s
