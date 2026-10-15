## 1. Data

- [ ] 1.1 `color` in `VeinData` with default, colours in the shipped vein JSON; JUnit for the colour parsing; run `:alchemy:test`
- [ ] 1.2 `FACING` on `GnomeHollowBlock` set on placement, blockstate datagen maps all facings to stone; GameTest that a placed Hollow faces the placer; run `runData`, `:alchemy:build` and `:alchemy:runGameTestServer`

## 2. Look

- [ ] 2.1 Texture generator script for the rock layer, crack mask (with position along the crack), cave entry and crystals; generated PNGs under `alchemy/src/main/resources/assets/servus_alchemy/textures/sight/`; run `:alchemy:build`
- [ ] 2.2 `HollowRevealRenderer`: rock, material and cave layers with the fill by ripeness, flow, nodules and crystals; item icons and the mushroom removed; list the overlapping layer planes for z-fighting; run `:alchemy:build`
- [ ] 2.3 Launch the client from main and look at an empty, a half ripe and a ripe Hollow under the sight, from several angles

## 3. Play check

- [ ] 3.1 User play check: stone without the sight; under the sight dry cracks when empty, material filling the cracks with ripeness, cave entry towards the placer, faster flow when gifted
