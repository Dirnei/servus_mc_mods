# Spec Delta

## ADDED Requirements

### Requirement: Tank recipe in recipe viewers
The Fluid Tank recipe SHALL be a plain shaped crafting recipe whose ingredients recipe viewers can read: its floor cell SHALL list every block the floor accepts. With JEI installed, looking up the Fluid Tank SHALL show this recipe with the floor cell cycling through the accepted blocks, and no second, display-only tank recipe.

#### Scenario: Look up the tank in JEI
- **WHEN** a player looks up the recipe of the Fluid Tank in JEI
- **THEN** one crafting recipe shows copper ingots, glass and a floor cell that cycles through blocks such as stone bricks and cobblestone, and never shows glass or a slab

#### Scenario: Transfer at the Storage Terminal
- **WHEN** a formed CPU and a Crafting Station are on the terminal's line, the warehouse holds cobblestone and no other floor block, and the player presses JEI's transfer button on the Fluid Tank recipe
- **THEN** the order grid shows copper ingots in the corners, glass on the three edges and cobblestone in the bottom middle, and the result shows a Fluid Tank with a cobblestone floor

#### Scenario: Existing tanks unchanged
- **WHEN** a world with Fluid Tanks crafted before this change is loaded
- **THEN** the tanks and tank items keep their floors
