## Purpose

Defines the Cutting Board and the Kitchen Knife of Servus: Delicacies and the data driven cutting recipes that turn whole vegetables into pieces for cooking.

## ADDED Requirements

### Requirement: Cutting Board and Kitchen Knife
Servus: Delicacies SHALL add a Cutting Board, a placed wooden board that holds one stack to cut and one stack of cut pieces, and a Kitchen Knife, a tool with durability that is tagged `c:tools/knife`. Breaking the board SHALL drop it and everything on it.

#### Scenario: Knife tag
- **WHEN** another mod's recipe asks for `c:tools/knife`
- **THEN** the Kitchen Knife is accepted

### Requirement: Cutting recipes
Cutting recipes (`servus_delicacies:cutting`) SHALL name one item ingredient, the tool that cuts it (by default anything in `c:tools/knife`), and a counted result. Each cut SHALL take one item from the board, put the result onto the board's piece stack and cost the tool one durability. A cut SHALL be refused when the pieces would not fit. Recipes SHALL come from datapacks and be shown to clients.

#### Scenario: Cutting a cabbage
- **WHEN** a Cabbage lies on the board and a player right clicks it with the Kitchen Knife, with the shipped recipe (example: two pieces)
- **THEN** the Cabbage is gone, the board holds two Shredded Cabbage and the knife lost one durability

#### Scenario: Without a knife
- **WHEN** a player right clicks a board holding an Onion with a stick
- **THEN** nothing is cut

#### Scenario: One per click
- **WHEN** three Onions lie on the board and the player clicks once with the knife
- **THEN** one Onion is cut and two remain

### Requirement: Board by hand
Right clicking the board with an item that some cutting recipe takes SHALL put as much of the held stack onto it as fits, when the board holds nothing else to cut. Right clicking with an empty hand SHALL give the player the cut pieces, or, when there are none, the item to cut. The board SHALL show the item to cut and the pieces lying on it.

#### Scenario: Taking pieces
- **WHEN** a player right clicks a board holding two Chopped Onion with an empty hand
- **THEN** the player gets the two Chopped Onion

#### Scenario: Not cuttable
- **WHEN** a player right clicks the board with Cobblestone
- **THEN** it stays in the player's hand

### Requirement: Board by automation
The board SHALL expose the standard item capability: insertion SHALL go onto the board only for items some cutting recipe takes, and extraction SHALL take only cut pieces.

#### Scenario: Hopper on the board
- **WHEN** a hopper holding Onions points down into a Cutting Board
- **THEN** the Onions move onto the board and wait to be cut

### Requirement: Starter cutting recipes
Servus: Delicacies SHALL ship cutting recipes for Cabbage into Shredded Cabbage and Onion into Chopped Onion, and crafting recipes for the Cutting Board (wooden slabs and a stick) and the Kitchen Knife (iron and a stick).

#### Scenario: Onion pieces
- **WHEN** an Onion is cut with the knife
- **THEN** the board holds Chopped Onion
