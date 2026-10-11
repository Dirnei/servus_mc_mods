## MODIFIED Requirements

### Requirement: Tier builds
The Shuttle Workshop SHALL build the next tier of the shuttle in its shuttle slot: Mk1 into Mk2 and Mk2 into Mk3. The GUI SHALL show the build sheet of the next tier with how many of each supply are in the workshop's supply slots and how many are needed. While every supply is there, a **Build now** button SHALL use exactly the needed supplies and turn the shuttle into the next tier, keeping its counts, tool, name and name plate, and a **Build by hand** button SHALL start the same build as a short minigame. A sheet SHALL also list the steps of its build by hand. A Mk3 shuttle SHALL show no build. Supplies SHALL stay in the workshop until a build uses them or a player takes them out.

#### Scenario: Build a Mk2
- **WHEN** a player puts a Mk1 shuttle and every supply of the Mk2 sheet into the workshop and presses Build now
- **THEN** the shuttle slot holds a Mk2 shuttle with the Mk1 shuttle's counts and tool, and the supplies are used

#### Scenario: Supplies missing
- **WHEN** one supply of the sheet is short
- **THEN** the list shows how many are missing and Build now and Build by hand are off

#### Scenario: Extra supplies stay
- **WHEN** the supply slots hold more than the sheet needs and a build is done
- **THEN** the rest stays in the supply slots

#### Scenario: Top tier
- **WHEN** a Mk3 shuttle is in the workshop
- **THEN** no build sheet and neither build button are shown
