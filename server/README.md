# Play test server

A NeoForge 1.21.1 server in Docker with the Servus mods and the same helper mods as the dev runs
(Modonomicon, JEI, Jade, FTB Ultimine with FTB Library and Architectury).

## Host

```powershell
.\server\pack.ps1                      # build, fill server\mods, write server\dist\*.mrpack
copy server\.env.example server\.env   # once: set OPS and WHITELIST
cd server; docker compose up -d
docker compose logs -f                 # wait for "Done"
```

After a code change: run `pack.ps1` again, then `docker compose restart`. The world lives in
`server\data` and survives restarts. Console: `docker attach servus-playtest`, leave with Ctrl+P Ctrl+Q.

## Players

`pack.ps1` writes three files to `server\dist`, all with the same jars:

| File | For |
|---|---|
| `servus-playtest-<version>.mrpack` | Prism Launcher, Modrinth App |
| `servus-playtest-<version>-curseforge.zip` | FTB App, CurseForge app |
| `servus-playtest-<version>-mods.zip` | any launcher, by hand |

**Prism Launcher:** Add Instance, Import, pick the `.mrpack`.

**FTB App:** Create Instance, Curse Import, pick the `-curseforge.zip`. If the import fails
(the FTB App has known bugs with NeoForge packs), make the instance by hand instead: Create
Instance, Vanilla, Minecraft 1.21.1 with NeoForge 21.1.255. Then open the instance folder and
unpack the jars from `-mods.zip` into its `mods` folder.

Give the instance 4 GB of memory, start it, Multiplayer, add the server.

For a new pack version, delete the old instance (or empty its `mods` folder and unpack the new
`-mods.zip`). Worlds are only on the server, so nothing is lost.

## Address

- Same network: the host's LAN IP, for example `192.168.1.20`.
- Elsewhere: forward TCP 25565 on the router, or put both machines on Tailscale and use the
  host's Tailscale IP.
