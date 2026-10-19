## MODIFIED Requirements

### Requirement: Rack is obtainable and dismantled
The rack SHALL be craftable from vanilla materials, one craft yielding several racks (example 3, set by the recipe), SHALL spill its contents when broken, and SHALL keep them on the dropped item when dismantled by sneaking with the Configuration Carrot. The Configuration Carrot SHALL NOT turn a rack; its facing is set when it is placed. Every crafted rack SHALL remember the wood of the planks it was made from: the planks that appear most often, or the middle one when all three differ.

#### Scenario: Crafting
- **WHEN** the player places iron ingots and planks in the rack pattern (for example ingots in the corners and planks across the middle)
- **THEN** the result is three Heavy Duty Racks

#### Scenario: Spruce rack
- **WHEN** the player crafts the rack with three spruce planks
- **THEN** each of the crafted racks remembers spruce

#### Scenario: Majority wins
- **WHEN** the planks row is oak, spruce, oak
- **THEN** the racks remember oak

#### Scenario: Three different woods
- **WHEN** the planks row is birch, spruce, oak
- **THEN** the racks remember spruce, the middle one

#### Scenario: Dismantle
- **WHEN** a player sneak dismantles a rack with two stacks and places it again
- **THEN** the new rack holds the two stacks

#### Scenario: Carrot click
- **WHEN** a player uses the Configuration Carrot on a rack without sneaking
- **THEN** the rack keeps its facing and its joins and nothing else happens
