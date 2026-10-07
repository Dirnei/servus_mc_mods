# Tasks

## 1. Tabs and pages

- [ ] 1.1 Rework `logistics/src/main/java/io/github/dirnei/mc/servus/logistics/client/UtilityDuctScreen.java` (tabs, page rebuild, layout per design.md) and `menu/UtilityDuctMenu.java` (page state, slot positions, inactive filter slots on other pages); regenerate the GUI texture `utility_duct.png` at 176 by 196 and add the tab lang keys. Run `gradlew :logistics:runData` and verify `gradlew build` and the Logistics GameTests are green.

## 2. Verification

- [ ] 2.1 Run `openspec validate m5-duct-gui-tabs --strict`, `gradlew runClientAll` launches. Commit.
- [ ] 2.2 Play check: a two line duct shows two tabs, each page fills the panel, filter slots only on the Item Pipe page.
