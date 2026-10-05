# CLAUDE.md

Project: Servus mod family for Minecraft 1.21.1 on NeoForge. The vision, architecture and milestones are in `PLAN.md`. Read it before starting work.

## Stack
- Java 21, NeoForge 21.1.x, ModDevGradle, Parchment mappings
- Multi project Gradle repo: `lib`, `manufacturing`, `logistics`, `storage`
- Mod IDs: `servus_lib`, `servus_manufacturing`, `servus_logistics`, `servus_storage`

## Rules
- Content mods depend only on `servus_lib`, never on each other.
- World, block entity and capability access only on the server thread.
- Networks use the mailbox pattern: events queued, drained once per server tick. No Akka, no Pekko.
- Never scan blocks every tick. React to placement and removal events.
- Balance numbers, recipes, techs and deposits live in datapack JSON, not in code.
- Pure logic (graphs, planning, ratios) has no Minecraft imports and is covered by JUnit tests.
- Use datagen for blockstates, models, loot tables, recipes, tags and lang.
- Expose standard capabilities (`IItemHandler`, `IEnergyStorage`) so other mods integrate.

## Spec-driven workflow with OpenSpec

This project uses OpenSpec (`openspec/`, schema `spec-driven`). All feature work goes through an OpenSpec change. No production code is written outside an active change.

Sources of truth:
- `PLAN.md` is the roadmap: vision, architecture principles, milestones, open decisions. It says what we want to build and in which order.
- `openspec/specs/` holds the main specs: what the system does today. Empty at the start, grows with every archived change.
- `openspec/changes/<name>/` holds one change in flight: `proposal.md`, `specs/` (delta), `design.md`, `tasks.md`.

Lifecycle of a change:
1. **Explore** (`/opsx:explore`) when a milestone item or an open decision from `PLAN.md` section 5 is not clear enough to propose. Thinking only, no files in the change directory.
2. **Propose** (`/opsx:propose`) to create the change and generate proposal, delta specs, design and tasks. Stop after the artifacts and wait for review. Never start implementing in the same response.
3. **Apply** (`/opsx:apply`) to work through `tasks.md`. Check off tasks as they are done. After each task: build, run the unit tests, and launch the game when block or GUI behavior changed.
4. **Archive** (`/opsx:archive`) once every task is checked and the build is green. Archiving merges the delta specs into `openspec/specs/`. Use `/opsx:sync` only when main specs need to be updated before the change is complete.
5. **Update** (`/opsx:update`) when a decision made during apply changes the plan. Keep proposal, specs, design and tasks consistent with each other.

Scoping changes:
- One change per milestone item from `PLAN.md`, not one change per milestone. A change should be implementable in one or two sessions.
- Change names are kebab-case with the milestone prefix: `m0-gradle-multi-project`, `m1-network-graph`, `m2-burner-extractor`.
- Capability paths under `specs/` are `<mod>/<capability>`: `lib/network-graph`, `lib/tick-mailbox`, `manufacturing/burner-extractor`, `logistics/utility-duct`, `storage/crates`. Cross mod integration specs go under `lib/`.
- Work milestones in order. Do not propose a change for milestone N+1 while milestone N has unarchived changes, unless the user says so.
- When a change discovers that `PLAN.md` is wrong or incomplete, say so and update `PLAN.md` in the same change.

Writing artifacts:
- Specs describe observable behavior: what a block, network or API does, in requirement and scenario form. Numbers that belong in datapack JSON are examples in specs, not requirements.
- Design records the decisions that `PLAN.md` leaves open for that change and the NeoForge APIs used. Verify a NeoForge 1.21.1 API in the docs or source before writing it into a design.
- Tasks are small and verifiable. Each task names the files it touches and ends with a build or test step. Pure logic tasks list the JUnit tests to add.

Useful commands:
```
openspec list                      # changes in flight
openspec status --change <name>    # artifact completion of one change
openspec validate <name>           # check a change or spec
openspec show <name>               # print a change or spec
openspec view                      # dashboard of specs and changes
```

## Verification
- After each task: `gradlew build` and `gradlew test` must pass.
- Block, GUI and worldgen behavior: launch the client run config and check it in game. GameTests where practical.
- Do not report a task as done without a green build.

## Git
- Small commits with clear messages, subject line only.
- Commit at the end of each task, or more often when it helps.

## Writing style
- No em dashes in docs, comments, specs or messages.
