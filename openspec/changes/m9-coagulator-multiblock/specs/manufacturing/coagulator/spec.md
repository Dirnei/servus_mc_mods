# Spec Delta

## MODIFIED Requirements

### Requirement: The player picks the recipe
The Coagulator SHALL run exactly one selected coagulation recipe. Its GUI SHALL list every coagulation recipe with its result and needs, and clicking one SHALL select it, unless the recipe names a journal chapter the owner's team has not finished. The selection SHALL persist across reloads and while the structure is unformed and formed again. Without a selection the Coagulator SHALL do nothing.

#### Scenario: Switch recipe
- **WHEN** the player clicks the Gunpowder recipe in a Coagulator that was making Salt Crystals
- **THEN** the Coagulator makes Gunpowder from then on and the selection survives a world reload

#### Scenario: Locked by the journal
- **WHEN** the player clicks a recipe that needs a chapter their team has not finished
- **THEN** the selection stays as it was

### Requirement: Coagulator works on time alone
The Coagulator SHALL need no power: no FE buffer, no fuel and no energy capability. The joined principles simply take the recipe's time to cool and set. It SHALL work only while its structure is formed. A cycle SHALL start only when its tanks hold the recipe's principles, its ingredient slot holds the recipe's item, and its output slot has room; the principles and the item SHALL be taken when the cycle completes.

#### Scenario: Missing Mercury
- **WHEN** the selected recipe needs 50 mB Mercury and the Mercury tank holds 20 mB
- **THEN** the Coagulator waits and keeps no progress

#### Scenario: No power needed
- **WHEN** a formed Coagulator holds 250 mB Salt with the Salt Crystal recipe selected and nothing touches it
- **THEN** it makes one Salt Crystal after the recipe's time

#### Scenario: Unformed does nothing
- **WHEN** an unformed Coagulator holds 250 mB Salt with the Salt Crystal recipe selected
- **THEN** it makes nothing

### Requirement: Coagulator GUI
The Coagulator GUI SHALL show the ingredient and output slots, progress, the three principle gauges and the recipe select button; it has no side panel. It SHALL open with an empty hand on a formed Coagulator. The recipe popup SHALL mark the selected recipe, SHALL show a scrollbar whenever it holds more recipes than fit, and SHALL close on Escape or a click outside it.

#### Scenario: Selected recipe marked
- **WHEN** the player opens the recipe popup of a Coagulator making Salt Crystals
- **THEN** the Salt Crystal entry in the popup is highlighted

#### Scenario: More recipes than fit
- **WHEN** the popup holds more recipes than its visible rows
- **THEN** a scrollbar shows beside the grid and the mouse wheel or dragging the bar scrolls it

#### Scenario: Close without choosing
- **WHEN** the player presses Escape or clicks outside the open popup
- **THEN** the popup closes, the GUI stays open and the selection is unchanged

### Requirement: Buckets pour in
A filled principle bucket or container SHALL pour into the matching tank when the player uses it on the Vat of that principle, and when the player clicks any principle gauge in the GUI with it on the cursor. The player SHALL get the empty container back.

#### Scenario: Salt bucket on the block
- **WHEN** a player uses a Salt bucket on the Salt Vat of a formed Coagulator with room in its Salt tank
- **THEN** the Salt tank gains 1,000 mB and the player holds an empty bucket

### Requirement: Coagulator reports to the journal
The Coagulator SHALL remember the player who formed it as its owner and SHALL report every finished cycle to the journal of the owner's team as a `coagulate` event for its result with the result's count.

#### Scenario: Salt Crystal counts
- **WHEN** a Coagulator a player formed makes a Salt Crystal
- **THEN** the player's team gets a `coagulate` event for one Salt Crystal

## REMOVED Requirements

### Requirement: Each principle into its own tank
**Reason**: The principles enter through the Vats of the structure, one Vat per principle, instead of sides with a Fluids layer.
**Migration**: Pipe each principle into its Vat (`manufacturing/coagulator-structure`, requirement "Vats take their principle").
