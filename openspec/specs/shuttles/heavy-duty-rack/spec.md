# shuttles/heavy-duty-rack Specification

## Purpose

The Heavy Duty Rack is the warehouse's mixed storage bay: a chest sized inventory that joins neighbouring bays into one continuous rack and shows what it mostly holds.

## Requirements

### Requirement: Chest sized inventory with a GUI
A Heavy Duty Rack bay SHALL hold any items in a fixed number of slots (`slots` in `data/<ns>/shuttles/rack.json`, example 27). Right clicking the front face of any bay with an empty hand or a non tool item SHALL open a chest style GUI for the whole section the bay belongs to: the slots of every bay of the section, bay after bay from left to right as seen from the front, all shown at once above the player inventory, with shift click moving stacks between them. The GUI SHALL grow with the section (example: 9 rows for a section of three bays of 27 slots) and SHALL NOT scroll. Each bay SHALL store its own part of the slots. The GUI SHALL close when a bay of its section is removed or the section changes. Clicking any other face SHALL behave like clicking a plain block, so a held block is placed against it.

#### Scenario: Open and store
- **WHEN** a player right clicks the front of a single rack and shift clicks a stack of planks from the inventory
- **THEN** the planks are in a rack slot and the GUI shows them

#### Scenario: Section GUI
- **WHEN** a player right clicks the front of the middle bay of a section of three
- **THEN** the GUI shows 81 slots in 9 rows: the left bay's slots first, then the middle bay's, then the right bay's

#### Scenario: Shift click fills the section
- **WHEN** the left bay of a section of two is full and a player shift clicks a stack into the section GUI
- **THEN** the stack lands in the right bay's slots

#### Scenario: Full rack
- **WHEN** all slots of the section hold items and a player shift clicks another stack
- **THEN** the stack stays in the player's inventory

#### Scenario: Bay broken from a section
- **WHEN** the right bay of a section of two that holds items in both bays is broken
- **THEN** only the right bay's items spill and the left bay keeps its items

#### Scenario: Section changes while open
- **WHEN** a player has the GUI of a section open and another player breaks one of its bays
- **THEN** the GUI closes

#### Scenario: Rack placed against a rack
- **WHEN** a player holding a rack right clicks the side of a rack
- **THEN** a new rack is placed beside it and no GUI opens

#### Scenario: Front with a rack in hand
- **WHEN** a player holding a rack right clicks the front of a rack
- **THEN** the GUI opens and nothing is placed

### Requirement: Automation access
A rack bay SHALL expose an item handler on every side, covering the slots that bay stores and not the rest of its section, while its section is standalone: no Warehouse Rail or Farm Rail stands in front of any bay of the section. Opening the section GUI by hand SHALL work whether or not the section is standalone.

#### Scenario: Hopper into a rack
- **WHEN** a hopper feeds cobblestone into a rack with no rail in front of it
- **THEN** the cobblestone appears in that bay's slots and in the section GUI

#### Scenario: Hand GUI on a docked rack
- **WHEN** a player right clicks the front of a rack whose section has a front rail of an online warehouse
- **THEN** the section GUI opens and the player can take and store items by hand

### Requirement: Bays connect visually
Racks with the same facing and the same wood that stand in a row SHALL form sections of up to `bays_per_section` bays (`rack.json`, example 3). A bay joins the section beside it on placement only while that section has room; otherwise it starts a new section. Joins SHALL be decided when a bay is placed and SHALL NOT move when further bays are placed anywhere. A bay placed on top of a row SHALL copy the section boundaries of the row below wherever a rack stands below it or below its neighbour. Within a section the shared posts disappear and the floor board, the middle shelf and the beams run through; posts stand at section ends. Stacked racks SHALL keep their top and bottom beams, so an iron band marks where two bays meet; the posts run through. A rack placed directly on top of another rack SHALL take that rack's facing, whatever its wood. Racks of different woods SHALL NOT join. Bays joined side by side SHALL share one inventory (see the GUI requirement); stacked bays SHALL keep separate inventories. The frame SHALL render without flickering faces.

#### Scenario: Row of three
- **WHEN** three racks facing the same way are placed one after another in a row
- **THEN** only the two outer edges show posts and the shelves and beams run along all three

#### Scenario: Row of five
- **WHEN** five racks are placed one after another from left to right
- **THEN** the first three form a section, the fourth and fifth form a second section, and a post stands between the third and the fourth

#### Scenario: Fourth bay stands alone
- **WHEN** a fourth rack is placed at either end of a row of three
- **THEN** the three keep their joins and the fourth shows posts on both sides

#### Scenario: Bay removed from a row
- **WHEN** the middle bay of a section of three is broken
- **THEN** the two remaining bays each show posts towards the gap and nothing else changes

#### Scenario: Shifted upper row
- **WHEN** a row of three and a row of two stand on the floor and a second row is placed on top, shifted by one bay
- **THEN** the upper row splits above the same post as the lower row

#### Scenario: Different facing
- **WHEN** two racks stand side by side but face different directions
- **THEN** each renders as its own bay

#### Scenario: Shared inventory
- **WHEN** a player fills the left bay of a joined section of two
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
A rack SHALL show the item types it holds the most of, up to a display count (`display_count` in `rack.json`, example 4), as item models standing on its shelf boards inside the frame, two per shelf level, without counts, updated within a second of a change. Ties SHALL be broken by slot order. An empty rack SHALL show nothing.

#### Scenario: Five types, four shown
- **WHEN** a rack holds 64 cobblestone, 32 dirt, 16 sand, 8 gravel and 1 apple
- **THEN** the shelves show cobblestone, dirt, sand and gravel

#### Scenario: Counts change
- **WHEN** 100 apples are added to that rack
- **THEN** within a second the shelves show apples instead of gravel

#### Scenario: Standing on the shelf
- **WHEN** a rack holds cobblestone and an iron ingot
- **THEN** a small cobblestone cube and an upright ingot stand on the lower shelf between the posts, visible from the aisle and from the sides

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
- **WHEN** a row of three bays is joined into one section
- **THEN** the two outer sides of the section are climbable and the inner joins, being covered by the neighbouring bays, offer nothing to climb

#### Scenario: Mob at a rack
- **WHEN** a zombie walks against the side of a rack
- **THEN** the zombie stays on the ground

### Requirement: Boards show the crafting wood
A rack's boards (back panel, floor and shelves) SHALL show the texture of the planks it remembers, for any block in the planks tag including modded woods. A rack that remembers no wood SHALL show oak. The wood SHALL stay with the rack when it is placed, broken or dismantled, and SHALL show on the item in hand and in inventories, in the item tooltip and in the breaking particles.

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
A rack SHALL be a stock holding node of the warehouse network that joins only through a rail in front of a bay of its section: a section belongs to the network of a rail standing in front of any of its bays, and the bays of one section belong together. A rack SHALL NOT join through touching the controller, stations, garages, elevators or racks of another section, on any side, and SHALL pass no connection on. Neither a rack nor any other member SHALL pull neighbouring inventories into the warehouse. Rails dock onto a rack's front only. A rack SHALL be picked from and filled only through a shuttle parked on a front rail of its section. A rack SHALL NOT carry a connection lamp.

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
- **WHEN** a section of three bays holds bread in its right bay and a rail of an online warehouse is placed in front of its left bay
- **THEN** the terminal lists the bread, and breaking that rail removes it from the list again

#### Scenario: Chest behind a rack
- **WHEN** a chest holding apples touches only the back of a rack that has a front rail of an online warehouse
- **THEN** the terminal does not list the apples

#### Scenario: Rack by shuttle
- **WHEN** a rack stands with its front on a rail and a garage with one shuttle is on that rail
- **THEN** a request for its bread sends the shuttle to the rack's front and the bread lands in the terminal's output slots

#### Scenario: Section served from one rail
- **WHEN** a section of three bays has a rail only in front of its left bay and the right bay holds bread
- **THEN** a request for bread sends the shuttle to the rail in front of the left bay and the bread lands in the terminal's output slots

#### Scenario: Neighbouring section not joined
- **WHEN** a lone rack stands beside a section with a front rail, does not join it, holds bread and has no front rail of its own
- **THEN** the terminal does not list that bread

### Requirement: Docked sections are shuttles only
While a rail stands in front of any bay of a section, every bay of that section SHALL expose no item handler, so hoppers and item pipes neither insert nor extract and pipes show no item arm toward it. Placing or breaking a rail in front of a bay, and a bay joining or leaving the section, SHALL update every bay of the section at once, without polling. Whether the rail belongs to an online warehouse SHALL NOT matter.

#### Scenario: Rail docks and closes the rack
- **WHEN** a hopper feeds cobblestone into a standalone rack and a rail is then placed in front of the rack
- **THEN** the hopper stops inserting, and a duct with the Item Pipe beside the rack loses its item arm toward it

#### Scenario: Rail removed opens the rack
- **WHEN** the only rail in front of a rack's section is broken
- **THEN** a hopper pointing into the rack inserts again and a duct with the Item Pipe beside it shows its item arm again

#### Scenario: One docked bay closes the section
- **WHEN** a section of three bays has a rail in front of its left bay only and a hopper points into its right bay
- **THEN** the hopper inserts nothing into the right bay

#### Scenario: Joining a docked section
- **WHEN** a standalone rack fed by a hopper is joined by a new bay placed beside it that has a rail in front of it
- **THEN** the hopper stops inserting into the first rack

#### Scenario: Rail without a controller
- **WHEN** a rail that belongs to no controller is placed in front of a rack fed by a hopper
- **THEN** the hopper stops inserting
