# alchemy/prelum Specification

## Purpose
The Prelum is the Press of the Rubedo stage and the end goal of Servus: Alchemy: built once with Dragon's Breath, it presses Caput Mortuum into Netherite Scrap.

## Requirements

### Requirement: Pressing recipes are data
Alchemy SHALL have a recipe type `servus_alchemy:pressing` with an input ingredient and count, one result and a time in ticks. Alchemy SHALL ship one pressing recipe; its numbers are examples a datapack may change: 16 Caput Mortuum into 1 Netherite Scrap in 600 ticks. Pressing recipes SHALL be sent to clients so screens and JEI see them.

#### Scenario: Shipped recipe
- **WHEN** the server has loaded its data
- **THEN** a pressing recipe takes 16 Caput Mortuum and makes 1 Netherite Scrap

#### Scenario: Retuned cost
- **WHEN** a datapack replaces the recipe with one that takes 8 Caput Mortuum in 200 ticks and the server reloads
- **THEN** the Prelum makes one Netherite Scrap from 8 Caput Mortuum every 200 ticks

### Requirement: The Prelum presses
The Prelum SHALL run the pressing recipe whose ingredient matches its input slot once the slot holds the recipe's count: after the recipe's time it SHALL consume that count and put the result into its output slot. It SHALL need no fuel, no energy and no principles. It SHALL stop and keep its progress at zero while no recipe has enough input or the output cannot take the result. Its input slot SHALL take only items some pressing recipe accepts.

#### Scenario: Caput Mortuum to Netherite Scrap
- **WHEN** 32 Caput Mortuum lie in the input slot of a Prelum
- **THEN** after two pressings the output slot holds 2 Netherite Scrap and the input slot is empty

#### Scenario: Not enough
- **WHEN** 15 Caput Mortuum lie in the input slot
- **THEN** the Prelum does not press and its progress stays at zero

#### Scenario: Other items refused
- **WHEN** a player or a hopper offers Cobblestone to the input slot
- **THEN** it is refused

#### Scenario: Output full
- **WHEN** the output slot holds 64 Netherite Scrap and 16 Caput Mortuum lie in the input
- **THEN** the Caput Mortuum stays and the progress does not advance

### Requirement: The Prelum is a Foundation machine
The Prelum SHALL follow the Foundation machine rules: it faces the player when placed, opens its screen on right click (input slot, progress arrow, output slot, player inventory and the side panel), exposes its items to automation on every side but the closed front with the side modes of the side configuration, accepts insertion only into the input and extraction only from the output, pushes its output into adjacent inventories through sides that push, is configured and dismantled with the carrot and drops itself and its contents when broken.

#### Scenario: Hopper above, chest below
- **WHEN** a hopper above a Prelum holds 16 Caput Mortuum and a chest stands below it
- **THEN** the Caput Mortuum goes into the input, and the Netherite Scrap ends up in the chest

#### Scenario: Output through the capability
- **WHEN** a mod extracts from the item capability of a Prelum's side while the output slot holds Netherite Scrap
- **THEN** it gets the Netherite Scrap, and it gets nothing from the input slot

#### Scenario: Front closed
- **WHEN** automation offers Caput Mortuum to the front of a Prelum
- **THEN** nothing goes in

### Requirement: The Prelum is a producer
The Prelum SHALL expose the Foundation production capability with one recipe per pressing recipe: the recipe's count of its first accepted item in, the result out, the recipe's time. It SHALL report the recipe that matches its input slot as active. It SHALL keep no upkeep. On a data line it SHALL be a producer member.

#### Scenario: Netherite Scrap on offer
- **WHEN** a mod asks the production capability of a Prelum
- **THEN** one of its recipes makes one Netherite Scrap from 16 Caput Mortuum in 600 ticks

#### Scenario: Active recipe
- **WHEN** the Prelum holds Caput Mortuum in its input slot
- **THEN** it reports the Caput Mortuum recipe as active

#### Scenario: On a data line
- **WHEN** a duct with the data line touches a Prelum
- **THEN** the Prelum is a producer member of the data network and no upkeep member

### Requirement: The look of the Prelum
The Prelum SHALL look like a heavy screw press: a polished deepslate base with a steel press bed, two dark oak posts with a dark oak head beam, and a steel screw through the beam with a turning bar on top and a steel press plate below it. While it presses, the bar SHALL turn and the screw and the plate SHALL go down onto the bed and back up; idle, the plate SHALL rest at the top. Moving and metal parts SHALL use the family's steel texture on every face. No two faces SHALL share a plane where they overlap.

#### Scenario: Pressing
- **WHEN** a Prelum presses Caput Mortuum
- **THEN** its bar turns and its plate moves down and up

#### Scenario: Item
- **WHEN** a player holds a Prelum
- **THEN** the item shows the frame with the screw, the bar and the plate at the top

### Requirement: Built once with Dragon's Breath
A Rubedo Athanor SHALL turn one bottle of Dragon's Breath and principles (example 500 Sulfur and 500 Mercury) into a Prelum Screw. The Prelum SHALL be crafted at a crafting table from the Prelum Screw at the top centre, a Heavy Weighted Pressure Plate in the middle, two Stripped Dark Oak Logs at its sides and three Polished Deepslate below. No other recipe SHALL make a Prelum Screw or a Prelum.

#### Scenario: Screw on a Rubedo Athanor
- **WHEN** a Dragon's Breath lies on an Athanor whose structure reaches Rubedo and its tanks hold the principles
- **THEN** the Athanor makes a Prelum Screw

#### Scenario: Not below Rubedo
- **WHEN** a Dragon's Breath lies on a Citrinitas Athanor
- **THEN** no Prelum Screw is made

#### Scenario: Crafting
- **WHEN** a player lays the shaped recipe in a crafting table
- **THEN** the result is one Prelum

### Requirement: Prelum in the journal and JEI
The Workshop of the journal SHALL describe the Prelum in an entry "The Prelum" that opens with the Basel and Salzburg chapter: the Prelum Screw craft on the Athanor, the crafting recipe and how to use it. JEI SHALL show a Pressing category with the input and its count, the result and the time, with the Prelum as its catalyst.

#### Scenario: Workshop entry
- **WHEN** a player has opened the Basel and Salzburg chapter
- **THEN** the Workshop shows "The Prelum" beside the Rubedo entries

#### Scenario: JEI
- **WHEN** a player looks up the uses of Caput Mortuum in JEI
- **THEN** the Pressing category shows 16 Caput Mortuum into 1 Netherite Scrap with the Prelum
