# foundation/duct-channels Specification

## Purpose
Named duct channels: each team keeps one list of channels with a name, a colour and an optional icon, which the item, power and fluid lines of every duct side can be put on.

## Requirements

### Requirement: Default channel
There SHALL be one Default channel shared by everyone: unnamed (shown as "Default"), white, without icon. It SHALL never be edited or deleted. Every new duct side SHALL be on Default, and a side whose stored channel no longer exists SHALL be on Default.

#### Scenario: New duct
- **WHEN** a player places an Item Pipe next to a chest and opens the endpoint GUI
- **THEN** the item line's side is on Default and its ring is white

#### Scenario: Default cannot be changed
- **WHEN** the player opens the channel popup
- **THEN** the Default entry shows no pencil and no trash

### Requirement: One channel list per team
Each team SHALL have one list of channels, kept with the world. A channel SHALL belong to the team of the player who created it and SHALL keep its identity when renamed, recoloured or given a new icon. Channels of other teams SHALL work on any duct side they are set on but SHALL not be listed in the player's popup.

#### Scenario: Shared within the team
- **WHEN** player A creates the channel "Iron ore" and player B on the same team opens a duct's channel popup on "All"
- **THEN** "Iron ore" is listed for player B

#### Scenario: Other team
- **WHEN** a player of another team opens the channel popup on "All"
- **THEN** "Iron ore" is not listed

#### Scenario: Kept after restart
- **WHEN** the server restarts
- **THEN** the team's channels and every side's channel are unchanged

### Requirement: Channel properties
A channel SHALL have a name of 1 to 24 characters, unique within its team ignoring case, one of the 16 dye colours, and optionally one item as icon. Creating or editing a channel with an empty, too long or taken name SHALL be refused with a message in the form.

#### Scenario: Create a channel
- **WHEN** the player creates "Smelter feed" in orange with a furnace as icon
- **THEN** the team's list holds "Smelter feed", orange, with the furnace icon

#### Scenario: Name taken
- **WHEN** the team already has "Iron ore" and the player creates "iron ORE"
- **THEN** the form says the name is taken and no channel is created

#### Scenario: Black and grey exist
- **WHEN** the player opens the colour palette of the form
- **THEN** it offers the 16 dye colours, red, green, blue, black and grey among them

### Requirement: Any team member edits
Any member of a channel's team SHALL be able to change its name, colour and icon. The change SHALL apply to every duct side on that channel at once.

#### Scenario: Recolour
- **WHEN** a team member changes "Iron ore" from red to blue
- **THEN** every loaded ring on "Iron ore" turns blue without the ducts being touched

### Requirement: Delete only unused channels
A team member SHALL be able to delete a channel only when no loaded duct side uses it, or when its only loaded use is the side whose GUI is open; that side SHALL then move to Default. Otherwise deleting SHALL be refused and the number of loaded sides using the channel SHALL be shown. Sides in unloaded areas that still name a deleted channel SHALL be on Default.

#### Scenario: Unused channel
- **WHEN** no loaded side uses "Old test" and the player deletes it
- **THEN** "Old test" is gone from the list

#### Scenario: Only this side uses it
- **WHEN** the open side is the only loaded side on "Gold" and the player deletes "Gold"
- **THEN** "Gold" is gone and the side is on Default

#### Scenario: Still in use
- **WHEN** three loaded sides use "Iron ore" and the player opens the popup on a fourth side
- **THEN** the trash of "Iron ore" is disabled and its tooltip says it is used on 3 loaded sides

### Requirement: Clients know every channel
Every client SHALL receive all channels with their team, name, colour and icon when the player joins and after every create, edit or delete, so rings and GUIs show current names and colours.

#### Scenario: Join
- **WHEN** a player joins a world with existing channels
- **THEN** the rings of nearby ducts show the channels' colours before any GUI is opened

### Requirement: Channel list logic is pure
Name checks, the delete rule, ordering and the popup's search and "This cable" filtering SHALL be plain Java without Minecraft classes and covered by unit tests.

#### Scenario: Unit tested
- **WHEN** the unit tests run
- **THEN** they cover name length and uniqueness, the delete rule with zero, one own and several uses, and filtering by text and by a set of used channel ids
