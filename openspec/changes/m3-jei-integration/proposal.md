# Proposal

## Why

Players need to see what the assembler can make and what it costs without reading JSON. PLAN.md milestone M3, item "JEI or EMI integration for machine recipes", pulled forward because the playtest wants it now. The same change adds the usual dev run conveniences the playtester asked for: Jade for block info and FTB Ultimine for vein mining.

## What Changes

- JEI as a soft dependency: API at compile time, full mod only in dev runs.
- JEI plugin in Manufacturing with an "Assembling" category: counted inputs, animated arrow, result, time. The Assembler Mk1 is the catalyst, so clicking it in JEI opens the category.
- Jade and FTB Ultimine (with FTB Library and Architectury) in the dev run only. No code depends on them.
- Recipe lookup for vanilla crafting recipes of machines comes for free from JEI.

## Capabilities

### New Capabilities
- `manufacturing/recipe-viewer`: what recipe viewers show for Manufacturing.

### Modified Capabilities
None.

## Non-goals

- EMI support (JEI covers the playtest; EMI can be added later through the same category code or its JEI compatibility).
- Showing deposit yields, research techs or machine stats in JEI (later).
- Shipping Jade or Ultimine with the mods; they are dev run conveniences only.

## Impact

- Root build gains the BlameJared, Modrinth, FTB and Architectury mavens for the manufacturing project; `gradle.properties` gains the versions.
- Builds without JEI still compile: the plugin is only loaded by JEI's annotation scan.
