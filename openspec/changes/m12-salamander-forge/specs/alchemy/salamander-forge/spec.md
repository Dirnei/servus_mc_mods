## Purpose

The Salamander Forge is Alchemy's furnace of the eternal flame: built with Ignis Perpetuus, it smelts like a vanilla furnace and never needs fuel.

## ADDED Requirements

### Requirement: The Forge smelts without fuel
The Salamander Forge SHALL smelt every smelting recipe (`minecraft:smelting`) that takes one item, one item per work cycle, from its input slot into its output slot. It SHALL have no fuel slot and SHALL never consume fuel. The time of a work cycle SHALL be the `work_ticks` of `servus_alchemy:salamander_forge` in the machine stats data (example 200 ticks, the pace of a vanilla furnace). It SHALL stop and keep its progress at zero while the input is empty or the output cannot take the result.

#### Scenario: Iron without fuel
- **WHEN** 2 Raw Iron lie in the input slot of a Forge and nothing else is in it
- **THEN** after two work cycles the output slot holds 2 Iron Ingots and the input slot is empty

#### Scenario: Output full
- **WHEN** the output slot holds 64 Iron Ingots and Raw Iron lies in the input
- **THEN** the Raw Iron stays and the progress does not advance

#### Scenario: Retuned speed
- **WHEN** a datapack sets `work_ticks` of `servus_alchemy:salamander_forge` to 100 and the server reloads
- **THEN** the Forge finishes an item every 100 ticks

#### Scenario: Not smeltable
- **WHEN** a player or a hopper offers Cobblestone Slabs to the input slot
- **THEN** they are refused

### Requirement: The eternal flame is always lit
The Forge SHALL be lit at all times, working or idle, show a burning fire in its mouth and give off light at level 13. Its screen SHALL show the flame as always full.

#### Scenario: Idle forge glows
- **WHEN** a Forge stands empty in a dark cave
- **THEN** it is lit, its fire burns and the blocks around it are lit

### Requirement: The Forge is a Foundation machine
The Forge SHALL follow the Foundation machine rules: it faces the player when placed, opens its screen on right click (input slot, flame, progress arrow, output slot, player inventory and the side panel), exposes its items to automation on every side but the closed front with the side modes of the side configuration, accepts insertion only into the input and extraction only from the output, pushes its output into adjacent inventories through sides that push, is configured and dismantled with the carrot, and drops itself and its contents when broken.

#### Scenario: Hopper above, chest below
- **WHEN** a hopper above a Forge holds Raw Iron and a chest stands below it
- **THEN** the Raw Iron goes into the input, and the Iron Ingots end up in the chest

#### Scenario: Pipe feeds the Forge
- **WHEN** a Utility Duct with an Item Pipe pulls Raw Iron out of a chest and delivers into the side of a Forge
- **THEN** the Forge smelts it

#### Scenario: Side set to Off
- **WHEN** the side of the Forge facing a hopper is set to Off
- **THEN** nothing goes in from that hopper

#### Scenario: Front closed
- **WHEN** automation offers Raw Iron to the front of a Forge
- **THEN** nothing goes in

### Requirement: The Forge is a producer
The Forge SHALL expose the Foundation production capability with one recipe per item a single item smelting recipe accepts: that one item in, the result out, its work time as the recipe time. It SHALL report the recipe of the item in its input slot as active. It SHALL keep no upkeep. On a data line it SHALL be a producer member.

#### Scenario: Smelting recipes on offer
- **WHEN** a mod asks the production capability of a Forge
- **THEN** one of its recipes makes one Iron Ingot from one Raw Iron in the Forge's work time

#### Scenario: Active recipe
- **WHEN** the Forge holds Raw Iron in its input slot
- **THEN** it reports the Raw Iron recipe as active

#### Scenario: On a data line
- **WHEN** a duct with the data line touches a Forge
- **THEN** the Forge is a producer member of the data network and no upkeep member

### Requirement: Making the Forge
The Forge SHALL be crafted at a crafting table from one Ignis Perpetuus in the middle, a Furnace below it and seven Deepslate Bricks around them. The Workshop of the journal SHALL describe the Forge with its crafting recipe in an entry that opens with the Wandering years chapter. JEI SHALL list the Forge as a station for smelting recipes.

#### Scenario: Crafting
- **WHEN** a player lays the shaped recipe in a crafting table
- **THEN** the result is one Salamander Forge

#### Scenario: Workshop entry
- **WHEN** a player has opened the Wandering years chapter
- **THEN** the Workshop shows "The Salamander Forge" beside "The Salamander"
