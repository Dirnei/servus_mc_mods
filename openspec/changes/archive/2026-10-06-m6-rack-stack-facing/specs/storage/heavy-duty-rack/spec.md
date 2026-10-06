# Spec Delta

## MODIFIED Requirements

### Requirement: Bays connect visually
A rack beside another rack with the same facing SHALL render as one continuous rack: posts stand at both ends of the row and after every few bays (`posts_every` in `rack.json`, example 3), and between them the floor board, the middle shelf and the beams run through from bay to bay. A rack directly above or below another rack SHALL continue the posts without a top or bottom frame between them. A rack placed directly on top of another rack SHALL take that rack's facing. Each rack SHALL keep its own inventory. The frame SHALL render without flickering faces.

#### Scenario: Row of three
- **WHEN** three racks facing the same way stand in a row
- **THEN** only the two outer edges show posts and the shelves and beams run along all three

#### Scenario: Row of five
- **WHEN** five racks facing the same way stand in a row with posts every three bays
- **THEN** posts stand at the left end, between the third and fourth bay, and at the right end

#### Scenario: Bay removed from a row
- **WHEN** the second bay of a row of five is broken
- **THEN** the remaining bays show posts at their new row ends and after every third bay from the left

#### Scenario: Different facing
- **WHEN** two racks stand side by side but face different directions
- **THEN** each renders as its own bay

#### Scenario: Separate inventories
- **WHEN** a player fills the left rack of a row
- **THEN** the right rack's GUI stays empty

#### Scenario: Placed on top
- **WHEN** a player looking south places a rack on top of a rack that faces east
- **THEN** the new rack faces east and the two render as one stacked bay
