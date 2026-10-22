# CLAUDE.md

Project: Servus mod family for Minecraft 26.1.2 on NeoForge (target pack ATM11). The vision, architecture and open points are in `PLAN.md`. Read it before starting work.

## Stack
- Java 25, Minecraft 26.1.2, NeoForge 26.1.2.x, ModDevGradle, Mojang's official names (26.1 is unobfuscated, no Parchment)
- Multi project Gradle repo: `foundation`, `alchemy`, `shuttles`, `delicacies`, `brewery`
- Mod IDs: `servus_foundation`, `servus_alchemy`, `servus_shuttles`, `servus_delicacies`, `servus_brewery`

## Rules
- Every mod depends on `servus_foundation` (API plus the Utility Duct, Fluid Tank and tools). Alchemy, Shuttles and Delicacies depend on nothing else and never on each other. Brewery depends on Foundation and Delicacies only.
- World, block entity and capability access only on the server thread.
- Networks use the mailbox pattern: events queued, drained once per server tick. No Akka, no Pekko.
- Never scan blocks every tick. React to placement and removal events.
- Balance numbers, recipes, techs and deposits live in datapack JSON, not in code.
- Pure logic (graphs, planning, ratios) has no Minecraft imports and is covered by JUnit tests.
- Use datagen for blockstates, models, loot tables, recipes, tags and lang.
- Expose the standard NeoForge transfer capabilities (`Capabilities.Item`, `Capabilities.Fluid`, `Capabilities.Energy`) so other mods integrate.

## Spec-driven workflow with OpenSpec

This project uses OpenSpec (`openspec/`, schema `spec-driven`). All feature work goes through an OpenSpec change. No production code is written outside an active change.

Sources of truth:
- `PLAN.md` is mainly the list of open points: what we still want to build, plus the vision, architecture principles and open decisions. It does not describe what is built; that lives in `openspec/specs/`.
- `openspec/specs/` holds the main specs: what the system does today. Empty at the start, grows with every archived change.
- `openspec/changes/<name>/` holds one change in flight: `proposal.md`, `specs/` (delta), `design.md`, `tasks.md`.

Lifecycle of a change:
1. **Explore** (`/opsx:explore`) when a milestone item or an open point or open decision from `PLAN.md` is not clear enough to propose. Thinking only, no files in the change directory.
2. **Propose** (`/opsx:propose`) to create the change and generate proposal, delta specs, design and tasks. Stop after the artifacts and wait for review. Never start implementing in the same response.
3. **Apply** (`/opsx:apply`) to work through `tasks.md`. Check off tasks as they are done. After each task: build, run the unit tests, and launch the game when block or GUI behavior changed.
4. **Archive** (`/opsx:archive`) once every task is checked and the build is green. Archiving merges the delta specs into `openspec/specs/`. Use `/opsx:sync` only when main specs need to be updated before the change is complete. Then groom `PLAN.md` in the same commit: remove what the change built (open points, "later" notes, history), keep only what is still open, and add any new open point the change decided.
   Also in that commit, add a short line for players under the `## Unreleased` section of the `CHANGELOG.md` of every mod the change touched (Added, Changed, Fixed). A release renames that section to its version.
5. **Update** (`/opsx:update`) when a decision made during apply changes the plan. Keep proposal, specs, design and tasks consistent with each other.

Scoping changes:
- One change per open point from `PLAN.md`, not one change per milestone. A change should be implementable in one or two sessions.
- Change names are kebab-case with the milestone prefix: `m0-gradle-multi-project`, `m1-network-graph`, `m2-burner-extractor`.
- Capability paths under `specs/` are `<mod>/<capability>`: `foundation/network-graph`, `foundation/utility-duct`, `alchemy/calciner`, `shuttles/shuttle-garage`. Cross mod integration specs go under `foundation/`.
- Milestones are a rough order, not a gate. Do not propose a change that modifies the same specs or the same code as an unarchived change, unless the user says so.
- When a change discovers that `PLAN.md` is wrong or incomplete, say so and update `PLAN.md` in the same change.

Writing artifacts:
- Specs describe observable behavior: what a block, network or API does, in requirement and scenario form. Numbers that belong in datapack JSON are examples in specs, not requirements.
- Design records the decisions that `PLAN.md` leaves open for that change and the NeoForge APIs used. Verify a NeoForge 26.1 API in the docs or source before writing it into a design.
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
- The docs workflow exports `docs/src/data/gamedata.json` with `gradlew :alchemy:runData` before it builds the site. The committed file is only for local docs work; run `runData` locally to refresh it.

## Code style
- Code explains itself through names and small methods. Extract a shared helper instead of copying logic.
- Javadoc on classes and public or protected members is welcome: short and precise, saying what it does and what a caller must know.
- Comments inside code only explain what the code cannot say itself (a non-obvious formula, an API quirk). No reasoning, history, alternatives, or references to specs, design, tasks, PLAN.md, play checks or other changes, and nothing that repeats the code.
- Delete dead code instead of commenting it out.

## Models and textures
- No two faces may share a plane where they overlap, or they flicker (z-fighting). This covers datagen model elements, standalone models drawn by renderers, and parts drawn several times (rings of segments, turned copies, overlapping belt or rail pieces).
- Before a model or renderer task is done, list for every pair of overlapping boxes whether any faces share a plane. Fix by ending a part exactly at its neighbour's edge, insetting the inner part (about 0.05 px), or giving repeated parts a tiny depth step per copy (about 0.005 px).
- A part that should read as solid needs real geometry. A flat textured face has no thickness and looks like paper from the side.
- Moving and metal parts use the family's steel textures, on every face including top and bottom, not vanilla `iron_block` (reads white) or a stone texture left on one face.
- In the play check, look for flicker on every new part from several angles and while it moves.

## Git
- Logical commits, not one per task: a commit holds a coherent piece of work. Messages are a subject line only.
- A finished worktree branch is rebased onto main and main is fast-forwarded to it. No merge commits.
- A worktree that is merged or no longer needed is deleted, folder and branch.

## Writing style
- No em dashes in docs, comments, specs or messages.
