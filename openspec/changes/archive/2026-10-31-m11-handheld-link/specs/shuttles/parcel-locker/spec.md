## ADDED Requirements

### Requirement: One fixed piece
The Parcel Locker SHALL be placed by one item as one piece 5 blocks wide, 2 high and 1 deep, only when all ten spaces are free and the blocks below have a solid top: from left to right a column of 4 S boxes, a column of 2 S boxes over 1 M box, the head with a screen over the shuttle dock, a column of 2 M boxes and a column of 1 L box. The dock SHALL face a rail beside it, else the player, by the stations' facing rule.

#### Scenario: Placed beside a rail
- **WHEN** a player places a Parcel Locker with a warehouse rail running in front of the middle space
- **THEN** a locker 5 wide and 2 high stands with its dock facing the rail

#### Scenario: No room
- **WHEN** one of the ten spaces is taken
- **THEN** nothing is placed and the item stays in hand

### Requirement: Boxes
A locker SHALL have 6 S, 3 M and 1 L boxes, each holding up to a number of stacks from data (example S 3, M 6, L 9). A box SHALL be free, reserved for an order, or holding a parcel.

#### Scenario: Box count
- **WHEN** a player opens an empty locker's GUI
- **THEN** it shows 10 boxes, 6 S, 3 M and 1 L, all free

### Requirement: Breaking drops everything
Breaking any part of a locker SHALL remove the whole locker, drop one Parcel Locker item and drop every item in its boxes. Orders still on their way to it SHALL end, their reserved stock SHALL be unlocked and the shuttles carrying them SHALL bring their loads back to the racks.

#### Scenario: Break with parcels
- **WHEN** a locker holding two parcels is broken
- **THEN** the locker item and all items of both parcels drop

### Requirement: Locker joins by rail
A locker SHALL belong to the warehouse whose rail runs in front of its dock, SHALL get a machine number and a name like the stations ("Parcel Locker #1"), and its boxes SHALL NOT count as stock.

#### Scenario: Numbered
- **WHEN** the first Parcel Locker of a warehouse is placed at its rail
- **THEN** it is Parcel Locker #1

### Requirement: Orders fill boxes
An order SHALL reserve as many free boxes of one locker as its stacks need, with the least spare room and then the fewest boxes, all marked with the order and the player who sent it. When the locker's free boxes cannot take the whole order, the order SHALL be refused before anything is picked or locked.

#### Scenario: Small order
- **WHEN** an order of 2 stacks goes to an empty locker
- **THEN** it reserves one S box marked with the order

#### Scenario: Several boxes
- **WHEN** an order of 12 stacks goes to an empty locker
- **THEN** it reserves the L box and one S box, both marked with the order

#### Scenario: Too big
- **WHEN** an order of 50 stacks goes to an empty locker of 45 stacks
- **THEN** the order is refused and nothing is picked

### Requirement: Delivery at the dock
The items of an order SHALL be picked from the racks like a terminal request, carried by shuttles to the rail in front of the dock and put into the order's boxes. When the last item of an order is in, the parcel SHALL be ready: the screen shows it, a chime plays at the locker and the ordering player's Handheld panel says which locker holds it.

#### Scenario: Parcel ready
- **WHEN** a player orders 128 cobblestone to Parcel Locker #1
- **THEN** a shuttle brings it to the dock, the cobblestone lies in the order's box and the player's panel says the parcel is ready at Parcel Locker #1

### Requirement: Scan opens the doors
Right clicking the locker's screen with a Handheld SHALL scan it: the screen flashes and greets the player with the number of their ready boxes, and those doors swing open one after another with a ding. With no ready box the screen says there is no parcel. Open doors SHALL close by themselves after a time from data (example 10 seconds).

#### Scenario: Doors open
- **WHEN** Steve has parcels in S-02 and M-02 and scans the screen
- **THEN** the screen greets him with 2 boxes and the doors of S-02 and M-02 swing open

#### Scenario: Nothing waiting
- **WHEN** a player without a parcel scans the screen
- **THEN** the screen says there is no parcel and no door opens

### Requirement: Taking from an open box
Right clicking one's own open box SHALL move its items into the player's inventory; what does not fit SHALL stay in the box. An emptied box SHALL close and be free for the next order. Clicking another player's box SHALL open nothing and the screen SHALL say it is not theirs. Nothing SHALL be put into a box by hand.

#### Scenario: Box freed
- **WHEN** Steve right clicks his open box S-02 holding 32 bread
- **THEN** the bread is in his inventory, the door closes and S-02 is free

#### Scenario: Not yours
- **WHEN** Steve right clicks Alex's box
- **THEN** it stays closed and the screen says it is not his

### Requirement: Locker GUI
Right clicking the screen with an empty hand SHALL open the locker GUI: its name, renamed with the pencil like the stations, and a map of all boxes, each taken box with the name of who ordered it and how far the order is, read only. The GUI SHALL show no items and SHALL let nothing be taken; items come out only through doors opened by a scan.

#### Scenario: Rename
- **WHEN** a player opens the GUI with an empty hand and types "Front Gate" after the pencil
- **THEN** the locker is called "Front Gate" in the picker, the panel and the potato report

#### Scenario: Read only
- **WHEN** a locker holds a parcel of Alex and one of Steve and Steve opens the GUI
- **THEN** he sees both boxes with who ordered them and can take nothing

### Requirement: Uncollected parcels return
A ready parcel not collected within a time from data (example one in-game day) SHALL be put away into the racks by shuttle, like an Inbound Station's buffer, by the zone rules; its boxes SHALL be free once empty. Items that find no place SHALL stay in the box and the locker SHALL say so.

#### Scenario: Back to the racks
- **WHEN** a parcel lies in a locker for a whole in-game day
- **THEN** shuttles carry its items back into the racks and the box is free

### Requirement: Locker look
The locker SHALL be yellow like the Handheld's bumper with a thin roof strip, doors with a small id (S-01) and a lamp each (dark free, ochre someone else's, amber reserved, green ready), a steel screen with a yellow edge, red scan window and green LCD with a QR code and a blinking SCAN, and a steel dock with a hatch and a lamp that blinks while a shuttle loads. An open box SHALL show its items. Faces SHALL follow the family's plane rules.

#### Scenario: Ready lamp
- **WHEN** a parcel is ready in box S-02
- **THEN** the lamp of S-02 is green

#### Scenario: Loading
- **WHEN** a shuttle delivers into the locker
- **THEN** the dock lamp blinks amber and the screen shows LOAD

### Requirement: Locker inspect
Using the Data Potato on any part SHALL print the locker's label, its free boxes per size and each parcel with its owner, its items and how long until it returns. It SHALL change nothing.

#### Scenario: Report
- **WHEN** Parcel Locker #1 holds one ready parcel of Steve in S-01
- **THEN** the chat shows Parcel Locker #1, 5 S, 3 M and 1 L free, and Steve's parcel in S-01 with its items and time left

### Requirement: Jade shows the locker
With Jade installed, looking at a locker SHALL show its name (like "Parcel Locker #1" or the name given) as the title line. Looking at a box SHALL add who ordered it and how far the order is, or that it is free; when the viewer ordered it, its items too. Looking at the screen or dock SHALL list every taken box with who ordered it. Items of other players' boxes SHALL NOT be shown.

#### Scenario: Own box
- **WHEN** Steve looks at his ready box S-02 holding 32 bread
- **THEN** Jade shows the locker's name, "S-02: Steve, ready" and 32 bread

#### Scenario: Someone else's box
- **WHEN** Steve looks at Alex's box
- **THEN** Jade shows the locker's name and that Alex ordered it, without its items

### Requirement: Locker is obtainable
The Parcel Locker SHALL be crafted shaped from yellow dye, iron, chests, a Data Cable and an Outbound Station (example) and SHALL be listed in the Shuttles creative tab and the recipe viewer.

#### Scenario: Crafting
- **WHEN** a player crafts the Parcel Locker recipe
- **THEN** the result is one Parcel Locker item

### Requirement: Box assignment is pure logic
Which free boxes an order takes SHALL be decided by pure logic without Minecraft, from the free boxes with their sizes and the stacks the order needs (each entry rounded up to whole stacks): the set that fits with the least spare room, then the fewest boxes.

#### Scenario: Pick the fit
- **WHEN** an order needs 7 stacks and S, S and M boxes are free
- **THEN** the M box and one S box are taken
