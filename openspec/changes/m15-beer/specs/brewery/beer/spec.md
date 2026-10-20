## Purpose

Defines the beer of Servus: Brewery: Yeast, the Fermenting Vat that ferments wort into Helles, Dunkles and Weissbier, the beer fluids, the Cask with its brass tap, the half litre glasses and drinking them.

## ADDED Requirements

### Requirement: Yeast
Servus: Brewery SHALL add Yeast, crafted from Wheat, Sugar and a Water Bottle. Fermenting SHALL never use it up.

#### Scenario: Crafting Yeast
- **WHEN** a player crafts Wheat, Sugar and a Water Bottle together
- **THEN** they get one Yeast

### Requirement: Fermenting Vat
Servus: Brewery SHALL add the Fermenting Vat, an open spruce vat with iron bands that drops itself when broken. It SHALL hold one tank of wort or beer (example: 4000 mB) and one Yeast. Its top SHALL show what it holds: nothing, wort, foam while it ferments, or beer.

#### Scenario: Placing a vat
- **WHEN** a player places a Fermenting Vat
- **THEN** it stands as an open spruce vat with two iron bands, empty inside

#### Scenario: Breaking a vat
- **WHEN** a player breaks a vat that holds Yeast and wort
- **THEN** the vat and the Yeast drop as items and the wort is lost

### Requirement: Fermenting
A Fermenting Vat that holds a wort a fermenting recipe takes and the Yeast of that recipe SHALL ferment the whole tank: after the recipe's time in game time (example: Pale Wort to Helles in 36000 ticks, Wheat Wort to Weissbier in 24000 ticks) the wort SHALL become the same amount of the recipe's beer. The time SHALL count while the chunk is unloaded. While a batch ferments no wort SHALL be added and the Yeast SHALL stay in. After the batch the Yeast SHALL still be in the vat and can be taken back.

#### Scenario: Wort ferments into beer
- **WHEN** a vat holds 1000 mB Pale Wort and a player adds Yeast
- **THEN** the top shows foam, and once the recipe's time has passed the vat holds 1000 mB Helles, shows beer, and still holds the Yeast

#### Scenario: No yeast, no beer
- **WHEN** a vat holds Dark Wort and no Yeast for longer than the recipe's time
- **THEN** it still holds the Dark Wort

#### Scenario: Not done before its time
- **WHEN** a batch has fermented for less than its time
- **THEN** the vat still holds wort

### Requirement: Fermenting recipes
Fermenting recipes SHALL be data (`servus_brewery:fermenting`). The mod SHALL ship Pale Wort to Helles, Dark Wort to Dunkles and Wheat Wort to Weissbier. The beer of the vat SHALL be final; it needs no ageing.

#### Scenario: Shipped recipes
- **WHEN** a world loads with the default data
- **THEN** a fermenting recipe exists for each of the three worts, and its beer can be tapped right away

### Requirement: Vat hand use and automation
A wort bucket SHALL fill the vat by hand, an empty bucket SHALL take finished beer, Yeast SHALL go in by hand, and sneaking with an empty hand SHALL take the Yeast back while no batch runs. An empty hand SHALL tell the contents and how far a batch is. Every side SHALL offer the item capability (Yeast in, Yeast out only while no batch runs) and the fluid capability (wort in while no batch runs, only finished beer out).

#### Scenario: Pipes on a vat
- **WHEN** a pipe pushes Pale Wort and a hopper pushes Yeast into a vat, and a pipe pulls from it while it ferments
- **THEN** the wort and the Yeast go in, and nothing comes out until the beer is done; then the beer comes out

#### Scenario: Wort stays in
- **WHEN** a pipe or an empty bucket tries to take wort from a vat
- **THEN** nothing comes out

### Requirement: Beer fluids
Servus: Brewery SHALL add the fluids Helles, Dunkles and Weissbier, each with a bucket, drawn as tinted water.

#### Scenario: A beer bucket
- **WHEN** a player takes beer from a vat or a Cask with an empty bucket
- **THEN** they get a bucket of that beer, which shows the beer's colour

### Requirement: Cask
Servus: Brewery SHALL add the Cask, a wooden barrel with a brass tap on its front that faces the player who places it, and drops itself when broken. It SHALL hold one beer (example: 16000 mB) and take only beer. Buckets SHALL fill and empty it by hand, an empty hand SHALL tell how much of which beer it holds, and every side SHALL offer the fluid capability. Its beer SHALL be lost when it is broken.

#### Scenario: Filling a cask
- **WHEN** a pipe moves Helles from a vat into a Cask
- **THEN** the Cask holds the Helles

#### Scenario: Wort stays out
- **WHEN** a player tries to fill a Cask with a wort bucket
- **THEN** the Cask takes nothing

### Requirement: Tapping glasses
Right clicking the front of a Cask, where the tap is, with an empty glass SHALL tap one glass of beer (example: 500 mB) when the Cask holds enough of a beer that glass takes, and give the full glass to the player. A Beer Glass SHALL take Helles and Dunkles; the Weissbier Glass SHALL take only Weissbier, and Weissbier SHALL fill only the Weissbier Glass. Which glass takes which beer SHALL be data (`servus_brewery:tapping`).

#### Scenario: A glass of Helles
- **WHEN** a player right clicks the tap of a Cask holding 1000 mB Helles with a Beer Glass
- **THEN** they get a Glass of Helles and the Cask holds 500 mB

#### Scenario: Weissbier refuses a plain glass
- **WHEN** a player right clicks the tap of a Cask holding Weissbier with a Beer Glass
- **THEN** nothing is tapped and the Cask keeps its Weissbier; with a Weissbier Glass they get a Glass of Weissbier

#### Scenario: Not enough beer
- **WHEN** a player taps a Cask holding less than one glass
- **THEN** nothing is tapped

### Requirement: Glasses
The empty Beer Glass and Weissbier Glass SHALL be crafted from glass and stack to 16. Full glasses SHALL stack to 1.

#### Scenario: Crafting glasses
- **WHEN** a player crafts Glass blocks in the Beer Glass or the Weissbier Glass shape
- **THEN** they get empty glasses of that kind that stack to 16

### Requirement: Drinking beer
A Glass of Helles, Dunkles or Weissbier SHALL be drunk with the drink animation, at any hunger. Drinking SHALL give back the empty glass it was tapped into, restore the hunger and saturation of the drink's entry in the Delicacies food data map (examples: 2 hunger, 1.0 saturation), and post a `BeerDrunkEvent` on the NeoForge event bus with the player and the drink, for other features such as the Rausch. The event SHALL be posted on the server only. Drinking SHALL give no effects of its own.

#### Scenario: Drinking returns the glass
- **WHEN** a hungry player drinks a Glass of Weissbier
- **THEN** their hand holds a Weissbier Glass, their hunger rises by the data value, and one `BeerDrunkEvent` is posted

### Requirement: Fermenting in JEI
When JEI is installed, it SHALL show a Fermenting category with every fermenting recipe: the Yeast, the wort, the beer and the time, with the Fermenting Vat as catalyst.

#### Scenario: Looking up Helles
- **WHEN** a player looks up the recipes of Helles in JEI
- **THEN** JEI shows Pale Wort and Yeast fermenting into Helles in the Fermenting Vat, with its time
