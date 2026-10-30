# foundation/mixer Specification

## Purpose
The Mixer is a Foundation machine that mixes several items, and optionally one fluid, into one item result only while a driver drives it. It covers the block, its slots, tank, sides, screen and look, the data driven `servus_foundation:mixing` recipe type with the shipped vanilla recipes, and the JEI category.

## Requirements

### Requirement: Mixing recipes
Foundation SHALL provide the recipe type `servus_foundation:mixing`: one to four item ingredients, each with a count, at most one fluid ingredient with an amount in mB, one item result with a count, and the work one mixing takes. The item ingredients SHALL be shapeless. Recipes SHALL be datapack JSON, SHALL be sent to clients, and any mod or datapack MAY add more.

#### Scenario: Datapack recipe
- **WHEN** a datapack adds a mixing recipe from 2 Sand, 1 Clay Ball and 500 mB of Water to 2 Clay with work 300
- **THEN** a Mixer holding at least that makes 2 Clay after 300 work and takes 2 Sand, 1 Clay Ball and 500 mB of Water

#### Scenario: Too many ingredients
- **WHEN** a datapack recipe lists five item ingredients
- **THEN** the recipe is rejected with a load error and the other mixing recipes still load

### Requirement: Shipped mixing recipes
Foundation SHALL ship mixing recipes for vanilla items: Dirt and Water into Mud, Dirt and Gravel into Coarse Dirt, and Sand, Gravel and one dye into Concrete Powder of that dye's colour, for all 16 colours. No two shipped mixing recipes SHALL match the same contents of a Mixer.

#### Scenario: Mud
- **WHEN** a driven Mixer holds 1 Dirt and 1,000 mB of Water
- **THEN** it makes 1 Mud and the tank holds 750 mB (example amount 250 mB per Mud)

#### Scenario: Coarse Dirt
- **WHEN** a driven Mixer holds 2 Dirt and 2 Gravel
- **THEN** it makes 4 Coarse Dirt, as many as the vanilla crafting recipe

#### Scenario: Concrete Powder
- **WHEN** a driven Mixer holds 4 Sand, 4 Gravel and 1 Red Dye
- **THEN** it makes 8 Red Concrete Powder

#### Scenario: No competing recipes
- **WHEN** the shipped recipes are loaded and each one's least contents are put into an otherwise empty Mixer
- **THEN** exactly that recipe matches

### Requirement: A mixing uses the whole tub
A recipe SHALL match only when every item ingredient is served by an input slot of its own holding at least its count, every input slot that is not empty serves an ingredient, and, for a recipe with a fluid, the tank holds that fluid in at least its amount. A recipe without a fluid SHALL ignore the tank. When several recipes match, one with a fluid SHALL win over one without.

#### Scenario: Leftover item blocks the mix
- **WHEN** a Mixer holds 1 Dirt and 1 Sand and 1,000 mB of Water
- **THEN** no recipe matches and a driver spends nothing on it

#### Scenario: Fluid ignored by a dry recipe
- **WHEN** a driven Mixer holds 2 Dirt, 2 Gravel and 1,000 mB of Water
- **THEN** it makes Coarse Dirt and the tank still holds 1,000 mB

#### Scenario: Missing fluid
- **WHEN** a driven Mixer holds 1 Dirt and its tank is empty
- **THEN** nothing is made and the Dirt stays

#### Scenario: Several mixings
- **WHEN** a driven Mixer holds 3 Dirt and 1,000 mB of Water and gets enough work for three mixings
- **THEN** it makes 3 Mud, and the Dirt slot is empty

### Requirement: Mixer block
Foundation SHALL provide the Mixer, a machine crafted from vanilla materials. It SHALL face the player when placed, open a screen with four input slots, a fluid gauge, a progress arrow, an output slot and the side panel on right click, and drop itself and its items when broken; its fluid is lost. The Configuration Carrot SHALL cycle its sides and dismantle it like other machines.

#### Scenario: Place and open
- **WHEN** a player places a Mixer and right clicks it with an empty hand
- **THEN** the Mixer's screen opens with the four inputs, the fluid gauge, the arrow, the output and the side panel

#### Scenario: Gauge tooltip
- **WHEN** the Mixer holds 750 mB of Water and the player hovers the gauge
- **THEN** the tooltip names Water and 750 / 4,000 mB (example capacity)

### Requirement: Input slots take mixable items, one kind per slot
An input slot SHALL take only items some mixing recipe uses. Each kind of item SHALL go into at most one input slot: into the slot that already holds it, or into an empty slot when no other input slot holds it. This holds for players, hoppers, pipes and other mods alike.

#### Scenario: Wrong item
- **WHEN** a player tries to put a Diamond into an input slot and no mixing recipe uses Diamonds
- **THEN** the slot refuses it

#### Scenario: Hopper of sand
- **WHEN** a hopper above an empty Mixer holds 64 Sand
- **THEN** all the Sand that goes in lands in one input slot and the other three stay empty

#### Scenario: Second kind
- **WHEN** one input slot holds Sand and a player puts Sand into another input slot
- **THEN** that slot refuses it

### Requirement: Mixer tank
The Mixer SHALL hold one fluid in one tank, with its capacity from datapack data (example 4,000 mB). The tank SHALL take only fluids some mixing recipe uses, and only one fluid at a time. Automation SHALL fill it only through its fluid sides (back, left and right) and SHALL never drain it. A bucket of a usable fluid used on the Mixer SHALL pour in; an empty bucket used on it SHALL take a full bucket out when the tank holds at least that.

#### Scenario: Pipe at the port
- **WHEN** a Fluid Tank with Water touches the Mixer's back, set to Pull
- **THEN** Water flows into the Mixer until its tank is full

#### Scenario: Other faces
- **WHEN** a duct with a fluid line touches the Mixer's top
- **THEN** it finds no fluid endpoint there

#### Scenario: Bucket in and out
- **WHEN** a player uses a Water Bucket on an empty Mixer and then an empty Bucket on it
- **THEN** the tank first holds 1,000 mB of Water, then it is empty again and the player holds a Water Bucket

#### Scenario: Unused fluid
- **WHEN** a player uses a Lava Bucket on a Mixer and no mixing recipe uses Lava
- **THEN** nothing pours in and the player keeps the Lava Bucket

#### Scenario: No draining by automation
- **WHEN** a duct on pull touches the Mixer's back while the tank holds Water
- **THEN** no Water leaves the Mixer

### Requirement: The Mixer mixes only while driven
The Mixer SHALL have no power or fuel of its own. It SHALL advance a mixing only by the work a driver gives it through the drive link, SHALL finish one mixing when the work given reaches the recipe's work, then take each ingredient's count from its slot, drain the recipe's fluid and put the result into the output. Without a driver its progress SHALL stay. It SHALL want work only while a recipe matches and its output has room for the result.

#### Scenario: Driven
- **WHEN** a Mixer holds 1 Dirt and 1,000 mB of Water and a driver gives it the recipe's work
- **THEN** the Dirt is gone, 250 mB of Water is gone and 1 Mud is in the output

#### Scenario: Not driven
- **WHEN** a Mixer holds 1 Dirt and Water and nothing drives it for ten seconds
- **THEN** the Dirt and the Water are still there and the output is empty

#### Scenario: Output full
- **WHEN** the Mixer's output holds a full stack of Mud
- **THEN** the Mixer wants no work and its drivers spend nothing

#### Scenario: Progress kept
- **WHEN** a Hand Crank gives a Mixer half a recipe's work and is let go, and later turned again
- **THEN** the mixing continues from half and finishes after the other half

### Requirement: Mixer item sides
The Mixer's item sides SHALL default to Input on the top, back, left and right and Auto on the bottom. Its front, where the chute is, SHALL be fixed to Push, so the result leaves through the chute and the bottom and nothing enters through the chute. Automation SHALL never take inputs out or put anything into the output.

#### Scenario: Hopper above, chest in front
- **WHEN** a hopper above a driven Mixer holds Dirt and Gravel and a chest stands in front of the chute
- **THEN** both go into the Mixer and the Coarse Dirt ends up in the chest

#### Scenario: Chute refuses input
- **WHEN** a hopper offers Dirt to the Mixer's front
- **THEN** nothing goes in

### Requirement: Mixer drive and fluid sides
The Mixer SHALL take drive on its back, left and right sides and not on its top, bottom or front. Its fluid sides SHALL be the back, left and right, each taking Input, Pull or Off and defaulting to Input; the top, bottom and front SHALL be fixed Off. A driver and a fluid connection never need the same side.

#### Scenario: Drive only on back and sides
- **WHEN** a player tries to place a Hand Crank on the Mixer's front
- **THEN** the crank is not placed, while it can be placed on the back, left or right

#### Scenario: Fluid side panel
- **WHEN** the player opens the Fluids tab of the Mixer's side panel
- **THEN** the back, left and right cells can be cycled through Input, Pull and Off, the top, bottom and front cannot, and it shows no tank chip

#### Scenario: Crank on the back, water from the side
- **WHEN** a Hand Crank sits on the Mixer's back and a Fluid Tank with Water set to Pull touches its left side
- **THEN** the crank drives the Mixer and Water flows in from the left

### Requirement: Mixer look
The Mixer SHALL look like a family steel machine: a heavy iron base, an open steel tub with a brass rim, a steel bridge across the rim carrying a brass paddle hub, a steel shaft with two crossed heavy iron paddle blades inside the tub, a steel chute out of the front and a brass fluid port on the back. No two overlapping faces of its parts SHALL share a plane.

#### Scenario: No flicker
- **WHEN** a player looks at a Mixer from any angle, also while the paddle turns
- **THEN** no face of it flickers

### Requirement: Paddle motion
While a driver turns the Mixer, the paddle SHALL turn as described in the drive's flywheel motion, also when the tub is empty; standing, it SHALL keep its pose.

#### Scenario: Paddle turns
- **WHEN** a Mixer is driven and mixing
- **THEN** its paddle turns smoothly, and when the drive stops it slows down to a stop within about a second

#### Scenario: Cranked while empty
- **WHEN** a player holds right click on a Hand Crank on an empty Mixer
- **THEN** the paddle turns with the crank, and the Mixer mixes nothing

### Requirement: Contents shown in the tub
While the Mixer holds fluid, a surface of that fluid SHALL show inside the tub. While it holds items but no fluid, a mix surface SHALL show there instead. An empty Mixer SHALL show the tub's floor. Every player near the Mixer SHALL see the same.

#### Scenario: Water poured in
- **WHEN** a player pours a Water Bucket into an empty Mixer
- **THEN** a water surface shows inside the tub, with the paddle blades rising through it

#### Scenario: Dry mix
- **WHEN** a hopper puts Dirt and Gravel into an empty Mixer
- **THEN** a mix surface shows inside the tub, and once the last item is mixed and the tank is empty the tub floor shows again

### Requirement: Mixing in JEI
When JEI is installed, a Mixing category SHALL list every mixing recipe with its counted items, its fluid and amount if it has one, its result and its work, with the Mixer as catalyst.

#### Scenario: Look up Mud
- **WHEN** the player looks up how to make Mud in JEI
- **THEN** the Mixing category shows 1 Dirt and 250 mB of Water into 1 Mud, its work, and the Mixer

#### Scenario: Look up Concrete Powder
- **WHEN** the player looks up how to make Blue Concrete Powder
- **THEN** the Mixing category shows 4 Sand, 4 Gravel and 1 Blue Dye into 8 Blue Concrete Powder

### Requirement: Slot logic is pure
Matching a shapeless list of counted ingredients against slots, choosing which slot serves which ingredient, and the one kind per slot rule SHALL be plain Java without Minecraft types, covered by JUnit tests.

#### Scenario: Unit tests
- **WHEN** `gradlew :foundation:test` runs
- **THEN** the slot matcher and one kind per slot tests pass without the game
