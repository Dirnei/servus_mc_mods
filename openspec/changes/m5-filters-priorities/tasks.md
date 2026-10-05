# Tasks

## 1. Filters, priorities and menu

- [x] 1.1 Extend `UtilityDuctBlockEntity` (filters, priorities, menu provider, `ConfigChanged`), rewrite `ItemChannelNetwork` routing with priorities and filters, add `UtilityDuctMenu`, `GhostSlot`, `ServusLogisticsMenus`, the new `UtilityDuctScreen`, client registration, GUI texture and lang, delete the payloads and `ClientHooks`. Verify `gradlew build` and `gradlew :logistics:runData` pass.

## 2. Verification

- [x] 2.1 Server check over RCON with all mods: filtered pull leaves dirt, priority 5 chest gets all 20 items, a full priority 5 chest overflows to priority 0. `gradlew :manufacturing:runGameTestServer` and `gradlew :storage:runGameTestServer` pass, `gradlew clean build` green, `gradlew runClientAll` reaches the title screen. Commit.
