# Tasks

## 1. Rename

- [x] 1.1 Rename Counter Terminal to Storage Terminal with a script and `git mv` (classes, ids, lang keys, textures, docs icon, specs text, PLAN.md, README, docs pages), move `openspec/specs/storage/counter-terminal` to `storage/storage-terminal`, `TerminalLayout.Counter` to `Storage`; regenerate data (`:storage:runData`, `:manufacturing:runData`). Verify with `gradlew build test`, `:storage:runGameTestServer` and a grep that finds no `counter_terminal` or `CounterTerminal` outside archives.

## 2. Input and output rows

- [x] 2.1 Block entity input handler (saved, dropped), store on change via the server tick, routing capability handler (request items to output, others to input); layout rows Input and Output; menu slots for both, shift click from the inventory into the input. GameTests `inputStores`, `inputKeepsLeftover`, `automationRouting`, `outputRowDelivers`. Verify with build, JUnit and Storage GameTests.

## 3. Crafting popup

- [x] 3.1 Layout: Craft button and popup rectangle; TerminalLayoutTest covers every height and scale with the popup inside the grid area. Menu crafting slots inactive while the popup is closed; screen draws the popup, closing it clears the grid; JEI transfer opens it. GameTest `popupCrafts`. Verify with build, JUnit and Storage GameTests.

## 4. Docs

- [ ] 4.1 Storage docs page describes the Storage Terminal with input, output and the crafting popup. Verify with docs tests.
