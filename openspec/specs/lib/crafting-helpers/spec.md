# lib/crafting-helpers Specification

## Purpose
Shared pure rules for crafting machines: how required item counts are taken out of input slots and how several candidates take turns.

## Requirements

### Requirement: Gathering takes exact counts across slots
Given slots with item keys and counts and a required count per item key, gathering SHALL return how much to take from each slot so that every requirement is met exactly, taking from slots in order, or report failure when any requirement cannot be met.

#### Scenario: Spread over slots
- **WHEN** iron is required three times and two slots hold one and five iron
- **THEN** one is taken from the first slot and two from the second

#### Scenario: Short
- **WHEN** three iron are required and only two exist
- **THEN** gathering fails and nothing is taken

### Requirement: Round robin picks the next eligible candidate
Given a cursor and an eligibility test, round robin SHALL return the first eligible index at or after the cursor, wrapping around, or -1 when none is eligible.

#### Scenario: Alternation
- **WHEN** two candidates are eligible and the cursor advances past the pick each time
- **THEN** picks alternate between them

#### Scenario: Skip
- **WHEN** only the last candidate is eligible and the cursor is at the first
- **THEN** the last candidate is picked
