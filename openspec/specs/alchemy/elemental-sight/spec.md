# alchemy/elemental-sight Specification

## Purpose
Elemental Sight lets a player see what Paracelsus' elementals do inside their element: hidden things that only the sight reveals. The Elixir of Sight grants it.

## Requirements

### Requirement: Elemental Sight effect
Alchemy SHALL register the beneficial effect Elemental Sight (`servus_alchemy:elemental_sight`). It SHALL change nothing in the world by itself; it only allows the client to draw what reveal renderers show.

#### Scenario: Effect listed
- **WHEN** a player has Elemental Sight
- **THEN** the effect shows in the inventory with its remaining time

### Requirement: Elixir of Sight
The Elixir of Sight SHALL be drunk like a potion, over a short time, SHALL give the drinker Elemental Sight for its duration (example 3,600 ticks), and SHALL leave a glass bottle. It SHALL stack to 16.

#### Scenario: Drink
- **WHEN** a player finishes drinking an Elixir of Sight
- **THEN** they have Elemental Sight for three minutes and hold a glass bottle

### Requirement: The elixir is an Athanor craft
The Elixir of Sight SHALL be made on an Athanor of at least Albedo from Quicksilver and the three principles (example 100 mB Salt, 100 mB Sulfur, 250 mB Mercury, 200 ticks).

#### Scenario: Below Albedo
- **WHEN** Quicksilver lies on a Nigredo Athanor with the principles in its tanks
- **THEN** no elixir is made

### Requirement: Reveal renderers draw only under the sight
The client SHALL keep a list of reveal renderers. Each frame, after translucent blocks, it SHALL call them only while the local player has Elemental Sight, and never otherwise.

#### Scenario: Sight ends
- **WHEN** the effect runs out
- **THEN** nothing a reveal renderer draws is visible any more
