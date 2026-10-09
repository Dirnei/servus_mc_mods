# Spec Delta

## Purpose

The Coagulator is built in the world as a casting table under three vats of Salt, Sulfur and Mercury, formed with Paracelsus' Journal and fed through its vats instead of configured sides.

## ADDED Requirements

### Requirement: Coagulator layout
The Coagulator structure SHALL be one block deep, 3 wide and 2 tall, seen from its front: the Coagulator in the middle of the bottom row with vanilla Stone Bricks left and right of it, and above them three Principle Vats: the Sulfur Vat on the left, the Salt Vat in the middle and the Mercury Vat on the right. Which principle a Vat holds SHALL follow from its position.

#### Scenario: Layout from the front
- **WHEN** a player looks at the front of a complete Coagulator structure
- **THEN** they see, top to bottom: `V V V`, `S C S` (V Principle Vat, S Stone Bricks, C Coagulator)

### Requirement: Principle Vat
Manufacturing SHALL add the Principle Vat, crafted at a crafting table from vanilla materials without research.

#### Scenario: Vat recipe
- **WHEN** the player crafts the Vat pattern of copper ingots and a bucket
- **THEN** one Principle Vat is produced

### Requirement: Forming the Coagulator
Right clicking the Coagulator with Paracelsus' Journal SHALL check the structure in each of the four horizontal facings and form it in the first that matches, making the player its owner, and SHALL NOT open the book. When nothing matches it SHALL name the first missing block of the best facing and where it belongs. The Configuration Carrot SHALL NOT form, turn or dismantle any block of the structure.

#### Scenario: Formed
- **WHEN** a player right clicks the Coagulator of a complete structure with the Journal
- **THEN** the structure forms and the player sees "The Coagulator is ready."

#### Scenario: Vat missing
- **WHEN** the Mercury Vat is missing
- **THEN** nothing forms and the player is told a Principle Vat is needed right of the middle Vat

### Requirement: Breaking unforms the Coagulator
Breaking the Coagulator, a Vat or a Stone Bricks block of a formed Coagulator SHALL unform it; the Vats keep nothing of their own, the principles stay in the Coagulator. An unformed Coagulator SHALL NOT work.

#### Scenario: Brick broken
- **WHEN** a player breaks a Stone Bricks block of a formed Coagulator
- **THEN** the Coagulator unforms and stops

### Requirement: Vats take their principle
Each Vat of a formed Coagulator SHALL accept through the fluid capability on every face only its own principle, into the Coagulator's tank of that principle, and SHALL never give fluid. A filled principle bucket used on a Vat SHALL pour into that Vat's tank when it is the Vat's principle. Unformed Vats SHALL expose nothing.

#### Scenario: Pipe into the Salt Vat
- **WHEN** a Fluid Pipe offers Salt and then Mercury to the Salt Vat
- **THEN** the Salt goes into the Salt tank and the Mercury is refused

#### Scenario: Bucket on the wrong Vat
- **WHEN** a player uses a Mercury bucket on the Sulfur Vat
- **THEN** nothing is poured

### Requirement: Items at the Coagulator
A formed Coagulator SHALL take the ingredient of its selected recipe through the item capability on every face of the Coagulator block and give its products from there; it SHALL take nothing into the output and give nothing from the ingredient. The other blocks SHALL expose no item capability.

#### Scenario: Hopper takes the product
- **WHEN** a hopper under the Coagulator of a formed structure and a Salt Crystal in its output
- **THEN** the hopper receives the Salt Crystal

### Requirement: Coagulator look
A formed Coagulator SHALL show copper spouts from each Vat down to the mold and each Vat's principle at the height its tank is full.

#### Scenario: Vat level
- **WHEN** the Coagulator's Salt tank is half full
- **THEN** the Salt Vat shows Salt at half its height

### Requirement: Coagulator schematic
The journal's Coagulator entry SHALL show the structure as a multiblock page with Visualize, standing on the clicked ground, and the Vat's crafting recipe. The schematic SHALL be generated from the same layout that forming checks.

#### Scenario: Visualize
- **WHEN** a player presses Visualize on the Coagulator page and clicks the ground
- **THEN** the ghost of the structure stands on that ground
