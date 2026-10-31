## ADDED Requirements

### Requirement: Shuttles commands live under /servus shuttles
Every Shuttles command SHALL be a branch `/servus shuttles` of the shared `/servus` root and SHALL need operator permission (level 2). A command SHALL start with its fixed words (the verb, for example `field age`) followed only by its parameters, so that every command fits the Edict catalog format. The old `/servus_warehouse` root SHALL no longer exist.

#### Scenario: Old root gone
- **WHEN** an operator types `/servus_warehouse info`
- **THEN** the command is unknown

#### Scenario: Not an operator
- **WHEN** a player without operator permission types `/servus shuttles`
- **THEN** no Shuttles command is suggested or run

### Requirement: A warehouse is named by number or here
Every parameter `<warehouse>` SHALL take the number of a warehouse (as listed by `list` and shown in its controller GUI) or `here`, which is the warehouse of the block the player looks at. An unknown number SHALL fail with a message naming the known numbers. `here` from a source that is not a player looking at a warehouse block, such as RCON or the server console, SHALL fail with a message saying to use a number. Tab completion SHALL suggest the warehouse numbers with their names and `here`.

#### Scenario: By number
- **WHEN** an operator runs `/servus shuttles info 2` and W2 is "North Mine"
- **THEN** the report is for North Mine, wherever the operator stands

#### Scenario: Here
- **WHEN** an operator looks at a rack of W1 and runs `/servus shuttles info here`
- **THEN** the report is for W1

#### Scenario: Here over RCON
- **WHEN** `/servus shuttles info here` arrives over RCON
- **THEN** the command fails saying a warehouse number is needed

### Requirement: A machine is named by kind and number or here
A machine SHALL be named by its kind (the kind id of its numbered label, such as `field_head`, `inbound_station`, `crafting_cpu` or `calciner`, also accepted with its mod namespace) followed by its number within the warehouse, or by `here` in place of the number for the machine the player looks at, whose kind must match. Commands that only work on one kind, such as `field`, SHALL take just the number. When both the warehouse and the machine are `here`, the looked at machine SHALL be used even when it has no number or belongs to no online warehouse. Tab completion SHALL suggest the kinds the warehouse has and their numbers.

#### Scenario: Numbered Field Head
- **WHEN** an operator runs `/servus shuttles field 1 2`
- **THEN** the report is for Field Head #2 of W1

#### Scenario: Unnumbered Field Head
- **WHEN** an operator looks at a Field Head whose field reaches no warehouse and runs `/servus shuttles field age here here 9000`
- **THEN** that Field Head counts 9000 mB toward aging

#### Scenario: Kind mismatch
- **WHEN** an operator looks at a Shuttle Garage and runs `/servus shuttles machine here calciner here`
- **THEN** the command fails saying the looked at machine is a Garage

#### Scenario: Missing number
- **WHEN** an operator runs `/servus shuttles field 1 7` and W1 has Field Heads #1 to #3
- **THEN** the command fails naming the Field Heads W1 has

### Requirement: Commands reach every dimension
A command naming a warehouse by number SHALL act on that warehouse in its own dimension, whatever the dimension of the command source, including RCON and the server console. A command that needs the warehouse's blocks while its chunks are not loaded SHALL fail with a message naming the warehouse and the `wake` command, and SHALL change nothing.

#### Scenario: Nether warehouse over RCON
- **WHEN** `/servus shuttles info 3` arrives over RCON and W3 stands loaded in the Nether
- **THEN** the report is for W3

#### Scenario: Unloaded warehouse
- **WHEN** an operator runs `/servus shuttles order 2 minecraft:stick 8` while W2's chunks are not loaded
- **THEN** the command fails saying W2 is not loaded and to run `/servus shuttles wake 2`, and no order is made

### Requirement: Listing warehouses and machines
`/servus shuttles list` SHALL list every warehouse of the world with its number, name, dimension and controller position, team, chunks and capacity, whether it is awake, and how many item types its last known stock holds. `/servus shuttles machines <warehouse> [kind]` SHALL list the numbered machines of the warehouse, or only those of one kind, by kind and number, with name and position.

#### Scenario: List
- **WHEN** an operator runs `/servus shuttles list` with two warehouses
- **THEN** chat lists W1 and W2 with their names and chunk counts

#### Scenario: Machines of one kind
- **WHEN** an operator runs `/servus shuttles machines 1 field_head` and W1 has Field Head #1 "Wheat" and #2
- **THEN** chat lists both with their positions and the name Wheat

### Requirement: Warehouse reports
`/servus shuttles info <warehouse>` SHALL print the online state, source count and the indexed items with counts, and for an online warehouse its name, number, chunk count and capacity. `/servus shuttles zones <warehouse>` SHALL list every zone name with its block count and the filter items and tags its blocks carry. `/servus shuttles fluids <warehouse>` SHALL list the fluid totals and every Fluid Rack shelf with its port position, zone, fluid, amount and capacity. `/servus shuttles producers <warehouse>` SHALL list every Outbound Station with the block it faces and that block's recipes, and the producers of the warehouse's data line with their recipes.

#### Scenario: Op checks a warehouse
- **WHEN** an operator runs the info command on W1 "North Mine" spanning 2 chunks with one upgrade of 9
- **THEN** chat lists each indexed item with its total and names North Mine with 2 of 9 chunks

#### Scenario: Op lists zones
- **WHEN** an operator runs the zones command on a warehouse with "Ores" (3 blocks, `c:ores`) and "Food" (1 block, bread)
- **THEN** chat lists both zones with their counts and filters

#### Scenario: Racks add up
- **WHEN** a docked Fluid Rack shelf holds 5000 mB of water and another 3000 mB
- **THEN** `/servus shuttles fluids` lists 8000 mB of water and both shelves

#### Scenario: Op lists producers
- **WHEN** an operator runs the producers command on a warehouse with one Outbound Station facing an Assembler
- **THEN** chat lists that station with the Assembler's recipes

### Requirement: Machine report
`/servus shuttles machine <warehouse> <kind> <number>` SHALL print the label, position and dimension of the machine and its current state as the Data Potato reports it.

#### Scenario: Report a Calciner
- **WHEN** an operator runs `/servus shuttles machine 1 calciner 1`
- **THEN** chat shows Calciner #1, its position and the same state lines the Data Potato shows

### Requirement: Field commands
`/servus shuttles field <warehouse> <field-head>` SHALL print, for that Field Head, its status, its field kind, rails, plots, planted and ripe counts, water level, crop, sapling or mode (2x2 included), whether a harvest is due, the number of rails in each corrosion stage, the number of dry rails and the water counted toward the next aging step. `/servus shuttles field recheck <warehouse> <field-head>` SHALL run the pass over all plots again. `/servus shuttles field age <warehouse> <field-head> <mB>` SHALL count that much water as used for aging, with the same upkeep and aging rules as water used by hydration, so a stage can be reached quickly for testing.

#### Scenario: Print a field
- **WHEN** an operator runs the field command on a head serving four rails
- **THEN** the output names four rails, the head's water level and how many rails are in each corrosion stage

#### Scenario: Age by command
- **WHEN** an operator runs the age command with `corrosion_water` mB on a head of a field of copper rails without anti-corrosion items
- **THEN** every wet rail becomes exposed

#### Scenario: Print a Trees field
- **WHEN** an operator runs the field command on a head of a 2x2 spruce field
- **THEN** the output names the Trees kind and Spruce Sapling 2x2

### Requirement: Stock commands
`/servus shuttles take <warehouse> <item> <count>` SHALL, as a tool that needs no shuttle, take up to one stack of the item straight out of the warehouse's racks and give it to the running player, or drop it at the controller when the source is not a player. `/servus shuttles feed <warehouse> <outbound> <item> <count>` SHALL ask the warehouse to deliver the items into that Outbound Station's buffer and SHALL answer with the refusal reason when it cannot. `/servus shuttles ship <warehouse> <kind> <number> <item> <count>` SHALL ship items from the warehouse to that data line member and report the refusal or the planned amount. `/servus shuttles move <warehouse> <from-kind> <from> <to-kind> <to> <item> <count>` SHALL start a data line move between two numbered members of the warehouse's data line and report the delivery id or why it failed.

#### Scenario: Op takes items
- **WHEN** an operator runs the take command for 64 cobblestone on a warehouse without a garage
- **THEN** a stack of up to 64 cobblestone lands in the operator's inventory and the index shrinks by that amount

#### Scenario: Op feeds a station
- **WHEN** an operator feeds 4 iron ingots to Outbound #1 while a rack holds 10 and a garage holds a shuttle
- **THEN** a shuttle brings 4 ingots into the station's buffer

#### Scenario: Ship planks
- **WHEN** an operator ships 16 planks to Crafting Station #1 on the data line
- **THEN** chat says 16 planks are planned and they arrive at the station

#### Scenario: Move by numbers
- **WHEN** an operator moves 8 sticks from Crafting Station #1 to Packing #1 of W1
- **THEN** chat shows the delivery id and the sticks arrive

### Requirement: Order and rule commands
`/servus shuttles orders <warehouse>` SHALL list the work orders of every CPU of the warehouse's data line with item, amount, state, reason and steps, and for every CPU its size, its slots and what its buffer holds per order. `/servus shuttles order <warehouse> <item> <count>` SHALL make an order without a request and SHALL answer with the reason when none can be made. `/servus shuttles rules <warehouse>` SHALL list the stock rules with their Make pair, cap and state. `/servus shuttles rule <warehouse> <item> <min> <max>` SHALL set the Make pair of a rule and keep its cap. `/servus shuttles cap <warehouse> <item> <amount>` SHALL set the cap and keep the Make pair. `/servus shuttles uncap <warehouse> <item>` SHALL clear the cap and remove the rule when no Make pair is left. `/servus shuttles unrule <warehouse> <item>` SHALL remove the whole rule. A rule command that would break the threshold rules SHALL fail with the reason.

#### Scenario: Op orders sticks
- **WHEN** an operator orders 8 sticks while a dock faces a Crafting Station with the sticks recipe
- **THEN** chat confirms the order and the orders command lists it with its state and steps

#### Scenario: Op orders the impossible
- **WHEN** an operator orders bread and nothing can make it
- **THEN** chat says there is no producer for bread

#### Scenario: Buffer listing
- **WHEN** an operator runs the orders command while a CPU holds 4 Oak Planks for a crafting table order
- **THEN** chat lists the CPU's buffer with 4 Oak Planks for that order

#### Scenario: Op sets a rule
- **WHEN** an operator runs the rule command for sticks 8 16 and then the rules command
- **THEN** chat lists "Stick: keep 8 to 16" with its state

#### Scenario: Op sets a cap
- **WHEN** an operator runs the cap command for cobblestone 4096 and then the rules command
- **THEN** chat lists "Cobblestone: refuse above 4096" with its state

#### Scenario: Uncap a cap only rule
- **WHEN** cobblestone has only a cap and an operator runs the uncap command for cobblestone
- **THEN** the cobblestone rule is gone

#### Scenario: Cap below the maximum
- **WHEN** sticks keep 8 to 16 and an operator runs the cap command for sticks 10
- **THEN** the command fails saying the cap must not be below the maximum

### Requirement: Wake command
`/servus shuttles wake <warehouse>` SHALL request a wake for the warehouse's site and SHALL report whether it woke or why it waits.

#### Scenario: Op wakes an outpost
- **WHEN** an operator runs the wake command for a sleeping outpost with enough upgrades
- **THEN** chat says the outpost wakes and its chunks load
