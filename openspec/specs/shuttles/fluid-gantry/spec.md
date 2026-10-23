# shuttles/fluid-gantry Specification

## Purpose

The Fluid Gantry is the warehouse's block for fluids: a frame over a rail with two built-in tanks, used as fluid storage, as a fluid inbound dock or as a fluid outbound dock, so that Tanker shuttles can move fluids through the warehouse.

## Requirements

### Requirement: Gantry placed over a rail
The Fluid Gantry SHALL be one item that places a frame of five block spaces around a straight Warehouse Rail: a leg two blocks tall on each side of the rail, at right angles to its run, and a beam in the space above the rail. It SHALL be placed by using the item on a straight rail (a run without a curve, crossing or dock arm across the run) when all five spaces are free and the blocks under both legs have a solid top. The gantry SHALL bring its own rail: used on the top of a block that is not a rail, it SHALL lay a straight Warehouse Rail on that block, running the way the player looks, and stand over it, when the rail space and the five spaces are free and the blocks under the rail and both legs have a solid top. A rail the gantry laid SHALL belong to it and SHALL go away with it, without dropping; a rail that was there before SHALL stay when the gantry goes. Otherwise nothing SHALL be placed and the player SHALL be told why. Breaking any part SHALL remove the whole gantry and drop one gantry item that keeps the fluid of both tanks, the locks, the mode and the Capacity Upgrades; sneaking with the Configuration Carrot on any part SHALL dismantle it the same way. Placing such an item SHALL bring all of that back.

#### Scenario: Placed on a rail
- **WHEN** a player uses a Fluid Gantry on a straight rail running north to south with free space on both sides and above
- **THEN** a leg stands east and west of the rail, two blocks tall each, and the beam fills the space above the rail

#### Scenario: Placed on the floor
- **WHEN** a player looking north uses a Fluid Gantry on the top of a stone floor with free space around
- **THEN** a straight rail running north to south lies on the floor, and the gantry stands over it with a leg east and west

#### Scenario: Own rail goes with it
- **WHEN** a gantry that laid its own rail is broken
- **THEN** the rail under its beam is gone and only the gantry item drops

#### Scenario: Existing rail stays
- **WHEN** a gantry placed on a rail that was already there is broken
- **THEN** the rail stays where it was

#### Scenario: Curve refused
- **WHEN** a player uses a Fluid Gantry on a rail curve
- **THEN** nothing is placed and the player is told the gantry needs a straight rail

#### Scenario: No room refused
- **WHEN** a block stands where the upper part of one leg would go
- **THEN** nothing is placed and the player is told there is no room

#### Scenario: Break keeps the fluid
- **WHEN** a player breaks the beam of a gantry holding 5000 mB of water in its left tank and places the dropped item on another rail
- **THEN** the whole old gantry is gone and the new one holds 5000 mB of water in its left tank with the same locks, mode and upgrades

### Requirement: Gantry joins by its rail
A Fluid Gantry SHALL be a member of the warehouse network only through the rail it stands over, and SHALL NOT join or pass on a warehouse by touching controllers, storage blocks, stations or other gantries. The rail SHALL keep its straight track look and SHALL show no dock arm toward the legs. Shuttles SHALL drive through the gantry on that rail, and the rail SHALL be the gantry's stop for shuttles. When the rail is removed the gantry SHALL stay and be offline until a rail is placed there again.

#### Scenario: Gantry over a warehouse rail
- **WHEN** a gantry is placed over a rail of an online warehouse
- **THEN** the gantry is part of that warehouse and its connection lamp is on

#### Scenario: Gantry touching the controller
- **WHEN** a gantry's leg touches a Warehouse Controller and its rail belongs to no warehouse
- **THEN** the gantry is not part of the warehouse

### Requirement: Two built-in tanks
A gantry SHALL have two tanks, one in each leg, each holding one fluid at a time up to the tank capacity: `tank_capacity` plus `capacity_per_upgrade` for every Capacity Upgrade in the gantry (in `data/<ns>/shuttles/gantry.json`, examples 16000 and 16000). A tank that holds a fluid SHALL refuse every other fluid until it is empty. The content of both tanks SHALL be kept when the world is saved.

#### Scenario: One fluid per tank
- **WHEN** a gantry tank holds water and lava is offered to it
- **THEN** it takes none

#### Scenario: Capacity with upgrades
- **WHEN** a gantry holds two Capacity Upgrades
- **THEN** each of its tanks holds up to 48000 mB

### Requirement: Fluid lock per tank
Each tank SHALL have a lock: one fluid, or Any. A tank locked to a fluid SHALL take only that fluid, also while empty. A tank on Any SHALL take any fluid while empty and then only the fluid it holds until it is empty again. A lock set to a fluid other than the one a tank holds SHALL be allowed: the tank SHALL keep its fluid, SHALL take no more of it, and the GUI SHALL show a warning line naming the tank. Locks SHALL be kept with the gantry.

#### Scenario: Locked tank refuses other fluids
- **WHEN** an empty tank is locked to Sulfur and water is offered to it
- **THEN** it takes none

#### Scenario: Any takes the first fluid
- **WHEN** an empty tank on Any is offered water and then lava
- **THEN** it takes the water and refuses the lava

#### Scenario: Lock changed while full
- **WHEN** a tank holding water is locked to Mercury
- **THEN** it keeps the water, takes no more water and the GUI warns about that tank

### Requirement: Modes decide pipe access
A gantry SHALL be in one of three modes, Storage by default, set in its GUI. In Storage mode the gantry SHALL expose no fluid handler to any side, so no pipe or other mod's block reaches its tanks. In Import mode it SHALL expose a fluid handler on every face of its legs that fills the tanks and gives nothing back. In Export mode it SHALL expose a fluid handler on every face of its legs that drains the tanks and takes nothing. Filling SHALL follow the locks: a tank locked to the fluid first, then a tank on Any holding it, then an empty tank on Any. Draining SHALL take from either tank.

#### Scenario: Storage has no pipe access
- **WHEN** a Utility Duct with a fluid line touches a leg of a Storage gantry
- **THEN** the duct finds no fluid endpoint there and the tanks stay as they are

#### Scenario: Import fills from a pipe
- **WHEN** a pump on water feeds a duct that touches a leg of an Import gantry with a tank locked to water
- **THEN** that tank fills and the gantry gives no water back into the duct

#### Scenario: Export is drained by a pipe
- **WHEN** a duct on pull touches a leg of an Export gantry holding water
- **THEN** the water flows out of the gantry and nothing flows in

### Requirement: Buckets by hand
Using a filled fluid container on any part of a gantry SHALL empty it into a tank by the fill order and the locks, in every mode. Using an empty container SHALL fill it from a tank that holds at least one container's worth, in every mode. A player in creative mode SHALL keep the container and get the filled one too, as with the Fluid Tank. Every other item and the empty hand SHALL open the GUI.

#### Scenario: Water bucket into a Storage gantry
- **WHEN** a player uses a water bucket on a Storage gantry whose left tank is locked to water
- **THEN** the left tank holds 1000 mB more and the player holds an empty bucket

#### Scenario: Bucket refused by the lock
- **WHEN** a player uses a lava bucket on a gantry whose tanks are both locked to water
- **THEN** nothing changes and the player keeps the lava bucket

### Requirement: Capacity upgrade slots on the gantry
A gantry SHALL have `upgrade_slots` capacity upgrade slots (in `gantry.json`, example 4) that grow both tanks together. Taking a Capacity Upgrade out SHALL be refused while either tank holds more than its capacity would be without it, and the GUI SHALL say why.

#### Scenario: Removal refused while full
- **WHEN** a gantry with one Capacity Upgrade holds 20000 mB in a tank and the player takes the upgrade out
- **THEN** the upgrade stays in its slot and the GUI says the content would no longer fit

### Requirement: Fluid stock of the warehouse
The tanks of every Storage gantry of a warehouse SHALL be its fluid stock: the warehouse SHALL know, per fluid, the total amount and which tanks hold it, and per tank its lock, fluid, amount and capacity. A change of a Storage gantry's tanks (by bucket, by shuttle, by an upgrade or a lock) SHALL reach the warehouse within a second, without polling every tick. Import and Export gantries SHALL NOT count as stock. `/servus_warehouse fluids <pos>` SHALL list, for the warehouse at the position, the fluid totals and every gantry with its mode, its tanks, locks, amounts and capacities.

#### Scenario: Storage tanks are stock
- **WHEN** two Storage gantries of a warehouse hold 3000 and 5000 mB of water
- **THEN** `/servus_warehouse fluids` lists 8000 mB of water and both gantries

#### Scenario: Import tank is not stock
- **WHEN** an Import gantry of the warehouse holds 4000 mB of water
- **THEN** the water is not part of the warehouse's fluid totals and the gantry is listed as Import

### Requirement: Gantry GUI
Right clicking a gantry with an empty hand or a non fluid item other than the Data Potato SHALL open a GUI with a gauge per tank showing fluid and amount against capacity, a lock button under each gauge that opens a fluid list (Any, every fluid that has a bucket, and "Lock to content"), a mode button cycling Storage, Import and Export, the capacity upgrade slots, a state line and the player inventory. The state line SHALL name the first thing that is wrong (a tank holding a fluid other than its lock, an upgrade that cannot come out) and otherwise say what the mode does. Shift clicking a Capacity Upgrade SHALL move it into the slots.

#### Scenario: Lock from the list
- **WHEN** a player opens the left lock list and picks Sulfur
- **THEN** the left tank is locked to Sulfur and its button shows Sulfur

#### Scenario: Lock to content
- **WHEN** the right tank holds Mercury on Any and the player picks "Lock to content"
- **THEN** the right tank is locked to Mercury

### Requirement: Gantry look
The legs SHALL look like glass tank columns two blocks tall in a blackened iron frame on a steel base and cap, with the tank's fluid drawn as real fluid (its still texture and tint) from the bottom of the column up to the fill share. The client SHALL learn each fill level in steps (for example 32 per column), and only when a step changes. Each leg SHALL carry a plate on both faces along the rail showing its lock: the locked fluid's texture, or a striped Any plate. The beam SHALL be a steel girder with a spout hanging over the rail and a sign on both faces along the rail showing the mode (Storage, Import or Export) beside a connection lamp and an activity lamp like the stations. Moving and metal parts SHALL use the family's steel and blackened iron textures on every face, and no two faces SHALL share a plane where they overlap.

#### Scenario: Level shows in the column
- **WHEN** a tank is filled to half
- **THEN** its leg shows fluid up to half its height

#### Scenario: Mode sign
- **WHEN** a player switches a gantry to Import
- **THEN** both signs on the beam show the Import mark

### Requirement: Gantry is obtainable
The Fluid Gantry SHALL be craftable from vanilla materials and Foundation parts (example: two Fluid Tanks, iron ingots, a copper ingot for the spout and a Warehouse Rail). The creative tab and the recipe viewer SHALL list it.

#### Scenario: Crafting
- **WHEN** a player crafts two Fluid Tanks, iron ingots, a copper ingot and a rail in the gantry pattern
- **THEN** the result is one Fluid Gantry

### Requirement: Keep level on Export tanks
Each tank of an Export gantry SHALL have a keep level, 0 by default, set in the GUI under its gauge with buttons for -1 B, +1 B, -10 B and +10 B and limited to the tank's capacity. A keep level SHALL only apply while the tank is locked to a fluid; on Any the GUI SHALL show the keep level greyed out with a hint to lock the tank. Keep levels SHALL be kept with the gantry and on its dropped item. Storage and Import gantries SHALL NOT show keep levels.

#### Scenario: Set a keep level
- **WHEN** a player sets the left tank of an Export gantry locked to water to 8 B
- **THEN** the left gauge shows a keep mark at 8000 mB

#### Scenario: Keep needs a lock
- **WHEN** the right tank of an Export gantry is on Any
- **THEN** its keep level is greyed out and the GUI says to lock the tank

### Requirement: Gantry inspect
Using the Data Potato on any part of a Fluid Gantry SHALL print a chat report: the gantry's mode, then one line per tank (left, then right) with its fluid, amount and capacity in mB, or that it is empty with its capacity, and its lock (a fluid or Any). It SHALL open no GUI and change nothing.

#### Scenario: Storage gantry report
- **WHEN** a Storage gantry with one Capacity Upgrade holds 12,000 mB of water in its left tank locked to water and an empty right tank on Any, and a player uses the potato on its beam
- **THEN** the chat shows Storage, left: water 12,000 of 32,000 mB, locked to water, and right: empty, 32,000 mB, Any

#### Scenario: Nothing opens
- **WHEN** a player uses the potato on a leg of a gantry
- **THEN** the report is printed, no GUI opens and the tanks, locks and mode are unchanged
