## MODIFIED Requirements

### Requirement: Chest sized inventory with a GUI
A Heavy Duty Rack bay SHALL hold any items in a fixed number of slots (`slots` in `data/<ns>/storage/rack.json`, example 27). Right clicking the front face of any bay with an empty hand or a non tool item SHALL open a chest style GUI for the whole section the bay belongs to: the slots of every bay of the section, bay after bay from left to right as seen from the front, all shown at once above the player inventory, with shift click moving stacks between them. The GUI SHALL grow with the section (example: 9 rows for a section of three bays of 27 slots) and SHALL NOT scroll. Each bay SHALL store its own part of the slots. The GUI SHALL close when a bay of its section is removed or the section changes. Clicking any other face SHALL behave like clicking a plain block, so a held block is placed against it.

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
A rack bay SHALL expose an item handler on every side covering the slots that bay stores, not the rest of its section.

#### Scenario: Hopper into a rack
- **WHEN** a hopper feeds cobblestone into a rack
- **THEN** the cobblestone appears in that bay's slots, in the section GUI and in the warehouse index

### Requirement: Rack is a storage block
A rack SHALL be a stock holding node of the warehouse network: its contents are indexed, it joins through touching storage blocks on any side and through rails on its front side only, and rails dock onto its front only. A rack SHALL be picked from and filled by the warehouse only through a shuttle, never instantly. A shuttle SHALL reach a rack through a rail at the front of any bay of the rack's section and SHALL park on that rail to pick from or put into any bay of the section. A rail touching a rack's side or back SHALL NOT count as a way to reach it, even when that rail belongs to the warehouse. A rack SHALL NOT carry a connection lamp.

#### Scenario: Rack beside a rail
- **WHEN** a rack is placed with a rail of an online warehouse in front of it
- **THEN** the rail shows a dock towards it and the terminal lists its contents

#### Scenario: Rail at the side
- **WHEN** a rail of an online warehouse runs along the side or the back of a rack and nothing else touches it
- **THEN** the rail shows no dock, and the rack's contents are not listed

#### Scenario: Side rail does not reach
- **WHEN** a rack touches the controller directly, holds bread, and a rail of the warehouse with a garage and a shuttle runs along the rack's side but not its front
- **THEN** the terminal lists the bread but a request for it is refused as needing a shuttle

#### Scenario: Rack without a shuttle
- **WHEN** a rack touches the controller directly, holds bread, and the warehouse has no garage
- **THEN** the terminal lists the bread but a request for it is refused as needing a shuttle

#### Scenario: Rack by shuttle
- **WHEN** the same rack stands with its front on a rail and a garage with one shuttle is on that rail
- **THEN** a request for bread sends the shuttle to the rack's front and the bread lands in the terminal's output slots

#### Scenario: Section served from one rail
- **WHEN** a section of three bays has a rail only in front of its left bay and the right bay holds bread
- **THEN** a request for bread sends the shuttle to the rail in front of the left bay and the bread lands in the terminal's output slots

#### Scenario: Neighbouring section not served
- **WHEN** a lone rack stands beside a section with a front rail, does not join it, holds bread and has no front rail of its own
- **THEN** a request for that bread is refused as needing a shuttle

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
