# foundation/machine-adapter Specification

## Purpose
The Machine Adapter lets machines that do not speak the Foundation production API take part in on-demand production: placed against a machine, it reports the machine's recipes, feeds it and hands its results to the data line, and keeps its fuel stocked.

## Requirements

### Requirement: Adapter faces a machine
The Machine Adapter SHALL face the block it was placed against, in any of the six directions, and SHALL act for that block only. It SHALL find the machine's profile by the block. Without a profile, or without a block, it SHALL report no recipes and forward nothing.

#### Scenario: Placed on a furnace
- **WHEN** a player places an adapter against the side of a furnace
- **THEN** the adapter faces the furnace and uses the furnace profile

#### Scenario: No profile
- **WHEN** an adapter faces a chest
- **THEN** it reports no recipes and refuses every item

### Requirement: Machine profiles
Machine profiles SHALL be datapack JSON files under `data/<ns>/machine_profiles/`. A profile SHALL name one or more blocks, a recipe type, and for each of the six sides of the machine relative to its facing (front, back, left, right, top, bottom) a role: ingredients, upkeep, output or off; sides it leaves out are off. It MAY name a default upkeep item and level. Foundation SHALL ship profiles for the vanilla furnace (smelting), smoker (smoking) and blast furnace (blasting) with ingredients on top, output at the bottom and upkeep on the back, left and right, and coal at level 16 as default upkeep. Profiles SHALL reload with datapacks.

#### Scenario: Furnace profile
- **WHEN** the default datapack is loaded
- **THEN** a furnace's top side is ingredients, its bottom side output and its back, left and right sides upkeep

#### Scenario: Pack adds a profile
- **WHEN** a datapack adds a profile for a modded smelter with the recipe type `minecraft:smelting`
- **THEN** an adapter on that smelter reports smelting recipes after a reload

### Requirement: Adapter is a producer
The adapter SHALL be a data member with the producer role. It SHALL report one production recipe per item of each recipe of the profile's recipe type: that one item in, the recipe's result out, the recipe's cooking time. It SHALL report the recipe of the item in the machine's ingredient slot as active while the machine has one.

#### Scenario: Smelting recipes
- **WHEN** a data duct touches an adapter on a furnace
- **THEN** the adapter is a producer of the line and one of its recipes makes one stone from one cobblestone in 200 ticks

#### Scenario: Working
- **WHEN** the furnace holds cobblestone in its ingredient slot
- **THEN** the adapter reports the cobblestone recipe as active

### Requirement: Forwarding and collecting
Items inserted into the adapter, by packets or any automation, SHALL go into the machine through the side with the ingredients role. Items the adapter is waiting for as upkeep SHALL go through the upkeep side instead. The adapter SHALL offer the items of the machine's output side for extraction, so the data line can collect them, and SHALL refuse what the machine refuses.

#### Scenario: Cobblestone in
- **WHEN** packets deliver 8 cobblestone into an adapter on a furnace
- **THEN** the cobblestone is in the furnace's ingredient slot

#### Scenario: Stone out
- **WHEN** the furnace has smelted 8 stone
- **THEN** the data line can take 8 stone out of the adapter

### Requirement: Upkeep stays stocked
The adapter SHALL keep the machine's upkeep side stocked with its upkeep item at its level: whenever the machine holds less than the level and no earlier request is still on its way, it SHALL ask a supplier on its data line for the difference, independent of orders. Recipes SHALL never contain upkeep. A request that has not arrived after a while SHALL be given up so the next check asks again. The GUI SHALL show the upkeep item as a ghost slot, the level with buttons to change it, and the count in the machine; an empty ghost slot means no upkeep.

#### Scenario: Refill
- **WHEN** an adapter with upkeep coal at level 16 faces a furnace holding 4 coal and a warehouse with coal is on the line
- **THEN** the adapter asks for 12 coal and the furnace ends up with 16

#### Scenario: Order without fuel
- **WHEN** a CPU plans 8 stone through an adapter
- **THEN** the order's steps contain cobblestone only, no coal

#### Scenario: Change the upkeep
- **WHEN** the player puts charcoal into the ghost slot and lowers the level to 8
- **THEN** the adapter keeps 8 charcoal in the machine from then on

### Requirement: Adapter is obtainable
The adapter SHALL be crafted from copper ingots, a hopper, redstone and a Data Cable, mined with a pickaxe, drop itself and keep nothing when broken, and be dismantled by sneaking with the Configuration Carrot.

#### Scenario: Crafting
- **WHEN** the player crafts copper ingots around a hopper with redstone and a Data Cable
- **THEN** one Machine Adapter is the result
