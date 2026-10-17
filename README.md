<img src="branding/servus.png" alt="Servus" width="96" align="right">

# Servus mod family

NeoForge mods for Minecraft 26.1.2.

| | Mod | Mod ID | |
|---|---|---|---|
| <img src="branding/foundation.png" width="40"> | **Servus: Foundation** | `servus_foundation` | The base: Utility Duct (items, power, fluids, data), Fluid Tank, pumps, Coal Generator, Sink, Ender Anchor, Machine Adapter and tools. |
| <img src="branding/alchemy.png" width="40"> | **Servus: Alchemy** | `servus_alchemy` | Paracelsian alchemy: Salt, Sulfur and Mercury from the Calciner, Coagulator and Athanor, gnomes that grow ore veins, a salamander from the Nether. |
| <img src="branding/shuttles.png" width="40"> | **Servus: Shuttles** | `servus_shuttles` | An automated warehouse run by shuttles on rails: storage, autocrafting, field farming and outposts. |

Alchemy and Shuttles each need Foundation, but not each other.

Player documentation: https://dirnei.github.io/servus_mc_mods/

## Build

Needs a JDK 21 to run Gradle; the Java 25 toolchain for the mods is downloaded automatically.

```
gradlew build                 # all mods, unit tests, jars in <mod>/build/libs
gradlew runClientAll          # client with all three mods
gradlew runServerAll          # dedicated server with all three mods
gradlew runGameTestServerAll  # GameTests
gradlew :alchemy:runData      # datagen, also writes docs/src/data/gamedata.json
```

Each project also has its own runs, such as `:shuttles:runClient`. Game directories are under `runs/` and not tracked.

## Docs site

`docs/` is an Astro Starlight site with the player docs and the Balance lab. CI fails when `docs/src/data/gamedata.json` is out of date, so run the data run after changing balance data.

```
cd docs
npm ci
npm run dev
npm test
```

## Releasing

Each mod is released on its own by pushing a tag `<mod>-v<version>`. The tag is the version; `mod_version` in `<mod>/gradle.properties` only names dev builds.

```
git tag -a shuttles-v0.2.0 -m "What changed"
git push origin shuttles-v0.2.0
```

The Release workflow builds the mod with the tag's version, creates the GitHub release and uploads the jar to CurseForge. The tag message is the changelog; a version containing `alpha` or `beta` is published as that type. It needs the secret `CURSEFORGE_TOKEN` and the variables `CURSEFORGE_ID_FOUNDATION`, `CURSEFORGE_ID_ALCHEMY` and `CURSEFORGE_ID_SHUTTLES`; a mod without an id goes to GitHub only.

## Development

Work is planned with OpenSpec: `openspec/specs/` describes what is built, `PLAN.md` what is still open. See `CLAUDE.md` for the workflow and project rules.

## License

MIT, see `LICENSE`.
