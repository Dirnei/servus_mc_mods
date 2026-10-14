---
title: "Servus: Foundation"
description: The base every Servus mod requires. The Utility Duct, the Fluid Tank, facades, the Configuration Carrot, Engineer Goggles, side configuration and the production API.
---

Servus: Foundation (`servus_foundation`) is the base of the mod family. Alchemy and Storage both require it and never depend on each other. It has two halves: shared code that makes the family look and behave the same, and the transport blocks every base needs.

Foundation loads on its own, and each content mod loads with only Foundation installed.

## Items

- **Configuration Carrot**: the wrench of the family. A carrot with a metal bar and a red tip instead of leaves. Use it on a face to cycle that side's mode, sneak use it to dismantle a block and keep its configuration. On blocks with a facing and no own handling it rotates the block.
- **Engineer Goggles**: worn in the head slot like a helmet without armor. While worn, or while a Configuration Carrot is held, hidden things show up, such as side mode markers and ducts behind facades.
- Every line has its own colour on the duct and on its item: Item Pipe orange, Power Cable red, Fluid Pipe blue, and the data line white, gray or black by tier.
- **Item Pipe**: places a duct with an item line, or adds the item line to a duct that lacks it. Eight from iron ingots, glass and a chest.
- **Power Cable**: places a duct with an energy line, or adds it to a duct. Eight from copper ingots, redstone and glass.
- **Fluid Pipe**: places a duct with a fluid line, or adds it to a duct. Eight from iron ingots, glass and a bucket.
- **Data Cable**: places a duct with the data line, or adds it to a duct. Four from two copper ingots and a redstone dust. 32 items per second, shared by both directions (simplex).
- **Duplex Data Cable**: the data line with 32 items per second in each direction at the same time. Two from two Data Cables and a quartz.
- **Fast Data Cable**: the data line with 128 items per second in each direction. Two from two Duplex Data Cables, a quartz and two gold ingots.
- **Speed Upgrade**: goes into the upgrade slots of a Servus block and doubles its rate per upgrade. Redstone, gold ingots and a copper ingot.
- **Facade**: any full block without a block entity plus one nugget of any kind (iron, gold or a mod's copper nugget) gives six facades of that block. Use one on a face of a duct to cover that face with a thin panel of the block. A facade closes the face, so ducts and machines behind it do not connect; sneak with the Configuration Carrot on it to take it off.
- **Hollow Facade**: eight facades of one block around an empty centre give eight hollow facades. They cover a face like a facade, but the duct passes through the hole in the middle and keeps its connection.
- **Fluid Tank**: a glass block with a copper frame that holds one fluid. Copper ingots in the corners, glass on the top, left and right edges, and any solid block in the bottom middle, which becomes the tank's floor.
- **Machine Adapter**: makes a machine of vanilla or another mod a producer on the data line. Copper ingots around a hopper, redstone and a Data Cable over a row of planks; it shows the wood of the planks, like the Storage blocks.

All of them are listed in the Servus creative tab.

## Utility Duct

One block that carries up to four lines: Item Pipe, Power Cable, Fluid Pipe and the data line. It works with any block that offers the standard item, energy or fluid capability.

- Each line forms its own network: an item network only spans ducts with an Item Pipe, and so on.
- Every side of every line has a mode: in and out (default), pull in, push out or disabled. A disabled side does not connect.
- Every side of every line sits on one of eight channels, each with a fixed color. Items, energy and fluids only move between sides on the same channel.
- **Items** inserted into a duct go round robin to outputs on the same channel, never back to where they came from. Pull in sides extract items from their inventory at a fixed interval.
- **Filters**: each item side has a whitelist of up to nine ghost items. An empty list passes everything.
- **Priorities**: each item side has a priority from -9 to 9. Higher priorities are filled first.
- **Energy** is shared evenly between receivers on a channel, up to a per tick limit.
- **Fluids** move between blocks with no rate limit of their own; pumps set the throughput.

### Endpoint GUI

Use the Configuration Carrot on an arm that touches an inventory or machine. The GUI has a tab per line with the mode button, the channel selector and a remove button, and for the item line also the filter slots and the priority. The carrot on the center or on an arm to another duct opens nothing, and a bare hand never does. Removing the last line removes the duct.

### Appearance

A duct grows thicker with its lines, and each line has its own color, so you can tell from a distance what a duct carries. An arm that ends at an inventory or machine carries a ring in its channel color. A duct with a facade renders as the facade block. While you wear Engineer Goggles or hold the carrot, facaded ducts within 32 blocks show as a translucent overlay. Sneak with the carrot to take the facade off.

Ducts rest on what holds them up instead of floating in the middle of the block:

- **Floor**: a duct above a full block lies on it. Ducts, bottom slabs and other partial blocks are no floor, and neither is a block the duct has an arm into.
- **Going up**: a duct with an arm up never lies down, so a run that leaves the floor, for example beside a machine it feeds, goes straight up from the middle of the block.
- **Middle across**: ducts never move toward a wall or into a corner. Runs along a floor stay in the middle across the run, and climbs run up the middle of their blocks.
- **Draping**: a duct with nothing under it takes the floor height of the neighbour ducts beside it, as long as they agree, so a run over the edge of a step or a short gap stays level. Only direct neighbours count: a long span stays in the middle between its two ends.
- **Steps**: where two connected ducts still sit at different heights, the arm between them makes a short step up or down so the run stays joined.
- **Endpoints**: an arm into an inventory or machine always ends in the middle of that face with its ring, stepping there from a resting duct.
- **Facades**: a duct with any facade stays in the middle of its block, so its arms pass the holes of hollow facades.

The outline, collision and the carrot follow the duct where it is drawn, including its steps.

### Data line

The data line is the wiring for on-demand autocrafting. It never moves anything on its own; it only links blocks that make or hold items, so later Servus blocks can find them without any setup.

- Ducts with the data line link to each other and form a data network.
- A block is a member when it can make items (a Crafting Station, an Assembler), holds stock (a Warehouse Controller) or is a link on the line without either (a Packing Station, through the `servus_foundation:data_link` capability). Other mods join by exposing the same Foundation capabilities. Chests and other plain inventories are not members.
- There are no channels, filters or priorities. In the endpoint GUI the data tab only switches a side on or off; an off side reaches no member and does not link to the next duct.
- Right click a duct with the data line with an empty hand to see how many ducts, producers and stock members its network has, and how many items per second passed this duct during the last second out of its bandwidth ("24 of 32 items/s").
- The data line shows its tier on the duct and on the cable item: white for the Data Cable, gray for the Duplex Data Cable, black for the Fast Data Cable. The duct body and the data line's ring at a member both take that colour.

### Cable tiers and bandwidth

Each tier of the data line lets a number of items per second through every duct, its bandwidth:

| Tier | Items per second | Directions |
| --- | --- | --- |
| Data Cable | 32 | simplex: both directions share the 32 |
| Duplex Data Cable | 32 | duplex: 32 each way at the same time |
| Fast Data Cable | 128 | duplex: 128 each way at the same time |

- Ducts of different tiers link into one data network.
- A delivery moves no faster than the slowest duct on its path, and no faster than the Packing Station or other transfer gate at its ends. Example: a Packing Station with two Speed Upgrades (32 per second) sends over two Fast Data Cable ducts and one Data Cable duct: it moves 32 items per second; a second Packing Station on the same Data Cable duct gains nothing until that duct is a Duplex or Fast Data Cable.
- Simplex and duplex: a Packing Station that sends 20 items per second out and receives 20 per second over the same Data Cable duct needs 40 of its 32, so both slow down to 32 together. Over a Duplex Data Cable each direction has its own 32.
- Deliveries that cross the same duct share its bandwidth; the older delivery goes first. A delivery takes the shortest path, and among equally short paths the one with the most bandwidth. There is no detour for bandwidth.
- Upgrade a duct in place: use a higher tier cable on it (no sneaking). The duct keeps its sides and connections, takes one cable from your hand and drops the old one. A lower or equal tier never replaces a duct; it places a new duct against the clicked face like any line item. Sneaking keeps the placement assist.
- A broken duct drops the cable of its tier.
- While you wear Engineer Goggles or hold the carrot, every data duct within 32 blocks shows its load over the last second through walls, as a box around the duct's center where it rests: green idle, then yellow, orange and red at its full bandwidth.

### Upgrades

Blocks with upgrade slots (the Packing Station, for now) accept only upgrade items, one per slot. Their rate is the base rate times the product of the multipliers of the upgrades they hold: a base of 8 with two Speed Upgrades of 2 makes 32. Upgrades drop when the block is broken. Other mods add better upgrades by registering an upgrade item (or tagging an item `servus_foundation:upgrades`) with its multiplier in `upgrades.json`.

### Packets

When a Servus block asks for a delivery, the data line carries the items as packets. Nothing else ever sends them.

- The data line is instant: items do not travel and distance does not matter. Each tick a delivery moves as many items as the source has, the target takes and the transfer gates on both ends allow.
- A transfer gate is a member that limits how fast items enter or leave it, through the `servus_foundation:transfer_gate` capability. The Packing Station is one: it is the border between the warehouse and the data line and converts 8 items per second in each direction. Machine to machine, machine to terminal and controller to member deliveries pass no gate and are limited only by what the target takes.
- Items wait at their source, never on the line: a move takes items out of the source only as far as the target accepts them. A stack a block hands over that does not fit is offered again every `retry_interval` ticks.
- Items go into the target's inventory on the side that touches the duct.
- When a duct is broken and no route is left, the delivery fails and what it has not moved stays at the source. A handed over stack goes back to the source, else into the target, else onto the ground. Items are never lost.
- Ops can test it with `/servus_foundation data move <from> <to> <item> <count>`.

## Machine Adapter

Place the adapter against a machine; it faces the block it was placed on. When a machine profile knows that block, the adapter joins the data line as a producer:

- It reports the machine's recipes, for a furnace every smelting recipe with its cooking time, so the Crafting CPU can plan with them.
- Packets with ingredients go into the machine's ingredient side, and the CPU takes the results out of its output side.
- **Upkeep** is kept apart from recipes: the adapter keeps an item, coal by default, at a level in the machine, 16 by default (see Upkeep below). An order for 64 stone only ever asks for cobblestone. Right click the adapter with an empty hand to change the upkeep item (ghost slot) and the level.
- Without a profile the adapter offers nothing.

Profiles live in `data/<namespace>/machine_profiles/*.json`. Foundation ships the furnace, smoker and blast furnace:

```json
{
  "blocks": ["minecraft:furnace"],
  "recipe_type": "minecraft:smelting",
  "sides": {"top": "ingredients", "bottom": "output", "back": "upkeep", "left": "upkeep", "right": "upkeep", "front": "off"},
  "upkeep": {"item": "minecraft:coal", "level": 16}
}
```

Sides are relative to the machine's facing; sides left out are off. A profile only covers recipes with a single ingredient. Modpacks can add profiles for other machines.

## Upkeep

Upkeep is a consumable a machine keeps stocked, such as its fuel. A block names its upkeep item, the level to keep and how many it holds through the `servus_foundation:upkeep` capability, and becomes an upkeep member of the data line, so the duct shows a data arm toward it. Every 40 ticks the data line looks at its upkeep members: when one holds less than its level and has no request on its way, the line asks a supplier on it (a Warehouse Controller or Packing Station, lowest position first) for the difference. What the member gains counts against the request; a request that is not complete after 1200 ticks is given up and asked again. Upkeep is never part of an order. The Machine Adapter, the Burner Pump, the Coal Generator and the Alchemy burner machines use it.

## Fluid Tank

Touching tank blocks that fill a complete rectangular box form one tank with a shared content. The lowest layer shows the floor block the tank was crafted with; sides and top are glass. Broken blocks keep their floor. A group that is not a full box works as single block tanks until it is. Buckets work on every face, and an empty hand shows what the tank holds. A broken block keeps its share of the fluid in its item.

## Machines

Foundation ships the utility machines every base needs, all crafted at a crafting table without research. They follow the same machine rules as every Servus machine: they face the player, open a GUI, push output into touching inventories and keep their configuration when dismantled with the carrot. Numbers are datapack defaults (`data/servus_foundation/machine/`).

- **Coal Generator**: burns furnace fuel for 40 FE per tick; one coal lasts 20 seconds. Pushes energy into touching consumers. A dark steel cube with a chimney on top, two blocks tall: it needs room above when placed and breaks as one; the fire window glows and the chimney smokes while it burns. Energy leaves through every face of the cube but its top, and fuel goes in through the cube's sides as set with the carrot; the chimney connects to nothing. A generator placed before it got its chimney has to be placed again. Iron, copper and a furnace.
- **Burner Pump** and **Electric Pump**: stand on an infinite water source (a source block with at least two source neighbours) and fill a 4,000 mB buffer without using the water up, 250 mB or 500 mB per second. Both are small beam engines two blocks tall: they need room above when placed, break as one, and give water, and take fuel or FE, only through the back and the two sides of the lower half, into a Fluid Pipe or a Fluid Tank. The Burner Pump rocks its beam and smokes while it burns. The Electric Pump has a motor with copper windings below and a crank wheel above, on an axle held by a bearing on the cap; a drive belt on the back runs from the motor pulley up to the crank axle, and the wheel drives the beam through a con rod while a fan spins behind its grille and a spark jumps at the motor now and then. Both splash at the well on each stroke. The Electric Pump is a Burner Pump with iron, copper and redstone; an Electric Pump placed before it became two blocks tall has to be placed again.
- **Sink** and **Chimney**: eight Sink blocks in a 2x2x2 cube, formed by clicking one side with the Configuration Carrot, void items, fluids and FE through every outer face but the front, and from a trash slot or a bucket. Chimney blocks stacked on a top corner raise the rate; from 5 blocks on it voids without limit (rates in `data/servus_foundation/sink/sink.json`).
- **Ender Anchor**: keeps a square of chunks loaded while powered: its own chunk alone, 3 by 3 with one Anchor Upgrade and 5 by 5 with two (two slots in its GUI). Four arrows in the GUI shift the square one chunk at a time as long as the anchor's own chunk stays inside; an arrow that would leave that limit is greyed out, and taking an upgrade out pulls the square back. It costs 10 FE per tick for every chunk of the square (250 FE per tick for 5 by 5), whether or not another anchor holds the same chunks, and holds 20,000 FE. The team cap counts chunks: at most 49 chunks per team (`maxLoadedChunksPerTeam` in `servus_foundation-common.toml`), counting every chunk held by the team's active anchors and woken warehouse sites once. An anchor that would go over the cap stays inactive with "team limit reached" and loads nothing; an active anchor that grows over the cap lets go of its whole square. The GUI shows the area size, the status, the team's load ("Team: 14 / 49 chunks") and a **Show range** toggle: while it is on, you (and only you) see the square's chunk borders as a purple band at the anchor's height with a purple column at each corner. The toggle is off whenever the game starts. Upgrades drop when the anchor is broken and stay in it, together with the offset, when it is dismantled with the carrot. It stays inactive when FTB Chunks is installed unless `enderAnchorRespectsFtbChunks` is off. Obsidian panels in a dark steel frame, a chunk grid on the sides and an ender eye on the front: the eye is shut while the anchor holds no chunk (no power, team limit, FTB Chunks or no owner), opens when it becomes active, blinks now and then and follows the nearest player in front of it, and purple particles drift around it while active. An ender pearl, iron, copper, redstone and two obsidian in the bottom corners.
- **Anchor Upgrade**: widens an Ender Anchor's square by one ring of chunks, and in a Storage Warehouse Controller lets the site wake. An Ender Anchor in the middle with an ender pearl above, iron ingots at the sides and redstone below. Only Anchor Upgrades fit the anchor's slots, one per slot; hoppers and ducts do not reach them.

## Side configuration

Every machine and station uses the same side setup. A side has one of these modes:

| Mode | Behavior |
|---|---|
| Auto | Insert and extract, and the block pushes its output. The default. |
| Input | Insertion only. |
| Output | Extraction only, nothing is pushed. |
| Both | Insertion and extraction. |
| Pull | Insertion, and the block pulls from the neighbor. |
| Push | Extraction, and the block pushes to the neighbor. |
| Off | Nothing. |

Sides are relative (front, back, left, right, top, bottom), so a configuration survives rotation. Blocks may support only some modes. The GUI shows a side panel left of the container, and the block draws a colored frame on every face that is not in its default mode. With the carrot or goggles, every face shows its frame and an arrow glyph.

Blocks with several tanks, such as the Calciner and the Coagulator, have a second layer for fluids. Their side panel shows two tabs above the grid, Items (a chest) and Fluids (a bucket). In the Fluids tab every side is Off, All fluids, or one of the block's tanks, drawn in that fluid's colour. A side set to a tank offers only that tank, so a pipe arm on it carries one fluid; output tanks also push into a touching tank on their sides. All fluids is the default and behaves like a block without layers. Fixed sides stay Off.

The Configuration Carrot carries a layer, Items by default. Right click the air with it to switch between Items and Fluids; its tooltip names the layer. A click on a face cycles that face in the carrot's layer, and while a carrot set to Fluids is in hand the faces show the Fluids layer in the tanks' colours.

## Shared systems

- **Network graph and manager**: ducts, tanks, rails and warehouses are connected graphs that merge and split on placement and removal. Nothing scans blocks every tick.
- **Tick mailbox**: network events are queued and handled once per server tick, in order.
- **Energy distribution**: fair sharing of FE between sources and sinks, used by the Power Cable.
- **Teams**: every player has a team id. Without a team mod it is the player's own id. Research and Ender Anchor limits are per team.
- **Production and stock API**: the capabilities `servus_foundation:production`, `servus_foundation:stock_provider` and `servus_foundation:stock_sink`, plus a pure order planner, and `servus_foundation:supplier` and `servus_foundation:upkeep` for keeping machines stocked. This is how a Storage warehouse learns what a Alchemy machine can make without the two mods knowing each other.

## Datapack values

Transfer rates live in `data/servus_foundation/duct/utility_duct.json`, the tank size in `data/servus_foundation/tank/fluid_tank.json`, the retry interval of deliveries in `data/servus_foundation/data/packets.json`, the data cable tiers in `data/servus_foundation/data/cable_tiers.json` and the upgrade multipliers in `data/servus_foundation/data/upgrades.json`. Files of other namespaces under `data/<namespace>/data/` add tiers and upgrades. Current values, which modpacks can change:

```json
{
  "items_per_tick": 16,
  "items_per_pull": 8,
  "pull_interval": 8,
  "energy_per_tick": 512
}
```

```json
{
  "capacity_per_block": 16000
}
```

```json
{
  "retry_interval": 20
}
```

```json
{
  "servus_foundation:data_cable": { "items_per_second": 32, "duplex": false },
  "servus_foundation:duplex_data_cable": { "items_per_second": 32, "duplex": true },
  "servus_foundation:fast_data_cable": { "items_per_second": 128, "duplex": true }
}
```

```json
{
  "servus_foundation:speed_upgrade": { "speed_multiplier": 2 }
}
```

## Config

The common config `config/servus_foundation-common.toml` has `wrenchRotatesBlocks` (default true), which decides whether the carrot rotates foreign blocks that have a facing.

## How it connects

- [Alchemy](/servus_mc_mods/mods/alchemy/) machines use the side panel and the carrot, report their recipes through the production capability, and use ducts as their pipes and cables.
- [Storage](/servus_mc_mods/mods/storage/) warehouses use the network graph, the stock capabilities and the order planner. Ducts can move items in and out of any storage block that exposes an item handler.
