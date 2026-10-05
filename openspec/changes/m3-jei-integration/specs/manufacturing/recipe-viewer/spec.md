# Spec Delta

## Purpose

Recipe viewers show how Manufacturing items are made so players never have to guess at assembler inputs, times or machine recipes.

## ADDED Requirements

### Requirement: Assembling recipes appear in JEI
When JEI is installed, every assembling recipe SHALL appear in an "Assembling" category showing each ingredient with its count, the result with its count, and the time in seconds. The Assembler Mk1 SHALL be listed as the category's machine.

#### Scenario: Look up red pack
- **WHEN** the player looks up the Red Pack recipe in JEI
- **THEN** the Assembling category shows one iron gear and two copper wire as inputs, one red pack as output and 5.0 s

#### Scenario: Click the assembler
- **WHEN** the player clicks the Assembler Mk1 in JEI
- **THEN** the Assembling category opens with all assembling recipes

### Requirement: Works without JEI
Servus Manufacturing SHALL load and work with JEI absent.

#### Scenario: No JEI
- **WHEN** the game runs without JEI
- **THEN** no error is logged and all machines work as before
