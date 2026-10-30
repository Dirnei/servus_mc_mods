# shuttles/heavy-duty-rack Specification

## Purpose

The Heavy Duty Rack is the warehouse's mixed storage bay: a chest sized inventory that joins neighbouring bays into one continuous rack and shows what it mostly holds.

## Requirements

### Requirement: Chest sized inventory with a GUI
A Heavy Duty Rack bay SHALL hold any items in a fixed number of slots (`slots` in `data/<ns>/shuttles/rack.json`, example 27). Right clicking the front face of any bay with an empty hand or a non tool item SHALL open a chest style GUI for the whole shelf the bay belongs to: the slots of every bay of the shelf, bay after bay from left to right as seen from the front, all shown at once above the player inventory, with shift click moving stacks between them. The GUI SHALL grow with the shelf (example: 9 rows for a shelf of three bays of 27 slots) and SHALL NOT scroll. Each bay SHALL store its own part of the slots. The GUI SHALL close when a bay of its shelf is removed or the shelf changes. Clicking any other face SHALL behave like clicking a plain block, so a held block is placed against it.

#### Scenario: Open and store
- **WHEN** a player right clicks the front of a single rack and shift clicks a stack of planks from the inventory
- **THEN** the planks are in a rack slot and the GUI shows them

#### Scenario: Section GUI
- **WHEN** a player right clicks the front of the middle bay of a shelf of three
- **THEN** the GUI shows 81 slots in 9 rows: the left bay's slots first, then the middle bay's, then the right bay's

#### Scenario: Shift click fills the section
- **WHEN** the left bay of a shelf of two is full and a player shift clicks a stack into the shelf GUI
- **THEN** the stack lands in the right bay's slots

#### Scenario: Full rack
- **WHEN** all slots of the shelf hold items and a player shift clicks another stack
- **THEN** the stack stays in the player's inventory

#### Scenario: Bay broken from a section
- **WHEN** the right bay of a shelf of two that holds items in both bays is broken
- **THEN** only the right bay's items spill and the left bay keeps its items

#### Scenario: Section changes while open
- **WHEN** a player has the GUI of a shelf open and another player breaks one of its bays
- **THEN** the GUI closes

#### Scenario: Rack placed against a rack
- **WHEN** a player holding a rack right clicks the side of a rack
- **THEN** a new rack is placed beside it and no GUI opens

#### Scenario: Front with a rack in hand
- **WHEN** a player holding a rack right clicks the front of a rack
- **THEN** the GUI opens and nothing is placed

### Requirement: Automation access
A rack bay SHALL expose an item handler on every side, covering the slots that bay stores and not the rest of its shelf, while its shelf is standalone: no Warehouse Rail or Farm Rail stands in front of any bay of the shelf. Opening the shelf GUI by hand SHALL work whether or not the shelf is standalone.

#### Scenario: Hopper into a rack
- **WHEN** a hopper feeds cobblestone into a rack with no rail in front of it
- **THEN** the cobblestone appears in that bay's slots and in the shelf GUI

#### Scenario: Hand GUI on a docked rack
- **WHEN** a player right clicks the front of a rack whose shelf has a front rail of an online warehouse
- **THEN** the shelf GUI opens and the player can take and store items by hand

### Requirement: Bays connect visually
Racks with the same facing and the same wood that stand in a row SHALL form shelves of up to `bays_per_shelf` bays (`rack.json`, example 3). A shelf is the bays joined side by side on one level, a bay standing alone being a shelf of one; the shelves stacked over each other form the rack. A bay joins the shelf beside it on placement only while that shelf has room; otherwise it starts a new shelf. Joins SHALL be decided when a bay is placed and SHALL NOT move when further bays are placed anywhere. A bay placed on top of a row SHALL copy the shelf boundaries of the row below wherever a rack stands below it or below its neighbour. Within a shelf the shared posts disappear and the floor board, the middle board and the beams run through; posts stand at shelf ends. Stacked racks SHALL keep their top and bottom beams, so an iron band marks where two bays meet; the posts run through. A rack placed directly on top of another rack SHALL take that rack's facing, whatever its wood. Racks of different woods SHALL NOT join. Bays joined side by side SHALL share one inventory (see the GUI requirement); stacked bays SHALL keep separate inventories. The frame SHALL render without flickering faces.

#### Scenario: Row of three
- **WHEN** three racks facing the same way are placed one after another in a row
- **THEN** only the two outer edges show posts and the boards and beams run along all three

#### Scenario: Row of five
- **WHEN** five racks are placed one after another from left to right
- **THEN** the first three form a shelf, the fourth and fifth form a second shelf, and a post stands between the third and the fourth

#### Scenario: Fourth bay stands alone
- **WHEN** a fourth rack is placed at either end of a row of three
- **THEN** the three keep their joins and the fourth shows posts on both sides

#### Scenario: Bay removed from a row
- **WHEN** the middle bay of a shelf of three is broken
- **THEN** the two remaining bays each show posts towards the gap and nothing else changes

#### Scenario: Shifted upper row
- **WHEN** a row of three and a row of two stand on the floor and a second row is placed on top, shifted by one bay
- **THEN** the upper row splits above the same post as the lower row

#### Scenario: Different facing
- **WHEN** two racks stand side by side but face different directions
- **THEN** each renders as its own bay

#### Scenario: Shared inventory
- **WHEN** a player fills the left bay of a joined shelf of two
- **THEN** the right bay's GUI shows the same items

#### Scenario: Separate inventories
- **WHEN** a player fills a rack that stands on top of another rack
- **THEN** the lower rack's GUI stays empty

#### Scenario: Placed on top
- **WHEN** a player looking south places a rack on top of a rack that faces east
- **THEN** the new rack faces east

#### Scenario: Different woods
- **WHEN** a spruce rack is placed beside an oak rack with the same facing
- **THEN** each renders as its own bay with posts between them

#### Scenario: Stacked bays
- **WHEN** a rack is placed on top of another rack
- **THEN** both keep their top and bottom beams and an iron band shows where they meet

### Requirement: Sample display
A rack SHALL show the item types it holds the most of, up to a display count (`display_count` in `rack.json`, example 4), as item models standing on its floor board and middle board inside the frame, two per board, without counts, updated within a second of a change. Ties SHALL be broken by slot order. An empty rack SHALL show nothing.

#### Scenario: Five types, four shown
- **WHEN** a rack holds 64 cobblestone, 32 dirt, 16 sand, 8 gravel and 1 apple
- **THEN** the boards show cobblestone, dirt, sand and gravel

#### Scenario: Counts change
- **WHEN** 100 apples are added to that rack
- **THEN** within a second the boards show apples instead of gravel

#### Scenario: Standing on the shelf
- **WHEN** a rack holds cobblestone and an iron ingot
- **THEN** a small cobblestone cube and an upright ingot stand on the floor board between the posts, visible from the aisle and from the sides

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

### Requirement: Sides climb like a ladder
A player touching an exposed side of a rack (a face at a right angle to the rack's facing) SHALL climb it like a vanilla ladder: pushing towards the side moves the player up, hanging against it without input slides the player down slowly, sneaking against it holds the position, and the player takes no fall damage from a fall that ends against a side. The front and the back of a rack SHALL NOT be climbable. Only players climb; other entities treat the rack as a full block.

#### Scenario: Climb a stack
- **WHEN** a player standing beside the side posts of three stacked racks walks into them and keeps pushing
- **THEN** the player rises along the posts and steps onto the top rack

#### Scenario: Front and back stay walls
- **WHEN** a player pushes against the front or the back of a rack
- **THEN** the player stays on the ground and does not rise

#### Scenario: Slide down
- **WHEN** a player steps off the top of a stack, stays in contact with its side and gives no movement input
- **THEN** the player slides down slowly and lands without fall damage

#### Scenario: Hold position
- **WHEN** a player climbing a side stops pushing and sneaks
- **THEN** the player stays at the current height

#### Scenario: Only the outer posts of a section
- **WHEN** a row of three bays is joined into one shelf
- **THEN** the two outer sides of the shelf are climbable and the inner joins, being covered by the neighbouring bays, offer nothing to climb

#### Scenario: Mob at a rack
- **WHEN** a zombie walks against the side of a rack
- **THEN** the zombie stays on the ground

### Requirement: Boards show the crafting wood
A rack's boards (back panel, floor board and middle board) SHALL show the texture of the planks it remembers, for any block in the planks tag including modded woods. A rack that remembers no wood SHALL show oak. The wood SHALL stay with the rack when it is placed, broken or dismantled, and SHALL show on the item in hand and in inventories, in the item tooltip and in the breaking particles.

#### Scenario: Placed spruce rack
- **WHEN** a player places a rack crafted from spruce planks
- **THEN** its boards show the spruce planks texture

#### Scenario: Modded wood
- **WHEN** a rack is crafted from a modded planks block in the planks tag
- **THEN** its boards show that block's texture

#### Scenario: Item look
- **WHEN** the player holds a spruce rack
- **THEN** the item shows spruce boards and its tooltip names the spruce planks

#### Scenario: Wood survives breaking
- **WHEN** a spruce rack is broken and the dropped item is placed again
- **THEN** the new rack shows spruce boards

#### Scenario: No stored wood
- **WHEN** a rack is given by command without a wood
- **THEN** it shows oak boards

### Requirement: Blackened iron frame
Every rack's frame (posts, beams and edges) SHALL show a blackened wrought iron texture, whatever its wood. The recipe cost SHALL stay iron ingots and planks.

#### Scenario: Frame look
- **WHEN** the player looks at any rack
- **THEN** its posts and beams are dark iron, not orange steel

### Requirement: One rack in the creative tab and JEI
The creative tab and the recipe viewer SHALL list one Heavy Duty Rack, the oak one; the recipe viewer SHALL show the rack recipe accepting any planks.

#### Scenario: Creative tab
- **WHEN** the player opens the Servus creative tab
- **THEN** one Heavy Duty Rack with oak boards is listed

### Requirement: Racks join only through a front rail
A rack SHALL be a stock holding node of the warehouse network that joins only through a rail in front of a bay of its shelf: a shelf belongs to the network of a rail standing in front of any of its bays, and the bays of one shelf belong together. A rack SHALL NOT join through touching the controller, stations, garages, elevators or racks of another shelf, on any side, and SHALL pass no connection on. Neither a rack nor any other member SHALL pull neighbouring inventories into the warehouse. Rails dock onto a rack's front only. A rack SHALL be picked from and filled only through a shuttle parked on a front rail of its shelf. A rack SHALL NOT carry a connection lamp.

#### Scenario: Rack beside a rail
- **WHEN** a rack is placed with a rail of an online warehouse in front of it
- **THEN** the rail shows a dock towards it and the terminal lists its contents

#### Scenario: Rack touching the controller
- **WHEN** a rack holding bread touches the controller directly and has no front rail
- **THEN** the terminal does not list the bread

#### Scenario: Rail at the side
- **WHEN** a rail of an online warehouse runs along the side or the back of a rack and nothing else touches it
- **THEN** the rail shows no dock, and the rack's contents are not listed

#### Scenario: Front rail connects the section
- **WHEN** a shelf of three bays holds bread in its right bay and a rail of an online warehouse is placed in front of its left bay
- **THEN** the terminal lists the bread, and breaking that rail removes it from the list again

#### Scenario: Chest behind a rack
- **WHEN** a chest holding apples touches only the back of a rack that has a front rail of an online warehouse
- **THEN** the terminal does not list the apples

#### Scenario: Rack by shuttle
- **WHEN** a rack stands with its front on a rail and a garage with one shuttle is on that rail
- **THEN** a request for its bread sends the shuttle to the rack's front and the bread lands in the terminal's output slots

#### Scenario: Section served from one rail
- **WHEN** a shelf of three bays has a rail only in front of its left bay and the right bay holds bread
- **THEN** a request for bread sends the shuttle to the rail in front of the left bay and the bread lands in the terminal's output slots

#### Scenario: Neighbouring section not joined
- **WHEN** a lone rack stands beside a shelf with a front rail, does not join it, holds bread and has no front rail of its own
- **THEN** the terminal does not list that bread

### Requirement: Docked shelves are shuttles only
While a rail stands in front of any bay of a shelf, every bay of that shelf SHALL expose no item handler, so hoppers and item pipes neither insert nor extract and pipes show no item arm toward it. Placing or breaking a rail in front of a bay, and a bay joining or leaving the shelf, SHALL update every bay of the shelf at once, without polling. Whether the rail belongs to an online warehouse SHALL NOT matter.

#### Scenario: Rail docks and closes the rack
- **WHEN** a hopper feeds cobblestone into a standalone rack and a rail is then placed in front of the rack
- **THEN** the hopper stops inserting, and a duct with the Item Pipe beside the rack loses its item arm toward it

#### Scenario: Rail removed opens the rack
- **WHEN** the only rail in front of a rack's shelf is broken
- **THEN** a hopper pointing into the rack inserts again and a duct with the Item Pipe beside it shows its item arm again

#### Scenario: One docked bay closes the section
- **WHEN** a shelf of three bays has a rail in front of its left bay only and a hopper points into its right bay
- **THEN** the hopper inserts nothing into the right bay

#### Scenario: Joining a docked section
- **WHEN** a standalone rack fed by a hopper is joined by a new bay placed beside it that has a rail in front of it
- **THEN** the hopper stops inserting into the first rack

#### Scenario: Rail without a controller
- **WHEN** a rail that belongs to no controller is placed in front of a rack fed by a hopper
- **THEN** the hopper stops inserting

### Requirement: Rack shelf inspect
Using the Data Potato on any face of a Heavy Duty Rack SHALL print a chat report for the whole shelf the bay belongs to: the zone of the shelf (or that it has none), the number of bays, the slots in use (holding items or locked by a job) against all slots of the shelf, a full mark when no slot is free, and the items it holds most (example: the top 5) with their total counts, largest first. An empty shelf SHALL say it is empty. It SHALL open no GUI and change nothing.

#### Scenario: Section report
- **WHEN** a shelf of three bays of 27 slots in zone "Ores", holds 40 iron ore in 1 slot and 1,800 cobblestone in 29 slots, and a player uses the potato on its middle bay
- **THEN** the chat shows zone "Ores", 3 bays, 30 of 81 slots, then cobblestone 1,800 and iron ore 40

#### Scenario: Locked slot counts as used
- **WHEN** a single bay holds items in 10 slots and a put-away has locked one more empty slot
- **THEN** the slot line shows 11 of 27 slots

#### Scenario: Full section
- **WHEN** every slot of a single bay holds items and a player uses the potato on it
- **THEN** the slot line shows 27 of 27 slots and marks the shelf as full

#### Scenario: Empty and unzoned
- **WHEN** a player uses the potato on an empty bay without a zone
- **THEN** the chat shows no zone, 1 bay, 0 of 27 slots and that it is empty

#### Scenario: Nothing opens
- **WHEN** a player uses the potato on the front of a rack
- **THEN** the report is printed, no GUI opens and the rack's items are unchanged

### Requirement: Top items tally is pure
Counting the slots of a shelf in use and adding up its stacks into totals per item, picking the items held most, SHALL be plain Java without Minecraft classes and covered by unit tests.

#### Scenario: Unit tested
- **WHEN** the unit tests run
- **THEN** they cover totals over several slots of one item, the order largest first, ties in slot order, the limit on how many items are listed, and a locked empty slot counting as used

### Requirement: Rack facing on placement
A rack SHALL take its facing on placement in this order: placed by clicking any face of a Heavy Duty Rack, it SHALL take that rack's facing; placed directly on top of a Heavy Duty Rack, it SHALL take the facing below; placed while sneaking, its front SHALL face the player; otherwise it SHALL turn its front to a rail by the shared rail facing rule (see `shuttles/rail-facing`). The wood of the other rack SHALL NOT matter.

#### Scenario: Placed against a rack's side
- **WHEN** a player clicks the east side of a rack facing north with another rack
- **THEN** the new rack stands east of it and faces north

#### Scenario: Placed against a rack's front
- **WHEN** a player clicks the front of a rack facing north with a rack, so the new one stands in front of it
- **THEN** the new rack faces north as well

#### Scenario: Placed from the aisle
- **WHEN** a player standing in the aisle looks north across a rail and places a rack on the floor beyond it, with no rack to copy
- **THEN** the rack's front faces south, towards the rail

### Requirement: Shelf layer on top
Clicking the top face of a rack with a Heavy Duty Rack item, without sneaking, SHALL place a rack on top of every bay of the clicked rack's shelf when every spot above the shelf is free and the held stack holds a rack item for every bay (always in creative), using one item per bay. The new racks SHALL take the facing and the shelf boundaries of the shelf below. Otherwise one rack SHALL be placed as usual.

#### Scenario: Layer over a section of three
- **WHEN** a player holding 10 racks clicks the top of the middle bay of a three bay shelf
- **THEN** three racks stand on top of the shelf, form one shelf of three with the same facing, and the player holds 7 racks

#### Scenario: Not enough racks
- **WHEN** a player holding 2 racks clicks the top of a three bay shelf
- **THEN** one rack is placed on the clicked bay

#### Scenario: A spot is taken
- **WHEN** a block stands above one bay of the shelf
- **THEN** one rack is placed on the clicked bay

#### Scenario: Only the clicked section
- **WHEN** a row has two shelves of three side by side and the player clicks the top of the left one
- **THEN** only the left shelf gets a layer

### Requirement: Sneak extends a rack row
Sneaking while looking at a Heavy Duty Rack with a Heavy Duty Rack item SHALL place the rack at the first free spot past the end of the clicked rack's row of racks with the same facing, through the Foundation placement assist with its ghost, arrow and reach. It SHALL take the row's facing; a row that ends at a block that cannot be replaced or runs past the reach SHALL place nothing.

#### Scenario: Walk towards a clicked side
- **WHEN** a player sneaks and looks at the exposed west side face of the rack at the west end of a row facing north
- **THEN** the walk goes west and the rack lands right beside it, facing north

#### Scenario: Extend to the right
- **WHEN** a player sneaks and looks at the right half of the front of the first rack in a row of four facing north
- **THEN** the walk goes towards the half looked at, the ghost shows a rack past the fourth rack, and the click places it there facing north

#### Scenario: Row ends at a wall
- **WHEN** the spot past the end of the row is stone
- **THEN** no ghost shows and the click places nothing

#### Scenario: Unsneaked click
- **WHEN** a player clicks the side of a rack without sneaking
- **THEN** the rack is placed against the clicked face as usual

### Requirement: Sneak stacks a rack column
Sneaking while looking at the top or bottom of a Heavy Duty Rack or Fluid Rack, or near the top or bottom edge of its front or back, with a rack item of the same kind SHALL place the rack at the first free spot above or below the column of racks of that kind with the same facing, through the Foundation placement assist with its ghost, an up or down arrow and reach, taking the column's facing.

#### Scenario: Stack up from the front
- **WHEN** a player sneaks and looks near the top edge of the front of a rack in a row facing north
- **THEN** the ghost and an up arrow show a rack on top of it, and the click places it there facing north

#### Scenario: Stack up a column
- **WHEN** a player sneaks and looks at the top of the lower rack of a column of two facing north
- **THEN** the ghost and an up arrow show a rack on top of the column, and the click places it there facing north

### Requirement: Sneak up lays a layer
A sneak placement that walks up a column SHALL place a whole layer over the shelf of the column's top rack, under the same conditions as the top click layer (every spot free, an item per bay or creative); otherwise it SHALL place the one rack on the column.

#### Scenario: Layer on top of a wall
- **WHEN** a player holding ten racks sneaks and looks near the top edge of the front of a bay of a shelf of three, twice
- **THEN** a layer of three goes on top each time, rows two and three, and six racks are used
