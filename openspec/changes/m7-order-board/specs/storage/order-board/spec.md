# Spec Delta

## Purpose

The Order Board is the warehouse's production overview: open orders, the machine each step runs on, and what is blocked and why.

## ADDED Requirements

### Requirement: Order Board joins the warehouse
The Order Board SHALL be a horizontal facing block that joins a warehouse network beside its stock blocks or controller like the Counter Terminal, SHALL hold no stock, SHALL show the online lamp, and SHALL open the board screen on right click. The wrench SHALL rotate it and the sneak wrench SHALL dismantle it.

#### Scenario: Placed beside a crate
- **WHEN** a board is placed against a crate of an online warehouse
- **THEN** its lamp lights and right click opens the board screen for that warehouse

#### Scenario: Not connected
- **WHEN** the board touches no warehouse block
- **THEN** the screen says the board is not connected

### Requirement: Machines section
The screen SHALL list every dock of the warehouse with the name of the block it faces and its state: working on an item when the machine's producer reports an active recipe, idle when it has recipes but none runs, no recipes when it has a producer without recipes, no machine when the dock faces no producer.

#### Scenario: Crafting Station at work
- **WHEN** a dock faces a Crafting Station that is crafting sticks
- **THEN** the board lists "Crafting Station: working on Stick"

#### Scenario: Empty dock
- **WHEN** a dock faces a chest
- **THEN** the board lists "Chest: no machine"

### Requirement: Orders section
The screen SHALL list every work order with its item, amount, state (running, blocked, done, cancelled), the blocked reason, and whether it comes from a request or a stock rule. Under each order it SHALL list every step with the machine on its dock, the output and batches, and the step state (waiting for dock, feeding, producing, done) with its reason. The list SHALL scroll with the mouse wheel when it does not fit.

#### Scenario: Blocked order
- **WHEN** an order for sticks waits for planks that are not in stock
- **THEN** the board shows the order as blocked with "missing Oak Planks" and its step as feeding

#### Scenario: Two steps
- **WHEN** an order makes planks from logs and then sticks
- **THEN** the board shows both steps under the order, the planks step first

### Requirement: Cancel from the board
Clicking an open order SHALL cancel it: steps not yet fed are dropped and their reservations released; a linked request keeps its missing amount.

#### Scenario: Cancel
- **WHEN** the player clicks a running order
- **THEN** it shows as cancelled and leaves the list after a short while

### Requirement: Live view
While the screen is open the board SHALL receive the current machines and orders every half second when something changed, and nothing when nothing changed.

#### Scenario: Step advances
- **WHEN** a step goes from feeding to producing while the board is open
- **THEN** the screen shows producing within a second
