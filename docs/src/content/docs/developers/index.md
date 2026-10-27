---
title: Developer guide
description: How machines of other mods join the Servus data line, and how to depend on Servus Foundation.
sidebar:
  label: Overview
---

This part of the site is for mod developers who want their machines to work with the Servus mods. It describes the API of **Servus: Foundation 0.4.0** as it is in that release.

## What you get

The Foundation **data line** links blocks that make or hold items. A Servus: Shuttles warehouse reads it to:

- order items from your machine when a player requests something that is not in stock (autocrafting),
- keep your machine's fuel or other consumable stocked (upkeep),
- see your machine's state in its Production Terminal.

Nothing moves through the data line unless one of these asks for it. Your machine keeps working as before for pipes, hoppers and players.

## Two ways in

1. **Your mod joins.** Your block exposes a few Foundation block capabilities. This works for any machine and lets you describe exactly what it makes. See [Join the data line](/servus_mc_mods/developers/join-the-data-line/).
2. **The Machine Adapter.** A player puts a Foundation Machine Adapter against a machine, and a datapack profile describes the machine. No code at all, but it only covers simple one-input machines. See [Machine Adapter](/servus_mc_mods/developers/machine-adapter/).

## Pages

- [Data line concepts](/servus_mc_mods/developers/data-line/): nodes, members, roles, upkeep, events and bandwidth.
- [Join the data line](/servus_mc_mods/developers/join-the-data-line/): what your block must implement, with a worked example.
- [Deliveries](/servus_mc_mods/developers/deliveries/): how items move over the line, and how to start a delivery yourself.
- [Machine Adapter](/servus_mc_mods/developers/machine-adapter/): machine profiles for blocks whose mod does nothing.

## Depending on Foundation

:::caution[No Maven repository]
Foundation 0.4.0 is **not published to any Maven repository**. The release workflow builds the jar and attaches it to the GitHub release of the tag `foundation-v0.4.0` (and uploads it to CurseForge where the project is set up). There is no sources jar and no Javadoc jar either.
:::

Until there is a Maven publication, compile against the release jar:

1. Download `servus_foundation-0.4.0.jar` from the GitHub release `foundation-v0.4.0` and put it into your project, for example under `libs/`.
2. Add it to the compile classpath, and to the runtime classpath of your dev runs if you want Foundation in game:

```groovy
dependencies {
    compileOnly files('libs/servus_foundation-0.4.0.jar')
    // For example the MDK's localRuntime configuration, so your dev runs load it.
    localRuntime files('libs/servus_foundation-0.4.0.jar')
}
```

3. Declare the dependency in your `neoforge.mods.toml`. Use `optional` if your mod also runs without Foundation:

```toml
[[dependencies.yourmodid]]
    modId = "servus_foundation"
    type = "optional"
    versionRange = "[0.4.0,)"
    ordering = "NONE"
    side = "BOTH"
```

The Javadoc lives in the source. Read it on GitHub at the tag `foundation-v0.4.0`, under `foundation/src/main/java/io/github/dirnei/mc/servus/foundation/`.

### Optional dependency

The capabilities are fields of Foundation classes, so any class that names them needs Foundation at runtime. If Foundation is optional for your mod, keep every Foundation reference in one class, and only register it when `ModList.get().isLoaded("servus_foundation")` is true.

## Rules that hold for the whole API

- **Server thread only.** The data line, deliveries and every capability call run on the server thread. Nothing checks this for you.
- **No per tick scanning.** The line reads members through NeoForge capability caches. When your block starts or stops offering a capability (for example a multiblock that forms), call `level.invalidateCapabilities(pos)`, as you would for item handlers.
- **Answer for every side, and for `null`.** The line finds members through the face that touches a duct, but the Crafting CPU and the upkeep check ask your block with a `null` side. Return the same provider for all of them.
- **Pre 1.0.** Foundation is at 0.4.0. The API lives in ordinary packages next to internal classes and has no stability promise yet. Expect changes between minor versions.
