## 1. No recipe

- [x] 1.1 `RECIPE_BUTTON_NONE` in `alchemy/.../CoagulatorMenu.java` clears the selection; GameTest in `CoagulatorGameTests` that the button stops a selected Coagulator; run `:alchemy:runGameTestServer`
- [x] 1.2 "No recipe" entry, tooltip and button label, popup and button tooltips replace earlier ones in `alchemy/.../client/CoagulatorScreen.java`, lang keys in `ServusAlchemyLanguageProvider`; run `runData` and `:alchemy:build`

## 2. Play check

- [ ] 2.1 User play check: "No recipe" stops a running Coagulator and is marked while nothing is selected; hovering a popup entry shows its tooltip
