# Tasks

Apply after `m10-rail-corrosion-per-rail` is archived.

## 1. Warehouse numbers

- [ ] 1.1 `shuttles/.../site/SiteRegistry` and `SiteRecord`: `numbers` map by warehouse id and `lastNumber`, saved and loaded; assign in `update`/`remember`; number old records on load sorted by dimension and position; `byNumber(int)`. Pure part (the numbering order and never reusing) in a small class without Minecraft types with JUnit `WarehouseNumbersTest`: first is 1, ids keep their number, removed ids are not reused, old records numbered by position order. Verify `gradlew :shuttles:test`
- [ ] 1.2 `shuttles/.../controller/WarehouseControllerMenu` and `client/WarehouseControllerScreen`: number as menu data, "W<n> " before the title name and each site row; the name editor edits only the name. GameTests in `ControllerSiteGameTests`: two controllers are W1 and W2, carrot move keeps W2, pickaxe break gives W3. Verify `gradlew build`

## 2. Shared root and targets

- [ ] 2.1 Check brigadier's `CommandNode.addChild` merge in the 26.1 sources and note it in the design if it differs. `foundation/.../command/ServusFoundationCommands`: `/servus foundation data move <from> <to> <item> <count> [dimension]` under the shared root, `requires` on the `foundation` node. `alchemy/.../journal/JournalCommand`: `/servus alchemy journal [player]` (other players need op) and `journal finish <player> <chapter>`. Update the Alchemy journal GameTests that run the commands. Verify `gradlew build`
- [ ] 2.2 New `shuttles/.../command/CommandTargets`: warehouse by number or `here`, loaded check pointing at `wake`, machine by kind and number or `here` over the warehouse's known members (network nodes, data line members, Field Heads of its fields), `here here` without a warehouse, suggestion providers for numbers, kinds and machine numbers, the error messages of the spec. Verify `gradlew build`

## 3. Shuttles commands

- [ ] 3.1 Split `ServusShuttlesCommands` into `WarehouseCommands` (list, machines, info, zones, fluids, producers, wake, machine), `FieldCommands` (field, field recheck, field age), `StockCommands` (take to the player or the controller, feed, ship, move), `OrderCommands` (orders, order, rules, rule, cap, uncap, unrule), all verb first under `/servus shuttles` with `CommandTargets`; remove `/servus_warehouse` and `sites`. The `machine` report reuses the Data Potato report lines. Verify `gradlew build`
- [ ] 3.2 GameTests: port every GameTest that runs a `/servus_warehouse` command to the new commands; add tests for `list`, `machines` with a kind, `machine` on a Calciner or Crafting Station, `here` on a looked at rack, `here here` on an unnumbered Field Head, an unknown warehouse number, a kind mismatch, a warehouse in another dimension reached by number from a console source, an unloaded warehouse refusing `order` and naming `wake`, `take` landing in the player's inventory, `move` by numbers. Verify `gradlew build` and the full GameTest run

## 4. Edict catalogs

- [ ] 4.1 `foundation/src/main/resources/edict/servus_foundation.edict.yaml`, `shuttles/src/main/resources/edict/servus_shuttles.edict.yaml`, `alchemy/src/main/resources/edict/servus_alchemy.edict.yaml`: every command with description, category, risk level, `remoteExecutable`, params (type, required, description, min, default), examples and response, as in the design. Check each against the Edict schema with a quick local validation. Verify `gradlew build`
- [ ] 4.2 Catalog check: a helper in Foundation's gametest package that walks `/servus <mod>` and compares paths and params with the catalog (SnakeYAML from the dev runtime, or a GameTest only dependency, never jar-in-jar), and one `CommandCatalogGameTests` per mod. Make it fail once on purpose locally (drop a command from a catalog) to see the message, then restore. Verify `gradlew build` and the full GameTest run

## 5. Docs

- [ ] 5.1 `docs/`: add `yaml` and `ajv`; vendor the Edict schema to `docs/src/edict/catalog.schema.json`; `docs/src/commands.mjs` reads each released mod's catalog with `git show <mod>-v<version>:<path>`, skips a missing one, validates and builds signatures by the Edict rules; `docs/src/components/CommandCatalog.astro`; `docs/src/content/docs/reference/commands.mdx`; sidebar entry in `docs/astro.config.mjs`. Check the page with a catalog from a local temporary tag, then delete the tag. Verify `npm run build` in `docs/`
- [ ] 5.2 `.github/workflows/docs.yml`: `fetch-depth: 0` on checkout and a `workflow_run` trigger on the Release workflow's completion. Verify the YAML with `gh workflow view` or a dry read and `npm run build`

## 6. Wrap up

- [ ] 6.1 Full `gradlew build`, tests and GameTests green; user play check: open a controller (number in title and site list), run `list`, `machines`, `field age here here 9000` on an unnumbered Field Head, `field age <n> <n>`, `order`, `take`, a Nether warehouse by number, `alchemy journal`; RCON check of `list` and `info <n>` on the dev server.
- [ ] 6.2 At archive: CHANGELOG lines (Shuttles Changed: commands under `/servus shuttles` by warehouse and machine number, warehouse numbers in the controller GUI; Foundation Changed: `/servus foundation data move` with a dimension; Alchemy Changed: `/servus alchemy journal` with a player), main specs merged, PLAN.md checked for an open point on commands.
