# Spec Delta

## ADDED Requirements

### Requirement: Side configuration has layers
A side configuration SHALL consist of an Items layer and, for blocks that hold fluid tanks, a Fluids layer. The Items layer SHALL keep the side modes of the requirements above. Each layer SHALL be stored, synced, kept on dismantling and cycled independently of the other.

#### Scenario: Layers are independent
- **WHEN** the player sets the left side of a Calciner's Fluids layer to Salt
- **THEN** the left side's Items mode stays what it was

#### Scenario: Kept on dismantle
- **WHEN** a Calciner with a configured Fluids layer is dismantled with the sneak carrot and placed again
- **THEN** both layers come back as they were

### Requirement: Fluids layer assigns tanks to sides
In the Fluids layer each side SHALL be Off, All, or one of the block's declared tanks. A side set to a tank SHALL expose only that tank through the fluid capability, in the direction the block declares for it (fill for an input tank, drain for an output tank). A side set to All SHALL expose every tank as the block did before layers existed. A side set to Off SHALL expose no fluid capability. New sides SHALL default to All, and fixed sides SHALL stay Off.

#### Scenario: One principle per side
- **WHEN** the Calciner's left side is set to Salt and its right side to Mercury
- **THEN** a pipe on the left drains only Salt and a pipe on the right drains only Mercury

#### Scenario: Default behaves as before
- **WHEN** a Calciner is placed and nothing is configured
- **THEN** every side but the front offers all three principles

### Requirement: Output tanks push on their sides
A block SHALL push the contents of an output tank into a touching fluid handler on every side set to that tank or to All, at the block's push interval. A block SHALL never push into a side set to an input tank or to Off.

#### Scenario: Tank beside a Salt side
- **WHEN** a Fluid Tank touches the Calciner's side set to Salt and the Calciner makes Salt and Mercury
- **THEN** only Salt flows into the tank

### Requirement: Pure layer model
The layer types, the tank assignment, cycling and their byte encoding SHALL have no Minecraft dependency and SHALL be covered by JUnit tests.

#### Scenario: Cycle through tanks
- **WHEN** a Fluids layer side of a block with tanks Salt, Sulfur and Mercury is cycled from Off
- **THEN** it moves to All, Salt, Sulfur, Mercury and back to Off

### Requirement: Side panel tabs
The side panel SHALL show one tab per layer the block has, above its 3 by 3 grid, and SHALL show the grid of the selected tab. Fluids layer cells SHALL be coloured with the assigned tank's fluid colour, with distinct colours for All and Off, and their tooltip SHALL name the side and the assignment. A click on a cell SHALL cycle that side in the selected layer. A block with only the Items layer SHALL show no tabs.

#### Scenario: Switch to fluids
- **WHEN** the player clicks the Fluids tab of a Calciner GUI and then the left cell
- **THEN** the left side's Fluids assignment cycles and the tooltip reads "Left: Salt" or the next assignment

### Requirement: Carrot layer
The Configuration Carrot SHALL carry a layer, Items by default. Using the carrot on the air SHALL switch it to the next layer and tell the player the new layer in the action bar. Using the carrot on a block face SHALL cycle that face in the carrot's layer; on a block without that layer it SHALL cycle the Items layer. The carrot's tooltip SHALL name its layer.

#### Scenario: Configure a fluid side by hand
- **WHEN** the player right clicks the air with the carrot, then right clicks the Calciner's top face
- **THEN** the action bar first shows "Layer: Fluids", then "Top: Salt" or the next assignment

### Requirement: Markers show the carrot's layer
While the player holds the carrot, side markers SHALL draw the layer the carrot is set to; Fluids layer frames SHALL use the assigned tank's fluid colour. While the player only wears the goggles, markers SHALL draw the Items layer.

#### Scenario: Fluid markers
- **WHEN** the player holds a carrot set to Fluids and looks at a configured Calciner
- **THEN** the Salt side shows a frame in the Salt colour and the Off sides show none
