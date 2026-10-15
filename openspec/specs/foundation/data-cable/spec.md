# foundation/data-cable Specification

## Purpose
The data line is the shared control wiring of the Servus family: a fourth line of the Utility Duct that links producers and stock without setup, so on-demand autocrafting can find them, and any mod joins by exposing the Servus: Foundation capabilities.

## Requirements

### Requirement: Data line in the duct
The Data Cable item SHALL place a Utility Duct carrying the data line or add the data line to a clicked duct that lacks it, like the other line items. The data line of a duct SHALL link to the data line of every neighbouring duct, and ducts linked this way SHALL form one data network; a duct between two networks SHALL merge them and removing a duct or its data line SHALL split a network where it no longer connects. Ducts without the data line SHALL NOT carry it on.

#### Scenario: Straight run
- **WHEN** three ducts carrying the data line stand in a row
- **THEN** each shows a data arm toward its neighbours and all three report the same data network

#### Scenario: Split
- **WHEN** the middle duct of that row is broken
- **THEN** the two remaining ducts report two different data networks

#### Scenario: Gap in the data line
- **WHEN** three ducts in a row carry the Item Pipe but only the outer two carry the data line
- **THEN** the outer two report two different data networks

### Requirement: Members by capability
A block SHALL be a member of a data network on a side that touches a duct's data line when it exposes, on that side, the production capability (role producer), the stock provider or stock sink capability (role stock), the data link capability `servus_foundation:data_link` (role link), or the upkeep capability `servus_foundation:upkeep` (role upkeep). A block with several kinds SHALL have all their roles. The duct SHALL show a data arm toward a member. Blocks without these capabilities, such as chests, hoppers and rails, SHALL NOT be members and the duct SHALL show no data arm toward them.

#### Scenario: Crafting Station
- **WHEN** a duct with the data line touches a Crafting Station
- **THEN** the duct shows a data arm toward it and the station is a producer of that network

#### Scenario: Warehouse Controller
- **WHEN** a duct with the data line touches a Warehouse Controller
- **THEN** the controller is a stock member of that network

#### Scenario: Link member
- **WHEN** a duct with the data line touches a Packing Station
- **THEN** the duct shows a data arm toward it and the Packing Station is a link member of that network

#### Scenario: Chest
- **WHEN** a duct carrying only the data line touches a chest
- **THEN** the duct shows no arm toward it and the network has no member there

#### Scenario: Upkeep member
- **WHEN** a duct with the data line touches a Coal Generator
- **THEN** the duct shows a data arm toward it and the generator is an upkeep member of that network

### Requirement: Data sides on and off
Every side of a duct's data line SHALL be on or off, on by default, and SHALL have no channel, filter or priority. A side that is off SHALL reach no member and SHALL NOT link to the data line of the duct on that side.

#### Scenario: Side off toward a member
- **WHEN** the data side of a duct toward a Warehouse Controller is turned off
- **THEN** the controller is no longer a member and the data arm toward it disappears

#### Scenario: Side off toward a duct
- **WHEN** the data side between two data ducts is turned off on one of them
- **THEN** the two ducts report two different data networks

### Requirement: Members follow the world
The member list SHALL change when a member is placed next to a data duct, broken, or its chunk unloads, and when a data duct is placed or broken next to it, without scanning blocks every tick. A block touched by several ducts of one network SHALL count as one member. A block touching two separate networks SHALL be a member of both.

#### Scenario: Member placed later
- **WHEN** a Crafting Station is placed next to an existing data duct
- **THEN** the network lists it as a producer from then on

#### Scenario: Member broken
- **WHEN** that Crafting Station is broken
- **THEN** the network no longer lists it

#### Scenario: Touched twice
- **WHEN** two data ducts of one network both touch the same Warehouse Controller
- **THEN** the network lists one stock member

### Requirement: Member query
Servus: Foundation SHALL let any mod ask, on the server, for the data network at the position of a duct with the data line or at a member position and for that network's members, each with its position and roles, filtered by role. A position that is neither a data duct nor a member SHALL have no data network.

#### Scenario: Ask from a member
- **WHEN** a mod asks for the data network at the position of a Warehouse Controller touched by a data duct
- **THEN** it gets that duct's network, and its stock members include the controller

#### Scenario: Nothing there
- **WHEN** a mod asks for the data network at a stone block
- **THEN** it gets no network

### Requirement: Data line moves nothing
The data line SHALL hold no items, energy or fluids and SHALL never move anything between its members on its own.

#### Scenario: Station and controller linked
- **WHEN** a data duct links a Warehouse Controller holding planks and a Crafting Station with the sticks recipe and nothing orders sticks
- **THEN** no planks leave the warehouse and the station's inputs stay empty

### Requirement: Network summary
Right clicking a duct that carries the data line with an empty hand SHALL show the player a one line summary in the action bar: how many ducts carry the network's data line, how many producers and how many stock members it has, and how many items per second pass this duct out of its bandwidth. Clicking with an item in hand SHALL keep that item's own use.

#### Scenario: Summary
- **WHEN** a player right clicks a duct of a data network of 5 ducts with one Crafting Station and one Warehouse Controller as members
- **THEN** the action bar says 5 cables, 1 producer, 1 stock

#### Scenario: Load in the summary
- **WHEN** 24 items per second pass a Data Cable duct of bandwidth 32 and the player right clicks it with an empty hand
- **THEN** the summary includes 24 of 32 items/s

### Requirement: Data Cable is obtainable
The Data Cable item SHALL be crafted from copper and redstone (example: two copper ingots around a redstone dust give 4) and SHALL use the duct placement assist like the other line items. A duct SHALL drop one Data Cable for its data line when broken.

#### Scenario: Crafting
- **WHEN** the player crafts a copper ingot, a redstone dust and a copper ingot in a row
- **THEN** four Data Cables are produced

#### Scenario: Broken duct
- **WHEN** a duct carrying the Item Pipe and the data line is broken
- **THEN** one Item Pipe and one Data Cable drop

### Requirement: Foundation alone
The data line SHALL work when no content mod is installed; its network then simply has no members.

#### Scenario: Only Foundation installed
- **WHEN** a game runs with only `servus_foundation` and a player places two data ducts and right clicks one with an empty hand
- **THEN** the action bar says 2 cables, 0 producers, 0 stock

### Requirement: Data carriers
Servus: Foundation SHALL let a block carry the data network to touching blocks like a piece of cable. A carrier SHALL link to every touching carrier and to every touching duct whose data side toward it is on, so touching carriers and ducts form one data network that merges and splits on placement and removal without scanning blocks every tick. A carrier SHALL be a member of its own network with the roles of its own capabilities and SHALL NOT make its other neighbours members. Carriers SHALL NOT count as cables in the summary. Packets SHALL route through carriers like through ducts.

#### Scenario: Two carriers touch
- **WHEN** two carriers that expose the data link capability touch and no duct is near
- **THEN** both report the same data network and both are link members of it

#### Scenario: Carrier beside a duct
- **WHEN** a carrier touches a duct whose data side toward it is on
- **THEN** the carrier and the duct share one network and the summary counts one cable

#### Scenario: Side off
- **WHEN** the duct's data side toward the carrier is off
- **THEN** the carrier is not part of the duct's network

#### Scenario: Neighbour of a carrier
- **WHEN** a block exposing the stock capability touches only a carrier
- **THEN** it is not a member of the carrier's network

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

### Requirement: Tier visible by colour
Every line SHALL have its own colour on the duct body and on its line item (examples: Item Pipe orange, Power Cable red, Fluid Pipe blue). The data line SHALL show its tier by colour on the duct body, on its bands of a multi line duct, on its ring at a member and on the cable item (examples: Data Cable white, Duplex Data Cable gray, Fast Data Cable black). Replacing a tier in place SHALL change the duct's colour without breaking the duct.

#### Scenario: Fast duct looks black
- **WHEN** a player uses a Fast Data Cable on a duct carrying a Data Cable data line and an Item Pipe line
- **THEN** the duct's data band turns from white to black while the item band stays orange

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
While a player is analysing (goggles worn or Data Potato held), data ducts SHALL show their load with a colour from idle to full, based on the share of their bandwidth used in the last second. Holding the Configuration Carrot alone SHALL not show the load.

#### Scenario: Busy duct
- **WHEN** a delivery uses the full bandwidth of a duct and the player wears goggles
- **THEN** that duct shows the full load colour while idle ducts show the idle colour

#### Scenario: Potato shows the load
- **WHEN** a player without goggles holds the Data Potato near the busy duct
- **THEN** that duct shows the full load colour

#### Scenario: Carrot does not
- **WHEN** a player without goggles holds only the Configuration Carrot near the busy duct
- **THEN** no load colour is drawn
