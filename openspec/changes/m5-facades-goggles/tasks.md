# Tasks

## 1. Goggles, facades, model and overlay

- [ ] 1.1 Add `GogglesItem` with registry, tab, model, recipe, lang and texture in Lib. Add `ServusLogisticsComponents`, `ServusLogisticsRecipes`, `FacadeItem`, `FacadeRecipe`, facade state in `UtilityDuctBlockEntity`, block changes, `UtilityDuctModel`, model wrapping, `DuctTracker`, `DuctOverlayRenderer`, `ServusRenderTypes`, datagen (special recipe, item model, lang), facade texture and the Logistics GameTest with its template. Verify `gradlew build`, `gradlew :lib:runData` and `gradlew :logistics:runData` pass.

## 2. Verification

- [ ] 2.1 `gradlew :logistics:runGameTestServer` passes; RCON on the Logistics server: a facade merged into a duct is reported back by `data get`; `gradlew :manufacturing:runGameTestServer` and `gradlew :storage:runGameTestServer` pass, `gradlew clean build` green, `gradlew runClientAll` reaches the title screen. Commit.
